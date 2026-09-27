package dev.fashion.core;

import dev.fashion.ui.Theme;
import dev.fashion.ui.hud.Notifications;

public final class Events {
    private Events() {
    }

    public static void toggled(Module m) {
        if (!Modules.ready || !Modules.notifications.accepts(0)) {
            return;
        }
        boolean on = m.enabled();
        Notifications.post("module:" + m.name(), m.name(), on ? "включена" : "выключена", m.icon(),
                on ? Theme.ACCENT_HI : Theme.TEXT_3);
    }

    public static void bind(Module m) {
        if (!Modules.ready || !Modules.notifications.accepts(1)) {
            return;
        }
        String sub = m.key() >= 0 ? "клавиша " + Keys.name(m.key()) : "бинд снят";
        Notifications.post("bind:" + m.name(), m.name(), sub, '', Theme.SILVER);
    }

    public static void config(boolean load, boolean ok) {
        if (!Modules.ready || !Modules.notifications.accepts(2)) {
            return;
        }
        Notifications.post(load ? "config:load" : "config:save", load ? "Конфиг загружен" : "Конфиг сохранён",
                "config/fashion.json", load ? '' : '', Theme.OK);
    }

    public static void error(String title, String detail) {
        if (!Modules.ready || !Modules.notifications.accepts(3)) {
            return;
        }
        Notifications.post("error:" + title, title, detail, '', Theme.DANGER);
    }
}
