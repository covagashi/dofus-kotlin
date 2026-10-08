#!/usr/bin/env python3
"""Headless NPC vendor shop smoke test.

Player must be on map 692 (has vendor NPC template 23 at cell 240, sells
item template 770). Flow:

  login -> charselect -> GC/GDM -> GI -> parse GM for the vendor's actor id
  -> ER0<actorId> -> expect ECK0 + EL<offer list>
  -> EB<tplId>|1 -> expect EBK (+ As stat refresh with lower kamas)
  -> EV -> clean exit.

Usage:
    DOFUS_LOGIN_HOST=starloco_login DOFUS_GAME_HOST=starloco_game \
        python3 tools/smoke/dofus_shop_smoke.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import login_and_enter, parse_gm_actors

LOGIN_HOST = os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1")
LOGIN_PORT = int(os.environ.get("DOFUS_LOGIN_PORT", "450"))
GAME_HOST = os.environ.get("DOFUS_GAME_HOST", "127.0.0.1")

VENDOR_TEMPLATE = 23   # npc_template id (sells item 770)
BUY_TEMPLATE = 770

g, gi = login_and_enter(LOGIN_HOST, LOGIN_PORT, GAME_HOST)

my_cell, groups, npcs = parse_gm_actors(gi)
print("me @", my_cell, " npcs:", npcs)

vendors = [aid for aid, tpl in npcs.items() if tpl == VENDOR_TEMPLATE]
if not vendors:
    print("FAIL: no vendor npc (tpl %d) on this map" % VENDOR_TEMPLATE)
    sys.exit(1)

vendor = vendors[0]
print(f">>> opening shop with vendor actorId={vendor}")
g.send(f"ER0|{vendor}")

pkts = g.drain(4)
for p in pkts:
    print("<<", p[:140])
if not any(p.startswith("ECK") for p in pkts):
    print("FAIL: no ECK (shop open)")
    sys.exit(1)
el = next((p for p in pkts if p.startswith("EL")), None)
if not el:
    print("FAIL: no EL (vendor item list)")
    sys.exit(1)

# EL<tplId>;<stats>;<currency>;<price>;;| ...
offers = {}
for offer in el[2:].split("|"):
    f = offer.split(";")
    if f and f[0].isdigit():
        offers[int(f[0])] = int(f[3]) if len(f) > 3 and f[3].isdigit() else 0
print("vendor offers:", list(offers.keys()))
if BUY_TEMPLATE not in offers:
    print(f"FAIL: template {BUY_TEMPLATE} not in offers")
    sys.exit(1)

price = offers[BUY_TEMPLATE]
print(f">>> buying 1x tpl {BUY_TEMPLATE} for {price} kamas")
g.send(f"EB{BUY_TEMPLATE}|1")
pkts = g.drain(4)
for p in pkts:
    print("<<", p[:140])
if not any(p.startswith("EBK") for p in pkts):
    print("FAIL: no EBK (buy confirm)")
    sys.exit(1)

# As stat packet should show reduced kamas
as_pkt = next((p for p in pkts if p.startswith("As")), None)
if as_pkt:
    kamas = as_pkt.split("|")[1] if "|" in as_pkt else "?"
    print("kamas after buy:", kamas)

g.send("EV")
g.drain(1)
g.s.close()
print("\n=== SHOP SMOKE PASSED ===")
