package dev.fashion.ui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.Draw;
import dev.fashion.ui.PlayerHead;
import dev.fashion.ui.Theme;

public final class TargetCard extends HudElement {
    private static final float W = 162f;
    private static final float H = 50f;

    private LivingEntity target;
    private float hold;
    private float lastHp = -1f;
    private final Spring bar = new Spring(1f, 42f, 0.92f);
    private final Spring trail = new Spring(1f, Motion.SOFT);
    private final Spring flash = new Spring(0f, 9f, 1f);
    private final Spring pulse = new Spring(0f, 24f, 0.42f);
    private final Spring shake = new Spring(0f, 34f, 0.3f);
    private final Spring swap = new Spring(1f, Motion.FLOW);
    private String name = "";
    private Identifier skin;
    private String prevName = "";
    private Identifier prevSkin;

    public TargetCard() {
        super("target", "Карточка цели", 0.62f, 0.6f);
    }

    private LivingEntity source(boolean editing) {
        LivingEntity t = Modules.attackAura.target();
        if (t == null && editing) {
            return MinecraftClient.getInstance().player;
        }
        return t;
    }

    @Override
    public boolean shown(boolean editing) {
        return Modules.hud.enabled() && Modules.hud.targetHud.on() && (target != null && hold > 0f || editing && source(true) != null);
    }

    @Override
    public float contentWidth() {
        return W;
    }

    @Override
    public float contentHeight() {
        return H;
    }

    @Override
    public void update(float dt, float sw, float sh, boolean editing) {
        LivingEntity t = source(editing);
        if (t != null && t.isAlive()) {
            if (t != target) {
                boolean visible = target != null && presence.get() > 0.05f;
                if (visible) {
                    prevName = name;
                    prevSkin = skin;
                    swap.snap(0f);
                    swap.to(1f);
                }
                target = t;
                lastHp = visible ? -2f : -1f;
            }
            hold = 1.1f;
        } else {
            hold -= dt;
        }
        if (target != null) {
            float max = Math.max(1f, target.getMaxHealth());
            float hp = Math.max(0f, Math.min(max, target.getHealth()));
            float frac = hp / max;
            if (lastHp == -2f) {
                bar.to(frac);
                trail.to(frac, 0.2f);
            } else if (lastHp < 0f) {
                bar.snap(frac);
                trail.snap(frac);
            } else if (hp < lastHp - 0.01f) {
                float dmg = (lastHp - hp) / max;
                flash.snap(1f);
                flash.to(0f);
                pulse.impulse(-2.4f - dmg * 6f);
                shake.impulse(45f + dmg * 90f);
                bar.to(frac);
                trail.to(frac, 0.38f);
            } else {
                bar.to(frac);
                if (frac > trail.destination()) {
                    trail.to(frac);
                }
            }
            lastHp = hp;
            name = target.getName().getString();
            skin = PlayerHead.skinOf(target);
        }
        bar.update(dt);
        trail.update(dt);
        swap.update(dt);
        flash.update(dt);
        pulse.update(dt);
        shake.update(dt);
        super.update(dt, sw, sh, editing);
    }

    @Override
    public void paintContent(Canvas c, float x, float y, float w, float h, float dt, boolean editing) {
        float s = 1f + 0.06f * pulse.get();
        float fl = flash.get();
        c.push();
        c.translate(shake.get(), 0f);
        c.scaleAround(x + w * 0.5f, y + h * 0.5f, s, s);
        c.shape(x, y, w, h).radius(11f).fill(0xC80B0914).glass()
                .border(c.px() * 1.1f, Colors.mix(0x4DFFFFFF, 0xCCFF5A6E, fl)).chrome(1f - fl)
                .shadow(0f, 3f + 3f * lift.get(), 9f + 6f * lift.get(), 0.5f)
                .glow(10f + 6f * fl, Colors.mix(Colors.withAlpha(Theme.GLOW, 0.1f), Colors.withAlpha(0xFFFF4D63, 0.55f), fl))
                .sheen(0.04f)
                .draw();
        float hs = 34f;
        float hx = x + 8f;
        float hy = y + 8f;
        float squish = 1f + 0.12f * pulse.get();
        c.push();
        c.scaleAround(hx + hs * 0.5f, hy + hs * 0.5f, 2f - squish, squish);
        int tint = Colors.mix(0xFFFFFFFF, 0xFFFF8A96, fl * 0.8f);
        float sw = Math.max(0f, Math.min(1f, swap.get()));
        if (sw < 0.999f) {
            c.pushAlpha(1f - sw);
            head(c, prevSkin, prevName, hx, hy, hs, tint);
            c.popAlpha();
        }
        c.pushAlpha(sw);
        c.push();
        c.scaleAround(hx + hs * 0.5f, hy + hs * 0.5f, 0.85f + 0.15f * swap.get(), 0.85f + 0.15f * swap.get());
        head(c, skin, name, hx, hy, hs, tint);
        c.pop();
        c.popAlpha();
        c.pop();
        float tx = hx + hs + 9f;
        float right = x + w - 9f;
        float frac = Math.max(0f, Math.min(1f, bar.get()));
        int hpCol = Theme.health(frac);
        c.pushClip(tx, y, right - tx - 34f, h, 0f, 6f);
        if (sw < 0.999f) {
            c.pushAlpha(1f - sw);
            c.text(Font.semibold(), 10.5f).color(Theme.TEXT).drawMid(prevName, tx, y + 17f - sw * 7f);
            c.popAlpha();
        }
        c.pushAlpha(sw);
        c.text(Font.semibold(), 10.5f).color(Theme.TEXT).drawMid(name, tx, y + 17f + (1f - sw) * 7f);
        c.popAlpha();
        c.popClip();
        float hpNum = frac * (target != null ? target.getMaxHealth() : 20f);
        String hpText = String.format("%.1f", hpNum);
        float hpw = c.text(Font.semibold(), 9f).width(hpText);
        c.text(Font.semibold(), 9f).color(Colors.mix(Theme.TEXT, hpCol, 0.6f)).drawMid(hpText, right - hpw, y + 17f);
        Draw.icon(c, '\uE00C', right - hpw - 7f, y + 17f, 8.5f, hpCol, 3f, Colors.withAlpha(hpCol, 0.7f));
        float by = y + 30f;
        float bw = right - tx;
        float bh = 7f;
        c.shape(tx, by, bw, bh).radius(3.5f).fill(0xFF07060C).border(c.px(), 0x1AFFFFFF).draw();
        float tr = Math.max(0f, Math.min(1f, trail.get()));
        if (tr > frac + 0.002f) {
            c.shape(tx, by, bw * tr, bh).radius(3.5f)
                    .fill(Colors.withAlpha(Colors.mix(0xFFFFFFFF, hpCol, 0.35f), 0.42f))
                    .draw();
        }
        if (frac > 0.002f) {
            c.shape(tx, by, Math.max(bh, bw * frac), bh).radius(3.5f)
                    .horizontal(Colors.mix(hpCol, 0xFF000000, 0.25f), hpCol)
                    .glow(6f, Colors.withAlpha(hpCol, 0.55f))
                    .sheen(0.25f)
                    .draw();
        }
        float dist = target != null && MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.distanceTo(target) : 0f;
        c.text(Font.regular(), 7.5f).color(Theme.TEXT_3).drawMid(String.format("%.1f б", dist), tx, y + 44f);
        c.pop();
    }

    private static void head(Canvas c, Identifier skin, String name, float hx, float hy, float hs, int tint) {
        if (!PlayerHead.draw(c, skin, hx, hy, hs, 9f, tint)) {
            c.shape(hx, hy, hs, hs).radius(9f).vertical(0xFF2B2146, 0xFF140F24).border(c.px(), 0x33FFFFFF).draw();
            String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            c.text(Font.bold(), 15f).vertical(0xFFFFFFFF, 0xFFC9BEF5).drawMidCenter(letter, hx + hs * 0.5f, hy + hs * 0.5f);
        }
    }
}
