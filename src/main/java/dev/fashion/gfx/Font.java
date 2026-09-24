package dev.fashion.gfx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import dev.fashion.FashionClient;

public final class Font {
    private static final Font INSTANCE = new Font();
    private static final int MIPS = 4;

    public static Font get() {
        return INSTANCE;
    }

    public record Glyph(float advance, float pl, float pb, float pr, float pt,
                       float u0, float v0, float u1, float v1, boolean empty) {
    }

    public static final class Face {
        final Font font;
        final Int2ObjectOpenHashMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();
        final Long2FloatOpenHashMap kerning = new Long2FloatOpenHashMap();
        public float ascender;
        public float descender;
        public float lineHeight;
        public float capHeight;
        public float xHeight;

        Face(Font font) {
            this.font = font;
        }

        public Glyph glyph(int cp) {
            Glyph g = glyphs.get(cp);
            return g != null ? g : glyphs.get('?');
        }

        public float kerning(int a, int b) {
            return kerning.isEmpty() ? 0f : kerning.get(((long) a << 32) | (b & 0xFFFFFFFFL));
        }

        public float width(CharSequence s, float size, float tracking) {
            float w = 0f;
            int prev = -1;
            for (int i = 0; i < s.length(); i++) {
                int cp = s.charAt(i);
                if (prev >= 0) {
                    w += kerning(prev, cp) * size;
                }
                Glyph g = glyph(cp);
                if (g == null) {
                    prev = -1;
                    continue;
                }
                w += g.advance() * size + tracking * size;
                prev = cp;
            }
            return s.length() > 0 ? w - tracking * size : 0f;
        }
    }

    private final Map<String, Face> faces = new HashMap<>();
    private GpuTexture texture;
    private GpuTextureView view;
    private int atlasWidth;
    private int atlasHeight;
    private float em;
    private float spread;
    private boolean ready;
    private boolean failed;

    public static Face regular() {
        return INSTANCE.face("regular");
    }

    public static Face medium() {
        return INSTANCE.face("medium");
    }

    public static Face semibold() {
        return INSTANCE.face("semibold");
    }

    public static Face bold() {
        return INSTANCE.face("bold");
    }

    public static Face display() {
        return INSTANCE.face("display");
    }

    public static Face icons() {
        return INSTANCE.face("icons");
    }

    public Face face(String name) {
        ensure();
        return faces.get(name);
    }

    public boolean ready() {
        ensure();
        return ready;
    }

    public GpuTextureView view() {
        return view;
    }

    public GpuSampler sampler() {
        return RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true);
    }

    public int atlasWidth() {
        return atlasWidth;
    }

    public int atlasHeight() {
        return atlasHeight;
    }

    public float em() {
        return em;
    }

    public float spread() {
        return spread;
    }

    private void ensure() {
        if (ready || failed) {
            return;
        }
        try {
            load();
            ready = true;
        } catch (Exception e) {
            failed = true;
            FashionClient.LOGGER.error("Font atlas failed to load", e);
        }
    }

    private void load() throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();
        Identifier jsonId = Identifier.of(FashionClient.MOD_ID, "sdf/atlas.json");
        Identifier pngId = Identifier.of(FashionClient.MOD_ID, "sdf/atlas.png");
        JsonObject root;
        try (Reader reader = mc.getResourceManager().getResource(jsonId).orElseThrow().getReader()) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonObject atlas = root.getAsJsonObject("atlas");
        atlasWidth = atlas.get("width").getAsInt();
        atlasHeight = atlas.get("height").getAsInt();
        em = atlas.get("em").getAsFloat();
        spread = atlas.get("spread").getAsFloat();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("fonts").entrySet()) {
            faces.put(e.getKey(), parseFace(e.getValue().getAsJsonObject()));
        }
        NativeImage image;
        try (InputStream in = mc.getResourceManager().getResource(pngId).orElseThrow().getInputStream()) {
            image = NativeImage.read(in);
        }
        GpuDevice device = RenderSystem.getDevice();
        texture = device.createTexture("fashion atlas", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                TextureFormat.RGBA8, atlasWidth, atlasHeight, 1, MIPS);
        CommandEncoder encoder = device.createCommandEncoder();
        encoder.writeToTexture(texture, image);
        int w = atlasWidth;
        int h = atlasHeight;
        byte[] level = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                level[y * w + x] = (byte) (image.getColorArgb(x, y) >> 16);
            }
        }
        image.close();
        for (int mip = 1; mip < MIPS; mip++) {
            int nw = Math.max(1, w / 2);
            int nh = Math.max(1, h / 2);
            byte[] next = new byte[nw * nh];
            for (int y = 0; y < nh; y++) {
                for (int x = 0; x < nw; x++) {
                    int s = (level[(2 * y) * w + 2 * x] & 255) + (level[(2 * y) * w + 2 * x + 1] & 255)
                            + (level[(2 * y + 1) * w + 2 * x] & 255) + (level[(2 * y + 1) * w + 2 * x + 1] & 255);
                    next[y * nw + x] = (byte) ((s + 2) >> 2);
                }
            }
            ByteBuffer buf = MemoryUtil.memAlloc(nw * nh * 4);
            for (int i = 0; i < nw * nh; i++) {
                byte v = next[i];
                buf.put(v).put(v).put(v).put(v);
            }
            buf.flip();
            encoder.writeToTexture(texture, buf, NativeImage.Format.RGBA, mip, 0, 0, 0, nw, nh);
            MemoryUtil.memFree(buf);
            level = next;
            w = nw;
            h = nh;
        }
        view = device.createTextureView(texture);
    }

    private Face parseFace(JsonObject o) {
        Face f = new Face(this);
        f.ascender = o.get("ascender").getAsFloat();
        f.descender = o.get("descender").getAsFloat();
        f.lineHeight = o.get("lineHeight").getAsFloat();
        f.capHeight = o.get("capHeight").getAsFloat();
        f.xHeight = o.get("xHeight").getAsFloat();
        for (JsonElement el : o.getAsJsonArray("glyphs")) {
            JsonArray a = el.getAsJsonArray();
            int cp = a.get(0).getAsInt();
            float adv = a.get(1).getAsFloat();
            if (a.size() < 10) {
                f.glyphs.put(cp, new Glyph(adv, 0, 0, 0, 0, 0, 0, 0, 0, true));
                continue;
            }
            float ax = a.get(6).getAsFloat();
            float ay = a.get(7).getAsFloat();
            float aw = a.get(8).getAsFloat();
            float ah = a.get(9).getAsFloat();
            f.glyphs.put(cp, new Glyph(adv,
                    a.get(2).getAsFloat(), a.get(3).getAsFloat(), a.get(4).getAsFloat(), a.get(5).getAsFloat(),
                    ax / atlasWidth, ay / atlasHeight, (ax + aw) / atlasWidth, (ay + ah) / atlasHeight, false));
        }
        for (JsonElement el : o.getAsJsonArray("kerning")) {
            JsonArray a = el.getAsJsonArray();
            f.kerning.put(((long) a.get(0).getAsInt() << 32) | (a.get(1).getAsInt() & 0xFFFFFFFFL), a.get(2).getAsFloat());
        }
        return f;
    }
}
