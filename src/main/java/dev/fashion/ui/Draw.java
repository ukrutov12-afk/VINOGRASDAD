package dev.fashion.ui;

import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;

public final class Draw {
    private Draw() {
    }

    public static void icon(Canvas c, char glyph, float cx, float cy, float size, int color) {
        c.text(Font.icons(), size).color(color).draw(String.valueOf(glyph), cx - size * 0.5f, cy + size * 0.5f);
    }

    public static void icon(Canvas c, char glyph, float cx, float cy, float size, int color, float glowRadius, int glowColor) {
        c.text(Font.icons(), size).color(color).glow(glowRadius, glowColor).draw(String.valueOf(glyph), cx - size * 0.5f, cy + size * 0.5f);
    }

    public static void chromeIcon(Canvas c, char glyph, float cx, float cy, float size, float glowRadius, int glowColor) {
        c.text(Font.icons(), size).color(0xFFFFFFFF).chrome(1f).glow(glowRadius, glowColor).draw(String.valueOf(glyph), cx - size * 0.5f, cy + size * 0.5f);
    }

    public static void hairline(Canvas c, float x, float y, float w, int color) {
        float t = c.px();
        float half = w * 0.5f;
        c.shape(x, y, half + t, t).horizontal(Colors.withAlpha(color, 0f), color).draw();
        c.shape(x + half, y, half, t).horizontal(color, Colors.withAlpha(color, 0f)).draw();
    }

    public static void vHairline(Canvas c, float x, float y, float h, int color) {
        float t = c.px();
        float half = h * 0.5f;
        c.shape(x, y, t, half + t).vertical(Colors.withAlpha(color, 0f), color).draw();
        c.shape(x, y + half, t, half).vertical(color, Colors.withAlpha(color, 0f)).draw();
    }

    public static String plural(int n, String one, String few, String many) {
        int m10 = n % 10;
        int m100 = n % 100;
        if (m10 == 1 && m100 != 11) {
            return one;
        }
        if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) {
            return few;
        }
        return many;
    }
}
