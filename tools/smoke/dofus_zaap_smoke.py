#!/usr/bin/env python3
"""Headless zaap (waypoint) smoke test.

Player must be on map 7411 (Astrub zaap) near cell 311 (the zaap object),
with zaaps "7411,<dest>" seeded in world_players.zaaps. Flow:

  login -> charselect -> GC/GDM -> GI
  -> GA500311;114 (use zaap object, skill 114 -> openZaapMenu)
  -> WC<zaap list>
  -> WU<dest> -> expect WV + GDM<dest> (teleport to the zaap cell)

Usage:
    DOFUS_LOGIN_HOST=starloco_login DOFUS_GAME_HOST=starloco_game \
        python3 tools/smoke/dofus_zaap_smoke.py [destMapId]
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import Conn, crypt_pass, login_and_enter, parse_gm_actors

LOGIN_HOST = os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1")
LOGIN_PORT = int(os.environ.get("DOFUS_LOGIN_PORT", "450"))
GAME_HOST = os.environ.get("DOFUS_GAME_HOST", "127.0.0.1")
ZAAP_CELL = 297  # obj2num 7000 = Zaap object (311 is the arrival cell)
DEST = int(sys.argv[1]) if len(sys.argv) > 1 else 8125


g, gi = login_and_enter(LOGIN_HOST, LOGIN_PORT, GAME_HOST)
my_cell, groups, npcs = parse_gm_actors(gi)
print("me @", my_cell, "on map 7411")

# use the zaap object: GA500<cell>;<skillId=114>
g.send(f"GA500{ZAAP_CELL};114")
pkts = g.drain(5)
for p in pkts:
    print("<<", p[:140])
wc = next((p for p in pkts if p.startswith("WC")), None)
if not wc:
    print("FAIL: no WC (zaap menu)")
    sys.exit(1)
print("zaap menu:", wc[:140])

# travel to dest
g.send(f"WU{DEST}")
pkts = g.drain(6)
for p in pkts:
    print("<<", p[:140])
if not any(p.startswith("WV") for p in pkts):
    print("FAIL: no WV (zaap close/leave)")
    sys.exit(1)
if not any(p.startswith("GDM|" + str(DEST) + "|") or p.startswith("GDM" + str(DEST)) for p in pkts):
    print("FAIL: no GDM for dest map", DEST)
    sys.exit(1)

print(f"\n=== ZAAP SMOKE PASSED: traveled 7411 -> {DEST} ===")
g.s.close()
