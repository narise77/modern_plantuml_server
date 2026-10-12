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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import net.sourceforge.plantuml.code.TranscoderUtil;
import com.github.narise77.modern_plantuml_server.utility.UmlExtractor;
import com.github.narise77.modern_plantuml_server.utility.UrlDataExtractor;

/**
 * ASCII encoder and decoder controller for the webapp.
 * Encodes the diagram in text format or decodes the compressed diagram string.
 */
@RestController
public class AsciiCoderController {

    /**
     * Regex pattern to fetch last part of the URL.
     */
    private static final Pattern URL_PATTERN = Pattern.compile("^.*[^a-zA-Z0-9\\-\\_]([a-zA-Z0-9\\-\\_]+)");

    /**
     * Decode the compressed diagram string.
     *
     * @param request http request
     * @param response http response
     *
     * @return decoded textual diagram source
     *
     * @throws IOException if an input or output exception occurred
     */
    @GetMapping(value = "/coder/**", produces = "text/plain;charset=UTF-8")
    public String decode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String encodedText = getEncodedTextFromUrl(request, "coder");
        String text = "";
        try {
            text = TranscoderUtil.getDefaultTranscoder().decode(encodedText);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
        response.addHeader("Access-Control-Allow-Origin", "*");
        return text;
    }

    /**
     * Encode the textual diagram source of the request body.
     *
     * @param request http request
     * @param response http response
     *
     * @return encoded diagram string
     *
     * @throws IOException if an input or output exception occurred
     */
    @PostMapping(value = "/coder/**", produces = "text/plain;charset=UTF-8")
    public String encode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String uml = UmlExtractor.readBody(request);
        String encoded = "";
        try {
            encoded = TranscoderUtil.getDefaultTranscoder().encode(uml);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
        response.addHeader("Access-Control-Allow-Origin", "*");
        return encoded;
    }

    /**
     * Get encoded textual diagram source from URL.
     *
     * @param request http request which contains the source URL
     * @param path mapping path without leading or tailing slash, e.g. {@code coder}
     *
     * @return if successful encoded textual diagram source from URL; otherwise empty string
     */
    static String getEncodedTextFromUrl(HttpServletRequest request, String path) {
        // textual diagram source from request URI
        String url = request.getRequestURI();
        final String contextpath = "/" + path + "/";
        if (url.contains(contextpath) && !url.endsWith(contextpath)) {
            final String encoded = UrlDataExtractor.getEncodedDiagram(request.getRequestURI(), "");
            if (!encoded.isEmpty()) {
                return encoded;
            }
        }
        // textual diagram source from "url" parameter
        url = request.getParameter("url");
        if (url != null && !url.trim().isEmpty()) {
            // Catch the last part of the URL if necessary
            final Matcher matcher = URL_PATTERN.matcher(url);
            if (matcher.find()) {
                url = matcher.group(1);
            }
            return url;
        }
        // nothing found
        return "";
    }

}
