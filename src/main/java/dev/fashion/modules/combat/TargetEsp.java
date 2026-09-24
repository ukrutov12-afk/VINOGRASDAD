package dev.fashion.modules.combat;

import dev.fashion.core.Category;
import dev.fashion.core.Module;
import dev.fashion.core.setting.BoolSetting;
import dev.fashion.core.setting.ModeSetting;

public final class TargetEsp extends Module {
    public final ModeSetting style = mode("Стиль", "Как выделять цель", 0, "Уголки", "Кольцо", "Звезда");
    public final BoolSetting spin = bool("Вращение", "Медленно вращать метку", true);

    public TargetEsp() {
        super("TargetESP", "Подсвечивает цель, на которую навелась аура", Category.COMBAT);
    }
}
