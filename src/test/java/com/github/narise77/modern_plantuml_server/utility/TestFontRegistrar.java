package com.github.narise77.modern_plantuml_server.utility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestFontRegistrar {

    @Test
    void testFallbackToOsFontsWhenFontPathNotSet() {
        FontRegistrar registrar = new FontRegistrar();
        registrar.setConfiguredFontPath(null);
        registrar.registerFonts();

        Set<String> families = registrar.getFontFamilies();
        assertNotNull(families);
        assertFalse(families.isEmpty(), "OS fonts should be populated when FONT_PATH is not set");
    }

    @Test
    void testFallbackToOsFontsWhenFontPathDoesNotExist() {
        FontRegistrar registrar = new FontRegistrar();
        registrar.setConfiguredFontPath("non/existing/path/for/fonts");
        registrar.registerFonts();

        Set<String> families = registrar.getFontFamilies();
        assertNotNull(families);
        assertFalse(families.isEmpty(), "OS fonts should be populated when FONT_PATH does not exist");
    }

    @Test
    void testLoadFontsFromSpecifiedFontPath(@TempDir Path tempDir) throws Exception {
        Path osFont = Path.of("C:/Windows/Fonts/arial.ttf");
        if (!Files.exists(osFont)) {
            Path linuxFontDir = Path.of("/usr/share/fonts");
            if (Files.exists(linuxFontDir)) {
                try (Stream<Path> stream = Files.walk(linuxFontDir)) {
                    osFont = stream.filter(p -> p.toString().toLowerCase().endsWith(".ttf"))
                                   .findFirst()
                                   .orElse(null);
                }
            }
        }

        if (osFont != null && Files.exists(osFont)) {
            Files.copy(osFont, tempDir.resolve("testfont.ttf"));

            FontRegistrar registrar = new FontRegistrar();
            registrar.setConfiguredFontPath(tempDir.toString());
            registrar.registerFonts();

            Set<String> families = registrar.getFontFamilies();
            assertNotNull(families);
            assertFalse(families.isEmpty(), "Fonts from specified FONT_PATH should be populated");
        }
    }

}
