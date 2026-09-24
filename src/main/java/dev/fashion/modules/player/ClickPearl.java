package dev.fashion.modules.player;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import dev.fashion.core.Category;
import dev.fashion.core.Module;
import dev.fashion.core.setting.ModeSetting;

public final class ClickPearl extends Module {
    private static final int[] BUTTONS = {GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GLFW.GLFW_MOUSE_BUTTON_4, GLFW.GLFW_MOUSE_BUTTON_5};

    private final ModeSetting button = mode("Кнопка", "Какой кнопкой мыши бросать", 0, "Колесо", "Боковая 4", "Боковая 5");
    private boolean wasDown;

    public ClickPearl() {
        super("ClickPearl", "Бросает эндер-жемчуг по клику из любого слота хотбара", Category.PLAYER);
    }

    @Override
    public String suffix() {
        return button.mode();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), BUTTONS[button.index()]) == GLFW.GLFW_PRESS;
        boolean click = down && !wasDown;
        wasDown = down;
        if (!click || mc.currentScreen != null) {
            return;
        }
        int slot = findPearl();
        if (slot < 0) {
            return;
        }
        ItemStack stack = mc.player.getInventory().getStack(slot);
        if (mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            return;
        }
        int previous = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(previous);
    }

    private int findPearl() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                return i;
            }
        }
        return -1;
    }
}
