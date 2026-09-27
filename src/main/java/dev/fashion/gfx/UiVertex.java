package dev.fashion.gfx;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public final class UiVertex {
    public static final VertexFormatElement POS = VertexFormatElement.register(20, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 2);
    public static final VertexFormatElement LOCAL = VertexFormatElement.register(21, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 2);
    public static final VertexFormatElement RECT = VertexFormatElement.register(22, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
    public static final VertexFormatElement RADII = VertexFormatElement.register(23, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
    public static final VertexFormatElement FILL = VertexFormatElement.register(24, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement LINE = VertexFormatElement.register(25, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement GLOW = VertexFormatElement.register(26, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement P1 = VertexFormatElement.register(27, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
    public static final VertexFormatElement P2 = VertexFormatElement.register(28, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
    public static final VertexFormatElement UV = VertexFormatElement.register(29, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);
    public static final VertexFormatElement CLIP = VertexFormatElement.register(30, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4);

    public static final VertexFormat FORMAT = VertexFormat.builder()
            .add("aPos", POS)
            .add("aLocal", LOCAL)
            .add("aRect", RECT)
            .add("aRadii", RADII)
            .add("aFill", FILL)
            .add("aLine", LINE)
            .add("aGlow", GLOW)
            .add("aP1", P1)
            .add("aP2", P2)
            .add("aUv", UV)
            .add("aClip", CLIP)
            .build();

    public static final int SIZE = FORMAT.getVertexSize();

    public static final float MODE_SHAPE = 0f;
    public static final float MODE_GLASS = 1f;
    public static final float MODE_TEXT = 2f;
    public static final float MODE_IMAGE = 3f;
    public static final float MODE_STORM = 4f;
    public static final float MODE_BACKDROP = 5f;

    private UiVertex() {
    }
}
