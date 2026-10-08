#!/usr/bin/env bash
# Runs the full protocol smoke suite against the Docker test stack.
# Each scenario needs the test character (id=1) at a fixed map/cell and a
# fresh game-server boot (the world is cached in memory), so the runner
# repositions the character and restarts starloco_game between scenarios.
#
# Usage: ./tools/smoke/run_all.sh            (from repo root)
#        COMPOSE_FILE=foo.yml ./tools/smoke/run_all.sh
set -u
cd "$(dirname "$0")/../.."

COMPOSE_FILE=${COMPOSE_FILE:-compose-test.yml}
GAME=dofus-kotlin-starloco_game-1
DB=dofus-kotlin-starloco_mariadb-1
DBPASS=${DBPASS:-CYoEw5SaBv1kIk}
NETWORK=dofus-kotlin_starloco
SMOKE_IMAGE=${SMOKE_IMAGE:-python:3-alpine}
BOOT_WAIT=${BOOT_WAIT:-55}

sql() { docker exec "$DB" mariadb -uroot -p"$DBPASS" starloco_login -e "$1"; }

reposition() { # map cell
    sql "UPDATE world_players SET map=$1, cell=$2, pdvper=100, logged=0 WHERE id=1"
    sql "UPDATE world_players SET map=$1, cell=$2, pdvper=100, logged=0 WHERE id=2"
    docker compose -f "$COMPOSE_FILE" restart starloco_game >/dev/null 2>&1
    echo "[runner] repositioned to map $1 cell $2; waiting ${BOOT_WAIT}s for boot..."
    sleep "$BOOT_WAIT"
}

run_smoke() { # script
    echo "===== $1 ====="
    docker run --rm --network "$NETWORK" -v "$PWD/tools:/tools" "$SMOKE_IMAGE" \
        sh -c "cd /tools/smoke && export DOFUS_LOGIN_HOST=starloco_login DOFUS_GAME_HOST=starloco_game && python3 -u $1" \
        2>&1 | tail -5
}

# name|map|cell
SCENARIOS="dofus_npc_smoke.py|1674|227
dofus_exchange_smoke.py|1674|227
dofus_items_smoke.py|1674|227
dofus_zaap_smoke.py|7411|311
dofus_mapchange_smoke.py|7411|311
dofus_shop_smoke.py|692|100
dofus_fight_smoke.py|4|281"

reposition 1674 227   # any valid spot for the position-independent login smoke
run_smoke dofus_smoke.py

echo "$SCENARIOS" | while IFS='|' read -r script map cell; do
    reposition "$map" "$cell"
    run_smoke "$script"
done

echo "[runner] done. Check 'PASSED'/'FAIL' lines above."
