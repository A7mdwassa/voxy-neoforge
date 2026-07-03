package me.cortex.voxy.client.core.gl.shader;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderLoaderCompatibilityTest {
    @Test
    void parseShaderSourceStripsNestedVersionsAndExpandsImports() throws Exception {
        String parsed = invokeParseShaderSource(
                "#version 460 core\n#import <voxy:test/imported.glsl>\nvoid main() {}",
                Map.of("voxy:test/imported.glsl", "#version 450 core\nvec4 importedValue = vec4(1.0);")
        );

        assertFalse(parsed.contains("#import"));
        assertFalse(parsed.contains("#version 450 core"));
        assertTrue(parsed.startsWith("#version 460 core\n"));
        assertTrue(parsed.contains("vec4 importedValue = vec4(1.0);"));
        assertTrue(parsed.contains("void main() {}"));
    }

    @Test
    void parseShaderSourceExpandsNestedImportsRecursively() throws Exception {
        String parsed = invokeParseShaderSource(
                "#version 460 core\n#import <voxy:test/level1.glsl>",
                Map.of(
                        "voxy:test/level1.glsl", "#import <voxy:test/level2.glsl>\nfloat level1 = 1.0;",
                        "voxy:test/level2.glsl", "float level2 = 2.0;"
                )
        );

        assertTrue(parsed.contains("float level2 = 2.0;"));
        assertTrue(parsed.contains("float level1 = 1.0;"));
    }

    @Test
    void extractShaderSourceSupportsLegacyStringReturnType() throws Exception {
        assertEquals("legacy-shader", invokeExtractShaderSource("legacy-shader"));
    }

    @Test
    void extractShaderSourceSupportsStructuredReturnTypeWithSrcAccessor() throws Exception {
        assertEquals("modern-shader", invokeExtractShaderSource(new ParsedShaderStub("modern-shader")));
    }

    @Test
    void extractShaderSourceRejectsUnknownReturnTypes() {
        InvocationTargetException error = assertThrows(
                InvocationTargetException.class,
                () -> invokeExtractShaderSource(new Object())
        );

        assertEquals(IllegalArgumentException.class, error.getCause().getClass());
    }

    private static String invokeExtractShaderSource(Object value) throws Exception {
        Method method = ShaderLoader.class.getDeclaredMethod("extractShaderSource", Object.class);
        method.setAccessible(true);
        return (String) method.invoke(null, value);
    }

    @SuppressWarnings("unchecked")
    private static String invokeParseShaderSource(String source, Map<String, String> imports) throws Exception {
        Method method = ShaderLoader.class.getDeclaredMethod("parseShaderSource", String.class, Function.class);
        method.setAccessible(true);
        Function<String, String> resolver = imports::get;
        return (String) method.invoke(null, source, resolver);
    }

    private record ParsedShaderStub(String src) {}
}
