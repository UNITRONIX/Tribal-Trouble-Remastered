package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final strictfp class PostProcessPipeline {
    // Bloom: higher threshold avoids fog glow; subtle intensity
    private static final float BLOOM_THRESHOLD = 0.85f;
    private static final float BLOOM_INTENSITY = 0.25f;
    // SSAO: match game projection near=2, far=8000; gentle occlusion
    private static final float SSAO_NEAR = 2.0f;
    private static final float SSAO_FAR = 8000.0f;
    private static final float SSAO_RADIUS = 0.3f;
    private static final float SSAO_INTENSITY = 0.6f;

    private PostProcessFBO sceneFBO;
    private PostProcessFBO brightFBO;
    private PostProcessFBO blurH_FBO;
    private PostProcessFBO blurV_FBO;
    private PostProcessFBO ssaoFBO;
    private PostProcessFBO ssaoBlurFBO;

    private final FullscreenQuad fsQuad = new FullscreenQuad();

    private ShaderProgram brightPassShader;
    private ShaderProgram blurShader;
    private ShaderProgram ssaoShader;
    private ShaderProgram ssaoBlurShader;
    private ShaderProgram compositeShader;

    private boolean initialized = false;
    private boolean supported = false;
    private int screenWidth;
    private int screenHeight;

    public void init(int width, int height) {
        if (initialized && width == screenWidth && height == screenHeight) return;
        if (initialized) delete(); // resize: recreate everything
        screenWidth = width;
        screenHeight = height;

        if (!GL.getCapabilities().OpenGL20 || !GL.getCapabilities().OpenGL30) {
            System.out.println("PostProcess: OpenGL 2.0/3.0 not supported, disabling effects");
            supported = false;
            initialized = true;
            return;
        }

        try {
            sceneFBO = new PostProcessFBO(width, height, true);
            int halfW = Math.max(1, width / 2);
            int halfH = Math.max(1, height / 2);
            brightFBO = new PostProcessFBO(halfW, halfH, false);
            blurH_FBO = new PostProcessFBO(halfW, halfH, false);
            blurV_FBO = new PostProcessFBO(halfW, halfH, false);
            ssaoFBO = new PostProcessFBO(width, height, false);
            ssaoBlurFBO = new PostProcessFBO(width, height, false);

            brightPassShader = new ShaderProgram(PASSTHROUGH_VERT, BRIGHT_PASS_FRAG);
            blurShader = new ShaderProgram(PASSTHROUGH_VERT, GAUSSIAN_BLUR_FRAG);
            ssaoShader = new ShaderProgram(PASSTHROUGH_VERT, SSAO_FRAG);
            ssaoBlurShader = new ShaderProgram(PASSTHROUGH_VERT, SSAO_BLUR_FRAG);
            compositeShader = new ShaderProgram(PASSTHROUGH_VERT, COMPOSITE_FRAG);

            fsQuad.init();
            supported = true;
            System.out.println("PostProcess: Bloom + SSAO initialized (" + width + "x" + height + ")");
        } catch (Exception e) {
            System.out.println("PostProcess: Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            supported = false;
        }
        initialized = true;
    }

    public boolean isSupported() {
        return supported;
    }

    public void beginScene() {
        if (!supported) return;
        sceneFBO.bind();
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void endSceneAndApply() {
        if (!supported) return;

        // Save full GL state so post-processing doesn't corrupt the engine state
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        // Set up clean state for fullscreen quad passes
        setupQuadState();

        // 1. SSAO pass (if enabled)
        if (Globals.process_ssao) {
            ssaoFBO.bind();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            ssaoShader.bind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFBO.getDepthTexture());
            ssaoShader.setUniform1i("depthTex", 0);
            ssaoShader.setUniform2f("texelSize", 1.0f / screenWidth, 1.0f / screenHeight);
            ssaoShader.setUniform1f("radius", SSAO_RADIUS);
            ssaoShader.setUniform1f("intensity", SSAO_INTENSITY);
            ssaoShader.setUniform1f("nearPlane", SSAO_NEAR);
            ssaoShader.setUniform1f("farPlane", SSAO_FAR);
            drawQuad();
            ShaderProgram.unbind();

            // Blur SSAO to remove noise
            ssaoBlurFBO.bind();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            ssaoBlurShader.bind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ssaoFBO.getColorTexture());
            ssaoBlurShader.setUniform1i("ssaoInput", 0);
            ssaoBlurShader.setUniform2f("texelSize", 1.0f / screenWidth, 1.0f / screenHeight);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFBO.getDepthTexture());
            ssaoBlurShader.setUniform1i("depthTex", 1);
            ssaoBlurShader.setUniform1f("nearPlane", SSAO_NEAR);
            ssaoBlurShader.setUniform1f("farPlane", SSAO_FAR);
            drawQuad();
            ShaderProgram.unbind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }

        // 2. Bloom passes (if enabled)
        if (Globals.process_bloom) {
            // Bright pass
            brightFBO.bind();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            brightPassShader.bind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFBO.getColorTexture());
            brightPassShader.setUniform1i("sceneTex", 0);
            brightPassShader.setUniform1f("threshold", BLOOM_THRESHOLD);
            drawQuad();
            ShaderProgram.unbind();

            // Horizontal blur
            blurH_FBO.bind();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            blurShader.bind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, brightFBO.getColorTexture());
            blurShader.setUniform1i("image", 0);
            blurShader.setUniform2f("direction", 1.0f / brightFBO.getWidth(), 0.0f);
            drawQuad();
            ShaderProgram.unbind();

            // Vertical blur
            blurV_FBO.bind();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            blurShader.bind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurH_FBO.getColorTexture());
            blurShader.setUniform1i("image", 0);
            blurShader.setUniform2f("direction", 0.0f, 1.0f / blurV_FBO.getHeight());
            drawQuad();
            ShaderProgram.unbind();
        }

        // 3. Final composite → default framebuffer
        PostProcessFBO.unbind();
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        compositeShader.bind();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFBO.getColorTexture());
        compositeShader.setUniform1i("sceneTex", 0);

        if (Globals.process_bloom) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurV_FBO.getColorTexture());
        }
        compositeShader.setUniform1i("bloomTex", 1);
        compositeShader.setUniform1f("bloomIntensity", Globals.process_bloom ? BLOOM_INTENSITY : 0.0f);

        if (Globals.process_ssao) {
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ssaoBlurFBO.getColorTexture());
        }
        compositeShader.setUniform1i("ssaoTex", 2);
        compositeShader.setUniform1f("useSSAO", Globals.process_ssao ? 1.0f : 0.0f);

        drawQuad();
        ShaderProgram.unbind();

        // Unbind textures from all units
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // Restore full GL state
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    /** Set up minimal GL state for drawing textured fullscreen quads */
    private static void setupQuadState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(-1, 1, -1, 1, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    /** Draw a unit quad covering the full viewport using VAO */
    private void drawQuad() {
        fsQuad.draw();
    }

    public void delete() {
        if (sceneFBO != null) { sceneFBO.delete(); sceneFBO = null; }
        if (brightFBO != null) { brightFBO.delete(); brightFBO = null; }
        if (blurH_FBO != null) { blurH_FBO.delete(); blurH_FBO = null; }
        if (blurV_FBO != null) { blurV_FBO.delete(); blurV_FBO = null; }
        if (ssaoFBO != null) { ssaoFBO.delete(); ssaoFBO = null; }
        if (ssaoBlurFBO != null) { ssaoBlurFBO.delete(); ssaoBlurFBO = null; }
        if (brightPassShader != null) { brightPassShader.delete(); brightPassShader = null; }
        if (blurShader != null) { blurShader.delete(); blurShader = null; }
        if (ssaoShader != null) { ssaoShader.delete(); ssaoShader = null; }
        if (ssaoBlurShader != null) { ssaoBlurShader.delete(); ssaoBlurShader = null; }
        if (compositeShader != null) { compositeShader.delete(); compositeShader = null; }
        fsQuad.delete();
        initialized = false;
    }

    // ======== GLSL SHADERS ========

    private static final String PASSTHROUGH_VERT =
        "#version 330 core\n" +
        "layout(location = 0) in vec2 aPos;\n" +
        "layout(location = 1) in vec2 aTexCoord;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    vTexCoord = aTexCoord;\n" +
        "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
        "}\n";

    // Bright pass: extract only truly bright pixels, ignore fog-brightened areas
    // Uses luminance with a smooth knee to avoid hard cutoff halos
    private static final String BRIGHT_PASS_FRAG =
        "#version 330 core\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 FragColor;\n" +
        "uniform sampler2D sceneTex;\n" +
        "uniform float threshold;\n" +
        "void main() {\n" +
        "    vec4 color = texture(sceneTex, vTexCoord);\n" +
        "    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));\n" +
        "    // Soft knee: smoothstep avoids hard cutoff that causes edge halos\n" +
        "    float knee = 0.1;\n" +
        "    float soft = luma - threshold + knee;\n" +
        "    soft = clamp(soft / (2.0 * knee), 0.0, 1.0);\n" +
        "    soft = soft * soft;\n" +
        "    float contribution = max(soft, 0.0) * max(luma - threshold, 0.0) / max(luma, 0.001);\n" +
        "    FragColor = color * contribution;\n" +
        "}\n";

    private static final String GAUSSIAN_BLUR_FRAG =
        "#version 330 core\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 FragColor;\n" +
        "uniform sampler2D image;\n" +
        "uniform vec2 direction;\n" +
        "void main() {\n" +
        "    vec2 uv = vTexCoord;\n" +
        "    vec4 result = texture(image, uv) * 0.227027;\n" +
        "    result += texture(image, uv + direction * 1.0) * 0.1945946;\n" +
        "    result += texture(image, uv - direction * 1.0) * 0.1945946;\n" +
        "    result += texture(image, uv + direction * 2.0) * 0.1216216;\n" +
        "    result += texture(image, uv - direction * 2.0) * 0.1216216;\n" +
        "    result += texture(image, uv + direction * 3.0) * 0.0540541;\n" +
        "    result += texture(image, uv - direction * 3.0) * 0.0540541;\n" +
        "    result += texture(image, uv + direction * 4.0) * 0.0162162;\n" +
        "    result += texture(image, uv - direction * 4.0) * 0.0162162;\n" +
        "    FragColor = result;\n" +
        "}\n";

    // SSAO: depth-only with proper near/far linearization and edge-aware sampling
    private static final String SSAO_FRAG =
        "#version 330 core\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 FragColor;\n" +
        "uniform sampler2D depthTex;\n" +
        "uniform vec2 texelSize;\n" +
        "uniform float radius;\n" +
        "uniform float intensity;\n" +
        "uniform float nearPlane;\n" +
        "uniform float farPlane;\n" +
        "\n" +
        "float linearizeDepth(float d) {\n" +
        "    return (2.0 * nearPlane) / (farPlane + nearPlane - d * (farPlane - nearPlane));\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = vTexCoord;\n" +
        "    float rawDepth = texture(depthTex, uv).r;\n" +
        "    // Sky pixels (depth=1.0) get no occlusion\n" +
        "    if (rawDepth > 0.9999) { FragColor = vec4(1.0); return; }\n" +
        "    float depth = linearizeDepth(rawDepth);\n" +
        "    float occlusion = 0.0;\n" +
        "    float totalWeight = 0.0;\n" +
        "    // Scale sample radius by depth so distant objects aren't over-occluded\n" +
        "    float sampleScale = radius / max(depth, 0.01);\n" +
        "    sampleScale = min(sampleScale, 40.0);\n" +
        "    for (int x = -2; x <= 2; x++) {\n" +
        "        for (int y = -2; y <= 2; y++) {\n" +
        "            if (x == 0 && y == 0) continue;\n" +
        "            vec2 offset = vec2(float(x), float(y)) * texelSize * sampleScale;\n" +
        "            float sampleRaw = texture(depthTex, uv + offset).r;\n" +
        "            float sampleDepth = linearizeDepth(sampleRaw);\n" +
        "            float diff = depth - sampleDepth;\n" +
        "            // Only count occlusion for nearby depth differences (not edges)\n" +
        "            float rangeCheck = 1.0 - smoothstep(0.0, 0.01, abs(diff));\n" +
        "            if (diff > 0.0001 && diff < 0.005) {\n" +
        "                occlusion += 1.0 * rangeCheck;\n" +
        "            }\n" +
        "            totalWeight += 1.0;\n" +
        "        }\n" +
        "    }\n" +
        "    float ao = 1.0 - (occlusion / totalWeight) * intensity;\n" +
        "    FragColor = vec4(ao, ao, ao, 1.0);\n" +
        "}\n";

    // Depth-aware blur for SSAO to remove noise without bleeding across edges
    private static final String SSAO_BLUR_FRAG =
        "#version 330 core\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 FragColor;\n" +
        "uniform sampler2D ssaoInput;\n" +
        "uniform sampler2D depthTex;\n" +
        "uniform vec2 texelSize;\n" +
        "uniform float nearPlane;\n" +
        "uniform float farPlane;\n" +
        "\n" +
        "float linearizeDepth(float d) {\n" +
        "    return (2.0 * nearPlane) / (farPlane + nearPlane - d * (farPlane - nearPlane));\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = vTexCoord;\n" +
        "    float centerDepth = linearizeDepth(texture(depthTex, uv).r);\n" +
        "    float result = 0.0;\n" +
        "    float totalWeight = 0.0;\n" +
        "    for (int x = -2; x <= 2; x++) {\n" +
        "        for (int y = -2; y <= 2; y++) {\n" +
        "            vec2 offset = vec2(float(x), float(y)) * texelSize;\n" +
        "            float sampleAO = texture(ssaoInput, uv + offset).r;\n" +
        "            float sampleDepth = linearizeDepth(texture(depthTex, uv + offset).r);\n" +
        "            // Weight by depth similarity to prevent edge bleeding\n" +
        "            float depthDiff = abs(centerDepth - sampleDepth);\n" +
        "            float w = exp(-depthDiff * 1000.0);\n" +
        "            result += sampleAO * w;\n" +
        "            totalWeight += w;\n" +
        "        }\n" +
        "    }\n" +
        "    float ao = result / max(totalWeight, 0.001);\n" +
        "    FragColor = vec4(ao, ao, ao, 1.0);\n" +
        "}\n";

    // Composite: combine scene + optional bloom + optional SSAO
    private static final String COMPOSITE_FRAG =
        "#version 330 core\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 FragColor;\n" +
        "uniform sampler2D sceneTex;\n" +
        "uniform sampler2D bloomTex;\n" +
        "uniform sampler2D ssaoTex;\n" +
        "uniform float bloomIntensity;\n" +
        "uniform float useSSAO;\n" +
        "void main() {\n" +
        "    vec2 uv = vTexCoord;\n" +
        "    vec4 scene = texture(sceneTex, uv);\n" +
        "    vec3 bloom = texture(bloomTex, uv).rgb;\n" +
        "    float ao = mix(1.0, texture(ssaoTex, uv).r, useSSAO);\n" +
        "    vec3 result = scene.rgb * ao + bloom * bloomIntensity;\n" +
        "    FragColor = vec4(result, scene.a);\n" +
        "}\n";
}
