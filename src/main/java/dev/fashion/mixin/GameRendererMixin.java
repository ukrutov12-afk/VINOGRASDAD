package dev.fashion.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.fashion.ui.Overlay;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;clear()V"))
    private void fashion$beforeGui(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        Overlay.beforeGui();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;incrementFrame()V"))
    private void fashion$afterGui(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        Overlay.afterGui();
    }
}
