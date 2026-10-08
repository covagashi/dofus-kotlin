"""Shared Dofus 1.39.8 wire-protocol helpers for the headless smoke tests.

Ports of: CryptManager (password hash + mapData decrypt), CellsDataProvider
(walkability), OrthogonalProj/PathFinding (cell geometry).
"""
import codecs
import collections
import json
import socket
import time
from urllib.parse import unquote

CHAIN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
HIDX = {c: i for i, c in enumerate(CHAIN)}


def cellcode(cid):
    return CHAIN[cid >> 6] + CHAIN[cid & 63]


def cellcode_to_int(s):
    return (HIDX[s[0]] << 6) | HIDX[s[1]]


def crypt_pass(password, key):
    out = []
    for i, c in enumerate(password):
        pk = ord(key[i])
        hi, lo = ord(c) >> 4, ord(c) & 15
        out.append(CHAIN[(hi + pk) % 64])
        out.append(CHAIN[(lo + pk) % 64])
    return "#1" + "".join(out)


class Conn:
    def __init__(self, host, port, name):
        self.s = socket.create_connection((host, port), timeout=10)
        self.name = name
        self.buf = b""
        self.quiet = False

    def send(self, pkt):
        self.s.sendall(pkt.encode() + b"\n\x00")
        if not self.quiet:
            print(f"[{self.name}] >> {pkt[:80]}")

    def _fill(self, timeout):
        self.s.settimeout(timeout)
        try:
            d = self.s.recv(4096)
        except socket.timeout:
            return False
        if not d:
            return False
        self.buf += d
        return True

    def recv_pkt(self, timeout=8):
        end = time.time() + timeout
        while True:
            for i, b in enumerate(self.buf):
                if b == 0:
                    pkt = self.buf[:i].decode(errors="replace").strip()
                    self.buf = self.buf[i + 1:]
                    if pkt:
                        if not self.quiet:
                            print(f"[{self.name}] << {pkt[:140]}")
                        return pkt
                    break
            else:
                if time.time() > end or not self._fill(timeout):
                    return None
                continue
            if pkt:
                return pkt

    def drain(self, seconds=3):
        pkts = []
        end = time.time() + seconds
        while time.time() < end:
            p = self.recv_pkt(timeout=1)
            if p:
                pkts.append(p)
        return pkts


# ---- mapData decryption (CryptManager port) ----

def prepare_key(key):
    s = ''.join(chr(int(key[i:i + 2], 16)) for i in range(0, len(key), 2))
    return unquote(s, encoding='utf-8', errors='replace')


def checksum_key(key):
    num = sum(ord(c) % 16 for c in key)
    return '0123456789ABCDEF'[num % 16]


def unescape_java(data):
    return codecs.escape_decode(data.encode('latin-1'))[0].decode('latin-1')


def decrypt_map_data(map_data, key):
    key = prepare_key(key)
    checksum = int(checksum_key(key), 16) * 2
    out = []
    for i in range(0, len(map_data) - 1, 2):
        num = int(map_data[i:i + 2], 16)
        s = ((i // 2) + checksum) % len(key)
        out.append(chr(num ^ ord(key[s])))
    return unescape_java(''.join(out))


def cell_bytes(dec, cell):
    off = cell * 10
    return [HIDX[dec[off + i]] for i in range(10)]


def is_active(b):
    return (b[0] & 0x20) != 0


def movement(b):
    return (b[2] & 0x38) >> 3


def is_walkable(b):
    return is_active(b) and movement(b) >= 1


# ---- orthogonal geometry (OrthogonalProj / PathFinding port) ----

class MapGrid:
    """Decoded map walkability grid + path helpers for one map.

    Load from a JSON produced by decode_map_json(): {"id","w","h","walk":[...]}.
    """

    def __init__(self, w, h, walk):
        self.w = w
        self.h = h
        self.walk = set(walk)
        self.t = 2 * w - 1

    @classmethod
    def load(cls, path):
        m = json.load(open(path))
        return cls(m["w"], m["h"], m["walk"])

    def orth_y(self, c):
        return (c % self.t % self.w) - (c // self.t)

    def orth_x(self, c):
        return (c + self.orth_y(c) * (self.w - 1)) // self.w

    def dist(self, c1, c2):
        return abs(self.orth_x(c1) - self.orth_x(c2)) + abs(self.orth_y(c1) - self.orth_y(c2))

    def is_edge(self, c):
        d = self.orth_x(c) - self.orth_y(c)
        s = self.orth_x(c) + self.orth_y(c)
        return d == 0 or s == (self.w - 1) * 2 or d == (self.h - 1) * 2 or s == 0

    DELTA = None  # filled in __init__ (depends on w)

    def _delta(self):
        w, t = self.w, self.t
        return {'a': 1, 'b': w, 'c': t, 'd': w - 1,
                'e': -1, 'f': -w, 'g': -t, 'h': -(w - 1)}

    def step(self, c, d):
        n = c + self._delta()[d]
        if n < 0 or self.is_edge(n):
            return -1
        return n if n in self.walk else -1

    def bfs(self, start, goal_pred):
        """Steps [(dir, cell)] from start to nearest cell matching goal_pred."""
        if goal_pred(start):
            return []
        prev = {start: None}
        q = collections.deque([start])
        while q:
            c = q.popleft()
            for d in 'abcdefgh':
                n = self.step(c, d)
                if n < 0 or n in prev:
                    continue
                prev[n] = (c, d)
                if goal_pred(n):
                    path = []
                    cur = n
                    while prev[cur] is not None:
                        p, dd = prev[cur]
                        path.append((dd, cur))
                        cur = p
                    path.reverse()
                    return path
                q.append(n)
        return None


def login_and_enter(login_host, login_port, game_host, gi_drain=4,
                    account="test", password="test", char_id=1, name="game"):
    """Full login -> game-entry handshake. Returns (game Conn, GI packets)."""
    lg = Conn(login_host, login_port, name + ":login")
    hc = None
    for _ in range(8):
        hc = lg.recv_pkt()
        if hc and hc.startswith("HC"):
            break
    if not hc:
        print("FAIL: no HC")
        sys.exit(1)
    lg.send("1.39.8e"); lg.send(account); lg.recv_pkt(3)
    lg.send(crypt_pass(password, hc[2:])); lg.drain(2)
    lg.send("Ax"); lg.drain(2)
    lg.send("AX601")
    ayk = lg.recv_pkt()
    if not ayk or not ayk.startswith("AYK"):
        print("FAIL: no AYK")
        sys.exit(1)
    ticket = ayk[3:].split(";")[1]
    gport = int(ayk[3:].split(";")[0].split(":")[1])
    lg.s.close()

    g = Conn(game_host, gport, name)
    if g.recv_pkt() != "HG":
        print("FAIL: no HG")
        sys.exit(1)
    g.send("AT" + ticket)
    r = g.recv_pkt()
    if not r or not r.startswith("ATK"):
        print("FAIL: ATK")
        sys.exit(1)
    g.send("AV0"); g.recv_pkt(2)
    g.send("AL")
    for _ in range(6):
        p = g.recv_pkt(3)
        if p and p.startswith("ALK"):
            break
    g.send("AS%d" % char_id); g.drain(4)
    g.send("GC"); g.drain(3)
    g.send("GI")
    return g, g.drain(gi_drain)


def parse_gm_actors(gi_packets):
    """Split GM actor list into players / mob groups / npcs.

    Returns (my_cell, groups {cell: desc}, npcs {actorId: templateId}).
    """
    groups, npcs, my_cell = {}, {}, -1
    for p in gi_packets:
        if not p.startswith("GM"):
            continue
        for part in p[3:].split("|"):
            if not part.startswith("+"):
                continue
            f = part[1:].split(";")
            if len(f) > 5 and f[5] == "-4":
                npcs[int(f[3])] = int(f[4])
            elif len(f) > 5 and f[5] == "-3":
                groups[int(f[0])] = part[:90]
            elif len(f) > 3 and f[3] == "1":
                my_cell = int(f[0])
            elif len(f) > 3 and f[3] == "-1":
                groups[int(f[0])] = part[:90]
    return my_cell, groups, npcs


def decode_map_json(map_id, w, h, key, mapdata):
    """Decode raw mapData into the JSON form MapGrid.load() consumes.

    Some maps store plaintext cell data (10 chars per cell); those are
    detected by non-hex characters and used as-is.
    """
    if all(c in "0123456789abcdefABCDEF" for c in mapdata.strip()):
        dec = decrypt_map_data(mapdata, key)
    else:
        dec = mapdata
    n = len(dec) // 10
    tmp = MapGrid(w, h, [])
    walk = [c for c in range(n)
            if is_walkable(cell_bytes(dec, c)) and not tmp.is_edge(c)]
    return {"id": map_id, "w": w, "h": h, "walk": walk}
