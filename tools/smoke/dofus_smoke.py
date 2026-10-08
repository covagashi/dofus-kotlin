#!/usr/bin/env python3
"""Headless login/game smoke test for the StarLoco Kotlin server.

Exercises: HC/auth -> AYK ticket -> AT/AV/AL/AS -> GC/GDM -> GI/GM/GDK
-> in-game chat (BM*/cMK) -> GA001 movement -> disconnect/reconnect.

Usage:
    DOFUS_LOGIN_HOST=127.0.0.1 DOFUS_GAME_HOST=127.0.0.1 \
        python3 tools/smoke/dofus_smoke.py

Defaults assume the docker-compose stack is published on localhost
(login 450, game 5555). Seeded account test/test, player id 1.
"""
import os
import sys
import time as _t

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import Conn, crypt_pass

LOGIN_HOST = os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1")
LOGIN_PORT = int(os.environ.get("DOFUS_LOGIN_PORT", "450"))
GAME_HOST = os.environ.get("DOFUS_GAME_HOST", "127.0.0.1")


def recv_any(conn, n=5, timeout=6):
    pkts = []
    for _ in range(n):
        p = conn.recv_pkt(timeout)
        if p is None:
            break
        pkts.append(p)
    return pkts


def login_phase(conn):
    hc = None
    for _ in range(8):
        hc = conn.recv_pkt()
        if hc and hc.startswith("HC"):
            break
    if not hc or not hc.startswith("HC"):
        print("FAIL: expected HC<key>, got", hc)
        sys.exit(1)
    key = hc[2:]
    print("login key:", key)

    conn.send("1.39.8e")
    conn.send("test")
    p = conn.recv_pkt(4)
    if p and p.startswith("AlE"):
        print("FAIL: account rejected", p)
        sys.exit(1)
    conn.send(crypt_pass("test", key))
    resp = recv_any(conn, 4)
    if any(r and r.startswith("AlE") for r in resp):
        print("FAIL: password rejected")
        sys.exit(1)

    conn.send("Ax")
    ax = recv_any(conn, 4)
    serverline = next((r for r in ax if r and r.startswith("AxK")), None)
    print("server list:", serverline[:120] if serverline else ax)
    conn.send("AX601")
    ayk = conn.recv_pkt()
    if not ayk or not ayk.startswith("AYK"):
        print("FAIL: expected AYK, got", ayk)
        sys.exit(1)
    addr, ticket = ayk[3:].split(";")
    host, port = addr.split(":")
    return ticket, int(port)


def game_auth(g, ticket):
    hg = g.recv_pkt()
    if hg != "HG":
        print("FAIL: expected HG, got", hg)
        sys.exit(1)
    g.send("AT" + ticket)
    r = g.recv_pkt()
    if not r or not r.startswith("ATK"):
        print("FAIL: expected ATK*, got", r)
        sys.exit(1)


# ---- LOGIN PHASE ----
lg = Conn(LOGIN_HOST, LOGIN_PORT, "login")
ticket, gport = login_phase(lg)
lg.s.close()

# ---- GAME PHASE ----
g = Conn(GAME_HOST, gport, "game")
game_auth(g, ticket)

g.send("AV0")
g.recv_pkt(3)
g.send("AL")
chars = recv_any(g, 5)
alk = next((c for c in chars if c and c.startswith("ALK")), None)
print("char list:", alk[:160] if alk else chars)
if alk is None:
    print("FAIL: no ALK")
    sys.exit(1)

g.send("AS1")
pkts = []
end = _t.time() + 10
while _t.time() < end:
    p = g.recv_pkt(timeout=3)
    if p is None:
        break
    pkts.append(p)
    if p.startswith("GDM"):
        end = _t.time() + 2
print("char-select packets:", len(pkts))
for x in pkts[:14]:
    print("   ", x[:90])
if not any(x.startswith("ASK") for x in pkts):
    print("FAIL: no ASK")
    sys.exit(1)

g.send("GC")
pkts2 = []
end = _t.time() + 8
while _t.time() < end:
    p = g.recv_pkt(timeout=3)
    if p is None:
        break
    pkts2.append(p)
    if p.startswith("GDM"):
        end = _t.time() + 1
print("game-create packets:", len(pkts2))
for x in pkts2[:10]:
    print("   ", x[:90])
if not any(x.startswith("GCK") for x in pkts2):
    print("FAIL: no GCK")
    sys.exit(1)
if not any(x.startswith("GDM") for x in pkts2):
    print("FAIL: no GDM (map data)")
    sys.exit(1)

g.send("GI")
pkts3 = []
end = _t.time() + 8
while _t.time() < end:
    p = g.recv_pkt(timeout=3)
    if p is None:
        break
    pkts3.append(p)
    if p == "GDK":
        end = _t.time() + 1
print("map-info packets:", len(pkts3))
for x in pkts3[:10]:
    print("   ", x[:90])
if not any(x.startswith("GM") or x == "GDK" for x in pkts3):
    print("FAIL: no GM/GDK")
    sys.exit(1)

# in-game chat: BM<channel>|<msg> -> cMK echo
g.send("BM*|hello_from_smoke")
chat = recv_any(g, 4)
print("chat packets:", [x[:70] for x in chat])
if not any(x.startswith("cMK") and "hello_from_smoke" in x for x in chat):
    print("FAIL: no cMK chat echo")
    sys.exit(1)

# movement probe: server may reject the path but must not crash
g.send("GA001aeycez")
mv = recv_any(g, 5)
print("move packets:", [x[:70] for x in mv])

# disconnect + reconnect: exercises session cleanup
g.s.close()
lg2 = Conn(LOGIN_HOST, LOGIN_PORT, "login2")
ticket2, gport = login_phase(lg2)
lg2.s.close()
g2 = Conn(GAME_HOST, gport, "game2")
game_auth(g2, ticket2)
g2.send("AL")
chars2 = recv_any(g2, 5)
if not any(c and c.startswith("ALK") for c in chars2):
    print("FAIL: no ALK on reconnect")
    sys.exit(1)
print("[reconnect] login+ticket+charlist ok")
g2.s.close()

print("\n=== SMOKE TEST PASSED: login+auth+charlist+charselect+mapdata+chat+reconnect OK ===")
