package dev.fashion.gfx;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import dev.fashion.FashionClient;

public final class Textures {
    private static final Map<String, GpuTextureView> VIEWS = new HashMap<>();

    private Textures() {
    }

    public static GpuSampler sampler() {
        return RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true);
    }

    public static GpuTextureView get(String path) {
        if (VIEWS.containsKey(path)) {
            return VIEWS.get(path);
        }
        GpuTextureView view = null;
        try {
            view = load(path);
        } catch (Exception e) {
            FashionClient.LOGGER.error("Texture {} failed to load", path, e);
        }
        VIEWS.put(path, view);
        return view;
    }

    private static GpuTextureView load(String path) throws Exception {
        NativeImage image;
        try (InputStream in = MinecraftClient.getInstance().getResourceManager()
                .getResource(Identifier.of(FashionClient.MOD_ID, path)).orElseThrow().getInputStream()) {
            image = NativeImage.read(NativeImage.Format.RGBA, in);
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int mips = 1;
        while ((w >> mips) >= 8 && (h >> mips) >= 8 && mips < 8) {
            mips++;
        }
        GpuDevice device = RenderSystem.getDevice();
        GpuTexture texture = device.createTexture("fashion " + path, GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                TextureFormat.RGBA8, w, h, 1, mips);
        CommandEncoder encoder = device.createCommandEncoder();
        encoder.writeToTexture(texture, image);
        float[] level = new float[w * h * 4];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getColorArgb(x, y);
                float a = (argb >>> 24) / 255f;
                int i = (y * w + x) * 4;
                level[i] = ((argb >> 16) & 255) / 255f * a;
                level[i + 1] = ((argb >> 8) & 255) / 255f * a;
                level[i + 2] = (argb & 255) / 255f * a;
                level[i + 3] = a;
            }
        }
        image.close();
        int cw = w;
        int ch = h;
        for (int mip = 1; mip < mips; mip++) {
            int nw = Math.max(1, cw / 2);
            int nh = Math.max(1, ch / 2);
            float[] next = new float[nw * nh * 4];
            for (int y = 0; y < nh; y++) {
                for (int x = 0; x < nw; x++) {
                    for (int k = 0; k < 4; k++) {
                        int x0 = Math.min(cw - 1, 2 * x);
                        int x1 = Math.min(cw - 1, 2 * x + 1);
                        int y0 = Math.min(ch - 1, 2 * y);
                        int y1 = Math.min(ch - 1, 2 * y + 1);
                        next[(y * nw + x) * 4 + k] = 0.25f * (level[(y0 * cw + x0) * 4 + k] + level[(y0 * cw + x1) * 4 + k]
                                + level[(y1 * cw + x0) * 4 + k] + level[(y1 * cw + x1) * 4 + k]);
                    }
                }
            }
            ByteBuffer buf = MemoryUtil.memAlloc(nw * nh * 4);
            for (int i = 0; i < nw * nh; i++) {
                float a = next[i * 4 + 3];
                float inv = a > 1e-4f ? 1f / a : 0f;
                for (int k = 0; k < 3; k++) {
                    buf.put((byte) Math.round(Math.min(1f, next[i * 4 + k] * inv) * 255f));
                }
                buf.put((byte) Math.round(a * 255f));
            }
            buf.flip();
            encoder.writeToTexture(texture, buf, NativeImage.Format.RGBA, mip, 0, 0, 0, nw, nh);
            MemoryUtil.memFree(buf);
            level = next;
            cw = nw;
            ch = nh;
        }
        return device.createTextureView(texture);
    }
}
