import json
import math
import os
import sys

import numpy as np
from fontTools.pens.basePen import BasePen
from fontTools.ttLib import TTFont
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fashion", "sdf")

EM = 48.0
SPREAD = 12.0
GAP = 2
ATLAS_W = 2048

TEXT = "".join(chr(c) for c in range(32, 127))
TEXT += "".join(chr(c) for c in range(0x410, 0x450)) + "Ёё"
TEXT += "«»—–…•·°№₽→←↑↓×✓±’“”"
LOGO = "".join(chr(c) for c in range(32, 127))

FONTS = [
    ("regular", "Inter-Regular.woff2", TEXT),
    ("medium", "Inter-Medium.woff2", TEXT),
    ("semibold", "Inter-SemiBold.woff2", TEXT),
    ("bold", "Inter-Bold.woff2", TEXT),
    ("display", "Unbounded-Bold-latin.woff", LOGO),
]


class FlatPen(BasePen):
    def __init__(self, glyphset, scale):
        super().__init__(glyphset)
        self.scale = scale
        self.contours = []
        self.cur = None

    def _p(self, pt):
        return (pt[0] * self.scale, pt[1] * self.scale)

    def _moveTo(self, pt):
        self.cur = [self._p(pt)]

    def _lineTo(self, pt):
        self.cur.append(self._p(pt))

    def _qCurveToOne(self, p1, p2):
        a = self.cur[-1]
        b = self._p(p1)
        c = self._p(p2)
        n = max(4, int(math.ceil((math.dist(a, b) + math.dist(b, c)) / 1.2)))
        for i in range(1, n + 1):
            t = i / n
            u = 1 - t
            self.cur.append((u * u * a[0] + 2 * u * t * b[0] + t * t * c[0],
                             u * u * a[1] + 2 * u * t * b[1] + t * t * c[1]))

    def _curveToOne(self, p1, p2, p3):
        a = self.cur[-1]
        b = self._p(p1)
        c = self._p(p2)
        d = self._p(p3)
        n = max(4, int(math.ceil((math.dist(a, b) + math.dist(b, c) + math.dist(c, d)) / 1.2)))
        for i in range(1, n + 1):
            t = i / n
            u = 1 - t
            self.cur.append((u ** 3 * a[0] + 3 * u * u * t * b[0] + 3 * u * t * t * c[0] + t ** 3 * d[0],
                             u ** 3 * a[1] + 3 * u * u * t * b[1] + 3 * u * t * t * c[1] + t ** 3 * d[1]))

    def _closePath(self):
        if self.cur and len(self.cur) > 2:
            self.contours.append(self.cur)
        self.cur = None

    def _endPath(self):
        self._closePath()


def segments(contours):
    a = []
    b = []
    for c in contours:
        pts = np.array(c, dtype=np.float64)
        a.append(pts)
        b.append(np.roll(pts, -1, axis=0))
    return np.concatenate(a), np.concatenate(b)


def seg_dist(px, py, a, b):
    ax = a[:, 0][None, :]
    ay = a[:, 1][None, :]
    bx = b[:, 0][None, :]
    by = b[:, 1][None, :]
    pax = px[:, None] - ax
    pay = py[:, None] - ay
    bax = bx - ax
    bay = by - ay
    den = bax * bax + bay * bay
    den = np.where(den == 0, 1e-12, den)
    h = np.clip((pax * bax + pay * bay) / den, 0, 1)
    dx = pax - bax * h
    dy = pay - bay * h
    return np.sqrt((dx * dx + dy * dy).min(axis=1))


def winding(px, py, a, b):
    ax = a[:, 0][None, :]
    ay = a[:, 1][None, :]
    bx = b[:, 0][None, :]
    by = b[:, 1][None, :]
    y = py[:, None]
    x = px[:, None]
    cross = (bx - ax) * (y - ay) - (x - ax) * (by - ay)
    up = (ay <= y) & (by > y) & (cross > 0)
    down = (ay > y) & (by <= y) & (cross < 0)
    return up.sum(axis=1) - down.sum(axis=1)


def polygon_sd(px, py, contours):
    a, b = segments(contours)
    d = seg_dist(px, py, a, b)
    inside = winding(px, py, a, b) != 0
    return np.where(inside, d, -d)


def cell_for(bounds):
    xmin, ymin, xmax, ymax = bounds
    x0 = math.floor(xmin) - SPREAD
    y0 = math.floor(ymin) - SPREAD
    w = int(math.ceil(xmax) - math.floor(xmin) + 2 * SPREAD)
    h = int(math.ceil(ymax) - math.floor(ymin) + 2 * SPREAD)
    return x0, y0, w, h


def grid(x0, y0, w, h):
    xs = x0 + np.arange(w) + 0.5
    ys = y0 + (h - 1 - np.arange(h)) + 0.5
    gx, gy = np.meshgrid(xs, ys)
    return gx.ravel(), gy.ravel()


def encode(sd, w, h):
    v = np.clip(0.5 + sd / (2 * SPREAD), 0, 1)
    return np.round(v * 255).astype(np.uint8).reshape(h, w)


def lookups(font):
    if "GPOS" not in font:
        return []
    gpos = font["GPOS"].table
    idx = []
    for fr in gpos.FeatureList.FeatureRecord:
        if fr.FeatureTag == "kern":
            for i in fr.Feature.LookupListIndex:
                if i not in idx:
                    idx.append(i)
    out = []
    for i in idx:
        lk = gpos.LookupList.Lookup[i]
        subs = []
        for st in lk.SubTable:
            if lk.LookupType == 9:
                if st.ExtensionLookupType != 2:
                    continue
                st = st.ExtSubTable
            elif lk.LookupType != 2:
                continue
            subs.append(st)
        if subs:
            out.append(subs)
    return out


def pair_value(sub, g1, g2):
    cov = sub.Coverage.glyphs
    if g1 not in cov:
        return None
    if sub.Format == 1:
        ps = sub.PairSet[cov.index(g1)]
        for rec in ps.PairValueRecord:
            if rec.SecondGlyph == g2:
                v = rec.Value1
                return getattr(v, "XAdvance", 0) if v else 0
        return None
    c1 = sub.ClassDef1.classDefs.get(g1, 0)
    c2 = sub.ClassDef2.classDefs.get(g2, 0)
    rec = sub.Class1Record[c1].Class2Record[c2]
    v = rec.Value1
    x = getattr(v, "XAdvance", 0) if v else 0
    return x if x else None


def kerning(font, chars, cmap, upm):
    subsets = lookups(font)
    pairs = []
    names = [(c, cmap[ord(c)]) for c in chars if ord(c) in cmap and c != " "]
    covered = []
    for subs in subsets:
        covered.append([(s, set(s.Coverage.glyphs)) for s in subs])
    for c1, g1 in names:
        for c2, g2 in names:
            total = 0
            for subs in covered:
                for s, cov in subs:
                    if g1 not in cov:
                        continue
                    v = pair_value(s, g1, g2)
                    if v is not None:
                        total += v
                        break
            if total:
                pairs.append([ord(c1), ord(c2), round(total / upm, 5)])
    return pairs


def font_glyphs(key, path, chars):
    font = TTFont(os.path.join(ROOT, "fonts", path))
    upm = font["head"].unitsPerEm
    scale = EM / upm
    cmap = font.getBestCmap()
    gs = font.getGlyphSet()
    hmtx = font["hmtx"]
    os2 = font["OS/2"]
    hhea = font["hhea"]
    out = []
    for ch in chars:
        cp = ord(ch)
        if cp not in cmap:
            continue
        name = cmap[cp]
        adv = hmtx[name][0] / upm
        pen = FlatPen(gs, scale)
        gs[name].draw(pen)
        if not pen.contours:
            out.append({"cp": cp, "adv": adv, "img": None})
            continue
        allp = np.concatenate([np.array(c) for c in pen.contours])
        bounds = (allp[:, 0].min(), allp[:, 1].min(), allp[:, 0].max(), allp[:, 1].max())
        x0, y0, w, h = cell_for(bounds)
        px, py = grid(x0, y0, w, h)
        sd = polygon_sd(px, py, pen.contours)
        out.append({"cp": cp, "adv": adv, "img": encode(sd, w, h), "x0": x0, "y0": y0})
    asc = hhea.ascent / upm
    desc = hhea.descent / upm
    meta = {
        "ascender": round(asc, 5),
        "descender": round(desc, 5),
        "lineHeight": round((hhea.ascent - hhea.descent + hhea.lineGap) / upm, 5),
        "capHeight": round(getattr(os2, "sCapHeight", 0) / upm, 5),
        "xHeight": round(getattr(os2, "sxHeight", 0) / upm, 5),
        "kerning": kerning(font, chars, cmap, upm),
    }
    return out, meta


def bez_q(a, b, c, n=24):
    pts = []
    for i in range(1, n + 1):
        t = i / n
        u = 1 - t
        pts.append((u * u * a[0] + 2 * u * t * b[0] + t * t * c[0], u * u * a[1] + 2 * u * t * b[1] + t * t * c[1]))
    return pts


def bez_c(a, b, c, d, n=32):
    pts = []
    for i in range(1, n + 1):
        t = i / n
        u = 1 - t
        pts.append((u ** 3 * a[0] + 3 * u * u * t * b[0] + 3 * u * t * t * c[0] + t ** 3 * d[0],
                    u ** 3 * a[1] + 3 * u * u * t * b[1] + 3 * u * t * t * c[1] + t ** 3 * d[1]))
    return pts


def circle_pts(cx, cy, r, n=96):
    return [(cx + r * math.cos(2 * math.pi * i / n), cy + r * math.sin(2 * math.pi * i / n)) for i in range(n)]


def stroke(pts, w, closed=False):
    return ("stroke", pts, w, closed)


def fill(pts):
    return ("fill", pts)


def rbox(x, y, w, h, r):
    return ("rbox", x, y, w, h, r)


def rbox_ring(x, y, w, h, r, sw):
    return ("rring", x, y, w, h, r, sw)


def disc(cx, cy, r):
    return ("disc", cx, cy, r)


ICONS = {
    0xE000: [
        stroke([(7.2, 16.8), (18.6, 5.4)], 2.3),
        stroke([(18.6, 5.4), (19.4, 4.6)], 1.2),
        stroke([(4.6, 14.2), (9.8, 19.4)], 2.3),
        stroke([(6.3, 17.7), (3.8, 20.2)], 2.3),
    ],
    0xE001: [
        stroke([(5, 6), (11, 12), (5, 18)], 2.3),
        stroke([(12.5, 6), (18.5, 12), (12.5, 18)], 2.3),
    ],
    0xE002: [
        stroke(bez_q((2.2, 12), (12, 2.6), (21.8, 12)) + bez_q((21.8, 12), (12, 21.4), (2.2, 12))[:-1], 2.1, True),
        disc(12, 12, 3.3),
    ],
    0xE003: [
        stroke(circle_pts(12, 7.8, 3.9), 2.1, True),
        stroke([(4.6, 20.6)] + bez_c((4.6, 20.6), (4.6, 15.6), (8, 13.8), (12, 13.8)) + bez_c((12, 13.8), (16, 13.8), (19.4, 15.6), (19.4, 20.6)), 2.1),
    ],
    0xE004: [
        rbox(3.5, 3.5, 7.5, 7.5, 2.2),
        rbox(13, 3.5, 7.5, 7.5, 2.2),
        rbox(3.5, 13, 7.5, 7.5, 2.2),
        rbox(13, 13, 7.5, 7.5, 2.2),
    ],
    0xE005: [
        stroke(circle_pts(10.4, 10.4, 6.3), 2.2, True),
        stroke([(15.1, 15.1), (19.8, 19.8)], 2.5),
    ],
    0xE006: [
        fill([(12, 0.8)] + bez_q((12, 0.8), (13.5, 10.5), (23.2, 12)) + bez_q((23.2, 12), (13.5, 13.5), (12, 23.2))
             + bez_q((12, 23.2), (10.5, 13.5), (0.8, 12)) + bez_q((0.8, 12), (10.5, 10.5), (12, 0.8))[:-1]),
    ],
    0xE007: [
        rbox_ring(3, 4, 18, 16, 3.2, 2.0),
        stroke([(3.5, 9.5), (20.5, 9.5)], 2.0),
        stroke([(10, 9.5), (10, 19.5)], 2.0),
    ],
    0xE008: [stroke([(6, 9), (12, 15), (18, 9)], 2.3)],
    0xE009: [stroke([(5, 12.5), (10, 17.5), (19, 7)], 2.5)],
    0xE00A: [stroke([(6.5, 6.5), (17.5, 17.5)], 2.3), stroke([(17.5, 6.5), (6.5, 17.5)], 2.3)],
    0xE00B: [
        rbox_ring(2.5, 6, 19, 12.5, 2.8, 1.9),
        disc(6.8, 10.2, 1.15), disc(10.3, 10.2, 1.15), disc(13.8, 10.2, 1.15), disc(17.3, 10.2, 1.15),
        stroke([(8.2, 14.4), (15.8, 14.4)], 1.9),
    ],
    0xE00C: [
        disc(8.1, 9.2, 4.7),
        disc(15.9, 9.2, 4.7),
        fill([(3.75, 11.1), (12, 20.8), (20.25, 11.1), (12, 7.5)]),
    ],
    0xE00D: [
        stroke(circle_pts(12, 12, 7), 2.0, True),
        stroke([(12, 1.8), (12, 6.2)], 2.0), stroke([(12, 17.8), (12, 22.2)], 2.0),
        stroke([(1.8, 12), (6.2, 12)], 2.0), stroke([(17.8, 12), (22.2, 12)], 2.0),
        disc(12, 12, 1.7),
    ],
    0xE00E: [
        stroke([(3.5, 7), (20.5, 7)], 2.0), disc(15, 7, 2.9),
        stroke([(3.5, 17), (20.5, 17)], 2.0), disc(9, 17, 2.9),
    ],
    0xE00F: [disc(12, 5, 1.8), disc(12, 12, 1.8), disc(12, 19, 1.8)],
    0xE010: [fill([(13.6, 1.8), (4.4, 13.6), (11.2, 13.6), (10.2, 22.2), (19.6, 9.8), (12.8, 9.8)])],
    0xE011: [stroke([(12, 5), (12, 19)], 2.3), stroke([(5, 12), (19, 12)], 2.3)],
    0xE012: [
        stroke([(20, 12)] + [(12 + 8 * math.cos(a), 12 - 8 * math.sin(a)) for a in np.linspace(0, 1.62 * math.pi, 60)[1:]], 2.2),
        stroke([(20.8, 7.6), (20, 12.4), (15.6, 10.6)], 2.2),
    ],
    0xE013: [
        stroke([(12, 3), (12, 21)], 2.0), stroke([(3, 12), (21, 12)], 2.0),
        stroke([(12, 3), (9.4, 5.6)], 2.0), stroke([(12, 3), (14.6, 5.6)], 2.0),
        stroke([(12, 21), (9.4, 18.4)], 2.0), stroke([(12, 21), (14.6, 18.4)], 2.0),
        stroke([(3, 12), (5.6, 9.4)], 2.0), stroke([(3, 12), (5.6, 14.6)], 2.0),
        stroke([(21, 12), (18.4, 9.4)], 2.0), stroke([(21, 12), (18.4, 14.6)], 2.0),
    ],
}

ICON_UNITS = 24.0


def shape_sd(px, py, sh):
    kind = sh[0]
    if kind == "stroke":
        pts = np.array(sh[1], dtype=np.float64)
        b = np.roll(pts, -1, axis=0)
        a = pts
        if not sh[3]:
            a = pts[:-1]
            b = pts[1:]
        return sh[2] / 2 - seg_dist(px, py, a, b)
    if kind == "fill":
        return polygon_sd(px, py, [sh[1]])
    if kind == "disc":
        return sh[3] - np.hypot(px - sh[1], py - sh[2])
    if kind in ("rbox", "rring"):
        x, y, w, h, r = sh[1:6]
        cx = x + w / 2
        cy = y + h / 2
        qx = np.abs(px - cx) - (w / 2 - r)
        qy = np.abs(py - cy) - (h / 2 - r)
        outside = np.hypot(np.maximum(qx, 0), np.maximum(qy, 0)) + np.minimum(np.maximum(qx, qy), 0) - r
        if kind == "rbox":
            return -outside
        return sh[6] / 2 - np.abs(outside)
    raise ValueError(kind)


def icon_glyphs():
    out = []
    k = EM / ICON_UNITS
    for cp, shapes in ICONS.items():
        x0 = -int(SPREAD)
        y0 = -int(SPREAD)
        w = int(EM + 2 * SPREAD)
        h = int(EM + 2 * SPREAD)
        px, py = grid(x0, y0, w, h)
        ux = px / k
        uy = ICON_UNITS - py / k
        sd = None
        for sh in shapes:
            s = shape_sd(ux, uy, sh) * k
            sd = s if sd is None else np.maximum(sd, s)
        out.append({"cp": cp, "adv": 1.0, "img": encode(sd, w, h), "x0": x0, "y0": y0})
    return out


def pack(items):
    order = sorted([it for it in items if it["img"] is not None], key=lambda it: -it["img"].shape[0])
    x = GAP
    y = GAP
    row = 0
    for it in order:
        h, w = it["img"].shape
        if x + w + GAP > ATLAS_W:
            x = GAP
            y += row + GAP
            row = 0
        it["ax"] = x
        it["ay"] = y
        x += w + GAP
        row = max(row, h)
    height = y + row + GAP
    height = 1 << (height - 1).bit_length()
    return height


def main():
    os.makedirs(OUT, exist_ok=True)
    fonts = {}
    every = []
    for key, path, chars in FONTS:
        glyphs, meta = font_glyphs(key, path, chars)
        fonts[key] = (glyphs, meta)
        every += glyphs
        print(key, len(glyphs), "glyphs", len(meta["kerning"]), "kern pairs", file=sys.stderr)
    icons = icon_glyphs()
    fonts["icons"] = (icons, {"ascender": 0.5, "descender": -0.5, "lineHeight": 1.0, "capHeight": 1.0, "xHeight": 1.0, "kerning": []})
    every += icons
    height = pack(every)
    atlas = np.zeros((height, ATLAS_W), dtype=np.uint8)
    for it in every:
        if it["img"] is None:
            continue
        h, w = it["img"].shape
        atlas[it["ay"]:it["ay"] + h, it["ax"]:it["ax"] + w] = it["img"]
    rgba = np.stack([atlas, atlas, atlas, atlas], axis=-1)
    Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, "atlas.png"), optimize=True)
    doc = {"atlas": {"width": ATLAS_W, "height": height, "em": EM, "spread": SPREAD}, "fonts": {}}
    for key, (glyphs, meta) in fonts.items():
        rows = []
        for g in glyphs:
            if g["img"] is None:
                rows.append([g["cp"], round(g["adv"], 5)])
                continue
            h, w = g["img"].shape
            rows.append([g["cp"], round(g["adv"], 5),
                         round(g["x0"] / EM, 5), round(g["y0"] / EM, 5),
                         round((g["x0"] + w) / EM, 5), round((g["y0"] + h) / EM, 5),
                         g["ax"], g["ay"], w, h])
        entry = dict(meta)
        entry["glyphs"] = rows
        doc["fonts"][key] = entry
    with open(os.path.join(OUT, "atlas.json"), "w", encoding="utf-8") as f:
        json.dump(doc, f, separators=(",", ":"))
    print("atlas", ATLAS_W, "x", height, file=sys.stderr)


if __name__ == "__main__":
    main()
