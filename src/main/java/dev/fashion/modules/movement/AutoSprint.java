package dev.fashion.modules.movement;

import dev.fashion.core.Category;
import dev.fashion.core.Module;

public final class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Держит бег, пока персонаж идёт вперёд", Category.MOVEMENT, '\uE032');
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        if (mc.player.input.hasForwardMovement() && !mc.player.horizontalCollision && !mc.player.isSneaking()
                && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
    }
}
