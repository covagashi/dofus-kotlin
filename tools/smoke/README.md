# Headless smoke tests

Raw-socket integration harness for the Kotlin server — drives the real
Dofus 1.39.8 wire protocol end-to-end without a Flash client.

## Files

| File | Purpose |
|---|---|
| `dofus_proto.py` | Shared protocol helpers: `Conn` (packet framing), password `crypt_pass`, `mapData` decryption (port of `CryptManager`), orthogonal grid + BFS pathing (port of `OrthogonalProj`/`PathFinding`). |
| `dofus_smoke.py` | Login → auth → charlist → charselect → `GCK`/`GDM` map load → `GM`/`GDK` actors → chat (`BM*`/`cMK`) → movement probe → disconnect/reconnect. |
| `dofus_fight_smoke.py` | Full PvM combat: locate mob group on map 4 → BFS path → `GA001` movement → aggro → `GJK`/`GDF`/`GP` fight init → `Gp` placement → `GR1` ready → turn loop (`GTS`/`Gt`), player spell cast (`GA300141`), mob AI, `GE` fight end. Handles reconnect-mid-fight. |
| `dofus_npc_smoke.py` | NPC dialog + bank: `DC<npcId>` → `DCK`/`DQ` question+answers → `DR<qid>|<ans>` → `DV` + `ECK5`/`EL` bank open → `EMG±<n>` kamas deposit/withdraw → `EMO±<guid>|<qty>` item deposit/withdraw → `EV`. Player must be on map 1674 (Astrub bank), cell ~227. |
| `dofus_zaap_smoke.py` | Zaap teleport: `GA500<zaapCell>;114` on the interactive zaap object (cell 297 on map 7411) → `WC<cur>|<dest>;<cost>|...` → `WU<dest>` → `GDM` map change + `WV`, kamas deducted. Player must be on map 7411 with zaaps unlocked (`7411,8125`). |
| `dofus_shop_smoke.py` | NPC vendor: `ER0|<actorId>` → `ECK0` + `EL` offer list → `EB<tplId>|<qty>` → `EBK` + `OAKO` + kamas spent → `EV`. Player must be on map 692 (vendor tpl 23). |
| `dofus_items_smoke.py` | Item equip/unequip: `OM<guid>|<pos>` → `OM` ack + `As`/`Oa`/`OT` stat+set refresh. Guid 7 = tpl 770 (boots → slot 5). |
| `dofus_exchange_smoke.py` | Player-to-player trade: `ER1|<playerId>` → `ECK1` + `EA` accept → `EMG<kamas>` offer → `EK` validate → `EK11`/`EK12` + `EVa` applied. Needs two accounts on the same map (player ids 1+2). |
| `dofus_mapchange_smoke.py` | Scripted map transition: BFS walk to an `onMovementEnd` trigger cell on map 7411 → `GA001` → server `GA<actionId>;1;...` echo → `GKK<actionId>` ack → `GA;2` + `GDM|7427` teleport. |
| `run_all.sh` | Runs every smoke in sequence: repositions the test characters, restarts `starloco_game`, waits for boot, then runs each script inside a throwaway python container on the compose network. |
| `decode_map.py` | Dumps a `maps` table row into the walkable-cell JSON used by `MapGrid`. |
| `map4.json` / `map7411.json` | Decoded walkability for maps 4 (fight) and 7411 (zaap). Regenerate with `decode_map.py <id>`. |

## Running

With the docker-compose stack up (`docker compose -f compose-test.yml up -d`):

```bash
# everything at once (takes a while: one game-server restart per scenario)
./tools/smoke/run_all.sh

# or individually: throwaway python container on the compose network
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

## Per-smoke map prerequisites

Each smoke expects the test player on a specific map. Reposition then
restart the game server (player state is cached in memory):

| Smoke | `map` | `cell` | Extra |
|---|---|---|---|
| `dofus_smoke` | any | any | — |
| `dofus_fight_smoke` | 4 | 268 | — |
| `dofus_npc_smoke` | 1674 | 227 | — |
| `dofus_zaap_smoke` | 7411 | 310 | `zaaps` col incl. `7411,8125` |
| `dofus_shop_smoke` | 692 | 241 | kamas > 0 |
| `dofus_items_smoke` | any | any | guid 7 owned, level ≥ 2 |
| `dofus_exchange_smoke` | 1674 | 227 | players 1+2 same map, kamas > 0 |
| `dofus_mapchange_smoke` | 7411 | 311 | — |

```bash
docker exec dofus-kotlin-starloco_mariadb-1 mariadb -uroot -p$DB_PASS \
  starloco_login -e \
  "UPDATE world_players SET map=<map>, cell=<cell> WHERE id=1"
docker compose restart starloco_game   # wait for "server is ready" + exchange
```

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
