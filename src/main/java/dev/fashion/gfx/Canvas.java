package dev.fashion.gfx;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public final class Canvas {
    private static final Canvas INSTANCE = new Canvas();
    private static final float NO_CLIP = 1.0e6f;

    public static Canvas get() {
        return INSTANCE;
    }

    private ByteBuffer data = MemoryUtil.memAlloc(UiVertex.SIZE * 4 * 1024);
    private int quads;
    private final List<Batch> batches = new ArrayList<>();
    private GpuBuffer vertexBuffer;
    private long vertexCapacity;
    private GpuBuffer frameUniform;

    private final Matrix3x2f[] mats = new Matrix3x2f[64];
    private int depth;
    private final float[][] clips = new float[32][6];
    private int clipDepth;
    private final float[] alphas = new float[32];
    private int alphaDepth;
    private float alpha = 1f;
    private float scale = 1f;
    private int width;
    private int height;
    private boolean usedGlass;

    private final Shape shape = new Shape();
    private final Text text = new Text();

    private static final class Batch {
        final int first;
        int count;
        final GpuTextureView image;
        final GpuSampler sampler;

        Batch(int first, GpuTextureView image, GpuSampler sampler) {
            this.first = first;
            this.image = image;
            this.sampler = sampler;
        }
    }

    private Canvas() {
        for (int i = 0; i < mats.length; i++) {
            mats[i] = new Matrix3x2f();
        }
        if (UiVertex.SIZE != 124) {
            throw new IllegalStateException("Unexpected vertex size " + UiVertex.SIZE);
        }
    }

    public void begin(int fbWidth, int fbHeight, float uiScale) {
        width = fbWidth;
        height = fbHeight;
        scale = uiScale;
        quads = 0;
        batches.clear();
        data.clear();
        depth = 0;
        mats[0].identity().scale(uiScale, uiScale);
        clipDepth = 0;
        float[] c = clips[0];
        c[0] = -NO_CLIP;
        c[1] = -NO_CLIP;
        c[2] = NO_CLIP;
        c[3] = NO_CLIP;
        c[4] = 0f;
        c[5] = 0f;
        alphaDepth = 0;
        alpha = 1f;
        usedGlass = false;
    }

    public float uiScale() {
        return scale;
    }

    public float logicalWidth() {
        return width / scale;
    }

    public float logicalHeight() {
        return height / scale;
    }

    public int framebufferWidth() {
        return width;
    }

    public int framebufferHeight() {
        return height;
    }

    public boolean usedGlass() {
        return usedGlass;
    }

    public Matrix3x2f matrix() {
        return mats[depth];
    }

    public float currentScale() {
        Matrix3x2f m = mats[depth];
        return (float) Math.sqrt(Math.abs(m.m00 * m.m11 - m.m01 * m.m10));
    }

    public float px() {
        return 1f / Math.max(currentScale(), 1e-4f);
    }

    public void push() {
        mats[depth + 1].set(mats[depth]);
        depth++;
    }

    public void pop() {
        depth--;
    }

    public void translate(float x, float y) {
        mats[depth].translate(x, y);
    }

    public void scale(float sx, float sy) {
        mats[depth].scale(sx, sy);
    }

    public void scaleAround(float cx, float cy, float sx, float sy) {
        Matrix3x2f m = mats[depth];
        m.translate(cx, cy);
        m.scale(sx, sy);
        m.translate(-cx, -cy);
    }

    public void rotateAround(float cx, float cy, float radians) {
        Matrix3x2f m = mats[depth];
        m.translate(cx, cy);
        m.rotate(radians);
        m.translate(-cx, -cy);
    }

    public void pushAlpha(float a) {
        alphas[alphaDepth++] = alpha;
        alpha *= Math.max(0f, Math.min(1f, a));
    }

    public void popAlpha() {
        alpha = alphas[--alphaDepth];
    }

    public float alpha() {
        return alpha;
    }

    public void pushClip(float x, float y, float w, float h, float r) {
        pushClip(x, y, w, h, r, 0f);
    }

    public void pushClip(float x, float y, float w, float h, float r, float feather) {
        Matrix3x2f m = mats[depth];
        float ax = m.m00 * x + m.m10 * y + m.m20;
        float ay = m.m01 * x + m.m11 * y + m.m21;
        float bx = m.m00 * (x + w) + m.m10 * (y + h) + m.m20;
        float by = m.m01 * (x + w) + m.m11 * (y + h) + m.m21;
        float[] p = clips[clipDepth];
        float[] c = clips[++clipDepth];
        float x0 = Math.min(ax, bx);
        float y0 = Math.min(ay, by);
        float x1 = Math.max(ax, bx);
        float y1 = Math.max(ay, by);
        c[0] = Math.max(x0, p[0]);
        c[1] = Math.max(y0, p[1]);
        c[2] = Math.min(x1, p[2]);
        c[3] = Math.min(y1, p[3]);
        float rr = r * currentScale();
        boolean same = Math.abs(c[0] - p[0]) < 0.5f && Math.abs(c[2] - p[2]) < 0.5f
                && Math.abs(c[1] - p[1]) < 0.5f && Math.abs(c[3] - p[3]) < 0.5f;
        c[4] = same ? Math.max(rr, p[4]) : rr;
        c[5] = feather > 0f ? Math.min(Math.round(feather * currentScale()), 60) : 0f;
    }

    public void popClip() {
        clipDepth--;
    }

    public boolean clipContains(float fx, float fy) {
        float[] c = clips[clipDepth];
        return fx >= c[0] && fx <= c[2] && fy >= c[1] && fy <= c[3];
    }

    public Shape shape(float x, float y, float w, float h) {
        return shape.reset(x, y, w, h);
    }

    public Text text(Font.Face face, float size) {
        return text.reset(face, size);
    }

    public final class Shape {
        float x;
        float y;
        float w;
        float h;
        float r0;
        float r1;
        float r2;
        float r3;
        int c0;
        int c1;
        int c2;
        int c3;
        float lineWidth;
        int lineColor;
        float glowRadius;
        int glowColor;
        float shadowX;
        float shadowY;
        float shadowBlur;
        float shadowAlpha;
        float sheen;
        float chrome;
        float clouds;
        float mode;
        GpuTextureView image;
        GpuSampler sampler;
        float u0;
        float v0;
        float u1;
        float v1;

        Shape reset(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            r0 = r1 = r2 = r3 = 0f;
            c0 = c1 = c2 = c3 = 0;
            lineWidth = 0f;
            lineColor = 0;
            glowRadius = 0f;
            glowColor = 0;
            shadowX = shadowY = shadowBlur = shadowAlpha = 0f;
            sheen = 0f;
            chrome = 0f;
            clouds = 0f;
            mode = UiVertex.MODE_SHAPE;
            image = null;
            sampler = null;
            u0 = v0 = 0f;
            u1 = v1 = 1f;
            return this;
        }

        public Shape radius(float r) {
            r0 = r1 = r2 = r3 = r;
            return this;
        }

        public Shape radii(float tl, float tr, float br, float bl) {
            r0 = tl;
            r1 = tr;
            r2 = br;
            r3 = bl;
            return this;
        }

        public Shape fill(int c) {
            c0 = c1 = c2 = c3 = c;
            return this;
        }

        public Shape vertical(int top, int bottom) {
            c0 = c1 = top;
            c2 = c3 = bottom;
            return this;
        }

        public Shape horizontal(int left, int right) {
            c0 = c3 = left;
            c1 = c2 = right;
            return this;
        }

        public Shape corners(int tl, int tr, int br, int bl) {
            c0 = tl;
            c1 = tr;
            c2 = br;
            c3 = bl;
            return this;
        }

        public Shape border(float width, int color) {
            lineWidth = width;
            lineColor = color;
            return this;
        }

        public Shape chrome(float amount) {
            chrome = amount;
            return this;
        }

        public Shape glow(float radius, int color) {
            glowRadius = radius;
            glowColor = color;
            return this;
        }

        public Shape shadow(float offX, float offY, float blur, float a) {
            shadowX = offX;
            shadowY = offY;
            shadowBlur = blur;
            shadowAlpha = a;
            return this;
        }

        public Shape sheen(float amount) {
            sheen = amount;
            return this;
        }

        public Shape glass() {
            mode = UiVertex.MODE_GLASS;
            usedGlass = true;
            return this;
        }

        public Shape clouds(float amount) {
            clouds = amount;
            return this;
        }

        public Shape backdrop() {
            mode = UiVertex.MODE_BACKDROP;
            usedGlass = true;
            return this;
        }

        public Shape storm(int cloud) {
            mode = UiVertex.MODE_STORM;
            lineColor = cloud;
            return this;
        }

        public Shape image(GpuTextureView view, GpuSampler s, float u0, float v0, float u1, float v1) {
            mode = UiVertex.MODE_IMAGE;
            image = view;
            sampler = s;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            return this;
        }

        public void draw() {
            emitShape(this);
        }
    }

    private void emitShape(Shape s) {
        if (s.w <= 0f || s.h <= 0f || alpha <= 0.002f) {
            return;
        }
        float hw = s.w * 0.5f;
        float hh = s.h * 0.5f;
        float cx = s.x + hw;
        float cy = s.y + hh;
        float maxR = Math.min(hw, hh);
        float r0 = Math.min(Math.max(s.r0, 0f), maxR);
        float r1 = Math.min(Math.max(s.r1, 0f), maxR);
        float r2 = Math.min(Math.max(s.r2, 0f), maxR);
        float r3 = Math.min(Math.max(s.r3, 0f), maxR);
        float pad = 1.5f * px();
        if (s.glowRadius > 0f && Colors.alpha(s.glowColor) > 0) {
            pad = Math.max(pad, s.glowRadius * 2.6f);
        }
        if (s.shadowAlpha > 0f) {
            pad = Math.max(pad, Math.max(Math.abs(s.shadowX), Math.abs(s.shadowY)) + s.shadowBlur * 1.6f);
        }
        float lx0 = -hw - pad;
        float ly0 = -hh - pad;
        float lx1 = hw + pad;
        float ly1 = hh + pad;
        Matrix3x2f m = mats[depth];
        float[] clip = clips[clipDepth];
        float ax = m.m00 * (cx + lx0) + m.m10 * (cy + ly0) + m.m20;
        float ay = m.m01 * (cx + lx0) + m.m11 * (cy + ly0) + m.m21;
        float bx = m.m00 * (cx + lx1) + m.m10 * (cy + ly0) + m.m20;
        float by = m.m01 * (cx + lx1) + m.m11 * (cy + ly0) + m.m21;
        float ccx = m.m00 * (cx + lx1) + m.m10 * (cy + ly1) + m.m20;
        float ccy = m.m01 * (cx + lx1) + m.m11 * (cy + ly1) + m.m21;
        float dx = m.m00 * (cx + lx0) + m.m10 * (cy + ly1) + m.m20;
        float dy = m.m01 * (cx + lx0) + m.m11 * (cy + ly1) + m.m21;
        float minX = Math.min(Math.min(ax, bx), Math.min(ccx, dx));
        float maxX = Math.max(Math.max(ax, bx), Math.max(ccx, dx));
        float minY = Math.min(Math.min(ay, by), Math.min(ccy, dy));
        float maxY = Math.max(Math.max(ay, by), Math.max(ccy, dy));
        if (maxX < clip[0] || minX > clip[2] || maxY < clip[1] || minY > clip[3] || maxX < 0 || maxY < 0 || minX > width || minY > height) {
            return;
        }
        ensureBatch(s.image, s.sampler);
        int line = Colors.mulAlpha(s.lineColor, alpha);
        int glow = Colors.mulAlpha(s.glowColor, alpha);
        float sa = s.shadowAlpha * alpha;
        float du = (s.u1 - s.u0) / s.w;
        float dv = (s.v1 - s.v0) / s.h;
        ensureCapacity();
        vertex(ax, ay, lx0, ly0, hw, hh, clip, s.mode, r0, r1, r2, r3, Colors.mulAlpha(s.c0, alpha), line, glow, s, sa,
                s.u0 + (lx0 + hw) * du, s.v0 + (ly0 + hh) * dv, s.clouds, 0f);
        vertex(dx, dy, lx0, ly1, hw, hh, clip, s.mode, r0, r1, r2, r3, Colors.mulAlpha(s.c3, alpha), line, glow, s, sa,
                s.u0 + (lx0 + hw) * du, s.v0 + (ly1 + hh) * dv, s.clouds, 0f);
        vertex(ccx, ccy, lx1, ly1, hw, hh, clip, s.mode, r0, r1, r2, r3, Colors.mulAlpha(s.c2, alpha), line, glow, s, sa,
                s.u0 + (lx1 + hw) * du, s.v0 + (ly1 + hh) * dv, s.clouds, 0f);
        vertex(bx, by, lx1, ly0, hw, hh, clip, s.mode, r0, r1, r2, r3, Colors.mulAlpha(s.c1, alpha), line, glow, s, sa,
                s.u0 + (lx1 + hw) * du, s.v0 + (ly0 + hh) * dv, s.clouds, 0f);
        quads++;
        batches.get(batches.size() - 1).count++;
    }

    private void vertex(float px, float py, float lx, float ly, float hw, float hh, float[] clip, float mode,
                        float r0, float r1, float r2, float r3, int fill, int line, int glow, Shape s, float sa,
                        float u, float v, float bias, float flags) {
        ByteBuffer b = data;
        b.putFloat(px).putFloat(py);
        b.putFloat(lx).putFloat(ly);
        b.putFloat(hw).putFloat(hh).putFloat(clipCode(clip)).putFloat(mode);
        b.putFloat(r0).putFloat(r1).putFloat(r2).putFloat(r3);
        putColor(b, fill);
        putColor(b, line);
        putColor(b, glow);
        b.putFloat(s.lineWidth).putFloat(s.glowRadius).putFloat(s.shadowBlur).putFloat(sa);
        b.putFloat(s.shadowX).putFloat(s.shadowY).putFloat(s.sheen * alpha).putFloat(s.chrome);
        b.putFloat(u).putFloat(v).putFloat(bias).putFloat(alpha);
        b.putFloat(clip[0]).putFloat(clip[1]).putFloat(clip[2]).putFloat(clip[3]);
    }

    private static float clipCode(float[] clip) {
        return Math.min(clip[4], 1000f) + clip[5] * 1024f;
    }

    private static void putColor(ByteBuffer b, int c) {
        b.put((byte) (c >> 16)).put((byte) (c >> 8)).put((byte) c).put((byte) (c >>> 24));
    }

    public final class Text {
        Font.Face face;
        float size;
        int top;
        int bottom;
        float glowRadius;
        int glowColor;
        float outline;
        int outlineColor;
        float chrome;
        float tracking;
        float weight;

        Text reset(Font.Face face, float size) {
            this.face = face;
            this.size = size;
            top = bottom = 0xFFFFFFFF;
            glowRadius = 0f;
            glowColor = 0;
            outline = 0f;
            outlineColor = 0;
            chrome = 0f;
            tracking = 0f;
            weight = 0f;
            return this;
        }

        public Text color(int c) {
            top = bottom = c;
            return this;
        }

        public Text vertical(int t, int b) {
            top = t;
            bottom = b;
            return this;
        }

        public Text glow(float radius, int color) {
            glowRadius = radius;
            glowColor = color;
            return this;
        }

        public Text outline(float width, int color) {
            outline = width;
            outlineColor = color;
            return this;
        }

        public Text chrome(float k) {
            chrome = k;
            return this;
        }

        public Text tracking(float em) {
            tracking = em;
            return this;
        }

        public Text weight(float w) {
            weight = w;
            return this;
        }

        public float width(CharSequence s) {
            return face.width(s, size, tracking);
        }

        public float draw(CharSequence s, float x, float baseline) {
            return emitText(this, s, x, baseline);
        }

        public float drawMid(CharSequence s, float x, float centerY) {
            return emitText(this, s, x, centerY + face.capHeight * size * 0.5f);
        }

        public float drawMidCenter(CharSequence s, float cx, float centerY) {
            return drawMid(s, cx - width(s) * 0.5f, centerY);
        }

        public float drawMidRight(CharSequence s, float right, float centerY) {
            return drawMid(s, right - width(s), centerY);
        }
    }

    private float emitText(Text t, CharSequence s, float x, float baseline) {
        Font.Face f = t.face;
        if (f == null || alpha <= 0.002f || s.length() == 0) {
            return f == null ? 0f : f.width(s, t.size, t.tracking);
        }
        Font font = f.font;
        ensureBatch(null, null);
        float size = t.size;
        float pen = x;
        float em = font.em();
        float glowAtlas = Math.min(t.glowRadius * em / size, font.spread() * 0.85f);
        float outlineAtlas = Math.min(t.outline * em / size, font.spread() * 0.7f);
        float screenPx = size * currentScale();
        float bias = screenPx < 11f ? 0.16f : screenPx < 16f ? 0.1f : 0.04f;
        bias += t.weight;
        int glow = Colors.mulAlpha(t.glowColor, alpha);
        int line = Colors.mulAlpha(t.outlineColor, alpha);
        int topC = Colors.mulAlpha(t.top, alpha);
        int botC = Colors.mulAlpha(t.bottom, alpha);
        boolean grad = topC != botC;
        float capTop = baseline - f.capHeight * size;
        float capH = Math.max(f.capHeight * size, 1e-3f);
        Matrix3x2f m = mats[depth];
        float[] clip = clips[clipDepth];
        int prev = -1;
        for (int i = 0; i < s.length(); i++) {
            int cp = s.charAt(i);
            if (prev >= 0) {
                pen += f.kerning(prev, cp) * size;
            }
            Font.Glyph g = f.glyph(cp);
            if (g == null) {
                prev = -1;
                continue;
            }
            if (!g.empty()) {
                float gx0 = pen + g.pl() * size;
                float gx1 = pen + g.pr() * size;
                float gy0 = baseline - g.pt() * size;
                float gy1 = baseline - g.pb() * size;
                float ax = m.m00 * gx0 + m.m10 * gy0 + m.m20;
                float ay = m.m01 * gx0 + m.m11 * gy0 + m.m21;
                float bx = m.m00 * gx1 + m.m10 * gy0 + m.m20;
                float by = m.m01 * gx1 + m.m11 * gy0 + m.m21;
                float cx = m.m00 * gx1 + m.m10 * gy1 + m.m20;
                float cy = m.m01 * gx1 + m.m11 * gy1 + m.m21;
                float dx = m.m00 * gx0 + m.m10 * gy1 + m.m20;
                float dy = m.m01 * gx0 + m.m11 * gy1 + m.m21;
                float minX = Math.min(Math.min(ax, bx), Math.min(cx, dx));
                float maxX = Math.max(Math.max(ax, bx), Math.max(cx, dx));
                float minY = Math.min(Math.min(ay, by), Math.min(cy, dy));
                float maxY = Math.max(Math.max(ay, by), Math.max(cy, dy));
                if (!(maxX < clip[0] || minX > clip[2] || maxY < clip[1] || minY > clip[3])) {
                    ensureCapacity();
                    float ty0 = (gy0 - capTop) / capH;
                    float ty1 = (gy1 - capTop) / capH;
                    int ct = grad ? Colors.mix(topC, botC, clamp01(ty0)) : topC;
                    int cb = grad ? Colors.mix(topC, botC, clamp01(ty1)) : topC;
                    glyphVertex(ax, ay, 0f, ty0, clip, ct, line, glow, outlineAtlas, glowAtlas, t.chrome, g.u0(), g.v0(), bias);
                    glyphVertex(dx, dy, 0f, ty1, clip, cb, line, glow, outlineAtlas, glowAtlas, t.chrome, g.u0(), g.v1(), bias);
                    glyphVertex(cx, cy, 1f, ty1, clip, cb, line, glow, outlineAtlas, glowAtlas, t.chrome, g.u1(), g.v1(), bias);
                    glyphVertex(bx, by, 1f, ty0, clip, ct, line, glow, outlineAtlas, glowAtlas, t.chrome, g.u1(), g.v0(), bias);
                    quads++;
                    batches.get(batches.size() - 1).count++;
                }
            }
            pen += g.advance() * size + t.tracking * size;
            prev = cp;
        }
        return pen - x - (s.length() > 0 ? t.tracking * size : 0f);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    private void glyphVertex(float px, float py, float lx, float ly, float[] clip, int fill, int line, int glow,
                             float outline, float glowR, float chrome, float u, float v, float bias) {
        ByteBuffer b = data;
        b.putFloat(px).putFloat(py);
        b.putFloat(lx).putFloat(ly);
        b.putFloat(0f).putFloat(0f).putFloat(clipCode(clip)).putFloat(UiVertex.MODE_TEXT);
        b.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(0f);
        putColor(b, fill);
        putColor(b, line);
        putColor(b, glow);
        b.putFloat(outline).putFloat(glowR).putFloat(0f).putFloat(0f);
        b.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(chrome);
        b.putFloat(u).putFloat(v).putFloat(bias).putFloat(0f);
        b.putFloat(clip[0]).putFloat(clip[1]).putFloat(clip[2]).putFloat(clip[3]);
    }

    private void ensureBatch(GpuTextureView image, GpuSampler sampler) {
        if (batches.isEmpty()) {
            batches.add(new Batch(quads, image, sampler));
            return;
        }
        Batch last = batches.get(batches.size() - 1);
        if (image == null || last.image == image) {
            return;
        }
        if (last.image == null && last.count == 0) {
            batches.set(batches.size() - 1, new Batch(last.first, image, sampler));
            return;
        }
        batches.add(new Batch(quads, image, sampler));
    }

    private void ensureCapacity() {
        int need = UiVertex.SIZE * 4;
        if (data.remaining() < need) {
            int cap = data.capacity() * 2;
            ByteBuffer next = MemoryUtil.memAlloc(cap);
            data.flip();
            next.put(data);
            MemoryUtil.memFree(data);
            data = next;
        }
    }

    public void flush(Framebuffer target, GpuTextureView backdrop, boolean backdropReady, float time) {
        if (quads == 0) {
            return;
        }
        Font font = Font.get();
        if (!font.ready()) {
            return;
        }
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        int bytes = quads * 4 * UiVertex.SIZE;
        if (vertexBuffer == null || vertexCapacity < bytes) {
            if (vertexBuffer != null) {
                vertexBuffer.close();
            }
            vertexCapacity = Math.max(bytes, 1 << 20);
            vertexCapacity = Long.highestOneBit(vertexCapacity - 1) << 1;
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "fashion ui vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, vertexCapacity);
        }
        if (frameUniform == null) {
            frameUniform = RenderSystem.getDevice().createBuffer(() -> "fashion ui frame",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 48L);
        }
        ByteBuffer upload = data.duplicate();
        upload.position(0).limit(bytes);
        encoder.writeToBuffer(vertexBuffer.slice(0, bytes), upload);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer u = Std140Builder.onStack(stack, 48)
                    .putVec4(width, height, 1f / width, 1f / height)
                    .putVec4(time, backdropReady ? 1f : 0f, font.atlasWidth(), font.atlasHeight())
                    .putVec4(font.spread(), 0f, 0f, 0f)
                    .get();
            encoder.writeToBuffer(frameUniform.slice(), u);
        }
        RenderSystem.ShapeIndexBuffer seq = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
        GpuBuffer indices = seq.getIndexBuffer(quads * 6);
        VertexFormat.IndexType indexType = seq.getIndexType();
        GpuSampler linear = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        try (RenderPass pass = encoder.createRenderPass(() -> "fashion ui", target.getColorAttachmentView(), OptionalInt.empty())) {
            pass.setPipeline(Pipelines.UI);
            pass.setUniform("UiFrame", frameUniform);
            pass.bindTexture("Atlas", font.view(), font.sampler());
            pass.bindTexture("Backdrop", backdrop != null ? backdrop : font.view(), linear);
            GpuTextureView lightView = Blur.light();
            pass.bindTexture("BackdropLight", lightView != null ? lightView : font.view(), linear);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indices, indexType);
            for (Batch batch : batches) {
                if (batch.count == 0) {
                    continue;
                }
                pass.bindTexture("Image", batch.image != null ? batch.image : font.view(),
                        batch.sampler != null ? batch.sampler : linear);
                pass.drawIndexed(0, batch.first * 6, batch.count * 6, 1);
            }
        }
        quads = 0;
        batches.clear();
        data.clear();
    }
}
