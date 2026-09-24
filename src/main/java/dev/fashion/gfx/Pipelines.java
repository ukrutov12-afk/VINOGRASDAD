package dev.fashion.gfx;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import dev.fashion.FashionClient;

public final class Pipelines {
    public static final BlendFunction PREMULTIPLIED = new BlendFunction(
            SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ZERO, DestFactor.ONE);

    public static final RenderPipeline UI = RenderPipeline.builder()
            .withLocation(id("pipeline/ui"))
            .withVertexShader(id("core/ui"))
            .withFragmentShader(id("core/ui"))
            .withUniform("UiFrame", UniformType.UNIFORM_BUFFER)
            .withSampler("Atlas")
            .withSampler("Backdrop")
            .withSampler("Image")
            .withVertexFormat(UiVertex.FORMAT, VertexFormat.DrawMode.QUADS)
            .withBlend(PREMULTIPLIED)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    public static final RenderPipeline BLUR_DOWN = blur("blur_down");
    public static final RenderPipeline BLUR_UP = blur("blur_up");

    private static RenderPipeline blur(String name) {
        return RenderPipeline.builder()
                .withLocation(id("pipeline/" + name))
                .withVertexShader(id("core/fullscreen"))
                .withFragmentShader(id("core/" + name))
                .withUniform("BlurPass", UniformType.UNIFORM_BUFFER)
                .withSampler("Source")
                .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                .withoutBlend()
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();
    }

    private static Identifier id(String path) {
        return Identifier.of(FashionClient.MOD_ID, path);
    }

    private Pipelines() {
    }
}
