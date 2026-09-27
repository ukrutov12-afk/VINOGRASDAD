package dev.fashion.ui;

import dev.fashion.anim.Clock;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;

public final class Logo {
    private Logo() {
    }

    private static void layer(Canvas c, char glyph, float cx, float cy, float size, int top, int bottom,
                              float chrome, float outline, float glow, int glowColor) {
        Canvas.Text t = c.text(Font.icons(), size).vertical(top, bottom);
        if (chrome > 0f) {
            t.chrome(chrome);
        }
        if (outline > 0f) {
            t.outline(outline, 0xE0080510);
        }
        if (glow > 0f) {
            t.glow(glow, glowColor);
        }
        t.draw(String.valueOf(glyph), cx - size * 0.5f, cy + size * 0.5f);
    }

    public static void draw(Canvas c, float cx, float cy, float size, float spin, float energy) {
        float t = Clock.time();
        float breathe = 0.5f + 0.5f * (float) Math.sin(t * 1.15f);
        c.shape(cx - size * 0.2f, cy - size * 0.2f, size * 0.4f, size * 0.4f).radius(size * 0.2f).fill(0)
                .glow(size * (0.42f + 0.08f * breathe + 0.2f * energy), Colors.withAlpha(0xFFA996FF, 0.26f + 0.12f * breathe + 0.25f * energy))
                .draw();
        layer(c, '', cx, cy, size, 0xFF4B2C8C, 0xFF1C0F34, 0f, 0f, 0f, 0);
        layer(c, '', cx, cy, size, 0xFFFFFFFF, 0xFFBFB8D8, 1f, size * 0.012f, 0f, 0);
        c.push();
        c.rotateAround(cx, cy, spin);
        layer(c, '', cx, cy, size, 0xFFFFFFFF, 0xFFC9C3E0, 1f, size * 0.016f, size * 0.03f, Colors.withAlpha(0xFFB7A6FF, 0.35f + 0.3f * energy));
        layer(c, '', cx, cy, size, 0xFFB29BFF, 0xFF4A2AA6, 0f, 0f, size * 0.02f, Colors.withAlpha(0xFF8E6BFF, 0.5f));
        c.pop();
        float orbGlow = 0.45f + 0.35f * breathe + 0.3f * energy;
        layer(c, '', cx, cy, size, 0xFF3F2372, 0xFF0C0616, 0f, size * 0.012f, size * 0.05f, Colors.withAlpha(0xFF9C7BFF, orbGlow));
        float hs = size * 0.035f;
        c.shape(cx - size * 0.045f - hs, cy - size * 0.05f - hs, hs * 2f, hs * 2f).radius(hs)
                .fill(Colors.withAlpha(0xFFFFFFFF, 0.55f)).glow(hs * 1.5f, Colors.withAlpha(0xFFFFFFFF, 0.25f)).draw();
    }
}
