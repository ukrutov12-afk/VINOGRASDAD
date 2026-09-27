package dev.fashion.ui;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.fashion.anim.Clock;
import dev.fashion.anim.Motion;
import dev.fashion.anim.Scroll;
import dev.fashion.anim.Spring;
import dev.fashion.core.Category;
import dev.fashion.core.Config;
import dev.fashion.core.Module;
import dev.fashion.core.Modules;
import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;
import dev.fashion.gfx.Font;
import dev.fashion.ui.gui.ModuleCard;
import dev.fashion.ui.gui.Row;
import dev.fashion.ui.hud.HudEditorScreen;

public final class ClickGui extends FashionScreen {
    public static final float W = 650f;
    public static final float H = 420f;
    public static final float SIDE = 184f;
    private static final float R = 18f;
    private static final float GAP = 10f;
    private static final float ITEM_H = 32f;
    private static final float HEADER = 70f;

    private static final Map<Module, ModuleCard> CARDS = new LinkedHashMap<>();

    private Category category;
    private final List<ModuleCard> listed = new ArrayList<>();
    private final Scroll scroll = new Scroll();
    private final StringBuilder query = new StringBuilder();
    private boolean focused;
    private final Spring focus = new Spring(0f, Motion.HOVER);
    private final Spring searchHover = new Spring(0f, Motion.HOVER);
    private final Spring clearIn = new Spring(0f, Motion.TOGGLE);
    private final Spring typed = new Spring(0f, Motion.FADE);
    private final Spring caret = new Spring(0f, Motion.FOLLOW);
    private final Spring offX = new Spring(0f, Motion.DRAG);
    private final Spring offY = new Spring(0f, Motion.DRAG);
    private final Spring backdrop = new Spring(0f, Motion.FADE);
    private final Spring indTop = new Spring(0f, Motion.PILL);
    private final Spring indBottom = new Spring(0f, Motion.PILL);
    private final Spring indAlpha = new Spring(1f, Motion.FADE);
    private final Spring[] catActive = new Spring[Category.values().length];
    private final Spring[] catHover = new Spring[Category.values().length];
    private final Spring[] catIn = new Spring[Category.values().length];
    private final Spring[] badgePulse = new Spring[Category.values().length];
    private final Spring[] badgeIn = new Spring[Category.values().length];
    private final int[] badgeCount = new int[Category.values().length];
    private final Spring hudHover = new Spring(0f, Motion.HOVER);
    private final Spring hudPress = new Spring(0f, Motion.PRESS);
    private final Spring[] iconHover = {new Spring(0f, Motion.HOVER), new Spring(0f, Motion.HOVER)};
    private final Spring[] iconPress = {new Spring(0f, Motion.PRESS), new Spring(0f, Motion.PRESS)};
    private final Spring footerIn = new Spring(0f, Motion.ARRIVAL);
    private final Spring emptyIn = new Spring(0f, Motion.FADE);
    private final Spring barAlpha = new Spring(0f, Motion.FADE);
    private final Spring logoSpin = new Spring(-2.4f, 7.5f, 0.55f);
    private final Spring logoEnergy = new Spring(1f, 3.5f, 1f);
    private final Spring logoHover = new Spring(0f, Motion.HOVER);
    private final Spring tipAlpha = new Spring(0f, Motion.FADE);
    private final Spring tipX = new Spring(0f, Motion.FOLLOW);
    private final Spring tipY = new Spring(0f, Motion.FOLLOW);
    private final Spring tipW = new Spring(0f, Motion.FLOW);
    private final FadeText tipText = new FadeText("");
    private Row tipRow;
    private float tipTime;
    private boolean tipShown;
    private final FadeText title;
    private final FadeText subtitle;
    private boolean dragging;
    private float dragMx;
    private float dragMy;
    private float dragX;
    private float dragY;
    private float wx;
    private float wy;
    private float lastIndex = -1f;
    private boolean first = true;
    private boolean logoWasHovered;

    public ClickGui() {
        super("Fashion");
        String saved = Config.ui().has("category") ? Config.ui().get("category").getAsString() : Category.COMBAT.name();
        category = Category.COMBAT;
        for (Category c : Category.values()) {
            if (c.name().equals(saved)) {
                category = c;
            }
        }
        for (int i = 0; i < catActive.length; i++) {
            catActive[i] = new Spring(Category.values()[i] == category ? 1f : 0f, Motion.HOVER);
            catHover[i] = new Spring(0f, Motion.HOVER);
            catIn[i] = new Spring(0f, Motion.ARRIVAL);
            catIn[i].to(1f, 0.1f + i * 0.045f);
            badgePulse[i] = new Spring(0f, Motion.PRESS);
            badgeIn[i] = new Spring(0f, Motion.TOGGLE);
            badgeCount[i] = -1;
        }
        footerIn.to(1f, 0.3f);
        backdrop.to(1f);
        logoSpin.to(0f);
        logoEnergy.to(0f);
        for (Module m : Modules.all()) {
            CARDS.computeIfAbsent(m, ModuleCard::new);
        }
        for (ModuleCard card : CARDS.values()) {
            card.listed = false;
            card.placed = false;
            card.presence.snap(0f);
        }
        title = new FadeText(category.title);
        subtitle = new FadeText("");
    }

    public float windowX() {
        return wx;
    }

    public float windowY() {
        return wy;
    }

    public float uiScale() {
        return scale;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    protected boolean fullyClosed() {
        return backdrop.get() < 0.01f;
    }

    private boolean searching() {
        return !query.isEmpty();
    }

    private List<ModuleCard> wanted() {
        List<ModuleCard> out = new ArrayList<>();
        String q = query.toString().toLowerCase(Locale.ROOT).trim();
        for (Module m : Modules.all()) {
            if (q.isEmpty()) {
                if (m.category() == category) {
                    out.add(CARDS.get(m));
                }
            } else if (m.name().toLowerCase(Locale.ROOT).contains(q)
                    || m.description().toLowerCase(Locale.ROOT).contains(q)
                    || m.category().title.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(CARDS.get(m));
            }
        }
        return out;
    }

    private void syncList() {
        List<ModuleCard> want = wanted();
        if (want.equals(listed) && !first) {
            return;
        }
        int leaving = 0;
        for (ModuleCard card : listed) {
            if (!want.contains(card)) {
                card.listed = false;
                card.presence.motion(Motion.DEPART);
                card.presence.to(0f, leaving * 0.018f);
                leaving++;
            }
        }
        float base = first ? 0.18f : leaving > 0 ? 0.06f : 0f;
        int arriving = 0;
        for (ModuleCard card : want) {
            if (!card.listed) {
                card.listed = true;
                if (card.presence.get() < 0.05f) {
                    card.placed = false;
                }
                card.presence.motion(Motion.ARRIVAL);
                card.presence.to(1f, base + arriving * 0.045f);
                arriving++;
            }
        }
        listed.clear();
        listed.addAll(want);
        first = false;
    }

    private void layoutCards(float cw, float dt) {
        float colW = (cw - GAP) * 0.5f;
        float rowY = 0f;
        float animY = 0f;
        int rank = 0;
        int i = 0;
        while (i < listed.size()) {
            ModuleCard a = listed.get(i);
            ModuleCard b = i + 1 < listed.size() ? listed.get(i + 1) : null;
            boolean pair = b != null && !a.expanded() && !b.expanded();
            if (pair) {
                place(a, 0f, animY, colW, cw, rank, dt);
                place(b, colW + GAP, animY, colW, cw, rank, dt);
                float hT = Math.max(a.targetHeight(colW), b.targetHeight(colW));
                float hA = Math.max(a.h.get(), b.h.get());
                rowY += hT + GAP;
                animY = Math.max(a.y.get(), b.y.get()) + hA + GAP;
                i += 2;
            } else {
                place(a, 0f, animY, cw, cw, rank, dt);
                rowY += a.targetHeight(cw) + GAP;
                animY = a.y.get() + a.h.get() + GAP;
                i++;
            }
            rank++;
        }
        contentBottom = rowY;
        for (ModuleCard card : CARDS.values()) {
            if (!card.listed && card.alive()) {
                card.update(dt, card.w.get());
            }
        }
    }

    private float contentBottom;

    private void place(ModuleCard card, float tx, float ty, float tw, float full, int rank, float dt) {
        card.x.to(tx);
        card.y.to(ty);
        card.w.to(tw);
        card.scrollLag.motion(Math.max(15f, 34f - rank * 2.4f), 0.8f);
        card.scrollLag.to(scroll.offset);
        if (!card.placed) {
            card.x.snap(tx);
            card.y.snap(ty);
            card.w.snap(tw);
            card.h.snap(card.targetHeight(tw));
            card.scrollLag.snap(scroll.offset);
            card.placed = true;
        }
        card.update(dt, tw);
    }

    @Override
    protected void paint(Canvas c, float dt, float sw, float sh) {
        syncList();
        backdrop.to(closing ? 0f : 1f);
        backdrop.update(dt);
        float bd = backdrop.get();
        c.pushAlpha(bd);
        c.shape(0f, 0f, sw, sh).fill(Theme.DIM).backdrop().draw();
        c.popAlpha();
        c.shape(0f, 0f, sw, sh).vertical(Colors.withAlpha(0xFF05040A, 0.05f * bd), Colors.withAlpha(0xFF05040A, 0.3f * bd)).draw();

        if (dragging) {
            float nx = dragX + (mouseX - dragMx);
            float ny = dragY + (mouseY - dragMy);
            float lx = (sw - W) * 0.5f - 8f;
            float ly = (sh - H) * 0.5f - 8f;
            offX.to(Math.max(-lx, Math.min(lx, nx)));
            offY.to(Math.max(-ly, Math.min(ly, ny)));
        }
        offX.update(dt);
        offY.update(dt);

        float p = open.get();
        wx = (sw - W) * 0.5f + offX.get();
        wy = (sh - H) * 0.5f + offY.get() + (1f - p) * 42f;
        float vx = offX.velocity;
        float vy = offY.velocity - 42f * open.velocity;
        float speed = (float) Math.hypot(vx, vy);
        float sx = 1f;
        float sy = 1f;
        if (speed > 1f) {
            float amount = Math.min(speed * 0.00003f, 0.032f);
            float ax = Math.abs(vx) / speed;
            float ay = Math.abs(vy) / speed;
            sx = 1f + amount * (ax - 0.55f * ay);
            sy = 1f + amount * (ay - 0.55f * ax);
        }
        float sc = 0.9f + 0.1f * p;
        c.push();
        c.scaleAround(wx + W * 0.5f, wy + H * 0.5f, sc * sx, sc * sy);
        c.pushAlpha(Math.max(0f, Math.min(1f, p * 1.5f)));
        paintWindow(c, dt);
        c.popAlpha();
        c.pop();
        paintTip(c, dt);
    }

    private void paintWindow(Canvas c, float dt) {
        float px = c.px();
        c.shape(wx, wy, W, H).radius(R).fill(Theme.WINDOW).glass().clouds(1f)
                .shadow(0f, 22f, 50f, 0.7f)
                .border(px * 1.3f, 0x5CFFFFFF)
                .chrome(1f)
                .glow(34f, Colors.withAlpha(Theme.GLOW, 0.1f))
                .draw();
        c.shape(wx, wy, SIDE, H).radii(R, 0f, 0f, R).horizontal(Theme.SIDEBAR, Colors.mulAlpha(Theme.SIDEBAR, 0.55f)).draw();
        c.shape(wx + SIDE, wy + 14f, px, H - 28f).fill(0x1EFFFFFF).draw();
        c.shape(wx + SIDE + px, wy + 14f, px, H - 28f).fill(0x66000000).draw();
        paintBrand(c, dt);
        paintCategories(c, dt);
        paintFooter(c, dt);
        paintHeader(c, dt);
        paintContent(c, dt);
    }

    private void paintBrand(Canvas c, float dt) {
        float lx = wx + 36f;
        float ly = wy + 40f;
        boolean over = Ui.inside(mouseX, mouseY, wx + 10f, wy + 10f, SIDE - 20f, 62f);
        if (over && !logoWasHovered) {
            logoSpin.impulse(5.5f);
            logoEnergy.snap(Math.max(logoEnergy.get(), 0.8f));
        }
        logoWasHovered = over;
        logoHover.to(over ? 1f : 0f);
        logoHover.update(dt);
        logoSpin.update(dt);
        logoEnergy.update(dt);
        if (Math.abs(logoSpin.velocity) < 0.02f && Math.abs(logoSpin.get()) > (float) Math.PI) {
            float turns = (float) (Math.round(logoSpin.get() / (Math.PI / 2)) * (Math.PI / 2));
            logoSpin.snap(logoSpin.get() - turns);
        }
        float sway = (float) Math.sin(Clock.time() * 0.45f) * 0.05f;
        float sz = 46f * (1f + 0.04f * logoHover.get());
        Logo.draw(c, lx, ly, sz, logoSpin.get() + sway, Math.max(logoEnergy.get(), 0.35f * logoHover.get()));
        c.text(Font.display(), 13f).color(0xFFFFFFFF).chrome(1f).tracking(0.07f)
                .glow(3f, Colors.withAlpha(0xFFB9A8FF, 0.3f)).drawMid("FASHION", wx + 66f, ly - 5f);
        c.text(Font.semibold(), 7f).color(Theme.TEXT_3).tracking(0.22f).drawMid("STORM EDITION", wx + 67f, ly + 10f);
        Draw.hairline(c, wx + 14f, wy + 78f, SIDE - 28f, 0x24FFFFFF);
    }

    private void paintCategories(Canvas c, float dt) {
        float x = wx + 10f;
        float w = SIDE - 20f;
        float y0 = wy + 108f;
        c.text(Font.semibold(), 7f).color(Theme.TEXT_3).tracking(0.2f).drawMid("РАЗДЕЛЫ", x + 12f, wy + 94f);
        Category[] cats = Category.values();
        int idx = category.ordinal();
        if (lastIndex < 0f) {
            indTop.snap(y0 + idx * ITEM_H - wy);
            indBottom.snap(y0 + idx * ITEM_H + ITEM_H - wy);
        } else if (lastIndex != idx) {
            boolean down = idx > lastIndex;
            indTop.motion(down ? 13f : 26f, down ? 0.8f : 0.62f);
            indBottom.motion(down ? 26f : 13f, down ? 0.62f : 0.8f);
        }
        lastIndex = idx;
        indTop.to(y0 + idx * ITEM_H - wy);
        indBottom.to(y0 + idx * ITEM_H + ITEM_H - wy);
        indTop.update(dt);
        indBottom.update(dt);
        indAlpha.to(searching() ? 0.25f : 1f);
        indAlpha.update(dt);
        float it = wy + indTop.get();
        float ib = wy + indBottom.get();
        float inA = Math.max(0f, Math.min(1f, catIn[idx].get()));
        c.pushAlpha(indAlpha.get() * inA);
        c.shape(x, it, w, ib - it).radius(9f)
                .horizontal(0xFF211A38, 0xFF15121F)
                .border(c.px() * 1.1f, Colors.withAlpha(Theme.GLOW_SILVER, 0.45f))
                .chrome(0.8f)
                .shadow(0f, 3f, 8f, 0.5f)
                .glow(10f, Colors.withAlpha(Theme.GLOW, 0.2f))
                .draw();
        float barH = Math.max(4f, ib - it - 16f);
        c.shape(x + 1.5f, it + (ib - it - barH) * 0.5f, 2.5f, barH).radius(1.25f).vertical(0xFFFFFFFF, Theme.ACCENT_HI)
                .glow(6f, Colors.withAlpha(Theme.GLOW, 0.9f)).draw();
        c.popAlpha();
        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            float iy = y0 + i * ITEM_H;
            boolean over = Ui.inside(mouseX, mouseY, x, iy, w, ITEM_H);
            catHover[i].to(over ? 1f : 0f);
            catActive[i].to(cat == category && !searching() ? 1f : 0f);
            catHover[i].update(dt);
            catActive[i].update(dt);
            catIn[i].update(dt);
            float act = catActive[i].get();
            float hv = catHover[i].get();
            float in = catIn[i].get();
            c.push();
            c.translate((1f - in) * -14f + hv * 1.5f, 0f);
            c.pushAlpha(Math.max(0f, Math.min(1f, in)));
            if (hv > 0.01f) {
                c.shape(x, iy, w, ITEM_H).radius(9f).fill(Colors.withAlpha(0xFFFFFFFF, 0.03f * hv * (1f - act))).draw();
            }
            float cy = iy + ITEM_H * 0.5f;
            int iconCol = Colors.mix(Colors.mix(Theme.TEXT_3, Theme.TEXT_2, hv), 0xFFFFFFFF, act);
            Draw.icon(c, cat.icon, x + 18f, cy, 13f, iconCol, 4f * act, Colors.withAlpha(Theme.GLOW_SILVER, 0.7f * act));
            c.text(Font.medium(), 10.5f).color(Colors.mix(Theme.TEXT_OFF, Theme.TEXT, Math.max(act, hv * 0.7f))).drawMid(cat.title, x + 34f, cy);
            int count = 0;
            for (Module m : Modules.of(cat)) {
                if (m.enabled()) {
                    count++;
                }
            }
            if (badgeCount[i] >= 0 && count != badgeCount[i]) {
                badgePulse[i].impulse(6f);
            }
            badgeCount[i] = count;
            badgeIn[i].to(count > 0 ? 1f : 0f);
            badgeIn[i].update(dt);
            badgePulse[i].update(dt);
            float bi = badgeIn[i].get();
            if (bi > 0.01f) {
                String n = String.valueOf(count);
                float bw = Math.max(15f, Font.semibold().width(n, 8f, 0f) + 9f);
                float bx = x + w - 10f - bw;
                float bs = Math.max(0f, bi) * (1f + 0.09f * badgePulse[i].get());
                c.push();
                c.scaleAround(bx + bw * 0.5f, cy, bs, bs);
                c.shape(bx, cy - 7f, bw, 14f).radius(7f)
                        .fill(Colors.withAlpha(Theme.ACCENT, 0.14f + 0.2f * act))
                        .border(c.px(), Colors.withAlpha(Theme.ACCENT_HI, 0.25f + 0.3f * act))
                        .draw();
                c.text(Font.semibold(), 8f).color(Colors.mix(Theme.TEXT_2, Theme.ACCENT_HI, 0.5f + 0.5f * act)).drawMidCenter(n, bx + bw * 0.5f, cy);
                c.pop();
            }
            c.popAlpha();
            c.pop();
        }
    }

    private float hudX() {
        return wx + 10f;
    }

    private float hudY() {
        return wy + H - 98f;
    }

    private float iconButtonX(int i) {
        return wx + SIDE - 12f - 24f - i * 28f;
    }

    private float iconButtonY() {
        return wy + H - 49f;
    }

    private void paintFooter(Canvas c, float dt) {
        footerIn.update(dt);
        hudPress.update(dt);
        float fin = footerIn.get();
        float x = hudX();
        float y = hudY();
        float w = SIDE - 20f;
        boolean over = Ui.inside(mouseX, mouseY, x, y, w, 32f);
        hudHover.to(over ? 1f : 0f);
        hudHover.update(dt);
        float hv = hudHover.get();
        c.pushAlpha(Math.max(0f, Math.min(1f, fin)));
        c.push();
        c.translate(0f, (1f - fin) * 14f - hv * 1.5f);
        float s = 1f + 0.015f * hv - 0.03f * hudPress.get();
        c.scaleAround(x + w * 0.5f, y + 16f, s, s);
        c.shape(x, y, w, 32f).radius(10f)
                .vertical(Colors.mix(Theme.CARD_TOP, Theme.CARD_HOVER_TOP, hv), Theme.CARD_BOTTOM)
                .border(c.px() * 1.1f, Colors.mix(0x26FFFFFF, Colors.withAlpha(Theme.GLOW_SILVER, 0.6f), hv))
                .chrome(0.8f)
                .shadow(0f, 2f + 3f * hv, 7f + 6f * hv, 0.45f)
                .glow(10f, Colors.withAlpha(Theme.GLOW, 0.24f * hv))
                .draw();
        Draw.icon(c, '', x + 17f, y + 16f, 13f, Colors.mix(Theme.TEXT_2, 0xFFFFFFFF, hv));
        c.text(Font.medium(), 10f).color(Colors.mix(Theme.TEXT_2, Theme.TEXT, hv)).drawMid("Редактор HUD", x + 32f, y + 16f);
        c.push();
        c.rotateAround(x + w - 14f + 2f * hv, y + 16f, (float) (-Math.PI * 0.5));
        Draw.icon(c, '', x + w - 14f + 2f * hv, y + 16f, 8f, Colors.withAlpha(Theme.TEXT_3, 0.9f));
        c.pop();
        c.pop();

        float py = wy + H - 58f;
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        boolean drew = mc.player != null && PlayerHead.draw(c, PlayerHead.skinOf(mc.player), x + 4f, py + 7f, 28f, 8f, 0xFFFFFFFF);
        if (!drew) {
            c.shape(x + 4f, py + 7f, 28f, 28f).radius(8f).vertical(0xFF2A2145, 0xFF151027).draw();
        }
        c.shape(x + 27f, py + 30f, 7f, 7f).radius(3.5f).fill(0xFF55E39B).border(c.px() * 1.5f, 0xFF0A0812)
                .glow(4f, Colors.withAlpha(0xFF55E39B, 0.6f)).draw();
        c.pushClip(x + 28f, py - 8f, iconButtonX(1) - x - 32f, 60f, 0f, 10f);
        c.text(Font.semibold(), 10f).color(Theme.TEXT).drawMid(name, x + 40f, py + 16f);
        c.text(Font.regular(), 8f).color(Theme.TEXT_3).drawMid("конфиг · default", x + 40f, py + 28f);
        c.popClip();
        char[] icons = {'', ''};
        for (int i = 0; i < 2; i++) {
            float bx = iconButtonX(i);
            float by = iconButtonY();
            boolean ov = Ui.inside(mouseX, mouseY, bx, by, 24f, 24f);
            iconHover[i].to(ov ? 1f : 0f);
            iconHover[i].update(dt);
            iconPress[i].update(dt);
            float ih = iconHover[i].get();
            float is = 1f + 0.05f * ih - 0.08f * iconPress[i].get();
            c.push();
            c.scaleAround(bx + 12f, by + 12f, is, is);
            c.translate(0f, -1.2f * ih);
            c.shape(bx, by, 24f, 24f).radius(7f)
                    .vertical(Colors.mix(Theme.CARD_TOP, Theme.CARD_HOVER_TOP, ih), Theme.CARD_BOTTOM)
                    .border(c.px(), Colors.mix(0x22FFFFFF, Colors.withAlpha(Theme.GLOW_SILVER, 0.6f), ih))
                    .shadow(0f, 1.5f + 2f * ih, 4f + 4f * ih, 0.4f)
                    .glow(7f, Colors.withAlpha(Theme.GLOW, 0.25f * ih))
                    .draw();
            Draw.icon(c, icons[i], bx + 12f, by + 12f, 12f, Colors.mix(Theme.TEXT_3, 0xFFFFFFFF, ih));
            c.pop();
        }
        c.popAlpha();
    }

    private void paintHeader(Canvas c, float dt) {
        float hx = wx + SIDE + 24f;
        int total = listed.size();
        int active = 0;
        for (ModuleCard card : listed) {
            if (card.module.enabled()) {
                active++;
            }
        }
        title.set(searching() ? "Поиск" : category.title);
        String sub = total + " " + Draw.plural(total, "функция", "функции", "функций") + " · " + active + " "
                + Draw.plural(active, "активна", "активны", "активно");
        if (total == 0) {
            sub = "Совпадений нет";
        }
        subtitle.set(sub);
        title.update(dt);
        subtitle.update(dt);
        paintFade(c, title, Font.bold(), 17f, hx, wy + 30f, 0xFFFFFFFF, 0xFFCBC3EC, 9f);
        paintFade(c, subtitle, Font.regular(), 9f, hx, wy + 48f, Theme.TEXT_2, Theme.TEXT_2, 5f);

        float sw = 190f;
        float sx = wx + W - 20f - sw;
        float sy = wy + 21f;
        float sh = 28f;
        searchHover.to(Ui.inside(mouseX, mouseY, sx, sy, sw, sh) ? 1f : 0f);
        focus.to(focused ? 1f : 0f);
        typed.to(searching() ? 1f : 0f);
        clearIn.to(searching() ? 1f : 0f);
        searchHover.update(dt);
        focus.update(dt);
        typed.update(dt);
        clearIn.update(dt);
        float f = focus.get();
        float hv = searchHover.get();
        c.shape(sx, sy, sw, sh).radius(9f)
                .vertical(Colors.mix(Theme.INSET, 0xFF0E0A1B, f), Colors.mix(Theme.INSET_TOP, 0xFF120D24, f))
                .border(c.px() * 1.1f, Colors.mix(Colors.mix(0x1CFFFFFF, 0x33FFFFFF, hv), Colors.withAlpha(Theme.ACCENT_HI, 0.62f), f))
                .glow(9f, Colors.withAlpha(Theme.GLOW, 0.3f * f))
                .draw();
        c.shape(sx + 1f, sy + 1f, sw - 2f, 5f).radii(8f, 8f, 0f, 0f).vertical(0x40000000, 0x00000000).draw();
        Draw.icon(c, '', sx + 15f, sy + sh * 0.5f, 11f, Colors.mix(Theme.TEXT_3, Theme.ACCENT_HI, f), 3f * f, Colors.withAlpha(Theme.GLOW, 0.6f * f));
        float tx = sx + 28f;
        float ty = sy + sh * 0.5f;
        c.pushClip(sx + 24f, sy, sw - 44f, sh, 0f);
        float ph = 1f - typed.get();
        if (ph > 0.01f) {
            c.pushAlpha(ph);
            c.text(Font.regular(), 10f).color(Theme.TEXT_3).drawMid("Поиск функций", tx + (1f - ph) * 8f, ty);
            c.popAlpha();
        }
        String q = query.toString();
        float qw = Font.regular().width(q, 10f, 0f);
        float shift = Math.max(0f, qw - (sw - 52f));
        c.text(Font.regular(), 10f).color(Theme.TEXT).drawMid(q, tx - shift, ty);
        caret.to(tx - shift + qw + 1f);
        caret.update(dt);
        float blink = 0.5f + 0.5f * (float) Math.cos(Clock.time() * 5.2f);
        float ca = f * (0.35f + 0.65f * blink);
        if (ca > 0.01f) {
            c.shape(caret.get(), ty - 6.5f, 1.3f, 13f).radius(0.65f).fill(Colors.withAlpha(Theme.ACCENT_HI, ca))
                    .glow(4f, Colors.withAlpha(Theme.GLOW, 0.6f * ca)).draw();
        }
        c.popClip();
        float ci = clearIn.get();
        if (ci > 0.01f) {
            float cx = sx + sw - 14f;
            float cy = sy + sh * 0.5f;
            boolean overClear = Ui.inside(mouseX, mouseY, cx - 8f, cy - 8f, 16f, 16f);
            c.push();
            c.scaleAround(cx, cy, Math.max(0f, ci), Math.max(0f, ci));
            c.shape(cx - 7f, cy - 7f, 14f, 14f).radius(7f).fill(Colors.withAlpha(0xFFFFFFFF, overClear ? 0.14f : 0.07f)).draw();
            Draw.icon(c, '', cx, cy, 8f, Theme.TEXT_2);
            c.pop();
        }
        Draw.hairline(c, wx + SIDE + 16f, wy + 66f, W - SIDE - 32f, 0x26FFFFFF);
    }

    private void paintFade(Canvas c, FadeText text, Font.Face face, float size, float x, float cy, int top, int bottom, float travel) {
        float p = text.progress();
        if (text.previous() != null) {
            c.pushAlpha(1f - Math.min(1f, p));
            c.text(face, size).vertical(top, bottom).drawMid(text.previous(), x, cy - p * travel);
            c.popAlpha();
        }
        c.pushAlpha(Math.max(0f, Math.min(1f, p)));
        c.text(face, size).vertical(top, bottom).drawMid(text.current(), x, cy + (1f - p) * travel);
        c.popAlpha();
    }

    private float contentX() {
        return wx + SIDE + 16f;
    }

    private float contentY() {
        return wy + HEADER + 6f;
    }

    private float contentW() {
        return W - SIDE - 32f;
    }

    private float contentH() {
        return H - HEADER - 6f;
    }

    private void paintContent(Canvas c, float dt) {
        float cx = contentX();
        float cy = contentY();
        float cw = contentW();
        float ch = contentH();
        layoutCards(cw, dt);
        scroll.bounds(contentBottom + 8f, ch - 4f);
        scroll.update(dt);
        c.pushClip(cx - 12f, cy - 6f, cw + 24f, ch + 6f, R, 14f);
        boolean interactive = !dragging && !closing;
        for (ModuleCard card : CARDS.values()) {
            if (card.alive()) {
                card.paint(c, cx, cy, mouseX, mouseY, dt, interactive && card.listed && Ui.inside(mouseX, mouseY, cx - 12f, cy, cw + 24f, ch));
            }
        }
        c.popClip();
        emptyIn.to(listed.isEmpty() ? 1f : 0f);
        emptyIn.update(dt);
        float e = emptyIn.get();
        if (e > 0.01f) {
            c.pushAlpha(e);
            float mid = cy + ch * 0.42f + (1f - e) * 10f;
            Draw.icon(c, '', cx + cw * 0.5f, mid - 14f, 22f, Theme.TEXT_3, 6f, Colors.withAlpha(Theme.GLOW, 0.4f));
            c.text(Font.medium(), 10.5f).color(Theme.TEXT_2).drawMidCenter("Ничего не найдено", cx + cw * 0.5f, mid + 12f);
            c.popAlpha();
        }
        float max = scroll.max();
        barAlpha.to(max > 1f && (scroll.activity() > 0.05f || Ui.inside(mouseX, mouseY, cx, cy, cw + 14f, ch)) ? 1f : 0f);
        barAlpha.update(dt);
        float ba = barAlpha.get();
        if (ba > 0.01f && max > 1f) {
            float trackH = ch - 16f;
            float content = max + ch;
            float thumb = Math.max(22f, trackH * (ch / content));
            float over = scroll.overscroll();
            float squash = Math.min(Math.abs(over) * 0.6f, thumb * 0.45f);
            float frac = Math.max(0f, Math.min(1f, scroll.offset / max));
            float ty = cy + 4f + (trackH - thumb) * frac;
            float th = thumb - squash;
            if (over > 0f) {
                ty += squash;
            }
            c.shape(wx + W - 9f, ty, 3f, th).radius(1.5f).fill(Colors.withAlpha(Theme.ACCENT_HI, 0.45f * ba))
                    .glow(4f, Colors.withAlpha(Theme.GLOW, 0.4f * ba)).draw();
        }
    }

    private void paintTip(Canvas c, float dt) {
        Row row = null;
        ModuleCard owner = null;
        if (!dragging && !closing) {
            for (ModuleCard card : listed) {
                Row r = card.rowAt(mouseX, mouseY);
                if (r != null && r.tip() != null) {
                    row = r;
                    owner = card;
                    break;
                }
            }
        }
        if (row != null && row == tipRow) {
            tipTime += dt;
        } else {
            tipRow = row;
            tipTime = 0f;
        }
        boolean show = tipRow != null && tipTime > 0.5f;
        float a = tipAlpha.get();
        if (show) {
            tipText.set(tipRow.tip());
            String t = tipText.current();
            float tw = Font.regular().width(t, 9f, 0f) + 22f;
            float lo = tipRow.left() - 4f;
            float hi = tipRow.left() + tipRow.width() + 4f - tw;
            float tx = Math.max(lo, Math.min(hi, mouseX + 14f));
            float ty = tipRow.labelY() - 9f;
            if (a < 0.05f) {
                tipX.snap(tx);
                tipY.snap(ty);
                tipW.snap(tw);
            }
            tipX.to(tx);
            tipY.to(ty);
            tipW.to(tw);
            tipShown = true;
        }
        tipAlpha.to(show ? 1f : 0f);
        tipAlpha.update(dt);
        tipX.update(dt);
        tipY.update(dt);
        tipW.update(dt);
        tipText.update(dt);
        a = tipAlpha.get();
        if (a < 0.01f || !tipShown) {
            return;
        }
        float x = tipX.get();
        float y = tipY.get();
        float w = tipW.get();
        float s = 0.92f + 0.08f * a;
        c.push();
        c.scaleAround(x, y + 9f, s, s);
        c.pushAlpha(a);
        c.shape(x, y, w, 18f).radius(9f).vertical(0xFA17141F, 0xFA0F0D16)
                .border(c.px(), 0x3DFFFFFF).chrome(0.7f)
                .shadow(0f, 4f, 12f, 0.65f)
                .glow(6f, Colors.withAlpha(Theme.GLOW, 0.14f))
                .draw();
        c.pushClip(x + 4f, y, w - 8f, 18f, 0f, 4f);
        float p = tipText.progress();
        if (tipText.previous() != null) {
            c.pushAlpha(1f - Math.min(1f, p));
            c.text(Font.regular(), 9f).color(Theme.TEXT_2).drawMid(tipText.previous(), x + 11f, y + 9f);
            c.popAlpha();
        }
        c.pushAlpha(Math.max(0f, Math.min(1f, p)));
        c.text(Font.regular(), 9f).color(Theme.TEXT_2).drawMid(tipText.current(), x + 11f, y + 9f);
        c.popAlpha();
        c.popClip();
        c.popAlpha();
        c.pop();
    }

    private ModuleCard listeningCard() {
        for (ModuleCard card : CARDS.values()) {
            if (card.bind().listening()) {
                return card;
            }
        }
        return null;
    }

    @Override
    protected boolean onMouseDown(float mx, float my, int button) {
        ModuleCard listening = listeningCard();
        float sw = 190f;
        float sx = wx + W - 20f - sw;
        float sy = wy + 21f;
        if (Ui.inside(mx, my, sx, sy, sw, 28f)) {
            if (searching() && mx > sx + sw - 24f) {
                query.setLength(0);
            }
            focused = true;
            return true;
        }
        focused = false;
        float x = wx + 10f;
        float y0 = wy + 108f;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            if (Ui.inside(mx, my, x, y0 + i * ITEM_H, SIDE - 20f, ITEM_H)) {
                if (cats[i] != category || searching()) {
                    category = cats[i];
                    query.setLength(0);
                    scroll.reset();
                    Config.ui().addProperty("category", category.name());
                    Config.markDirty();
                }
                return true;
            }
        }
        if (Ui.inside(mx, my, hudX(), hudY(), SIDE - 20f, 32f)) {
            hudPress.impulse(-4f);
            MinecraftClient.getInstance().setScreen(new HudEditorScreen());
            return true;
        }
        for (int i = 0; i < 2; i++) {
            if (Ui.inside(mx, my, iconButtonX(i), iconButtonY(), 24f, 24f)) {
                iconPress[i].impulse(-5f);
                if (i == 0) {
                    Config.load();
                } else {
                    Config.saveNow();
                }
                return true;
            }
        }
        if (Ui.inside(mx, my, wx + 10f, wy + 10f, SIDE - 20f, 62f)) {
            logoSpin.impulse(9f);
            logoEnergy.snap(1f);
            return true;
        }
        if (Ui.inside(mx, my, contentX() - 12f, contentY(), contentW() + 24f, contentH())) {
            for (int i = listed.size() - 1; i >= 0; i--) {
                ModuleCard card = listed.get(i);
                if (card.contains(mx, my)) {
                    if (listening != null && listening != card) {
                        listening.bind().stop();
                    }
                    return card.mouseDown(mx, my, button);
                }
            }
        }
        if (listening != null) {
            listening.bind().stop();
        }
        if (button == 0 && Ui.inside(mx, my, wx + SIDE, wy, W - SIDE, 64f) || button == 0 && Ui.inside(mx, my, wx, wy, SIDE, 80f)) {
            dragging = true;
            dragMx = mx;
            dragMy = my;
            dragX = offX.target;
            dragY = offY.target;
            return true;
        }
        return Ui.inside(mx, my, wx, wy, W, H);
    }

    @Override
    protected boolean onMouseUp(float mx, float my, int button) {
        dragging = false;
        for (ModuleCard card : listed) {
            card.mouseUp(mx, my);
        }
        return true;
    }

    @Override
    protected boolean onScroll(float mx, float my, float amount) {
        if (Ui.inside(mx, my, contentX() - 12f, contentY(), contentW() + 24f, contentH())) {
            scroll.wheel(amount);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onKey(int key, int modifiers) {
        ModuleCard listening = listeningCard();
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                listening.bind().stop();
            } else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                listening.bind().assign(-1);
            } else {
                listening.bind().assign(key);
            }
            return true;
        }
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && key == GLFW.GLFW_KEY_F) {
            focused = true;
            return true;
        }
        if (!focused) {
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (ctrl) {
                    query.setLength(0);
                } else if (!query.isEmpty()) {
                    query.setLength(query.length() - 1);
                }
                scroll.reset();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (searching()) {
                    query.setLength(0);
                } else {
                    focused = false;
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                focused = false;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected boolean onChar(int codepoint) {
        if (listeningCard() != null) {
            return true;
        }
        if (!focused) {
            if (!Character.isLetterOrDigit(codepoint)) {
                return false;
            }
            focused = true;
        }
        if (query.length() < 24 && !Character.isISOControl(codepoint)) {
            query.appendCodePoint(codepoint);
            scroll.reset();
        }
        return true;
    }

    public void typeForTest(String s) {
        focused = true;
        query.setLength(0);
        query.append(s);
    }

    public void selectForTest(Category c) {
        category = c;
        query.setLength(0);
        scroll.reset();
    }

    public void expandForTest(String module, boolean e) {
        for (ModuleCard card : CARDS.values()) {
            if (card.module.name().equals(module)) {
                card.setExpanded(e);
            }
        }
    }

    public void listenForTest(String module) {
        for (ModuleCard card : CARDS.values()) {
            if (card.module.name().equals(module)) {
                card.bind().listen();
            }
        }
    }

    public void keyForTest(int key) {
        onKey(key, 0);
    }

    public void scrollForTest(float notches) {
        scroll.wheel(notches);
    }

    public void dragForTest(float dx, float dy) {
        offX.to(offX.target + dx);
        offY.to(offY.target + dy);
    }
}
