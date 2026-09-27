package dev.fashion.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import dev.fashion.anim.Motion;
import dev.fashion.anim.Spring;
import dev.fashion.gfx.Canvas;

public abstract class FashionScreen extends Screen {
    protected final Spring open = new Spring(0f, Motion.ARRIVAL);
    protected boolean closing;
    protected float scale = 1f;
    protected float mouseX;
    protected float mouseY;

    protected FashionScreen(String title) {
        super(Text.literal(title));
        open.to(1f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public final void paintFrame(Canvas c, float dt) {
        MinecraftClient mc = MinecraftClient.getInstance();
        scale = layoutScale(c.framebufferWidth(), c.framebufferHeight());
        double rawX = mc.mouse.getX() * c.framebufferWidth() / Math.max(1, mc.getWindow().getWidth());
        double rawY = mc.mouse.getY() * c.framebufferHeight() / Math.max(1, mc.getWindow().getHeight());
        if (testMouseX >= 0f) {
            rawX = testMouseX;
            rawY = testMouseY;
        }
        mouseX = (float) rawX / scale;
        mouseY = (float) rawY / scale;
        open.update(dt);
        c.push();
        c.scale(scale, scale);
        paint(c, dt, c.framebufferWidth() / scale, c.framebufferHeight() / scale);
        c.pop();
        if (closing && open.get() < 0.012f && Math.abs(open.velocity) < 0.3f && fullyClosed()) {
            closing = false;
            onClosed();
            mc.setScreen(null);
        }
    }

    protected float layoutScale(int fbW, int fbH) {
        return Math.max(0.75f, Math.min(fbW * 0.72f / 620f, fbH * 0.68f / 400f));
    }

    protected boolean fullyClosed() {
        return true;
    }

    public static float testMouseX = -1f;
    public static float testMouseY = -1f;

    protected abstract void paint(Canvas c, float dt, float screenW, float screenH);

    protected void onClosed() {
    }

    public float openAmount() {
        return open.get();
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            open.motion(Motion.DEPART);
            open.to(0f);
        }
    }

    public boolean isClosing() {
        return closing;
    }

    protected boolean onMouseDown(float x, float y, int button) {
        return false;
    }

    protected boolean onMouseUp(float x, float y, int button) {
        return false;
    }

    protected boolean onScroll(float x, float y, float amount) {
        return false;
    }

    protected boolean onKey(int key, int modifiers) {
        return false;
    }

    protected boolean onChar(int codepoint) {
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing) {
            return true;
        }
        if (dev.fashion.ui.hud.HudLayer.click()) {
            return true;
        }
        return onMouseDown(mouseX, mouseY, click.button());
    }

    @Override
    public boolean mouseReleased(Click click) {
        return onMouseUp(mouseX, mouseY, click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        return onScroll(mouseX, mouseY, (float) vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (closing) {
            return true;
        }
        if (onKey(input.key(), input.modifiers())) {
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        return !closing && onChar(input.codepoint());
    }
}
