#!/usr/bin/env python3
"""Headless item equip/unequip smoke test.

Player must own object guid 7 (item template 770 = Fecaflip, type 11 boots).
Flow:

  login -> charselect -> GC/GDM -> GI
  -> OM<guid>|5    (equip into BOTTES slot)
     expect OM<hexGuid>|<hexPos> (OBJET_MOVE) + As (stats refresh)
  -> OM<guid>|-1   (unequip back to inventory)
     expect OM again

Usage:
    DOFUS_LOGIN_HOST=starloco_login DOFUS_GAME_HOST=starloco_game \
        python3 tools/smoke/dofus_items_smoke.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import login_and_enter, parse_gm_actors

LOGIN_HOST = os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1")
LOGIN_PORT = int(os.environ.get("DOFUS_LOGIN_PORT", "450"))
GAME_HOST = os.environ.get("DOFUS_GAME_HOST", "127.0.0.1")

ITEM_GUID = int(os.environ.get("DOFUS_ITEM_GUID", "7"))
DOFUS_SLOT = 5          # ITEM_POS_BOTTES (tpl 770 is type 11 = boots)
UNEQUIP = -1            # ITEM_POS_NO_EQUIPED

g, gi = login_and_enter(LOGIN_HOST, LOGIN_PORT, GAME_HOST)
my_cell, groups, npcs = parse_gm_actors(gi)
print("me @", my_cell)

print(f">>> equipping guid {ITEM_GUID} to dofus slot")
g.send(f"OM{ITEM_GUID}|{DOFUS_SLOT}")
pkts = g.drain(4)
for p in pkts:
    print("<<", p[:140])
om = next((p for p in pkts if p.startswith("OM")), None)
if not om:
    print("FAIL: no OM (object move ack)")
    sys.exit(1)
# OM<guid>|<pos> — decimal fields, empty pos = unequipped
body = om[2:].split("|")
if int(body[0]) != ITEM_GUID or int(body[1]) != DOFUS_SLOT:
    print("FAIL: OM ack wrong guid/pos:", om)
    sys.exit(1)
print("equipped:", om)
if not any(p.startswith("As") for p in pkts):
    print("FAIL: no As stats refresh after equip")
    sys.exit(1)

print(f">>> unequipping guid {ITEM_GUID}")
g.send(f"OM{ITEM_GUID}|{UNEQUIP}")
pkts = g.drain(4)
for p in pkts:
    print("<<", p[:140])
om = next((p for p in pkts if p.startswith("OM")), None)
if not om:
    print("FAIL: no OM on unequip")
    sys.exit(1)
body = om[2:].split("|")
# unequip = OM<guid>| with EMPTY position field
if int(body[0]) != ITEM_GUID or body[1] != "":
    print("FAIL: unequip OM wrong:", om)
    sys.exit(1)
print("unequipped:", om)

g.s.close()
print("\n=== ITEM EQUIP SMOKE PASSED ===")
