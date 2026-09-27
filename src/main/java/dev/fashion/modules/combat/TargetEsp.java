package dev.fashion.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import dev.fashion.core.Category;
import dev.fashion.core.Module;
import dev.fashion.core.Modules;
import dev.fashion.core.setting.BoolSetting;
import dev.fashion.core.setting.ModeSetting;
import dev.fashion.core.setting.NumberSetting;

public final class TargetEsp extends Module {
    public final ModeSetting style = mode("Пресет", "Как выглядит метка на цели", 0, "Квадрат", "Кольцо", "Компас");
    public final ModeSetting source = mode("Цель", "Кого отмечать", 2, "Аура", "Прицел", "Обе");
    public final BoolSetting spin = bool("Вращение", "Метка медленно вращается", true);
    public final NumberSetting speed = number("Скорость", "Скорость вращения метки", 1.0, 0.2, 3.0, 0.1, "×")
            .visibleWhen(spin::on);

    public TargetEsp() {
        super("TargetESP", "Вращающаяся метка на цели, чтобы легче наводиться", Category.COMBAT, '');
    }

    public LivingEntity target() {
        if (!enabled() || mc.player == null) {
            return null;
        }
        LivingEntity aura = Modules.attackAura.target();
        if (aura != null && !source.is(1)) {
            return aura;
        }
        if (!source.is(0)) {
            Entity e = mc.targetedEntity;
            if (e instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return null;
    }
}
