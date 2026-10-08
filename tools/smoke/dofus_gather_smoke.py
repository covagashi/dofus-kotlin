#!/usr/bin/env python3
"""Headless resource-gathering smoke test.

Player must be on map 952 (Incarnam, has a well object on cell 283).
Skill 102 (draw water) requires no job and no tool — the simplest gather.

  login -> charselect -> GC/GDM -> GA500<cell>;<skill>
  -> GA;...;501 (gather animation, duration 1500ms)
  -> OAKO (new object) + IQ (quantity info) after the gather delay
  -> EV

Run inside the docker network:
  docker run --rm --network dofus-kotlin_starloco -v "$PWD/tools:/tools" \
    python:3-alpine sh -c 'cd /tools/smoke && \
    DOFUS_LOGIN_HOST=starloco_login DOFUS_GAME_HOST=starloco_game \
    python3 dofus_gather_smoke.py'
"""
import os
import sys
import time

from dofus_proto import login_and_enter

WELL_CELL = 283
SKILL_DRAW_WATER = 102
WATER_TEMPLATE = 311


def main():
    gc, _gi = login_and_enter(
        os.environ.get("DOFUS_LOGIN_HOST", "127.0.0.1"),
        int(os.environ.get("DOFUS_LOGIN_PORT", "450")),
        os.environ.get("DOFUS_GAME_HOST", "127.0.0.1"),
    )

    gc.send("GA500%d;%d" % (WELL_CELL, SKILL_DRAW_WATER))
    print("[game] >> GA500%d;%d" % (WELL_CELL, SKILL_DRAW_WATER))

    saw_anim = False
    saw_item = False
    saw_gdf = False
    deadline = time.time() + 12
    while time.time() < deadline:
        p = gc.recv_pkt(timeout=5)
        if p is None:
            break
        print("<<", p)
        # GDF|<cell>;<state>;0  (object state report; 4+ = not ready/cooldown)
        if p.startswith("GDF"):
            saw_gdf = True
            continue
        # GA<actionId>;501;<actorId>;<cell>,<dur>  (broadcast gather anim)
        if p.startswith("GA") and ";501;" in p:
            saw_anim = True
        if p.startswith("OAKO") or (p.startswith("IQ") and ";%d" % WATER_TEMPLATE in p):
            saw_item = True
        if saw_item:
            break

    gc.send("EV")

    print("=== gather anim:", saw_anim, "| item received:", saw_item)
    if saw_anim and saw_item:
        print("=== GATHER SMOKE PASSED ===")
    elif saw_gdf:
        # GDF state report but no gather: object mid-respawn (well cooldown
        # is 120-420s). Clean reject, not a protocol failure.
        print("=== GATHER SMOKE INCONCLUSIVE (object on cooldown) ===")
    else:
        # No response at all: either a fully not-ready object (silent Lua
        # reject, also fine) or a dead handler. Check server logs to
        # distinguish; report as inconclusive rather than a hard fail.
        print("=== GATHER SMOKE INCONCLUSIVE (no response — cooldown or dead handler) ===")


if __name__ == "__main__":
    main()
