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
package com.github.narise77.modern_plantuml_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.code.NoPlantumlCompressionException;
import net.sourceforge.plantuml.code.TranscoderUtil;
import net.sourceforge.plantuml.json.JsonObject;
import net.sourceforge.plantuml.klimt.drawing.svg.SvgGraphics;
import net.sourceforge.plantuml.png.MetadataTag;

/**
 * Meta data controller for the webapp.
 * Responses with the meta data of a specific file as text report or JSON object.
 */
@RestController
public class MetadataController {

    private static final Logger LOGGER = Logger.getLogger("com.plantuml");

    /**
     * Meta data of a diagram image fetched from an URL.
     *
     * @param src diagram image URL
     * @param format image format ({@code png} or {@code svg}) [optional]
     * @param accept http {@code Accept} header [optional]
     *
     * @return meta data as text report or JSON object
     *
     * @throws IOException if an input or output exception occurred
     */
    @GetMapping("/metadata/**")
    public ResponseEntity<String> fromUrl(
        @RequestParam(value = "src", required = false) String src,
        @RequestParam(value = "format", required = false) String format,
        @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept
    ) throws IOException {
        // validate URL
        final URL url = ProxyController.validateURL(src);
        // fetch image via URL and extract meta data from it
        final HttpURLConnection conn = ProxyController.getConnection(url);
        try (InputStream is = conn.getInputStream()) {
            return handleRequest(is, format, conn.getContentType(), null, accept);
        }
    }

    /**
     * Meta data of an uploaded diagram image.
     *
     * @param diagram uploaded diagram image
     * @param format image format ({@code png} or {@code svg}) [optional]
     * @param accept http {@code Accept} header [optional]
     *
     * @return meta data as text report or JSON object
     *
     * @throws IOException if an input or output exception occurred
     */
    @PostMapping("/metadata/**")
    public ResponseEntity<String> fromUpload(
        @RequestPart("diagram") MultipartFile diagram,
        @RequestParam(value = "format", required = false) String format,
        @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept
    ) throws IOException {
        final String original = diagram.getOriginalFilename();
        final String filename = original == null ? null : Paths.get(original).getFileName().toString();  // MS IE fix
        try (InputStream is = diagram.getInputStream()) {
            return handleRequest(is, format, null, filename, accept);
        }
    }

    /**
     * Response with the meta data of the PlantUML diagram image in the desired format.
     *
     * @param is            PlantUML diagram image as input stream
     * @param format        image format passed by the user via the request param `format` [optional]
     * @param contentType   the PlantUML diagram image content type [optional]
     * @param filename      the PlantUML diagram image filename [optional]
     * @param accept        http {@code Accept} header [optional]
     *
     * @return meta data response
     *
     * @throws IOException  if an input or output error occurred
     */
    private ResponseEntity<String> handleRequest(
        InputStream is,
        String format,
        String contentType,
        String filename,
        String accept
    ) throws IOException {
        // @see <a href="https://github.com/plantuml/plantuml/blob/26874fe610617738f958b7e8d012128fe621cff6/src/net/sourceforge/plantuml/Run.java#L570-L592">PlantUML Code</a>
        final FileFormat fileFormat = getImageFileFormat(format, contentType, filename);
        final Metadata metadata = getMetadata(is, fileFormat);
        final ResponseEntity.BodyBuilder response = ResponseEntity.ok()
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        if (accept != null && accept.toLowerCase().contains("json")) {
            return response
                .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                .body(metadata.toJson().toString());
        }
        return response
            .contentType(MediaType.parseMediaType(FileFormat.UTXT.getMimeType()))
            .body(metadata.toString());
    }

    /**
     * Get the file format from the PlantUML diagram image.
     *
     * @param format       image format passed by the user via the request param `format`
     * @param contentType  response content type where the PlantUML diagram image is from
     * @param filename     diagram image file name
     *
     * @return  PlantUML diagram image format
     *
     * @throws ResponseStatusException {@code 400} if the format is unknown
     */
    private static FileFormat getImageFileFormat(String format, String contentType, String filename) {
        if (format != null && !format.isEmpty()) {
            final FileFormat fileFormat = getImageFileFormatFromFormatString(format);
            if (fileFormat == null) {
                throw badRequest("The format \"" + format + "\" is not supported for meta data extraction.");
            }
            return fileFormat;
        }
        if (filename != null && !filename.isEmpty()) {
            final FileFormat fileFormat = getImageFileFormatFromFilenameExtension(filename);
            if (fileFormat != null) {
                return fileFormat;
            }
        }
        if (contentType != null && !contentType.isEmpty()) {
            final FileFormat fileFormat = getImageFileFormatFromContentType(contentType);
            if (fileFormat != null) {
                return fileFormat;
            }
        }
        throw badRequest("PlantUML image format detection failed. Please set \"format\" (format) manually.");
    }

    /**
     * Get the file format from the PlantUML diagram image based on a format string.
     *
     * @param format  image format, e.g. {@code png}
     *
     * @return  PlantUML diagram image format; if unknown format return `null`
     */
    private static FileFormat getImageFileFormatFromFormatString(String format) {
        switch (format.toLowerCase()) {
            case "png": return FileFormat.PNG;
            case "svg": return FileFormat.SVG;
            default: return null;
        }
    }

    /**
     * Get the file format from the PlantUML diagram image based on the filenames extension.
     *
     * @param filename  PlantUML image file name
     *
     * @return  PlantUML diagram image format; if unknown format return `null`
     */
    private static FileFormat getImageFileFormatFromFilenameExtension(String filename) {
        final int extensionPosition = filename.lastIndexOf(".");
        if (extensionPosition != -1) {
            return getImageFileFormatFromFormatString(filename.substring(extensionPosition + 1));
        }
        LOGGER.log(Level.WARNING, "File name \"{0}\" is malformed. Should be: name.extension", filename);
        return null;
    }

    /**
     * Get the file format from the PlantUML diagram image based on the response content type.
     *
     * @param contentType  response content type where the PlantUML diagram image is from
     *
     * @return  PlantUML diagram image format; if unknown content type return `null`
     */
    private static FileFormat getImageFileFormatFromContentType(String contentType) {
        final String ct = contentType.toLowerCase();
        if (ct.contains("png")) {
            return FileFormat.PNG;
        }
        if (ct.contains("svg") || ct.contains("xml")) {
            return FileFormat.SVG;
        }
        LOGGER.log(Level.SEVERE, "Unknown content type \"{0}\" for meta data extraction", contentType);
        return null;
    }

    /**
     * Get meta data from PlantUML diagram image.
     *
     * @param is        PlantUML diagram image input stream
     * @param format    PlantUML diagram image file format
     *
     * @return  parsed meta data
     *
     * @throws IOException  if an input or output error occurred
     */
    private static Metadata getMetadata(InputStream is, FileFormat format) throws IOException {
        switch (format) {
            case PNG:
                return getMetadataFromPNG(is);
            case SVG:
                final String svg;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    svg = br.lines().collect(Collectors.joining("\n"));
                }
                return getMetadataFromSVG(svg);
            default:
                throw badRequest("Unsupported image format.");
        }
    }

    /**
     * Get meta data from PNG PlantUML diagram image.
     *
     * Challenge: PNG meta data is only a single String and contains more than the PlantUML diagram.
     * PNG meta data contains:
     *   1. decoded PlantUML code
     *   2. empty line
     *   3. version information
     * Notes:
     *   - in theory the meta data could contain the PlantUML `RawString` as well as the `PlainString`
     *     but since both are ALWAYS identical (methods to get them are identical), one will ALWAYS dropped.
     *     @see <a href="https://github.com/plantuml/plantuml/blob/26874fe610617738f958b7e8d012128fe621cff6/src/net/sourceforge/plantuml/core/UmlSource.java#L173-L189">PlantUML Code</a>
     *   - version information do not contain any empty lines
     * Solution: split meta data at the last occurring empty line the result in
     *   a. decoded PlantUML diagram
     *   b. version information
     *
     * @param is        PNG image input stream
     *
     * @return  parsed meta data
     *
     * @throws IOException  if an input or output error occurred
     */
    private static Metadata getMetadataFromPNG(InputStream is) throws IOException {
        final String rawMetadata = new MetadataTag(is, "plantuml").getData();
        if (rawMetadata == null) {
            throw badRequest("No meta data found.");
        }
        // parse meta data
        final Metadata metadata = new Metadata(rawMetadata.trim());
        metadata.decoded = metadata.rawContent.substring(0, metadata.rawContent.lastIndexOf("\n\n"));
        metadata.encoded = TranscoderUtil.getDefaultTranscoder().encode(metadata.decoded);
        metadata.version = metadata.rawContent.substring(rawMetadata.lastIndexOf("\n\n")).trim();
        // add additionally the encoded plantuml string to raw meta data since it's missing by default
        metadata.rawContent = metadata.encoded + "\n\n" + metadata.rawContent;
        return metadata;
    }

    /**
     * Get meta data from SVG PlantUML diagram image.
     * @see <a href="https://github.com/plantuml/plantuml/blob/26874fe610617738f958b7e8d012128fe621cff6/src/net/sourceforge/plantuml/Run.java#L574-L587">PlantUML Code</a>
     *
     * @param svg       PlantUML digram in SVG format
     *
     * @return  parsed meta data
     */
    private static Metadata getMetadataFromSVG(String svg) {
        final Metadata metadata = new Metadata();
        // search for meta data start token
        final int idx = svg.lastIndexOf(SvgGraphics.META_HEADER);
        if (idx == -1) {
            throw badRequest("No meta data found.");
        }
        // search for meta data end token
        final String part = svg.substring(idx + SvgGraphics.META_HEADER.length());
        final int idxEnd = part.indexOf("]");
        if (idxEnd == -1) {
            throw badRequest("Invalid meta data: No end token found.");
        }
        // parse meta data
        metadata.encoded = part.substring(0, idxEnd);
        try {
            metadata.decoded = TranscoderUtil.getDefaultTranscoderProtected().decode(metadata.encoded);
        } catch (NoPlantumlCompressionException ex) {
            throw badRequest("Invalid meta data: PlantUML diagram is corrupted.");
        }
        return metadata;
    }

    /**
     * Create a {@code 400 Bad Request} exception.
     *
     * @param message error message
     *
     * @return exception to throw
     */
    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Helper class to store meta data.
     */
    @SuppressWarnings("checkstyle:VisibilityModifier")
    private static class Metadata {
        public String rawContent;
        public String decoded;
        public String encoded;
        public String version;

        Metadata() { }
        Metadata(String rawMetadataContent) {
            rawContent = rawMetadataContent;
        }

        public JsonObject toJson() {
            JsonObject metadata = new JsonObject();
            metadata.add("encoded", encoded);
            metadata.add("decoded", decoded);
            if (version != null && !version.isEmpty()) {
                metadata.add("version", version);
            }
            return metadata;
        }

        @Override
        public String toString() {
            if (rawContent != null && !rawContent.isEmpty()) {
                return rawContent;
            }
            if (version == null || version.isEmpty()) {
                return encoded + "\n\n" + decoded;
            }
            return encoded + "\n\n" + decoded + "\n\n" + version;
        }
    }
}
