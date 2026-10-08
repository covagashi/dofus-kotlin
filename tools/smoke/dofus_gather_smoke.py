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
    deadline = time.time() + 12
    while time.time() < deadline:
        p = gc.recv_pkt(timeout=5)
        if p is None:
            break
        print("<<", p)
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
    else:
        print("=== GATHER SMOKE FAILED ===")
        sys.exit(1)


if __name__ == "__main__":
    main()
