package me.cortex.voxy.client.core.gl.shader;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NeoForge-compatible shader loader for Voxy.
 *
 * Newer Sodium builds changed internal shader parsing substantially, so Voxy now
 * resolves its own #import graph and leaves runtime define injection to Shader.Builder.
 */
public class ShaderLoader {
    private static final Pattern IMPORT_PATTERN = Pattern.compile("#import <(?<namespace>.*):(?<path>.*)>");

    public static String parse(String id) {
        String shaderSource = getShaderSource(id);
        return parseShaderSource(shaderSource, ShaderLoader::getShaderSource);
    }

    static String parseShaderSource(String source, Function<String, String> importResolver) {
        List<String> lines = new ArrayList<>();
        parseInto(lines, source, importResolver);
        return "#version 460 core\n" + String.join("\n", lines);
    }

    private static void parseInto(List<String> output, String source, Function<String, String> importResolver) {
        for (String line : toLines(source)) {
            String trimmed = line.trim();

            if (trimmed.startsWith("#version")) {
                continue;
            }

            if (trimmed.startsWith("#import")) {
                Matcher matcher = IMPORT_PATTERN.matcher(trimmed);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Unknown import directive: " + line);
                }

                String importId = matcher.group("namespace") + ":" + matcher.group("path");
                String importedSource = importResolver.apply(importId);
                if (importedSource == null) {
                    throw new RuntimeException("Shader import not found: " + importId);
                }
                parseInto(output, importedSource, importResolver);
                continue;
            }

            output.add(line);
        }
    }

    private static List<String> toLines(String source) {
        return new BufferedReader(new StringReader(source)).lines().toList();
    }

    /**
     * Load shader source using Voxy's classloader.
     * Path format: "namespace:path" -> "/assets/{namespace}/shaders/{path}"
     */
    private static String getShaderSource(String id) {
        String[] parts = id.split(":", 2);
        String namespace = parts.length > 1 ? parts[0] : "voxy";
        String path = parts.length > 1 ? parts[1] : parts[0];

        String resourcePath = String.format("/assets/%s/shaders/%s", namespace, path);

        try (InputStream in = ShaderLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new RuntimeException("Shader not found: " + resourcePath + " (id=" + id + ")");
            }
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader source: " + resourcePath, e);
        }
    }

    // Retained for compatibility tests that lock down the previous investigation.
    static String extractShaderSource(Object parsedShader) {
        if (parsedShader instanceof String shaderSource) {
            return shaderSource;
        }

        for (String methodName : new String[]{"src", "source", "getSource"}) {
            String extracted = tryExtractShaderSource(parsedShader, methodName);
            if (extracted != null) {
                return extracted;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported ShaderParser return type: " + (parsedShader == null ? "null" : parsedShader.getClass().getName())
        );
    }

    private static String tryExtractShaderSource(Object parsedShader, String methodName) {
        if (parsedShader == null) {
            return null;
        }

        try {
            Method method = parsedShader.getClass().getMethod(methodName);
            if (method.getReturnType() == String.class) {
                return (String) method.invoke(parsedShader);
            }
        } catch (ReflectiveOperationException ignored) {
            // Try the next known accessor name.
        }

        return null;
    }
}
