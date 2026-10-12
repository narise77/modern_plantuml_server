/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * Project Info:  https://plantuml.com
 *
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package com.github.narise77.modern_plantuml_server.utility;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Registers fonts with the JVM on startup so that PlantUML can use them
 * when rendering diagrams, and provides the list of available font families.
 * <p>
 * If the environment variable {@code FONT_PATH} is specified and exists,
 * fonts in that path are registered and displayed.
 * If {@code FONT_PATH} does not exist or is not set, OS fonts (and bundled fonts)
 * are displayed.
 */
@Component
public class FontRegistrar {

    private static final String FONTS_PATTERN = "classpath*:/fonts/*";

    private static final Logger LOGGER = Logger.getLogger("com.plantuml");

    @Value("${FONT_PATH:${plantuml.font-path:}}")
    private String configuredFontPath;

    /**
     * Sorted set of registered font family names.
     */
    private final Set<String> families = new TreeSet<>();

    /**
     * Register fonts based on FONT_PATH or OS fonts.
     */
    @PostConstruct
    public void registerFonts() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        // Always register bundled fonts with the JVM
        registerBundledFonts(ge);

        final String fontPath = resolveFontPath();
        boolean loadedFromFontPath = false;

        if (fontPath != null && !fontPath.isEmpty()) {
            loadedFromFontPath = loadFontsFromPath(fontPath, ge);
        }

        // If FONT_PATH does not exist or no fonts were found, display OS fonts
        if (!loadedFromFontPath || families.isEmpty()) {
            LOGGER.log(Level.INFO, "FONT_PATH is not set or not found. Loading OS fonts.");
            for (String osFamily : ge.getAvailableFontFamilyNames()) {
                if (osFamily != null && !osFamily.trim().isEmpty()) {
                    families.add(osFamily.trim());
                }
            }
        }
    }

    private String resolveFontPath() {
        if (configuredFontPath != null && !configuredFontPath.trim().isEmpty()) {
            return configuredFontPath.trim();
        }
        final String env = System.getenv("FONT_PATH");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        final String prop = System.getProperty("FONT_PATH");
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim();
        }
        return null;
    }

    private boolean loadFontsFromPath(String fontPath, GraphicsEnvironment ge) {
        final String[] pathTokens = fontPath.split("[" + File.pathSeparator + ",]+");
        boolean anyPathExisted = false;

        for (String token : pathTokens) {
            final String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final File file = new File(trimmed);
            if (!file.exists()) {
                LOGGER.log(Level.WARNING, "Specified FONT_PATH does not exist: {0}", trimmed);
                continue;
            }
            anyPathExisted = true;
            if (file.isFile()) {
                if (isFontFile(file.toPath())) {
                    registerFontFile(file, ge);
                }
            } else if (file.isDirectory()) {
                try (Stream<Path> stream = Files.walk(file.toPath())) {
                    stream.filter(Files::isRegularFile)
                          .filter(this::isFontFile)
                          .forEach(path -> registerFontFile(path.toFile(), ge));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to scan font directory: " + trimmed, e);
                }
            }
        }
        return anyPathExisted && !families.isEmpty();
    }

    private void registerFontFile(File file, GraphicsEnvironment ge) {
        try {
            final Font[] fonts = Font.createFonts(file);
            for (Font font : fonts) {
                ge.registerFont(font);
                families.add(font.getFamily());
                LOGGER.log(
                    Level.INFO, "Registered font from FONT_PATH: {0} ({1})",
                    new Object[] {font.getFontName(), font.getFamily()}
                );
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to register font: " + file.getAbsolutePath(), e);
        }
    }

    private boolean isFontFile(Path path) {
        final String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".ttf") || name.endsWith(".ttc") || name.endsWith(".otf");
    }

    private void registerBundledFonts(GraphicsEnvironment ge) {
        final Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(FONTS_PATTERN);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to list bundled fonts", e);
            return;
        }
        for (Resource resource : resources) {
            final String name = String.valueOf(resource.getFilename());
            final String lower = name.toLowerCase();
            if (!(lower.endsWith(".ttf") || lower.endsWith(".ttc") || lower.endsWith(".otf"))) {
                continue;
            }
            try (InputStream is = resource.getInputStream()) {
                for (Font font : Font.createFonts(is)) {
                    ge.registerFont(font);
                    LOGGER.log(
                        Level.INFO, "Registered bundled font: {0} ({1})",
                        new Object[] {font.getFontName(), font.getFamily()}
                    );
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to register bundled font: " + name, e);
            }
        }
    }

    /**
     * Setter for tests or programmatic configuration.
     *
     * @param configuredFontPath font path to use
     */
    public void setConfiguredFontPath(String configuredFontPath) {
        this.configuredFontPath = configuredFontPath;
    }

    /**
     * Get the registered font family names.
     *
     * @return sorted, unmodifiable set of font family names
     */
    public Set<String> getFontFamilies() {
        return Collections.unmodifiableSet(families);
    }

}
