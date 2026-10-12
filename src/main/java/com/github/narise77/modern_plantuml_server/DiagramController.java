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

import javax.imageio.IIOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import net.sourceforge.plantuml.FileFormat;
import com.github.narise77.modern_plantuml_server.utility.UmlExtractor;
import com.github.narise77.modern_plantuml_server.utility.UrlDataExtractor;

/**
 * Diagram controller of the webapp.
 * Produces the diagram from the compressed UML source contained in the end part of the requested URI
 * (GET) or from the textual UML source in the request body (POST).
 */
@Controller
public class DiagramController {

    /**
     * PNG image.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @RequestMapping(value = {"/png/**", "/img/**"}, method = {RequestMethod.GET, RequestMethod.POST})
    public void png(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, FileFormat.PNG);
    }

    /**
     * SVG image.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @RequestMapping(value = "/svg/**", method = {RequestMethod.GET, RequestMethod.POST})
    public void svg(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, FileFormat.SVG);
    }

    /**
     * Draw.io diagram XML.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @RequestMapping(value = "/drawio/**", method = {RequestMethod.GET, RequestMethod.POST})
    public void drawio(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String url = request.getRequestURI();
        final int idx = UrlDataExtractor.getIndex(url, 0);
        final String uml;
        if ("POST".equals(request.getMethod())) {
            uml = UmlExtractor.readBody(request);
        } else {
            try {
                uml = UmlExtractor.getUmlSource(UrlDataExtractor.getEncodedDiagram(url, ""));
            } catch (Exception e) {
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
                return;
            }
        }
        try {
            new DiagramResponse(response, FileFormat.SVG, request).sendDrawio(uml, idx);
        } catch (IIOException e) {
            // Browser has closed the connection, silently catch
        }
    }


    /**
     * EPS image.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @RequestMapping(value = "/eps/**", method = {RequestMethod.GET, RequestMethod.POST})
    public void eps(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, FileFormat.EPS);
    }

    /**
     * EPS image with text.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @RequestMapping(value = "/epstext/**", method = {RequestMethod.GET, RequestMethod.POST})
    public void epsText(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, FileFormat.EPS_TEXT);
    }

    /**
     * Base64 encoded PNG image.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @RequestMapping(value = "/base64/**", method = {RequestMethod.GET, RequestMethod.POST})
    public void base64(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, FileFormat.BASE64);
    }

    /**
     * ASCII art text.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @RequestMapping(value = "/txt/**", method = {RequestMethod.GET, RequestMethod.POST})
    public void txt(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response, FileFormat.UTXT);
    }

    /**
     * Image map of the diagram in HTML format.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @GetMapping("/map/**")
    public void map(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String url = request.getRequestURI();
        final String uml = UmlExtractor.getUmlSource(UrlDataExtractor.getEncodedDiagram(url, ""));
        final int idx = UrlDataExtractor.getIndex(url, 0);
        try {
            new DiagramResponse(response, FileFormat.UTXT, request).sendMap(uml, idx);
        } catch (IIOException e) {
            // Browser has closed the connection, do nothing
        }
    }

    /**
     * Syntax check report in text format.
     *
     * @param request http request
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @GetMapping("/check/**")
    public void check(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String uml = UmlExtractor.getUmlSource(UrlDataExtractor.getEncodedDiagram(request.getRequestURI(), ""));
        try {
            new DiagramResponse(response, FileFormat.UTXT, request).sendCheck(uml);
        } catch (IIOException e) {
            // Browser has closed the connection, do nothing
        }
    }

    /**
     * Send the diagram of the request in the given format.
     *
     * @param request http request
     * @param response http response
     * @param format output format
     *
     * @throws IOException if an input or output exception occurred
     */
    private void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        FileFormat format
    ) throws IOException {
        final String url = request.getRequestURI();
        final int idx = UrlDataExtractor.getIndex(url, 0);
        final String uml;
        if ("POST".equals(request.getMethod())) {
            // textual diagram source from request body
            uml = UmlExtractor.readBody(request);
        } else {
            // build the UML source from the compressed request parameter
            try {
                uml = UmlExtractor.getUmlSource(UrlDataExtractor.getEncodedDiagram(url, ""));
            } catch (Exception e) {
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
                return;
            }
        }
        try {
            new DiagramResponse(response, format, request).sendDiagram(uml, idx);
        } catch (IIOException e) {
            // Browser has closed the connection, so the HTTP OutputStream is closed
            // Silently catch the exception to avoid annoying log
        }
    }

}
