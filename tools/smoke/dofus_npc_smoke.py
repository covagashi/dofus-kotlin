#!/usr/bin/env python3
"""Headless NPC-dialog + bank smoke test.

Player must be on map 1674 (Astrub bank) near a bank clerk (template 100,
three instances at cells 166/226/286). Flow:

  login -> charselect -> GC/GDM -> GI -> parse GM for the clerk's actor id
  -> DC<actorId> -> expect DCK + DQ<questionId>|<answers>
  -> DR<qid>|259 ("open my bank") -> expect ECK5 + EL (bank contents)
  -> EV (leave exchange) -> clean exit.

Usage:
    DOFUS_LOGIN_HOST=starloco_login DOFUS_GAME_HOST=starloco_game \
        python3 tools/smoke/dofus_npc_smoke.py
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import Conn, crypt_pass

LOGIN_HOST = os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1")
LOGIN_PORT = int(os.environ.get("DOFUS_LOGIN_PORT", "450"))
GAME_HOST = os.environ.get("DOFUS_GAME_HOST", "127.0.0.1")
CLERK_TEMPLATE = 100  # bank clerk


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
    return g, g.drain(5)


g, gi = login_and_enter()

# NPCs in GM: +cell;dir;0;<actorId>;<templateId>;-4;gfx^scale;...
npcs = {}
my_cell = -1
for p in gi:
    if not p.startswith("GM"):
        continue
    for part in p[3:].split("|"):
        if not part.startswith("+"):
            continue
        f = part[1:].split(";")
        if len(f) > 5 and f[5] == "-4":
            npcs[int(f[3])] = int(f[4])
        elif len(f) > 3 and f[3] == "1":
            my_cell = int(f[0])
print("me @", my_cell, " npcs:", npcs)

clerk_ids = [aid for aid, tpl in npcs.items() if tpl == CLERK_TEMPLATE]
if not clerk_ids:
    print("FAIL: no bank clerk on this map")
    sys.exit(1)

clerk = clerk_ids[0]
print(f">>> talking to clerk actorId={clerk}")
g.send(f"DC{clerk}")

pkts = g.drain(4)
for p in pkts:
    print("<<", p[:140])
dck = next((p for p in pkts if p.startswith("DCK")), None)
dq = next((p for p in pkts if p.startswith("DQ")), None)
if not dck:
    print("FAIL: no DCK")
    sys.exit(1)
if not dq:
    print("FAIL: no DQ (question+answers)")
    sys.exit(1)

# DQ<questionId>[;<param>]|<answerId;answerId;...>
body = dq[2:]
qid_raw, answers = body.split("|")
qid = qid_raw.split(";")[0]  # strip the param field; DR wants the bare id
print(f"question {qid_raw}, answers {answers}")
answer_list = answers.split(";")
if "259" not in answer_list:
    print("FAIL: answer 259 (open bank) not offered")
    sys.exit(1)

g.send(f"DR{qid}|259")
pkts = g.drain(4)
for p in pkts:
    print("<<", p[:140])
if not any(p.startswith("ECK") for p in pkts):
    print("FAIL: no ECK (exchange/bank open)")
    sys.exit(1)
print("bank open packet ok — contents:",
      next((p[:120] for p in pkts if p.startswith("EL")), "(no EL seen)"))

g.send("EV")
g.drain(1)
g.s.close()
print("\n=== NPC DIALOG + BANK SMOKE PASSED ===")
