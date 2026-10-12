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
import java.io.PrintStream;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import net.sourceforge.plantuml.syntax.LanguageDescriptor;

/**
 * Controller used to inspect the language keywords of the running PlantUML server.
 * Same as {@code java -jar plantuml.jar -language}
 */
@Controller
public class LanguageController {

    /**
     * Send the language keywords.
     *
     * @param response http response
     *
     * @throws IOException if an input or output exception occurred
     */
    @GetMapping("/language")
    public void language(HttpServletResponse response) throws IOException {
        response.setContentType("text/text");
        final PrintStream ps = new PrintStream(response.getOutputStream());
        new LanguageDescriptor().print(ps);
        ps.flush();
    }

}
