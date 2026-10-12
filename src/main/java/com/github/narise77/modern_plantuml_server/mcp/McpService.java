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
package com.github.narise77.modern_plantuml_server.mcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.ErrorUml;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.NullOutputStream;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.code.Transcoder;
import net.sourceforge.plantuml.code.TranscoderUtil;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.core.DiagramDescription;
import net.sourceforge.plantuml.error.PSystemError;
import com.github.narise77.modern_plantuml_server.utility.SampleLoader;
import net.sourceforge.plantuml.version.Version;

/**
 * Service providing core PlantUML operations for MCP tools and resources.
 */
@Service
public class McpService {

    private final SampleLoader sampleLoader;
    private final Transcoder transcoder;

    public McpService(SampleLoader sampleLoader) {
        this.sampleLoader = sampleLoader;
        this.transcoder = TranscoderUtil.getDefaultTranscoder();
    }

    /**
     * Render PlantUML code to SVG, PNG, or ASCII text.
     *
     * @param code PlantUML code
     * @param format "svg", "png", or "txt"
     * @return map with content and metadata
     * @throws IOException on render error
     */
    public Map<String, Object> renderDiagram(String code, String format) throws IOException {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("PlantUML code cannot be empty");
        }
        final String fmt = (format != null) ? format.trim().toLowerCase() : "svg";
        final SourceStringReader reader = new SourceStringReader(code);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final Map<String, Object> result = new HashMap<>();

        if ("png".equals(fmt)) {
            reader.outputImage(out, new FileFormatOption(FileFormat.PNG, false));
            final byte[] pngBytes = out.toByteArray();
            final String base64 = Base64.getEncoder().encodeToString(pngBytes);
            result.put("format", "png");
            result.put("mimeType", "image/png");
            result.put("data", base64);
            result.put("isBinary", true);
        } else if ("txt".equals(fmt) || "atxt".equals(fmt) || "utxt".equals(fmt)) {
            reader.outputImage(out, new FileFormatOption(FileFormat.UTXT, false));
            final String text = out.toString(StandardCharsets.UTF_8);
            result.put("format", "txt");
            result.put("mimeType", "text/plain");
            result.put("data", text);
            result.put("isBinary", false);
        } else if ("drawio".equals(fmt)) {
            reader.outputImage(out, new FileFormatOption(FileFormat.SVG, false));
            final String svg = out.toString(StandardCharsets.UTF_8);
            final String drawioXml = com.github.narise77.modern_plantuml_server.utility.DrawioConverter.convert(svg);
            result.put("format", "drawio");
            result.put("mimeType", "application/vnd.jgraph.mxfile");
            result.put("data", drawioXml);
            result.put("isBinary", false);

            // Also generate PNG preview image so MCP clients can display an image preview alongside Draw.io XML
            try {
                final ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
                reader.outputImage(pngOut, new FileFormatOption(FileFormat.PNG, false));
                final String base64Png = Base64.getEncoder().encodeToString(pngOut.toByteArray());
                result.put("imageData", base64Png);
                result.put("imageMimeType", "image/png");
            } catch (Exception ignored) {
            }
        } else {
            // Default: SVG
            reader.outputImage(out, new FileFormatOption(FileFormat.SVG, false));
            final String svg = out.toString(StandardCharsets.UTF_8);
            result.put("format", "svg");
            result.put("mimeType", "image/svg+xml");
            result.put("data", svg);
            result.put("isBinary", false);
        }

        return result;
    }

    /**
     * Check syntax of PlantUML code.
     *
     * @param code PlantUML code
     * @return syntax check result map
     * @throws IOException on check error
     */
    public Map<String, Object> checkSyntax(String code) throws IOException {
        if (code == null) {
            code = "";
        }
        final SourceStringReader reader = new SourceStringReader(code);
        final DiagramDescription desc = reader.outputImage(
            new NullOutputStream(),
            new FileFormatOption(FileFormat.PNG, false)
        );

        final Map<String, Object> result = new HashMap<>();
        final String description = (desc != null) ? desc.getDescription() : "";

        final List<Map<String, Object>> errorList = new ArrayList<>();
        final Collection<BlockUml> blocks = reader.getBlocks();
        boolean hasSystemError = false;
        for (BlockUml block : blocks) {
            final Diagram diagram = block.getDiagram();
            if (diagram instanceof PSystemError) {
                hasSystemError = true;
                final PSystemError systemError = (PSystemError) diagram;
                for (ErrorUml err : systemError.getErrorsUml()) {
                    final Map<String, Object> errorDetail = new HashMap<>();
                    errorDetail.put("message", err.getError());
                    errorDetail.put("line", err.getLineLocation().getPosition());
                    errorList.add(errorDetail);
                }
            }
        }

        final boolean isError = hasSystemError
            || description.startsWith("(Error)")
            || description.toLowerCase().contains("syntax error")
            || blocks.isEmpty();

        result.put("valid", !isError);
        result.put("description", description.isEmpty() && isError ? "(Error) No valid diagram block found" : description);
        result.put("errors", errorList);
        return result;
    }

    /**
     * Encode PlantUML code into URL hash string.
     *
     * @param code PlantUML code
     * @return encoded string
     * @throws IOException on encode error
     */
    public String encode(String code) throws IOException {
        if (code == null) {
            code = "";
        }
        return transcoder.encode(code.trim());
    }

    /**
     * Decode PlantUML URL hash string back to source code.
     *
     * @param encoded encoded hash
     * @return decoded PlantUML code
     * @throws IOException on decode error
     */
    public String decode(String encoded) throws IOException {
        if (encoded == null || encoded.trim().isEmpty()) {
            return "";
        }
        return transcoder.decode(encoded.trim());
    }

    /**
     * Get available sample diagrams from SampleLoader.
     *
     * @return list of sample diagram info
     */
    public List<Map<String, String>> getSamples() {
        final List<SampleLoader.SampleDiagram> sampleList = sampleLoader.getSamples();
        final List<Map<String, String>> result = new ArrayList<>();
        for (SampleLoader.SampleDiagram s : sampleList) {
            final Map<String, String> item = new HashMap<>();
            item.put("filename", s.getFilename());
            item.put("name", s.getName());
            item.put("code", s.getCode());
            result.add(item);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Get server and PlantUML version information.
     *
     * @return version string
     */
    public String getVersion() {
        return Version.fullDescription();
    }
}
