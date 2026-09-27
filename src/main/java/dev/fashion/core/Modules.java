package dev.fashion.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.fashion.modules.combat.AttackAura;
import dev.fashion.modules.combat.TargetEsp;
import dev.fashion.modules.misc.Placeholder;
import dev.fashion.modules.movement.AutoSprint;
import dev.fashion.modules.player.ClickPearl;
import dev.fashion.modules.render.Hud;
import dev.fashion.modules.render.NotificationsModule;

public final class Modules {
    private static final List<Module> ALL = new ArrayList<>();
    public static boolean ready;

    public static AttackAura attackAura;
    public static TargetEsp targetEsp;
    public static Hud hud;
    public static NotificationsModule notifications;

    private Modules() {
    }

    public static void init() {
        attackAura = register(new AttackAura());
        targetEsp = register(new TargetEsp());
        register(new AutoSprint());
        hud = register(new Hud());
        notifications = register(new NotificationsModule());
        register(new ClickPearl());
        register(new Placeholder());
        hud.setEnabled(true);
        notifications.setEnabled(true);
        ready = true;
    }

    private static <M extends Module> M register(M m) {
        ALL.add(m);
        return m;
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<Module> of(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) {
            if (m.category() == c) {
                out.add(m);
            }
        }
        return out;
    }

    public static Module byName(String name) {
        for (Module m : ALL) {
            if (m.name().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public static void tick() {
        for (Module m : ALL) {
            if (m.enabled()) {
                m.onTick();
            }
        }
    }
}
