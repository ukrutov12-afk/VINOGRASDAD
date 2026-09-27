package dev.fashion.ui;

import dev.fashion.gfx.Colors;

public final class Theme {
    public static final int DIM = 0xA006050C;
    public static final int WINDOW = 0xF30B0A12;
    public static final int SIDEBAR = 0x66020105;
    public static final int PLAQUE = 0xE60C0B13;
    public static final int CARD_TOP = 0xFF1B1826;
    public static final int CARD_BOTTOM = 0xFF0F0D16;
    public static final int CARD_HOVER_TOP = 0xFF221D32;
    public static final int CARD_ON_TOP = 0xFF1F1934;
    public static final int CARD_ON_BOTTOM = 0xFF130F22;
    public static final int INSET = 0xFF07060C;
    public static final int INSET_TOP = 0xFF0A0911;
    public static final int LINE = 0xFF252133;
    public static final int EDGE = 0x3DFFFFFF;
    public static final int SURFACE = 0xF0110E1A;
    public static final int SURFACE_TOP = 0xF0181424;
    public static final int TEXT = 0xFFF2F0F9;
    public static final int TEXT_2 = 0xFFB3ADC6;
    public static final int TEXT_3 = 0xFF6F6987;
    public static final int TEXT_OFF = 0xFF918AA6;
    public static final int ACCENT = 0xFF8E6BFF;
    public static final int ACCENT_HI = 0xFFCABDFF;
    public static final int ACCENT_DEEP = 0xFF4A2CB6;
    public static final int SILVER = 0xFFE3E5F1;
    public static final int GLOW = 0xFF7D5CFF;
    public static final int GLOW_SILVER = 0xFFB9A9FF;
    public static final int CLOUD = 0x7A9A8FD6;
    public static final int OK = 0xFF7FE0B0;
    public static final int DANGER = 0xFFFF6B7D;

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
