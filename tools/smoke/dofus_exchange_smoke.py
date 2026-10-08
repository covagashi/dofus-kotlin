#!/usr/bin/env python3
"""Headless player-to-player exchange smoke test.

Requires two accounts/characters online on the SAME map:
  - account test,  char id 1 (Bredravorveidurroth)
  - account test2, char id 2 (SmokeBuddy)
Both must be on the same map (e.g. 1674 cell ~227).

Flow (P1 initiates, P2 accepts, P1 gives kamas, both validate):

  P1: ER1|<p2id>      -> both ERK<p1>|<p2>|1
  P2: EA             -> both ECK1 (+ EL/Em exchange panels)
  P1: EMG<kamas>     -> P1 EMKG<k>  /  P2 EmKG<k>
  P1: EK             -> both EK1<p1id>
  P2: EK             -> both EK1<p2id> -> exchange applies -> both EV
                        + P1 As kamas down / P2 As kamas up

Usage:
    DOFUS_LOGIN_HOST=starloco_login DOFUS_GAME_HOST=starloco_game \
        python3 tools/smoke/dofus_exchange_smoke.py
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import login_and_enter, parse_gm_actors

LOGIN_HOST = os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1")
LOGIN_PORT = int(os.environ.get("DOFUS_LOGIN_PORT", "450"))
GAME_HOST = os.environ.get("DOFUS_GAME_HOST", "127.0.0.1")
KAMAS = 100

print(">>> P1 login")
g1, gi1 = login_and_enter(LOGIN_HOST, LOGIN_PORT, GAME_HOST,
                          account="test", char_id=1, name="p1")
print(">>> P2 login")
g2, gi2 = login_and_enter(LOGIN_HOST, LOGIN_PORT, GAME_HOST,
                          account="test2", char_id=2, name="p2")

# find each other's actor id from GM
_, _, _ = parse_gm_actors(gi1)
p2_actor = None
for p in gi1:
    if p.startswith("GM"):
        for part in p[3:].split("|"):
            if part.startswith("+") and "SmokeBuddy" in part:
                p2_actor = int(part[1:].split(";")[3])
if not p2_actor:
    # maybe P2's GM arrives after; drain g1 briefly
    for p in g1.drain(3):
        if p.startswith("GM") and "SmokeBuddy" in p:
            p2_actor = int(p[4:].split(";")[3])
if not p2_actor:
    print("FAIL: P2 not visible on P1's map")
    sys.exit(1)
print("P2 actor id on map:", p2_actor)

print(">>> P1 requests exchange")
g1.send(f"ER1|{p2_actor}")
p1_pkts = g1.drain(3)
p2_pkts = g2.drain(1)
erk = next((p for p in p1_pkts if p.startswith("ERK")), None)
if not erk:
    print("FAIL: no ERK on initiator")
    sys.exit(1)
print("P1 <<", erk)
time.sleep(1)
p2_pkts += g2.drain(3)
if not any(p.startswith("ERK") for p in p2_pkts):
    print("FAIL: no ERK on target")
    sys.exit(1)
print("P2 <<", next(p for p in p2_pkts if p.startswith("ERK")))

print(">>> P2 accepts")
g2.send("EA")
pkts1 = g1.drain(4)
pkts2 = g2.drain(1) + g2.drain(2)
if not any(p.startswith("ECK") for p in pkts1 + pkts2):
    print("FAIL: no ECK after accept")
    sys.exit(1)
print("exchange open:", [p[:50] for p in pkts1 if p.startswith("ECK")])

print(f">>> P1 puts {KAMAS} kamas")
g1.send(f"EMG{KAMAS}")
pkts1 = g1.drain(4)
pkts2 = g2.drain(2)
emk1 = next((p for p in pkts1 if p.startswith("EMK")), None)
emk2 = next((p for p in pkts2 if p.startswith("EmK")), None)
print("P1 <<", emk1, "  P2 <<", emk2)
if not emk1 or str(KAMAS) not in emk1:
    print("FAIL: no EMK on initiator")
    sys.exit(1)
if not emk2:
    print("FAIL: no EmK on target")
    sys.exit(1)

print(">>> both validate")
g1.send("EK")
time.sleep(0.5)
g2.send("EK")
pkts1 = g1.drain(6)
pkts2 = g2.drain(3)
ev1 = any(p == "EV" or p.startswith("EV") for p in pkts1)
ev2 = any(p == "EV" or p.startswith("EV") for p in pkts2)
print("P1 pkts:", [p[:40] for p in pkts1 if p.startswith(("E", "As"))])
print("P2 pkts:", [p[:40] for p in pkts2 if p.startswith(("E", "As"))])
if not ev1 or not ev2:
    print("FAIL: exchange did not complete (missing EV)")
    sys.exit(1)

as2 = next((p for p in pkts2 if p.startswith("As")), None)
if as2:
    print("P2 kamas now:", as2.split("|")[1])

g1.s.close(); g2.s.close()
print("\n=== EXCHANGE SMOKE PASSED ===")
