package dev.fashion.gfx;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public final class Blur {
    private static final int MAX_LEVELS = 6;

    private static SimpleFramebuffer[] levels = new SimpleFramebuffer[0];
    private static SimpleFramebuffer light;
    private static GpuBuffer uniforms;
    private static int stride;
    private static int sourceW;
    private static int sourceH;
    private static int levelCount;
    private static long signature;
    private static boolean ready;
    private static int recomputes;
    private static int reuses;

    private Blur() {
    }

    public static boolean ready() {
        return ready;
    }

    public static GpuTextureView texture() {
        return ready ? levels[0].getColorAttachmentView() : null;
    }

    public static int recomputes() {
        return recomputes;
    }

    public static int reuses() {
        return reuses;
    }

    public static GpuTextureView light() {
        return ready ? light.getColorAttachmentView() : null;
    }

    public static void invalidate() {
        signature = 0L;
    }

    public static void update(Framebuffer source) {
        int w = source.textureWidth;
        int h = source.textureHeight;
        int count = h >= 1300 ? 6 : h >= 700 ? 5 : 4;
        long sig = signature(w, h, count);
        if (ready && sig == signature) {
            reuses++;
            return;
        }
        ensure(w, h, count);
        GpuSampler linear = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        writeUniforms(encoder, w, h);
        GpuTextureView prev = source.getColorAttachmentView();
        for (int i = 0; i < levelCount; i++) {
            pass(encoder, Pipelines.BLUR_DOWN, prev, levels[i], linear, i);
            prev = levels[i].getColorAttachmentView();
        }
        pass(encoder, Pipelines.BLUR_UP, levels[1].getColorAttachmentView(), light, linear, MAX_LEVELS * 2 - 1);
        for (int i = levelCount - 1; i > 0; i--) {
            pass(encoder, Pipelines.BLUR_UP, levels[i].getColorAttachmentView(), levels[i - 1], linear, levelCount + (levelCount - 1 - i));
        }
        signature = sig;
        ready = true;
        recomputes++;
    }

    private static long signature(int w, int h, int count) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long s = 1469598103934665603L;
        s = mix(s, w);
        s = mix(s, h);
        s = mix(s, count);
        if (mc.world == null || !mc.isPaused()) {
            s = mix(s, System.nanoTime());
            return s;
        }
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d p = cam.getCameraPos();
        s = mix(s, Double.doubleToLongBits(p.x));
        s = mix(s, Double.doubleToLongBits(p.y));
        s = mix(s, Double.doubleToLongBits(p.z));
        s = mix(s, Float.floatToIntBits(cam.getYaw()));
        s = mix(s, Float.floatToIntBits(cam.getPitch()));
        s = mix(s, mc.world.getTime());
        return s;
    }

    private static long mix(long s, long v) {
        s ^= v;
        s *= 1099511628211L;
        return s ^ (s >>> 29);
    }

    private static void ensure(int w, int h, int count) {
        if (w == sourceW && h == sourceH && count == levelCount && levels.length == count) {
            return;
        }
        for (SimpleFramebuffer fb : levels) {
            fb.delete();
        }
        if (light != null) {
            light.delete();
        }
        levels = new SimpleFramebuffer[count];
        int lw = w;
        int lh = h;
        for (int i = 0; i < count; i++) {
            lw = Math.max(1, lw / 2);
            lh = Math.max(1, lh / 2);
            levels[i] = new SimpleFramebuffer("fashion blur " + i, lw, lh, false);
        }
        light = new SimpleFramebuffer("fashion blur light", levels[0].textureWidth, levels[0].textureHeight, false);
        sourceW = w;
        sourceH = h;
        levelCount = count;
        ready = false;
    }

    private static void writeUniforms(CommandEncoder encoder, int w, int h) {
        if (uniforms == null) {
            stride = Math.max(16, RenderSystem.getDevice().getUniformOffsetAlignment());
            uniforms = RenderSystem.getDevice().createBuffer(() -> "fashion blur passes",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, (long) stride * MAX_LEVELS * 2);
        }
        for (int i = 0; i < levelCount; i++) {
            int srcW = i == 0 ? w : levels[i - 1].textureWidth;
            int srcH = i == 0 ? h : levels[i - 1].textureHeight;
            write(encoder, i, 1.0f / srcW, 1.0f / srcH, 0f, 1f);
        }
        for (int i = levelCount - 1; i > 0; i--) {
            int slot = levelCount + (levelCount - 1 - i);
            float o = 0.5f + 0.35f * (levelCount - 1 - i) / Math.max(1, levelCount - 1);
            boolean last = i == 1;
            write(encoder, slot, o / levels[i].textureWidth, o / levels[i].textureHeight, last ? 1f : 0f, 0.9f);
        }
        write(encoder, MAX_LEVELS * 2 - 1, 0.75f / levels[1].textureWidth, 0.75f / levels[1].textureHeight, 0f, 1f);
    }

    private static void write(CommandEncoder encoder, int slot, float tx, float ty, float tone, float gain) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, 16).putVec4(tx, ty, tone, gain).get();
            encoder.writeToBuffer(uniforms.slice((long) slot * stride, 16), data);
        }
    }

    private static void pass(CommandEncoder encoder, RenderPipeline pipeline, GpuTextureView source,
                             Framebuffer target, GpuSampler sampler, int slot) {
        try (RenderPass pass = encoder.createRenderPass(() -> "fashion blur", target.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            pass.bindTexture("Source", source, sampler);
            pass.setUniform("BlurPass", uniforms.slice((long) slot * stride, 16));
            pass.draw(0, 3);
        }
    }
}
