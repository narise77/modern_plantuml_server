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

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller exposing Model Context Protocol (MCP) HTTP and SSE endpoints.
 */
@RestController
public class McpController {

    private final McpServer mcpServer;
    private final Map<String, SseEmitter> sseSessions = new ConcurrentHashMap<>();

    public McpController(McpServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    /**
     * Stateless JSON-RPC HTTP endpoint.
     * Handles POST /mcp and POST /mcp/rpc.
     *
     * @param requestBody JSON-RPC request string
     * @return JSON-RPC response
     */
    @PostMapping(value = {"/mcp", "/mcp/rpc"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleRpc(@RequestBody String requestBody) {
        final String response = mcpServer.handleMessage(requestBody);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    /**
     * MCP Server-Sent Events (SSE) stream endpoint.
     * Sends endpoint event with the URL to post messages to.
     *
     * @param request HTTP request
     * @return SseEmitter instance
     */
    @GetMapping(value = "/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse(HttpServletRequest request) {
        final SseEmitter emitter = new SseEmitter(0L); // Infinite timeout
        final String sessionId = UUID.randomUUID().toString();
        sseSessions.put(sessionId, emitter);

        emitter.onCompletion(() -> sseSessions.remove(sessionId));
        emitter.onTimeout(() -> sseSessions.remove(sessionId));
        emitter.onError(e -> sseSessions.remove(sessionId));

        try {
            final String contextPath = request.getContextPath();
            final String endpointUri = (contextPath != null ? contextPath : "") + "/mcp/message?sessionId=" + sessionId;
            emitter.send(SseEmitter.event().name("endpoint").data(endpointUri));
        } catch (IOException e) {
            sseSessions.remove(sessionId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * MCP message endpoint for active SSE sessions.
     *
     * @param sessionId active SSE session ID
     * @param messageJson incoming JSON-RPC message
     * @return HTTP 202 Accepted, or direct JSON-RPC response if no active SSE session
     */
    @PostMapping(value = "/mcp/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleSseMessage(
        @RequestParam(name = "sessionId", required = false) String sessionId,
        @RequestBody String messageJson
    ) {
        final String response = mcpServer.handleMessage(messageJson);

        if (sessionId != null && sseSessions.containsKey(sessionId)) {
            final SseEmitter emitter = sseSessions.get(sessionId);
            if (response != null && emitter != null) {
                try {
                    emitter.send(SseEmitter.event().name("message").data(response));
                } catch (IOException e) {
                    sseSessions.remove(sessionId);
                }
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }

        // Fallback for direct invocation without established SSE stream
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response);
    }
}
