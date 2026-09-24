package dev.fashion.ui;

import dev.fashion.gfx.Colors;

public final class Theme {
    public static final int VOID = 0xFF050409;
    public static final int WINDOW = 0x9E0A0812;
    public static final int SIDEBAR = 0x5A040308;
    public static final int SURFACE = 0xF0110E1A;
    public static final int SURFACE_TOP = 0xF0181424;
    public static final int SURFACE_HI = 0xF0201B30;
    public static final int INSET = 0xFF08070E;
    public static final int LINE = 0xFF2A2440;
    public static final int LINE_SOFT = 0x14FFFFFF;
    public static final int TEXT = 0xFFEEECF8;
    public static final int TEXT_2 = 0xFFA9A3BF;
    public static final int TEXT_3 = 0xFF6B6583;
    public static final int ACCENT = 0xFF8D6CFF;
    public static final int ACCENT_HI = 0xFFC6B8FF;
    public static final int ACCENT_DEEP = 0xFF4B2EB8;
    public static final int CHROME = 0xFFEDEEF7;
    public static final int CLOUD = 0x7A9A8FD6;
    public static final int STORM = 0xFF0B0916;
    public static final int GLOW = 0xFF7C5CFF;

    private Theme() {
    }

    public static int health(float fraction) {
        float f = Math.max(0f, Math.min(1f, fraction));
        float hue = 25f + (145f - 25f) * f;
        float l = 0.66f + 0.08f * f;
        float c = 0.19f - 0.02f * f;
        return Colors.oklch(l, c, hue, 1f);
    }
}
