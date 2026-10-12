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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import net.sourceforge.plantuml.api.PlantumlUtils;
import net.sourceforge.plantuml.code.NoPlantumlCompressionException;
import net.sourceforge.plantuml.code.TranscoderUtil;
import net.sourceforge.plantuml.png.MetadataTag;
import com.github.narise77.modern_plantuml_server.utility.Configuration;
import com.github.narise77.modern_plantuml_server.utility.FontRegistrar;
import com.github.narise77.modern_plantuml_server.utility.SampleLoader;
import com.github.narise77.modern_plantuml_server.utility.UmlExtractor;
import com.github.narise77.modern_plantuml_server.utility.UrlDataExtractor;
import net.sourceforge.plantuml.version.Version;

/**
 * Web UI controller of the PlantUML server (editor and previewer).
 *
 * Original idea from Achim Abeling for Confluence macro.
 *
 * Modified by Arnaud Roques
 * Modified by Pablo Lalloni
 * Modified by Maxime Sinclair
 */
@Controller
public class PlantUmlController {

    /**
     * Default encoded uml text.
     * Bob -> Alice : hello
     */
    private static final String DEFAULT_ENCODED_TEXT = "SyfFKj2rKt3CoKnELR1Io4ZDoSa70000";

    /**
     * Bundled fonts.
     */
    private final FontRegistrar fontRegistrar;

    /**
     * Sample diagrams loader.
     */
    private final SampleLoader sampleLoader;

    /**
     * Create the controller.
     *
     * @param registrar bundled fonts
     * @param sampleLoader sample diagrams loader
     */
    public PlantUmlController(FontRegistrar registrar, SampleLoader sampleLoader) {
        this.fontRegistrar = registrar;
        this.sampleLoader = sampleLoader;
    }

    /**
     * Render the web UI for the requested diagram.
     *
     * @param request http request
     * @param model template variables
     *
     * @return template name or redirect
     *
     * @throws IOException if an input or output exception occurred
     */
    @GetMapping({"/", "/welcome", "/form", "/uml/**", "/start/**"})
    public String show(HttpServletRequest request, Model model) throws IOException {
        // textual diagram source
        final String text = getText(request).trim();

        // no Text form has been submitted
        if (text.isEmpty()) {
            return redirect(DEFAULT_ENCODED_TEXT, -1);
        }

        // diagram index to render
        final int idx = UrlDataExtractor.getIndex(request.getRequestURI());
        prepareTemplateVariables(request, model, text, idx);

        final String view = request.getParameter("view");
        if (view != null && view.equalsIgnoreCase("previewer")) {
            return "previewer";
        }
        return "index";
    }

    /**
     * Encode the submitted diagram source and redirect to it.
     *
     * @param request http request
     *
     * @return redirect
     */
    @PostMapping({"/", "/welcome", "/form", "/uml/**", "/start/**"})
    public String submit(HttpServletRequest request) {
        // diagram index to render
        final int idx = UrlDataExtractor.getIndex(request.getRequestURI());

        // encoded diagram source
        String encoded;
        try {
            encoded = TranscoderUtil.getDefaultTranscoder().encode(getText(request).trim());
        } catch (Exception e) {
            encoded = DEFAULT_ENCODED_TEXT;
            e.printStackTrace();
        }
        return redirect(encoded, idx);
    }

    /**
     * Get textual diagram.
     * Search for textual diagram in following order:
     * 1. URL
     * 2. metadata
     * 3. request parameter "text"
     *
     * @param request http request
     *
     * @return if successful textual diagram source; otherwise empty string
     *
     * @throws IOException if an input or output exception occurred
     */
    private String getText(final HttpServletRequest request) throws IOException {
        String text;
        // 1. URL
        try {
            text = TranscoderUtil.getDefaultTranscoder().decode(
                AsciiCoderController.getEncodedTextFromUrl(request, "uml")
            );
            if (text != null && !text.isEmpty()) {
                return text;
            }
        } catch (NoPlantumlCompressionException e) {
            // no textual diagram source available from Url
            // ignore and try 2. method (metadata) below
            // do not spam output console
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 2. metadata
        String metadata = request.getParameter("metadata");
        if (metadata != null) {
            try (InputStream img = getImage(new URL(metadata))) {
                MetadataTag metadataTag = new MetadataTag(img, "plantuml");
                String data = metadataTag.getData();
                if (data != null) {
                    return data;
                }
            }
        }
        // 3. request parameter text
        text = request.getParameter("text");
        if (text != null && !text.isEmpty()) {
            return text;
        }
        // nothing found
        return "";
    }

    /**
     * Prepare the thymeleaf template variables.
     *
     * @param request http request
     * @param model template variables
     * @param text textual diagram source
     * @param idx diagram index
     *
     * @throws IOException if an input or output exception occurred
     */
    private void prepareTemplateVariables(
        HttpServletRequest request,
        Model model,
        String text,
        int idx
    ) throws IOException {
        final String encoded = TranscoderUtil.getDefaultTranscoder().encode(text);
        final String index = (idx < 0) ? "" : String.valueOf(idx);
        // diagram sources
        model.addAttribute("encoded", encoded);
        model.addAttribute("decoded", text);
        model.addAttribute("index", index);
        model.addAttribute("diagramUrl", (index.isEmpty() ? "" : index + "/") + encoded);
        model.addAttribute("contextPath", request.getContextPath());
        // properties
        model.addAttribute("showSocialButtons", Configuration.get("SHOW_SOCIAL_BUTTONS"));
        model.addAttribute("showGithubRibbon", Configuration.get("SHOW_GITHUB_RIBBON"));
        model.addAttribute("fontFamilies", fontRegistrar.getFontFamilies());
        model.addAttribute("samples", sampleLoader.getSamples());
        model.addAttribute("plantumlVersion", Version.fullDescription());
        // map for diagram source if necessary
        String map = "";
        if (PlantumlUtils.hasCMapData(text)) {
            try {
                map = UmlExtractor.extractMap(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        model.addAttribute("map", map);
        model.addAttribute("hasMap", !map.isEmpty());
    }

    /**
     * Redirect to encoded uml text.
     *
     * @param encoded encoded uml text
     * @param index diagram index; negative for none
     *
     * @return redirect view name (relative to the context path)
     */
    private static String redirect(String encoded, int index) {
        if (index < 0) {
            return "redirect:/uml/" + encoded;
        }
        return "redirect:/uml/" + index + "/" + encoded;
    }

    /**
     * Get image input stream from URL.
     *
     * @param url URL to open connection
     *
     * @return response input stream from URL
     *
     * @throws IOException if an input or output exception occurred
     */
    private static InputStream getImage(URL url) throws IOException {
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setReadTimeout(10000); // 10 seconds
        con.connect();
        return con.getInputStream();
    }

}
