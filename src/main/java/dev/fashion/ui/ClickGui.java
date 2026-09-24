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
import dev.fashion.ui.hud.HudEditorScreen;

public final class ClickGui extends FashionScreen {
    public static final float W = 620f;
    public static final float H = 400f;
    public static final float SIDE = 172f;
    private static final float R = 18f;
    private static final float GAP = 10f;
    private static final float ITEM_H = 31f;

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
    private final Spring footerIn = new Spring(0f, Motion.ARRIVAL);
    private final Spring emptyIn = new Spring(0f, Motion.FADE);
    private final Spring barAlpha = new Spring(0f, Motion.FADE);
    private final Spring tipAlpha = new Spring(0f, Motion.FADE);
    private final Spring tipX = new Spring(0f, Motion.FOLLOW);
    private final Spring tipY = new Spring(0f, Motion.FOLLOW);
    private String tipText;
    private String tipShown;
    private float tipTime;
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

    public static float windowWidth() {
        return W;
    }

    public static float windowHeight() {
        return H;
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
        float base = first ? 0.16f : leaving > 0 ? 0.06f : 0f;
        int arriving = 0;
        for (ModuleCard card : want) {
            if (!card.listed) {
                card.listed = true;
                if (card.presence.get() < 0.05f) {
                    card.placed = false;
                }
                card.presence.motion(Motion.ARRIVAL);
                card.presence.to(1f, base + arriving * 0.04f);
                arriving++;
            }
        }
        listed.clear();
        listed.addAll(want);
        first = false;
    }

    private void layoutCards(float colW, float dt) {
        ModuleCard[] last = new ModuleCard[2];
        int[] rank = new int[2];
        for (int i = 0; i < listed.size(); i++) {
            ModuleCard card = listed.get(i);
            int col = i % 2;
            float tx = col * (colW + GAP);
            float ty = last[col] == null ? 0f : last[col].y.get() + last[col].h.get() + GAP;
            card.x.to(tx);
            card.y.to(ty);
            card.scrollLag.motion(Math.max(15f, 34f - rank[col] * 2.2f), 0.8f);
            card.scrollLag.to(scroll.offset);
            card.update(dt, colW);
            if (!card.placed) {
                card.x.snap(tx);
                card.y.snap(ty);
                card.scrollLag.snap(scroll.offset);
                card.placed = true;
            }
            last[col] = card;
            rank[col]++;
        }
        for (ModuleCard card : CARDS.values()) {
            if (!card.listed && card.alive()) {
                card.update(dt, colW);
            }
        }
    }

    private float contentHeight() {
        float max = 0f;
        for (ModuleCard card : listed) {
            max = Math.max(max, card.y.target + card.h.target);
        }
        return max;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    protected boolean fullyClosed() {
        return backdrop.get() < 0.01f;
    }

    @Override
    protected void paint(Canvas c, float dt, float sw, float sh) {
        syncList();
        backdrop.to(closing ? 0f : 1f);
        backdrop.update(dt);
        float bd = backdrop.get();
        c.shape(0f, 0f, sw, sh).vertical(Colors.withAlpha(0xFF05040A, 0.18f * bd), Colors.withAlpha(0xFF05040A, 0.42f * bd)).draw();

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

    private void paintTip(Canvas c, float dt) {
        String tip = null;
        if (!dragging && !closing) {
            for (ModuleCard card : listed) {
                String t = card.tipAt(mouseX, mouseY);
                if (t != null) {
                    tip = t;
                    break;
                }
            }
        }
        if (tip != null && tip.equals(tipText)) {
            tipTime += dt;
        } else {
            tipText = tip;
            tipTime = 0f;
        }
        boolean show = tipText != null && tipTime > 0.55f;
        if (show && !tipText.equals(tipShown)) {
            if (tipAlpha.get() < 0.05f) {
                tipX.snap(mouseX + 12f);
                tipY.snap(mouseY + 16f);
            }
            tipShown = tipText;
        }
        tipAlpha.to(show ? 1f : 0f);
        tipX.to(mouseX + 12f);
        tipY.to(mouseY + 16f);
        tipAlpha.update(dt);
        tipX.update(dt);
        tipY.update(dt);
        float a = tipAlpha.get();
        if (a < 0.01f || tipShown == null) {
            return;
        }
        java.util.List<String> lines = Ui.wrap(Font.regular(), 9f, tipShown, 190f, 3);
        float w = 0f;
        for (String l : lines) {
            w = Math.max(w, Font.regular().width(l, 9f, 0f));
        }
        w += 20f;
        float h = 12f + lines.size() * 12f;
        float x = tipX.get();
        float y = tipY.get() + (1f - a) * 6f;
        c.pushAlpha(a);
        c.shape(x, y, w, h).radius(8f).fill(0xE00C0A16).glass().border(c.px(), 0x40FFFFFF).chrome(1f)
                .shadow(0f, 4f, 12f, 0.5f).glow(8f, Colors.withAlpha(Theme.GLOW, 0.14f)).draw();
        for (int i = 0; i < lines.size(); i++) {
            c.text(Font.regular(), 9f).color(Theme.TEXT_2).drawMid(lines.get(i), x + 10f, y + 12f + i * 12f);
        }
        c.popAlpha();
    }

    private void paintWindow(Canvas c, float dt) {
        float px = c.px();
        c.shape(wx, wy, W, H).radius(R).fill(Theme.WINDOW).glass()
                .shadow(0f, 20f, 48f, 0.62f)
                .border(px * 1.25f, 0x66FFFFFF)
                .chrome(1f)
                .glow(30f, Colors.withAlpha(Theme.GLOW, 0.09f))
                .draw();
        c.shape(wx, wy, SIDE, H).radii(R, 0f, 0f, R)
                .horizontal(Colors.withAlpha(0xFF030208, 0.42f), Colors.withAlpha(0xFF030208, 0.12f)).draw();
        paintAmbience(c);
        Draw.vHairline(c, wx + SIDE, wy + 16f, H - 32f, 0x2EFFFFFF);
        paintBrand(c, dt);
        paintCategories(c, dt);
        paintFooter(c, dt);
        paintHeader(c, dt);
        paintContent(c, dt);
    }

    private void paintAmbience(Canvas c) {
        float t = Clock.time();
        c.pushClip(wx, wy, W, H, R);
        c.shape(wx, wy + H * 0.45f, W, H * 0.55f).vertical(0x00050409, 0x5C050409).draw();
        float lx = wx + 70f + 12f * (float) Math.sin(t * 0.21f);
        float ly = wy + 28f + 6f * (float) Math.cos(t * 0.17f);
        c.shape(lx - 40f, ly - 16f, 80f, 32f).radius(16f).fill(0).glow(90f, Colors.withAlpha(0xFF8C7BFF, 0.075f)).draw();
        c.shape(wx + W - 150f, wy - 40f, 120f, 40f).radius(20f).fill(0).glow(80f, Colors.withAlpha(0xFF5B4FC4, 0.06f)).draw();
        c.popClip();
    }

    private void paintBrand(Canvas c, float dt) {
        float bx = wx + 10f;
        float by = wy + 10f;
        float bw = SIDE - 20f;
        float bh = 62f;
        c.shape(bx, by, bw, bh).radius(12f).vertical(0xFF110D22, 0xFF06050C).storm(Theme.CLOUD)
                .border(c.px(), 0x2EFFFFFF).chrome(0.9f).shadow(0f, 2f, 6f, 0.35f).draw();
        float t = Clock.time();
        float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.3f);
        float starX = bx + 25f;
        float starY = by + bh * 0.5f;
        c.shape(starX - 5f, starY - 5f, 10f, 10f).radius(5f).fill(0x00FFFFFF)
                .glow(15f + 4f * tw, Colors.withAlpha(0xFFBCB0FF, 0.5f + 0.15f * tw)).draw();
        c.push();
        c.rotateAround(starX, starY, (float) Math.sin(t * 0.35f) * 0.12f);
        float ss = 18f + 0.8f * tw;
        Draw.chromeIcon(c, '\uE006', starX, starY, ss, 3.5f, Colors.withAlpha(0xFFFFFFFF, 0.75f));
        c.pop();
        c.text(Font.display(), 12.5f).color(0xFFFFFFFF).chrome(1f).tracking(0.07f)
                .glow(3f, Colors.withAlpha(0xFFB9A8FF, 0.35f)).drawMid("FASHION", bx + 45f, starY - 6f);
        c.text(Font.semibold(), 7.5f).color(Theme.TEXT_3).tracking(0.18f).drawMid("STORM · 2.0", bx + 45f, starY + 9f);
    }

    private void paintCategories(Canvas c, float dt) {
        float x = wx + 10f;
        float w = SIDE - 20f;
        float y0 = wy + 104f;
        c.text(Font.semibold(), 7.5f).color(Theme.TEXT_3).tracking(0.16f).drawMid("РАЗДЕЛЫ", x + 12f, wy + 90f);
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
        indAlpha.to(searching() ? 0.3f : 1f);
        indAlpha.update(dt);
        float it = wy + indTop.get();
        float ib = wy + indBottom.get();
        float inA = Math.max(0f, Math.min(1f, catIn[idx].get()));
        c.pushAlpha(indAlpha.get() * inA);
        c.shape(x, it, w, ib - it).radius(9f)
                .horizontal(Colors.withAlpha(Theme.ACCENT, 0.26f), Colors.withAlpha(Theme.ACCENT, 0.04f))
                .border(c.px(), Colors.withAlpha(Theme.ACCENT_HI, 0.3f))
                .glow(10f, Colors.withAlpha(Theme.GLOW, 0.16f))
                .draw();
        float barH = Math.max(4f, ib - it - 16f);
        c.shape(x + 1.5f, it + (ib - it - barH) * 0.5f, 2.5f, barH).radius(1.25f).fill(Theme.ACCENT_HI)
                .glow(6f, Colors.withAlpha(Theme.GLOW, 0.85f)).draw();
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
                c.shape(x, iy, w, ITEM_H).radius(9f).fill(Colors.withAlpha(0xFFFFFFFF, 0.035f * hv * (1f - act))).draw();
            }
            float cy = iy + ITEM_H * 0.5f;
            int iconCol = Colors.mix(Colors.mix(Theme.TEXT_3, Theme.TEXT_2, hv), Theme.ACCENT_HI, act);
            Draw.icon(c, cat.icon, x + 17f, cy, 12f, iconCol, 4f * act, Colors.withAlpha(Theme.GLOW, 0.7f * act));
            c.text(Font.medium(), 10.5f).color(Colors.mix(Theme.TEXT_2, Theme.TEXT, Math.max(act, hv * 0.7f))).drawMid(cat.title, x + 32f, cy);
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
                        .fill(Colors.withAlpha(Theme.ACCENT, 0.16f + 0.2f * act))
                        .border(c.px(), Colors.withAlpha(Theme.ACCENT_HI, 0.25f + 0.25f * act))
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
        return wy + H - 94f;
    }

    private void paintFooter(Canvas c, float dt) {
        footerIn.update(dt);
        hudPress.update(dt);
        float fin = footerIn.get();
        float x = hudX();
        float y = hudY();
        float w = SIDE - 20f;
        boolean over = Ui.inside(mouseX, mouseY, x, y, w, 30f);
        hudHover.to(over ? 1f : 0f);
        hudHover.update(dt);
        float hv = hudHover.get();
        c.push();
        c.translate(0f, (1f - fin) * 14f - hv * 1.5f);
        c.pushAlpha(Math.max(0f, Math.min(1f, fin)));
        float s = 1f + 0.015f * hv - 0.03f * hudPress.get();
        c.scaleAround(x + w * 0.5f, y + 15f, s, s);
        c.shape(x, y, w, 30f).radius(9f)
                .vertical(Colors.withAlpha(0xFF1C1730, 0.7f + 0.3f * hv), Colors.withAlpha(0xFF0E0B18, 0.8f))
                .border(c.px(), Colors.mix(0x22FFFFFF, Colors.withAlpha(Theme.ACCENT_HI, 0.5f), hv))
                .chrome(0.6f)
                .shadow(0f, 2f + 2f * hv, 6f + 5f * hv, 0.35f)
                .glow(9f, Colors.withAlpha(Theme.GLOW, 0.22f * hv))
                .draw();
        Draw.icon(c, '\uE007', x + 16f, y + 15f, 12f, Colors.mix(Theme.TEXT_2, Theme.ACCENT_HI, hv));
        c.text(Font.medium(), 10f).color(Colors.mix(Theme.TEXT_2, Theme.TEXT, hv)).drawMid("Редактор HUD", x + 30f, y + 15f);
        c.push();
        c.rotateAround(x + w - 14f + 2f * hv, y + 15f, (float) (-Math.PI * 0.5));
        Draw.icon(c, '\uE008', x + w - 14f + 2f * hv, y + 15f, 8f, Colors.withAlpha(Theme.TEXT_3, 0.9f));
        c.pop();
        c.pop();

        float py = wy + H - 54f;
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        boolean drew = mc.player != null && PlayerHead.draw(c, PlayerHead.skinOf(mc.player), x + 4f, py + 6f, 28f, 8f, 0xFFFFFFFF);
        if (!drew) {
            c.shape(x + 4f, py + 6f, 28f, 28f).radius(8f).vertical(0xFF2A2145, 0xFF151027).draw();
        }
        c.shape(x + 27f, py + 29f, 7f, 7f).radius(3.5f).fill(0xFF55E39B).border(c.px() * 1.5f, 0xFF0A0812)
                .glow(4f, Colors.withAlpha(0xFF55E39B, 0.6f)).draw();
        c.text(Font.semibold(), 10.5f).color(Theme.TEXT).drawMid(name, x + 42f, py + 15f);
        c.text(Font.regular(), 8.5f).color(Theme.TEXT_3).drawMid("в игре · Fashion", x + 42f, py + 27f);
        c.popAlpha();
    }

    private void paintHeader(Canvas c, float dt) {
        float hx = wx + SIDE + 22f;
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
        paintFade(c, title, Font.bold(), 16.5f, hx, wy + 27f, 0xFFFFFFFF, 0xFFCFC5F7, 9f);
        paintFade(c, subtitle, Font.regular(), 9f, hx, wy + 45f, Theme.TEXT_2, Theme.TEXT_2, 5f);

        float sw = 180f;
        float sx = wx + W - 18f - sw;
        float sy = wy + 18f;
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
                .vertical(Colors.mix(0xFF09080F, 0xFF0F0B1D, f), Colors.mix(0xFF0B0A13, 0xFF120D24, f))
                .border(c.px() * 1.1f, Colors.mix(Colors.mix(0x1CFFFFFF, 0x33FFFFFF, hv), Colors.withAlpha(Theme.ACCENT_HI, 0.62f), f))
                .glow(9f, Colors.withAlpha(Theme.GLOW, 0.3f * f))
                .draw();
        Draw.icon(c, '\uE005', sx + 15f, sy + sh * 0.5f, 11f, Colors.mix(Theme.TEXT_3, Theme.ACCENT_HI, f), 3f * f, Colors.withAlpha(Theme.GLOW, 0.6f * f));
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
            Draw.icon(c, '\uE00A', cx, cy, 8f, Theme.TEXT_2);
            c.pop();
        }
        Draw.hairline(c, wx + SIDE + 14f, wy + 62f, W - SIDE - 28f, 0x2AFFFFFF);
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
        return wx + SIDE + 14f;
    }

    private float contentY() {
        return wy + 70f;
    }

    private float contentW() {
        return W - SIDE - 28f;
    }

    private float contentH() {
        return H - 70f;
    }

    private void paintContent(Canvas c, float dt) {
        float cx = contentX();
        float cy = contentY();
        float cw = contentW();
        float ch = contentH();
        float colW = (cw - GAP) * 0.5f;
        scroll.bounds(contentHeight() + 12f, ch - 4f);
        scroll.update(dt);
        layoutCards(colW, dt);
        c.pushClip(cx - 12f, cy - 7f, cw + 24f, ch + 7f, R, 14f);
        boolean interactive = !dragging && !closing;
        for (ModuleCard card : CARDS.values()) {
            if (card.alive()) {
                card.paint(c, cx, cy, colW, mouseX, mouseY, dt, interactive && card.listed && Ui.inside(mouseX, mouseY, cx - 12f, cy, cw + 24f, ch));
            }
        }
        c.popClip();
        emptyIn.to(listed.isEmpty() ? 1f : 0f);
        emptyIn.update(dt);
        float e = emptyIn.get();
        if (e > 0.01f) {
            c.pushAlpha(e);
            float mid = cy + ch * 0.42f + (1f - e) * 10f;
            Draw.icon(c, '\uE005', cx + cw * 0.5f, mid - 14f, 22f, Theme.TEXT_3, 6f, Colors.withAlpha(Theme.GLOW, 0.4f));
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
        float sw = 180f;
        float sx = wx + W - 18f - sw;
        float sy = wy + 18f;
        if (Ui.inside(mx, my, sx, sy, sw, 28f)) {
            if (searching() && mx > sx + sw - 24f) {
                query.setLength(0);
            }
            focused = true;
            return true;
        }
        focused = false;
        float x = wx + 10f;
        float y0 = wy + 104f;
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
        if (Ui.inside(mx, my, hudX(), hudY(), SIDE - 20f, 30f)) {
            hudPress.impulse(-4f);
            MinecraftClient.getInstance().setScreen(new HudEditorScreen());
            return true;
        }
        if (Ui.inside(mx, my, contentX() - 12f, contentY(), contentW() + 24f, contentH())) {
            for (int i = listed.size() - 1; i >= 0; i--) {
                ModuleCard card = listed.get(i);
                if (card.contains(mx, my)) {
                    if (listening != null && listening != card) {
                        listening.bind().stopListening();
                    }
                    return card.mouseDown(mx, my, button);
                }
            }
        }
        if (listening != null) {
            listening.bind().stopListening();
        }
        if (button == 0 && Ui.inside(mx, my, wx, wy, W, 64f) || button == 0 && Ui.inside(mx, my, wx, wy, SIDE, 80f)) {
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
                listening.bind().stopListening();
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

    public void scrollForTest(float notches) {
        scroll.wheel(notches);
    }

    public void dragForTest(float dx, float dy) {
        offX.to(offX.target + dx);
        offY.to(offY.target + dy);
    }
}
