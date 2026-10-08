# Headless smoke tests

Raw-socket integration harness for the Kotlin server — drives the real
Dofus 1.39.8 wire protocol end-to-end without a Flash client.

## Files

| File | Purpose |
|---|---|
| `dofus_proto.py` | Shared protocol helpers: `Conn` (packet framing), password `crypt_pass`, `mapData` decryption (port of `CryptManager`), orthogonal grid + BFS pathing (port of `OrthogonalProj`/`PathFinding`). |
| `dofus_smoke.py` | Login → auth → charlist → charselect → `GCK`/`GDM` map load → `GM`/`GDK` actors → chat (`BM*`/`cMK`) → movement probe → disconnect/reconnect. |
| `dofus_fight_smoke.py` | Full PvM combat: locate mob group on map 4 → BFS path → `GA001` movement → aggro → `GJK`/`GDF`/`GP` fight init → `Gp` placement → `GR1` ready → turn loop (`GTS`/`Gt`), player spell cast (`GA300141`), mob AI, `GE` fight end. Handles reconnect-mid-fight. |
| `decode_map.py` | Dumps a `maps` table row into the walkable-cell JSON used by `MapGrid`. |
| `map4.json` | Decoded walkability for map 4 (fixed mob group at cell 280 area; mobs aggro at distance ≤ 2). Regenerate with `decode_map.py 4`. |

## Running

With the docker-compose stack up (`docker compose -f compose-test.yml up -d`):

```bash
# recommended: throwaway python container on the compose network
docker run --rm --network dofus-kotlin_starloco \
  -v "$PWD/tools:/tools" python:3-alpine sh -c \
  'cd /tools/smoke && DOFUS_LOGIN_HOST=starloco_login \
   DOFUS_GAME_HOST=starloco_game python3 dofus_smoke.py'

docker run --rm --network dofus-kotlin_starloco \
  -v "$PWD/tools:/tools" python:3-alpine sh -c \
  'cd /tools/smoke && DOFUS_LOGIN_HOST=starloco_login \
   DOFUS_GAME_HOST=starloco_game python3 dofus_fight_smoke.py'
```

Running from the host (`127.0.0.1`) only works if nothing else binds
`450`/`5555` locally — on this dev machine an ssh tunnel owns `:450` and
another `server` process owns `:5555`, which silently swallows the game
connection (no `HG`). The docker-network route avoids the issue entirely.

Env vars: `DOFUS_LOGIN_HOST` (default `127.0.0.1`), `DOFUS_LOGIN_PORT`
(`450`), `DOFUS_GAME_HOST` (`127.0.0.1`). The game port is read from the
login server's `AYK` response.

## Fight smoke prerequisites

1. Seeded account `test`/`test`, player id `1` must exist.
2. Player must be **out of combat** and on **map 4** near the fixed mob
   group (cells ~260–290). To reset:

   ```bash
   docker exec dofus-kotlin-starloco_mariadb-1 mariadb -uroot -p$DB_PASS \
     starloco_login -e \
     "UPDATE world_players SET map=4, cell=268, logged=0 WHERE id=1"
   ```

3. **Restart the game server after direct DB edits** — the `Player`
   object is cached in memory and stale position/fight state is saved
   back on disconnect:

   ```bash
   docker compose restart starloco_game   # also starloco_login (caches too)
   ```

4. Wait for the game→login exchange to validate before connecting
   (check game logs for the exchange confirmation), or auth returns
   `AXEd`.

## What the fight smoke verifies

- Map mob groups visible in `GM` (`;-1;` actor type)
- Path-based movement accepted (`GA0` echo, `GM` cell updates)
- Aggro triggers `startFightVersusMonstres` (`GJK`, `GDF`, `GP`, `GA;950`)
- Placement cells decode (`Gp`), ready toggles turn order (`GIC`, `GS`,
  `GTL`, `GTM`, `Gd`, `GTS`)
- Player spell cast emits `GA;300` + damage `GA;100` + cost `GA;102`
- Mob AI acts on its turns (`GA0` moves, `GA;300` casts)
- `GE`/`GV` terminates the fight cleanly; disconnect mid-fight leaves no
  server exceptions
