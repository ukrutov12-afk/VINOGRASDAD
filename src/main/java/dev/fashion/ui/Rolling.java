package dev.fashion.ui;

import java.util.ArrayList;
import java.util.List;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Font;

public final class Rolling {
    private static final class Column {
        final Spring value = new Spring(0f, 16f, 0.82f);
        final Spring in = new Spring(0f, Motion.TOGGLE);
        int digit;
        boolean placed;
    }

    private final List<Column> columns = new ArrayList<>();
    private final Spring width = new Spring(0f, Motion.FLOW);
    private String text = "";

    public void set(String s) {
        text = s;
        while (columns.size() < s.length()) {
            columns.add(new Column());
        }
        for (int i = 0; i < columns.size(); i++) {
            Column col = columns.get(i);
            int idx = s.length() - 1 - i;
            if (idx < 0) {
                col.in.to(0f);
                continue;
            }
            col.in.to(1f);
            char ch = s.charAt(idx);
            int d = Character.isDigit(ch) ? ch - '0' : 0;
            if (!col.placed) {
                col.value.snap(d);
                col.digit = d;
                col.placed = true;
                continue;
            }
            if (d != col.digit) {
                int diff = ((d - col.digit) % 10 + 15) % 10 - 5;
                if (diff == -5) {
                    diff = 5;
                }
                col.value.to(col.value.target + diff);
                col.digit = d;
            }
        }
    }

    public float width(Font.Face face, float size) {
        return width.get();
    }

    public float draw(Canvas c, Font.Face face, float size, int color, float x, float centerY, float dt) {
        float cw = face.width("0", size, 0f);
        width.to(cw * text.length());
        width.update(dt);
        float lineH = size * 1.15f;
        float right = x + width.get();
        c.pushClip(x - 1f, centerY - lineH * 0.5f, width.get() + 2f, lineH, 0f, 3f);
        for (int i = 0; i < columns.size(); i++) {
            Column col = columns.get(i);
            col.value.update(dt);
            col.in.update(dt);
            float a = Math.max(0f, Math.min(1f, col.in.get()));
            if (a < 0.01f) {
                continue;
            }
            float cx = right - cw * (i + 1);
            int idx = text.length() - 1 - i;
            char ch = idx >= 0 ? text.charAt(idx) : '0';
            c.pushAlpha(a);
            if (!Character.isDigit(ch)) {
                c.text(face, size).color(color).drawMidCenter(String.valueOf(ch), cx + cw * 0.5f, centerY);
            } else {
                float v = col.value.get();
                float base = (float) Math.floor(v);
                float frac = v - base;
                int d0 = Math.floorMod((int) base, 10);
                int d1 = Math.floorMod((int) base + 1, 10);
                c.pushAlpha(1f - frac * 0.6f);
                c.text(face, size).color(color).drawMidCenter(String.valueOf(d0), cx + cw * 0.5f, centerY - frac * lineH);
                c.popAlpha();
                if (frac > 0.001f) {
                    c.pushAlpha(0.4f + frac * 0.6f);
                    c.text(face, size).color(color).drawMidCenter(String.valueOf(d1), cx + cw * 0.5f, centerY + (1f - frac) * lineH);
                    c.popAlpha();
                }
            }
            c.popAlpha();
        }
        c.popClip();
        return width.get();
    }
}
