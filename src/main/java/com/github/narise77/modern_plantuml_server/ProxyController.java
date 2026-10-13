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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.IIOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

/**
 * Proxy controller of the webapp.
 * Retrieves the diagram source of a web resource (web html page) and renders it.
 */
@Controller
public class ProxyController {

    /**
     * Old proxy request URI regex pattern.
     */
    private static final Pattern PROXY_PATTERN = Pattern.compile("/\\w+/proxy/((\\d+)/)?((\\w+)/)?(https?://[^@]*)");

    /**
     * Check if the URL is in a forbidden format (e.g. IP-Address).
     *
     * @param full URL to check
     *
     * @return true if forbidden
     */
    public static boolean forbiddenURL(String full) {
        if (full == null) {
            return true;
        }
        if (full.contains("@")) {
            return true;
        }
        if (!full.startsWith("https://") && !full.startsWith("http://")) {
            return true;
        }
        if (full.matches("^https?://[-#.0-9:\\[\\]+]+/.*")) {
            return true;
        }
        if (full.matches("^https?://[^.]+/.*")) {
            return true;
        }
        return full.matches("^https?://[^.]+$");
    }

    /**
     * Validate external URL.
     *
     * @param url URL to validate
     *
     * @return valid URL
     *
     * @throws ResponseStatusException {@code 400} if the URL is malformed or forbidden
     */
    public static URL validateURL(String url) {
        final URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException mue) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL malformed.");
        }
        if (forbiddenURL(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Forbidden URL format.");
        }
        return parsedUrl;
    }

    /**
     * Render the diagram source of the {@code src} URL.
     *
     * @param src diagram source URL
     * @param fmt output format name
     * @param idx diagram index
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @GetMapping("/proxy")
    public void proxy(
        @RequestParam(value = "src", required = false) String src,
        @RequestParam(value = "fmt", required = false) String fmt,
        @RequestParam(value = "idx", defaultValue = "0") int idx,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        final URL srcUrl = validateURL(src);

        // fetch diagram from URL
        final String uml;
        final HttpURLConnection conn = getConnection(srcUrl);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            uml = br.lines().collect(Collectors.joining("\n"));
        } finally {
            conn.disconnect();
        }

        // generate the response
        DiagramResponse dr = new DiagramResponse(response, getOutputFormat(fmt), request);
        try {
            // special handling for the MAP since it's not using "#sendDiagram()" like the other types
            if ("map".equals(fmt)) {
                dr.sendMap(uml, idx);
            } else {
                dr.sendDiagram(uml, idx);
            }
        } catch (IIOException e) {
            // Browser has closed the connection, so the HTTP OutputStream is closed
            // Silently catch the exception to avoid annoying log
        }
    }

    /**
     * Old proxy: {@code /proxy/[idx/][format/]https://...}.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @GetMapping("/proxy/**")
    public void oldProxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Matcher proxyMatcher = PROXY_PATTERN.matcher(request.getRequestURI());
        if (!proxyMatcher.matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL malformed.");
            return;
        }
        final String num = proxyMatcher.group(2); // Optional number of the diagram source
        final String format = proxyMatcher.group(4); // Expected format of the generated diagram
        final String sourceURL = proxyMatcher.group(5);
        if (forbiddenURL(sourceURL)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Forbidden URL format.");
            return;
        }

        final String source;
        final URL url = new URL(sourceURL);
        final java.net.URLConnection urlConn = url.openConnection();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()))) {
            source = br.lines().collect(Collectors.joining("\n")).trim();
        } finally {
            if (urlConn instanceof HttpURLConnection) {
                ((HttpURLConnection) urlConn).disconnect();
            }
        }
        final SourceStringReader reader = new SourceStringReader(source);
        final int n = num == null ? 0 : Integer.parseInt(num);

        final FileFormat fileFormat = getOldOutputFormat(format);
        response.addHeader("Content-Type", fileFormat.getMimeType());
        final byte[] imageBytes;
        try (ByteArrayOutputStream outstream = new ByteArrayOutputStream()) {
            reader.outputImage(outstream, n, new FileFormatOption(fileFormat, false));
            imageBytes = outstream.toByteArray();
        }
        DiagramResponse.writeBody(request, response, imageBytes);
    }

    /**
     * Get {@link FileFormat} instance from string.
     *
     * @param format file format name
     *
     * @return corresponding file format instance,
     *         if {@code format} is null or unknown the default {@link FileFormat#PNG} will be returned
     */
    private static FileFormat getOutputFormat(String format) {
        if (format == null) {
            return FileFormat.PNG;
        }
        switch (format.toLowerCase()) {
            case "png": return FileFormat.PNG;
            case "svg": return FileFormat.SVG;
            case "eps": return FileFormat.EPS;
            case "epstext": return FileFormat.EPS_TEXT;
            case "txt": return FileFormat.UTXT;
            case "map": return FileFormat.UTXT;
            default: return FileFormat.PNG;
        }
    }

    /**
     * Get {@link FileFormat} instance from string for the old proxy.
     *
     * @param format file format name
     *
     * @return corresponding file format instance,
     *         if {@code format} is null or unknown the default {@link FileFormat#PNG} will be returned
     */
    private static FileFormat getOldOutputFormat(String format) {
        if ("txt".equals(format)) {
            return FileFormat.ATXT;
        }
        if ("map".equals(format)) {
            return FileFormat.PNG;
        }
        return getOutputFormat(format);
    }

    /**
     * Get open http connection from URL.
     *
     * @param url URL to open connection
     *
     * @return open http connection
     *
     * @throws IOException if an input or output exception occurred
     */
    public static HttpURLConnection getConnection(final URL url) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        String token = System.getenv("HTTP_AUTHORIZATION");
        if (token != null) {
            conn.setRequestProperty("Authorization", token);
        }
        final String timeoutString = System.getenv("HTTP_PROXY_READ_TIMEOUT");
        int timeout = 10000; // 10 seconds as default
        if (timeoutString != null && timeoutString.matches("^\\d+$")) {
            timeout = Integer.parseInt(timeoutString);
        }
        conn.setReadTimeout(timeout);
        conn.connect();
        return conn;
    }

}
