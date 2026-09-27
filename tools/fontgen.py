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


T = 1.6


def arc(cx, cy, r, a0, a1, n=48):
    return [(cx + r * math.cos(a), cy + r * math.sin(a)) for a in np.linspace(a0, a1, n)]


def kite(angle, length, width, shoulder, back=-0.02, cx=12.0, cy=12.0, scale=11.6):
    dx = math.sin(angle)
    dy = -math.cos(angle)
    nx = math.cos(angle)
    ny = math.sin(angle)
    tip = (cx + dx * length * scale, cy + dy * length * scale)
    sh = (cx + dx * shoulder * scale, cy + dy * shoulder * scale)
    bk = (cx + dx * back * scale, cy + dy * back * scale)
    return fill([tip, (sh[0] + nx * width * scale, sh[1] + ny * width * scale), bk,
                 (sh[0] - nx * width * scale, sh[1] - ny * width * scale)])


def ring(r0, r1, scale=11.6):
    return ("ring", 12.0, 12.0, r0 * scale, r1 * scale)


def star_points():
    out = []
    for k in range(4):
        out.append(kite(k * math.pi / 2, 1.0, 0.1, 0.2))
    for k in range(4):
        out.append(kite(math.pi / 4 + k * math.pi / 2, 0.8, 0.082, 0.18))
    for k in range(8):
        out.append(kite(math.pi / 8 + k * math.pi / 4, 0.52, 0.05, 0.13))
    return out


def star_inlay():
    out = []
    for k in range(4):
        out.append(kite(k * math.pi / 2, 0.86, 0.044, 0.3, 0.19))
    for k in range(4):
        out.append(kite(math.pi / 4 + k * math.pi / 2, 0.67, 0.036, 0.27, 0.19))
    return out


ICONS = {
    0xE000: [
        stroke([(6.5, 17.5), (18.5, 5.5)], T),
        stroke([(18.5, 5.5), (19.2, 4.8)], T * 0.7),
        stroke([(4.8, 14.6), (9.4, 19.2)], T),
        stroke([(6.2, 17.8), (4.0, 20.0)], T),
    ],
    0xE001: [
        stroke([(5.5, 6.5), (11, 12), (5.5, 17.5)], T),
        stroke([(12.5, 6.5), (18, 12), (12.5, 17.5)], T),
    ],
    0xE002: [
        stroke(bez_q((2.5, 12), (12, 3.2), (21.5, 12)) + bez_q((21.5, 12), (12, 20.8), (2.5, 12))[:-1], T, True),
        stroke(circle_pts(12, 12, 3.1), T, True),
    ],
    0xE003: [
        stroke(circle_pts(12, 8, 3.7), T, True),
        stroke([(5, 20.2)] + bez_c((5, 20.2), (5, 15.6), (8.2, 14), (12, 14)) + bez_c((12, 14), (15.8, 14), (19, 15.6), (19, 20.2)), T),
    ],
    0xE004: [
        rbox_ring(4, 4, 6.8, 6.8, 1.8, T),
        rbox_ring(13.2, 4, 6.8, 6.8, 1.8, T),
        rbox_ring(4, 13.2, 6.8, 6.8, 1.8, T),
        rbox_ring(13.2, 13.2, 6.8, 6.8, 1.8, T),
    ],
    0xE005: [
        stroke(circle_pts(10.6, 10.6, 6.0), T * 1.15, True),
        stroke([(15.2, 15.2), (19.6, 19.6)], T * 1.3),
    ],
    0xE007: [
        rbox_ring(3.5, 4.5, 17, 15, 3, T),
        stroke([(4, 9.5), (20, 9.5)], T),
        stroke([(10, 9.5), (10, 19)], T),
    ],
    0xE008: [stroke([(6, 9), (12, 15), (18, 9)], T * 1.25)],
    0xE009: [stroke([(5, 12.5), (10, 17.5), (19, 7)], T * 1.4)],
    0xE00A: [stroke([(6.5, 6.5), (17.5, 17.5)], T * 1.3), stroke([(17.5, 6.5), (6.5, 17.5)], T * 1.3)],
    0xE00C: [
        disc(8.1, 9.2, 4.7),
        disc(15.9, 9.2, 4.7),
        fill([(3.75, 11.1), (12, 20.8), (20.25, 11.1), (12, 7.5)]),
    ],
    0xE00E: [
        stroke([(3.5, 7), (20.5, 7)], T), stroke(circle_pts(15, 7, 2.4), T, True),
        stroke([(3.5, 17), (20.5, 17)], T), stroke(circle_pts(9, 17, 2.4), T, True),
    ],
    0xE012: [
        stroke([(20, 12)] + [(12 + 8 * math.cos(a), 12 - 8 * math.sin(a)) for a in np.linspace(0, 1.62 * math.pi, 60)[1:]], T),
        stroke([(20.6, 7.4), (20, 12.4), (15.4, 10.6)], T),
    ],
    0xE013: [
        stroke(circle_pts(12, 12, 8.2), T, True),
        fill([(12, 4.6), (14.2, 12), (12, 13.2), (9.8, 12)]),
        stroke([(12, 13.2), (14.2, 12), (12, 19.4), (9.8, 12), (12, 13.2)], T * 0.8),
    ],
    0xE030: [
        stroke(circle_pts(12, 12, 7.2), T, True),
        stroke([(12, 2.5), (12, 7)], T), stroke([(12, 17), (12, 21.5)], T),
        stroke([(2.5, 12), (7, 12)], T), stroke([(17, 12), (21.5, 12)], T),
        fill([(12, 9.6), (14.4, 12), (12, 14.4), (9.6, 12)]),
    ],
    0xE031: [
        stroke([(3.5, 9), (3.5, 3.5), (9, 3.5)], T), stroke([(15, 3.5), (20.5, 3.5), (20.5, 9)], T),
        stroke([(20.5, 15), (20.5, 20.5), (15, 20.5)], T), stroke([(9, 20.5), (3.5, 20.5), (3.5, 15)], T),
        stroke(circle_pts(12, 12, 2.6), T, True),
    ],
    0xE032: [
        stroke([(3, 8), (11, 8)], T), stroke([(5, 12), (13, 12)], T), stroke([(3, 16), (10, 16)], T),
        stroke([(14, 6.5), (19.5, 12), (14, 17.5)], T),
    ],
    0xE033: [
        rbox_ring(3, 4, 18, 16, 3, T),
        rbox_ring(5.6, 6.6, 5.4, 3.2, 1.2, T * 0.8),
        stroke([(13.5, 8.2), (18, 8.2)], T * 0.8),
        stroke([(5.6, 16.6), (11, 16.6)], T * 0.8),
    ],
    0xE034: [
        stroke(circle_pts(12, 12, 4.6), T, True),
        stroke([(12 + 9.5 * math.cos(a) * 1.0, 12 + 3.6 * math.sin(a)) for a in np.linspace(0.35, 2 * math.pi - 0.35, 60)], T * 0.85),
        disc(16.2, 9.6, 1.3),
    ],
    0xE035: [
        disc(6, 6, 1.4), disc(12, 6, 1.4), disc(18, 6, 1.4),
        disc(6, 12, 1.4), disc(12, 12, 1.4), disc(18, 12, 1.4),
        disc(6, 18, 1.4), disc(12, 18, 1.4), disc(18, 18, 1.4),
    ],
    0xE036: [
        stroke([(6, 16.5)] + bez_c((6, 16.5), (7.2, 14.6), (6.6, 12), (6.6, 10.5)) + bez_c((6.6, 10.5), (6.6, 6.8), (9, 4.6), (12, 4.6))
               + bez_c((12, 4.6), (15, 4.6), (17.4, 6.8), (17.4, 10.5)) + bez_c((17.4, 10.5), (17.4, 12), (16.8, 14.6), (18, 16.5)) + [(6, 16.5)], T),
        stroke(arc(12, 17.8, 2.2, 0.15, math.pi - 0.15), T),
    ],
    0xE037: [
        stroke(circle_pts(8, 12, 3.8), T, True),
        stroke([(11.8, 12), (20.5, 12)], T), stroke([(17.5, 12), (17.5, 15.2)], T), stroke([(20.5, 12), (20.5, 14.6)], T),
    ],
    0xE038: [
        stroke([(12, 3.5), (12, 14.5)], T), stroke([(7.8, 10.4), (12, 14.6), (16.2, 10.4)], T),
        stroke([(4.5, 14.5), (4.5, 19.5), (19.5, 19.5), (19.5, 14.5)], T),
    ],
    0xE039: [
        stroke([(12, 3.4), (21, 19.4), (3, 19.4), (12, 3.4)], T),
        stroke([(12, 9.4), (12, 14.2)], T * 1.2), disc(12, 16.8, 1.1),
    ],
    0xE03A: [
        stroke(arc(12, 12.8, 7.6, -math.pi / 2 + 0.75, 3 * math.pi / 2 - 0.75), T),
        stroke([(12, 3.2), (12, 11.4)], T),
    ],
    0xE020: [ring(0.62, 0.805)],
    0xE021: [ring(0.8, 0.865), ring(0.59, 0.64), ring(0.44, 0.475)],
    0xE022: star_points() + [disc(12, 12, 0.205 * 11.6)],
    0xE023: star_inlay(),
    0xE024: [disc(12, 12, 0.15 * 11.6)],
}

HIRES = {0xE020, 0xE021, 0xE022, 0xE023, 0xE024}
HIRES_EM = 160.0

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
    if kind == "ring":
        r = np.hypot(px - sh[1], py - sh[2])
        return np.minimum(r - sh[3], sh[4] - r)
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
    for cp, shapes in ICONS.items():
        em = HIRES_EM if cp in HIRES else EM
        k = em / ICON_UNITS
        x0 = -int(SPREAD)
        y0 = -int(SPREAD)
        w = int(em + 2 * SPREAD)
        h = int(em + 2 * SPREAD)
        px, py = grid(x0, y0, w, h)
        ux = px / k
        uy = ICON_UNITS - py / k
        sd = None
        for sh in shapes:
            sv = shape_sd(ux, uy, sh) * k
            sd = sv if sd is None else np.maximum(sd, sv)
        out.append({"cp": cp, "adv": 1.0, "img": encode(sd, w, h), "x0": x0, "y0": y0, "em": em})
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
            em = g.get("em", EM)
            rows.append([g["cp"], round(g["adv"], 5),
                         round(g["x0"] / em, 5), round(g["y0"] / em, 5),
                         round((g["x0"] + w) / em, 5), round((g["y0"] + h) / em, 5),
                         g["ax"], g["ay"], w, h])
        entry = dict(meta)
        entry["glyphs"] = rows
        doc["fonts"][key] = entry
    with open(os.path.join(OUT, "atlas.json"), "w", encoding="utf-8") as f:
        json.dump(doc, f, separators=(",", ":"))
    print("atlas", ATLAS_W, "x", height, file=sys.stderr)


if __name__ == "__main__":
    main()
