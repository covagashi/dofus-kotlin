#!/usr/bin/env python3
"""Headless map-transition smoke test.

Player must be on map 7411 (Astrub zaap). Its script declares
onMovementEnd triggers; stepping on cell 376 teleports to map 7427.

Flow:

  login -> charselect -> GC/GDM -> GI -> BFS path to cell 376
  -> GA001<path> -> expect GDM|7427 (map transition)

Usage:
    DOFUS_LOGIN_HOST=starloco_login DOFUS_GAME_HOST=starloco_game \
        python3 tools/smoke/dofus_mapchange_smoke.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import login_and_enter, parse_gm_actors, MapGrid, cellcode

LOGIN_HOST = os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1")
LOGIN_PORT = int(os.environ.get("DOFUS_LOGIN_PORT", "450"))
GAME_HOST = os.environ.get("DOFUS_GAME_HOST", "127.0.0.1")
MAP_FILE = os.environ.get("DOFUS_MAP_JSON",
                         os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                      "map7411.json"))
TRIGGER_CELL = 376    # onMovementEnd[376] = teleport(7427, 363)
DEST_MAP = 7427

g, gi = login_and_enter(LOGIN_HOST, LOGIN_PORT, GAME_HOST)
my_cell, groups, npcs = parse_gm_actors(gi)
print("me @", my_cell)

M = MapGrid.load(MAP_FILE)
path = M.bfs(my_cell, lambda c: c == TRIGGER_CELL)
if not path:
    print("FAIL: no walkable path to trigger cell", TRIGGER_CELL)
    sys.exit(1)
payload = "".join(dd + cellcode(c) for dd, c in path)
print("path:", payload[:60])
g.send("GA001" + payload)

# server echoes GA<gaId>;<actionId>;<actorId>;<path>
# the client acks arrival with GKK<gaId>
pkts = g.drain(6)
ga_id = None
for p in pkts:
    print("<<", p[:100])
    if p.startswith("GA") and ";" in p and p[2:p.index(";")].isdigit():
        ga_id = p[2:p.index(";")]
if not ga_id:
    print("FAIL: no GA echo")
    sys.exit(1)

import time
time.sleep(len(path) // 2 + 3)   # rough walk duration
print(">>> sending GKK" + ga_id)
g.send("GKK" + ga_id)

pkts = g.drain(8)
for p in pkts:
    print("<<", p[:100])
gdm = next((p for p in pkts if p.startswith("GDM")), None)
if not gdm:
    print("FAIL: no GDM (map transition) after stepping on trigger cell")
    sys.exit(1)
new_map = int(gdm.split("|")[1])
if new_map != DEST_MAP:
    print(f"FAIL: transitioned to map {new_map}, expected {DEST_MAP}")
    sys.exit(1)

g.s.close()
print(f"\n=== MAP TRANSITION SMOKE PASSED: 7411 -> {new_map} ===")
