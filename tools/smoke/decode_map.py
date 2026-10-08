#!/usr/bin/env python3
"""Decode a map row from the StarLoco `maps` table into the walkable-cell
JSON consumed by dofus_fight_smoke.py / MapGrid.

Usage (docker compose stack running):
    python3 tools/smoke/decode_map.py 4 > tools/smoke/map4.json

Env: DB_CONTAINER (default dofus-kotlin-starloco_mariadb-1), DB_NAME
(default starloco_game), DB_USER (root), DB_PASS (dev compose password).
"""
import json
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dofus_proto import decode_map_json

DB_CONTAINER = os.environ.get("DB_CONTAINER", "dofus-kotlin-starloco_mariadb-1")
DB_NAME = os.environ.get("DB_NAME", "starloco_game")
DB_USER = os.environ.get("DB_USER", "root")
DB_PASS = os.environ.get("DB_PASS", "CYoEw5SaBv1kIk")


def main():
    map_id = sys.argv[1]
    out = subprocess.run(
        ["docker", "exec", DB_CONTAINER, "mariadb", f"-u{DB_USER}",
         f"-p{DB_PASS}", DB_NAME, "-N", "-e",
         f"SELECT id, width, heigth, `key`, mapData FROM maps WHERE id={int(map_id)}"],
        capture_output=True, text=True, check=True).stdout.strip()
    if not out:
        print(f"map {map_id} not found", file=sys.stderr)
        sys.exit(1)
    row = out.split("\t")
    j = decode_map_json(int(row[0]), int(row[1]), int(row[2]), row[3], row[4])
    json.dump(j, sys.stdout)
    print()


if __name__ == "__main__":
    main()
