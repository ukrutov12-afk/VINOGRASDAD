package dev.fashion.modules.render;

import dev.fashion.core.Category;
import dev.fashion.core.Module;
import dev.fashion.core.setting.BoolSetting;

public final class Hud extends Module {
    public final BoolSetting watermark = bool("Вотермарка", "Название клиента, FPS и пинг", true);
    public final BoolSetting arrayList = bool("Список функций", "Включённые функции у края экрана", true);
    public final BoolSetting coords = bool("Координаты", "Позиция в мире и в Незере", true);
    public final BoolSetting targetHud = bool("Карточка цели", "Здоровье текущей цели", true);

    public Hud() {
        super("HUD", "Вотермарка, список функций и координаты", Category.RENDER, '\uE033');
    }
}
