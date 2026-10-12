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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs MCP standard I/O (stdio) transport loop if started with --mcp or --stdio flag.
 * Useful for Claude Desktop, Cursor, or local CLI MCP integration.
 */
@Component
public class McpStdioRunner implements ApplicationRunner {

    private final McpServer mcpServer;

    public McpStdioRunner(McpServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        final boolean isMcpMode = args.containsOption("mcp")
            || args.containsOption("stdio")
            || args.getNonOptionArgs().contains("mcp");

        if (!isMcpMode) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1).trim();
                }
                if (line.isEmpty()) {
                    continue;
                }
                final String response = mcpServer.handleMessage(line);
                if (response != null) {
                    System.out.println(response);
                    System.out.flush();
                }
            }
        }
    }
}
