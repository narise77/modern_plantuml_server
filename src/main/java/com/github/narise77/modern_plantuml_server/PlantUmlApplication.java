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

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot entry point of the PlantUML server.
 */
@SpringBootApplication
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")  // instantiated by Spring
public class PlantUmlApplication {

    static {
        // Initialize the PlantUML server.
        DiagramResponse.init();
    }

    /**
     * Start the PlantUML server.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        final String envContextPath = System.getenv("PLANTUML_SERVER_CONTEXTPATH");
        if (envContextPath != null && !envContextPath.trim().isEmpty()) {
            final String trimmed = envContextPath.trim();
            if (!trimmed.startsWith("/")) {
                System.setProperty("server.servlet.context-path", "/" + trimmed);
            }
        }

        boolean isMcp = false;
        if (args != null) {
            for (String arg : args) {
                if ("--mcp".equalsIgnoreCase(arg) || "--stdio".equalsIgnoreCase(arg) || "mcp".equalsIgnoreCase(arg)) {
                    isMcp = true;
                    break;
                }
            }
        }

        if (isMcp) {
            System.setProperty("logging.level.root", "OFF");
            final SpringApplication app = new SpringApplication(PlantUmlApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            app.setBannerMode(Banner.Mode.OFF);
            app.run(args);
        } else {
            SpringApplication.run(PlantUmlApplication.class, args);
        }
    }

}
