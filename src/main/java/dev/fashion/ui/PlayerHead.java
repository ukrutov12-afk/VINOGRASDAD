package dev.fashion.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import dev.fashion.gfx.Canvas;
import dev.fashion.gfx.Colors;

public final class PlayerHead {
    private PlayerHead() {
    }

    public static Identifier skinOf(Entity e) {
        if (e instanceof ClientPlayerLikeEntity p) {
            return p.getSkin().body().texturePath();
        }
        return null;
    }

    public static boolean draw(Canvas c, Identifier skin, float x, float y, float size, float radius, int tint) {
        if (skin == null) {
            return false;
        }
        AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(skin);
        if (tex == null || tex.getGlTextureView() == null) {
            return false;
        }
        var view = tex.getGlTextureView();
        var sampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
        float u = 1f / 64f;
        c.shape(x, y, size, size).radius(radius).fill(tint).image(view, sampler, 8 * u, 8 * u, 16 * u, 16 * u).draw();
        float o = size * 0.035f;
        c.shape(x - o, y - o, size + 2 * o, size + 2 * o).radius(radius + o).fill(tint).image(view, sampler, 40 * u, 8 * u, 48 * u, 16 * u).draw();
        c.shape(x, y, size, size).radius(radius).fill(0).border(c.px(), Colors.withAlpha(0xFFFFFFFF, 0.14f * Colors.alpha(tint) / 255f)).draw();
        return true;
    }
}
