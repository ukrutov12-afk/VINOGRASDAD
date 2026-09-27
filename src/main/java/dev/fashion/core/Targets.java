package dev.fashion.core;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.PlayerLikeEntity;

import java.util.List;

import dev.fashion.core.setting.MultiSetting;

public final class Targets {
    public static final List<String> KINDS = List.of("Игроки", "Мобы");

    private Targets() {
    }

    public static MultiSetting setting(String description) {
        return new MultiSetting("Цели", description, KINDS, true, true);
    }

    public static boolean accepts(MultiSetting filter, LivingEntity e) {
        if (e == null) {
            return false;
        }
        if (e instanceof PlayerLikeEntity) {
            return filter.has(0);
        }
        return filter.has(1);
    }
}
