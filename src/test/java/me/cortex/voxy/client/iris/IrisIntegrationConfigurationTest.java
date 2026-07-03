package me.cortex.voxy.client.iris;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class IrisIntegrationConfigurationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void buildDoesNotExcludeIrisIntegrationSources() throws Exception {
        String build = Files.readString(ROOT.resolve("build.gradle"));

        assertFalse(build.contains("exclude 'me/cortex/voxy/client/iris/**'"));
        assertFalse(build.contains("exclude 'me/cortex/voxy/client/core/IrisVoxyRenderPipeline.java'"));
        assertFalse(build.contains("exclude 'me/cortex/voxy/client/core/util/IrisUtil.java'"));
        assertFalse(build.contains("exclude 'me/cortex/voxy/client/mixin/iris/**'"));
    }

    @Test
    void mixinConfigRegistersIrisIntegrationMixins() throws Exception {
        String mixins = Files.readString(ROOT.resolve("src/main/resources/client.voxy.mixins.json"));

        assertTrue(mixins.contains("\"iris.CustomUniformsAccessor\""));
        assertTrue(mixins.contains("\"iris.IrisRenderingPipelineAccessor\""));
        assertTrue(mixins.contains("\"iris.MixinIrisRenderingPipeline\""));
        assertTrue(mixins.contains("\"iris.MixinIrisSamplers\""));
        assertTrue(mixins.contains("\"iris.MixinMatrixUniforms\""));
        assertTrue(mixins.contains("\"iris.MixinProgramSet\""));
        assertTrue(mixins.contains("\"iris.MixinShaderPackSourceNames\""));
        assertTrue(mixins.contains("\"iris.MixinStandardMacros\""));
    }

    @Test
    void renderPipelineFactoryCanSelectIrisPipeline() throws Exception {
        String factory = Files.readString(ROOT.resolve("src/main/java/me/cortex/voxy/client/core/RenderPipelineFactory.java"));

        assertTrue(factory.contains("IrisUtil.IRIS_INSTALLED"));
        assertTrue(factory.contains("createIrisPipeline"));
        assertTrue(factory.contains("new IrisVoxyRenderPipeline"));
        assertFalse(factory.contains("Stubbed out - Iris integration disabled"));
    }

    @Test
    void distantHorizonsImpersonationRemainsOptIn() throws Exception {
        String patch = Files.readString(ROOT.resolve("src/main/java/me/cortex/voxy/client/iris/IrisShaderPatch.java"));

        assertTrue(patch.contains("System.getProperty(\"voxy.impersonateDHShader\", \"false\")"));
        assertTrue(patch.contains(".equalsIgnoreCase(\"true\")"));
        assertFalse(Boolean.getBoolean("voxy.impersonateDHShader"));
    }

    @Test
    void irisViewportSetupInjectionIsOptional() throws Exception {
        String mixin = Files.readString(ROOT.resolve("src/main/java/me/cortex/voxy/client/mixin/iris/MixinIrisRenderingPipeline.java"));

        assertTrue(mixin.contains("voxy$injectViewportSetup"));
        assertTrue(mixin.contains("method = \"beginLevelRendering\""));
        assertTrue(mixin.contains("require = 0"));
    }
}
