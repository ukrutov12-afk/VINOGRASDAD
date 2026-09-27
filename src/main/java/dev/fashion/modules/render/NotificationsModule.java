package dev.fashion.modules.render;

import java.util.List;

import dev.fashion.core.Category;
import dev.fashion.core.Module;
import dev.fashion.core.setting.ModeSetting;
import dev.fashion.core.setting.MultiSetting;
import dev.fashion.core.setting.NumberSetting;
import dev.fashion.ui.hud.Notifications;

public final class NotificationsModule extends Module {
    public final ModeSetting corner = mode("Угол экрана", "Куда прижата стопка уведомлений", 0,
            "Справа снизу", "Справа сверху", "Слева снизу", "Слева сверху");
    public final NumberSetting max = number("Максимум", "Сколько карточек видно одновременно", 4, 1, 8, 1, "");
    public final NumberSetting duration = number("Время показа", "Сколько секунд висит уведомление", 3.5, 1, 10, 0.5, " с");
    public final MultiSetting events = multi("События", "О чём сообщать",
            List.of("Функции", "Бинды", "Конфиг", "Ошибки"), true, true, true, true);

    public NotificationsModule() {
        super("Notifications", "Всплывающие карточки о включении функций, биндах и конфиге", Category.RENDER, '');
        corner.onChange(() -> {
            Notifications.snapToCorner(corner.index());
            dev.fashion.core.Config.markDirty();
        });
    }

    public boolean accepts(int event) {
        return enabled() && events.has(event);
    }
}
