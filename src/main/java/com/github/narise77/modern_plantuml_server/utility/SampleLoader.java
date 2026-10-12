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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads sample diagrams from the directory specified by {@code SAMPLE_PATH},
 * or from {@code classpath*:/static/sample/*} if not specified.
 */
@Component
public class SampleLoader {

    private static final String SAMPLE_PATTERN = "classpath*:/static/sample/*";
    private static final Logger LOGGER = Logger.getLogger("com.plantuml");

    @Value("${SAMPLE_PATH:${plantuml.sample-path:}}")
    private String configuredSamplePath;

    private final List<SampleDiagram> samples = new ArrayList<>();

    /**
     * Load all sample diagrams from SAMPLE_PATH or classpath resources.
     */
    @PostConstruct
    public void loadSamples() {
        samples.clear();
        final String samplePath = resolveSamplePath();
        boolean loadedFromPath = false;

        if (samplePath != null && !samplePath.isEmpty()) {
            loadedFromPath = loadSamplesFromPath(samplePath);
        }

        // Fall back to classpath resources if SAMPLE_PATH is not set or empty
        if (!loadedFromPath || samples.isEmpty()) {
            loadSamplesFromClasspath();
        }
    }

    private String resolveSamplePath() {
        if (configuredSamplePath != null && !configuredSamplePath.trim().isEmpty()) {
            return configuredSamplePath.trim();
        }
        final String env = System.getenv("SAMPLE_PATH");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        final String prop = System.getProperty("SAMPLE_PATH");
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim();
        }
        return null;
    }

    private boolean loadSamplesFromPath(String samplePath) {
        final File dir = new File(samplePath);
        if (!dir.exists() || !dir.isDirectory()) {
            LOGGER.log(Level.WARNING, "Specified SAMPLE_PATH is not a valid directory: {0}", samplePath);
            return false;
        }

        try (Stream<Path> stream = Files.list(dir.toPath())) {
            final List<Path> files = stream
                .filter(Files::isRegularFile)
                .filter(this::isSampleFile)
                .sorted(Comparator.comparing(
                    p -> p.getFileName().toString(),
                    String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

            for (Path path : files) {
                try {
                    final String filename = path.getFileName().toString();
                    final String content = Files.readString(path, StandardCharsets.UTF_8);
                    String title = extractTitle(content);
                    if (title == null || title.isEmpty()) {
                        title = extractFirstComment(content);
                    }
                    if (title == null || title.isEmpty()) {
                        title = filename.replaceFirst("\\.[^.]+$", "");
                    }
                    samples.add(new SampleDiagram(filename, title, content));
                    LOGGER.log(
                        Level.INFO, "Loaded sample from SAMPLE_PATH: {0} ({1})",
                        new Object[] {filename, title}
                    );
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to read sample file: " + path, e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to list SAMPLE_PATH directory: " + samplePath, e);
            return false;
        }

        return !samples.isEmpty();
    }

    private boolean isSampleFile(Path path) {
        final String name = path.getFileName().toString();
        if (name.startsWith(".")) {
            return false;
        }
        final String lower = name.toLowerCase();
        return lower.endsWith(".plantuml")
            || lower.endsWith(".planuml")
            || lower.endsWith(".puml")
            || lower.endsWith(".uml")
            || lower.endsWith(".pu")
            || lower.endsWith(".txt");
    }

    private void loadSamplesFromClasspath() {
        LOGGER.log(Level.INFO, "Loading sample diagrams from classpath resources: {0}", SAMPLE_PATTERN);
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            final Resource[] resources = resolver.getResources(SAMPLE_PATTERN);
            final List<Resource> resourceList = new ArrayList<>();
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    resourceList.add(resource);
                }
            }
            // Sort by filename for stable ordering
            resourceList.sort(Comparator.comparing(
                Resource::getFilename,
                Comparator.nullsLast(String::compareToIgnoreCase)
            ));

            for (Resource resource : resourceList) {
                try {
                    final String filename = resource.getFilename();
                    if (filename == null) {
                        continue;
                    }
                    final String content = readResource(resource);
                    String title = extractTitle(content);
                    if (title == null || title.isEmpty()) {
                        title = extractFirstComment(content);
                    }
                    if (title == null || title.isEmpty()) {
                        title = filename.replaceFirst("\\.[^.]+$", "");
                    }
                    samples.add(new SampleDiagram(filename, title, content));
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to load sample resource: " + resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to resolve sample resources", e);
        }
    }

    /**
     * Setter for tests or programmatic configuration.
     *
     * @param configuredSamplePath sample directory path
     */
    public void setConfiguredSamplePath(String configuredSamplePath) {
        this.configuredSamplePath = configuredSamplePath;
    }

    /**
     * Get the loaded sample diagrams.
     *
     * @return unmodifiable list of sample diagrams
     */
    public List<SampleDiagram> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    /**
     * Read content of a resource into a UTF-8 string.
     *
     * @param resource resource to read
     * @return content string
     * @throws IOException on read error
     */
    private String readResource(Resource resource) throws IOException {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private static final Pattern TITLE_PATTERN = Pattern.compile(
        "(?im)^[ \\t]*title(?::|\\s+)(?:\"([^\"]+)\"|'([^']+)'|([^\\r\\n]+))"
    );

    /**
     * Extract title from PlantUML content (e.g. `title ガントチャート`).
     *
     * @param content PlantUML text
     * @return title string or null if not found
     */
    public static String extractTitle(String content) {
        if (content == null) {
            return null;
        }
        final Matcher matcher = TITLE_PATTERN.matcher(content);
        if (matcher.find()) {
            String title = matcher.group(1);
            if (title == null) {
                title = matcher.group(2);
            }
            if (title == null) {
                title = matcher.group(3);
            }
            if (title != null) {
                title = title.trim();
                if (title.startsWith("\"") && title.endsWith("\"") && title.length() >= 2) {
                    title = title.substring(1, title.length() - 1).trim();
                }
                if (!title.isEmpty()) {
                    return title;
                }
            }
        }
        return null;
    }

    /**
     * Extract the first comment text from PlantUML content.
     * Checks single-line comments starting with ' and block comments /' ... '/.
     *
     * @param content PlantUML text
     * @return first comment string or null if none
     */
    public static String extractFirstComment(String content) {
        if (content == null) {
            return null;
        }
        final String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.startsWith("'")) {
                final String comment = trimmed.substring(1).trim();
                if (!comment.isEmpty()) {
                    return comment;
                }
            }
        }
        final Pattern blockPattern = Pattern.compile("/'(.*?)'/", Pattern.DOTALL);
        final Matcher matcher = blockPattern.matcher(content);
        if (matcher.find()) {
            final String comment = matcher.group(1).trim();
            if (!comment.isEmpty()) {
                return comment;
            }
        }
        return null;
    }

    /**
     * Data object representing a sample diagram.
     */
    public static class SampleDiagram {
        private final String filename;
        private final String name;
        private final String code;

        public SampleDiagram(String filename, String name, String code) {
            this.filename = filename;
            this.name = name;
            this.code = code;
        }

        public String getFilename() {
            return filename;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }
    }
}
