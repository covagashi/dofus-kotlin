#!/usr/bin/env python3
"""Headless PvM fight smoke test for the StarLoco Kotlin server.

Full combat pipeline over raw TCP:
  login -> ticket -> charselect -> GC/GDM -> GI -> locate mob group
  -> BFS pathing on the decoded grid -> GA001 -> aggro
  -> GJK/GDF/GP/GA;950/GM -> Gp placement -> GR1
  -> GIC/GS/GTL/GTM/Gd/GTS -> GA300141 (spell cast) -> Gt pass
  -> mob AI turns (GA0 movement, GA;300 casts) -> GE fight end.

Prereqs: seeded test player must be on map 4 near the fixed mob group
(see tools/smoke/README.md) and server restarted after any DB edit.

Usage:
    DOFUS_LOGIN_HOST=127.0.0.1 DOFUS_GAME_HOST=127.0.0.1 \
        python3 tools/smoke/dofus_fight_smoke.py [map4.json]
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import Conn, MapGrid, cellcode, cellcode_to_int, crypt_pass

LOGIN_HOST = os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1")
LOGIN_PORT = int(os.environ.get("DOFUS_LOGIN_PORT", "450"))
GAME_HOST = os.environ.get("DOFUS_GAME_HOST", "127.0.0.1")
MAP_JSON = (sys.argv[1] if len(sys.argv) > 1
            else os.path.join(os.path.dirname(os.path.abspath(__file__)), "map4.json"))

M = MapGrid.load(MAP_JSON)
dist = M.dist
step = M.step
bfs = M.bfs


def login_and_enter():
    lg = Conn(LOGIN_HOST, LOGIN_PORT, "login")
    hc = None
    for _ in range(8):
        hc = lg.recv_pkt()
        if hc and hc.startswith("HC"):
            break
    if not hc:
        print("FAIL: no HC")
        sys.exit(1)
    lg.send("1.39.8e"); lg.send("test"); lg.recv_pkt(3)
    lg.send(crypt_pass("test", hc[2:])); lg.drain(2)
    lg.send("Ax"); lg.drain(2)
    lg.send("AX601")
    ayk = lg.recv_pkt()
    if not ayk or not ayk.startswith("AYK"):
        print("FAIL: no AYK")
        sys.exit(1)
    ticket = ayk[3:].split(";")[1]
    gport = int(ayk[3:].split(";")[0].split(":")[1])
    lg.s.close()

    g = Conn(GAME_HOST, gport, "game")
    if g.recv_pkt() != "HG":
        print("FAIL: no HG")
        sys.exit(1)
    g.send("AT" + ticket)
    r = g.recv_pkt()
    if not r or not r.startswith("ATK"):
        print("FAIL: ATK")
        sys.exit(1)
    g.send("AV0"); g.recv_pkt(2)
    g.send("AL")
    for _ in range(6):
        p = g.recv_pkt(3)
        if p and p.startswith("ALK"):
            break
    g.send("AS1"); g.drain(4)
    g.send("GC"); g.drain(3)
    g.send("GI")
    return g, g.drain(4)


g, gi = login_and_enter()

groups, my_cell = {}, -1
for p in gi:
    if not p.startswith("GM"):
        continue
    for part in p[3:].split("|"):
        if not part.startswith("+"):
            continue
        f = part[1:].split(";")
        if len(f) > 3 and f[3] == "-1":
            groups[int(f[0])] = part[:90]
        elif len(f) > 3 and f[3] == "1":
            my_cell = int(f[0])
print("me @", my_cell, " groups @", {k: v[:60] for k, v in groups.items()})

# if we reconnected inside an active fight, jump straight to turn loop
has_fighters = any(
    p.startswith("GM") and ";-2;" in part
    for p in gi for part in p[3:].split("|") if part.startswith("+"))
if has_fighters or not groups:
    print("reconnected into active fight -> draining turns")
    end_seen = False
    for _ in range(120):
        pkts = g.drain(2)
        for p in pkts:
            if p.startswith("GTS1"):
                g.send("Gt")
            if p.startswith("GE") or p.startswith("GV"):
                end_seen = True
        if end_seen:
            break
    print("=== RECONNECTED FIGHT, end seen:", end_seen, "===")
    g.s.close()
    sys.exit(0 if end_seen else 1)

if not groups or my_cell < 0:
    print("FAIL: missing actor data")
    sys.exit(1)

g.send("BM*|.walkfast")
g.drain(1)


def parse_gm_move(pkts):
    global my_cell, groups
    for p in pkts:
        if p.startswith("GM"):
            for part in p[3:].split("|"):
                if not part.startswith("+"):
                    continue
                f = part[1:].split(";")
                if len(f) > 3 and f[3] == "-1":
                    groups[int(f[0])] = part[:90]
                elif len(f) > 3 and f[3] == "1":
                    my_cell = int(f[0])


FIGHT_PRE = ("GJK", "GIC", "GDF", "GP", "GS", "GTM", "GTF", "GTS", "GTL", "GTR", "fS")


def any_fight(pkts):
    return [p for p in pkts if p[:3] in FIGHT_PRE or p.startswith("GP") or p.startswith("GS")]


in_fight = False
for attempt in range(60):
    gcell = min(groups, key=lambda c: dist(c, my_cell))
    d = dist(my_cell, gcell)
    if d <= 2:
        n = -1
        for dd in 'adehbfcg':
            n = step(my_cell, dd)
            if n >= 0:
                break
        if n < 0:
            g.send("GA001a" + cellcode(my_cell))
        else:
            g.send("GA001" + 'a' + cellcode(n))
        pkts = g.drain(2)
        parse_gm_move(pkts)
        if any_fight(pkts):
            in_fight = True
            break
    path = bfs(my_cell, lambda c: dist(c, gcell) <= 2)
    if not path:
        print(f"attempt {attempt}: no path to group {gcell} from {my_cell}")
        break
    payload = "".join(dd + cellcode(c) for dd, c in path)
    print(f"attempt {attempt}: me={my_cell} grp={gcell} d={d} path={payload[:48]}")
    g.send("GA001" + payload)
    pkts = g.drain(3)
    parse_gm_move(pkts)
    fp = any_fight(pkts)
    if fp:
        in_fight = True
        print("FIGHT INIT:", [p[:60] for p in fp][:8])
        break
    if dist(my_cell, gcell) <= 2:
        g.send("GA001a" + cellcode(my_cell))
        pkts = g.drain(2)
        if any_fight(pkts):
            in_fight = True
            break

if not in_fight:
    print("=== NO FIGHT ===  me @", my_cell, "groups:", list(groups))
    g.s.close()
    sys.exit(1)

# ---- placement phase ----
more = g.drain(8)
for p in more:
    print("<<", p[:120])
place_cells = []
for p in more:
    if p.startswith("GP"):
        body = p[2:].split("|")[0]
        place_cells = [cellcode_to_int(body[i:i + 2]) for i in range(0, len(body) - 1, 2)]
        break
if place_cells:
    g.send("Gp" + str(place_cells[0]))
    g.drain(1)
g.send("GR1")

# ---- turn loop ----
saw_gts = False
end_seen = False
my_id_seen = False
did_attack = False
mob_cells = {}
my_fcell = -1


def track_fighter_cells(p):
    global my_fcell
    if p.startswith("GIC"):
        for part in p[4:].split("|"):
            f = part.split(";")
            if len(f) >= 2:
                try:
                    fid, cell = int(f[0]), int(f[1])
                    if fid < 0:
                        mob_cells[fid] = cell
                    elif fid == 1:
                        my_fcell = cell
                except ValueError:
                    pass
    if p.startswith("GTM"):
        for part in p[4:].split("|"):
            f = part.split(";")
            if len(f) >= 6:
                try:
                    fid, cell = int(f[0]), int(f[5])
                    if fid < 0:
                        mob_cells[fid] = cell
                    elif fid == 1:
                        my_fcell = cell
                except ValueError:
                    pass


end = time.time() + 240
while time.time() < end and not end_seen:
    pkts = g.drain(3)
    for p in pkts:
        print("<<", p[:140])
        track_fighter_cells(p)
        if p.startswith("GTS"):
            saw_gts = True
            fid = p[3:].split("|")[0]
            if fid == "1":
                my_id_seen = True
                time.sleep(0.3)
                attacked = False
                if mob_cells and my_fcell >= 0:
                    tcell = min(mob_cells.values(), key=lambda c: dist(c, my_fcell))
                    print(f">>> attacking mob cell {tcell} (me@{my_fcell})")
                    g.send(f"GA300141;{tcell}")
                    pk2 = g.drain(2)
                    for q in pk2:
                        print("<<", q[:140])
                        track_fighter_cells(q)
                    if any(q.startswith("GA;300") or q.startswith("GA0;") for q in pk2):
                        attacked = True
                    if not attacked:
                        g.send(f"GA303{tcell}")
                        pk3 = g.drain(2)
                        for q in pk3:
                            print("<<", q[:140])
                            track_fighter_cells(q)
                        if any(q.startswith("GA") for q in pk3):
                            attacked = True
                    did_attack = did_attack or attacked
                g.send("Gt")
        if p.startswith("GE") or p.startswith("GV"):
            end_seen = True
print("\n=== saw GTS:", saw_gts, "| my turn:", my_id_seen,
      "| attacked:", did_attack, "| fight end:", end_seen, "===")
g.s.close()
sys.exit(0 if (saw_gts and my_id_seen) else 1)
