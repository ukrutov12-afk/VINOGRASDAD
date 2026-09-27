package dev.fashion.modules.combat;

import net.minecraft.entity.LivingEntity;

import dev.fashion.core.Category;
import dev.fashion.core.Module;
import dev.fashion.core.Modules;
import dev.fashion.core.setting.ModeSetting;
import dev.fashion.core.setting.NumberSetting;

public final class TargetEsp extends Module {
    public final ModeSetting texture = mode("Текстура", "Какая текстура крутится на цели", 0, "Ромб", "Маркер");
    public final NumberSetting size = number("Размер", "Размер текстуры на экране", 150, 60, 260, 5, "");

    public TargetEsp() {
        super("TargetESP", "Крутящаяся текстура на цели ауры", Category.RENDER, '');
    }

    public LivingEntity target() {
        return enabled() ? Modules.attackAura.target() : null;
    }
}
