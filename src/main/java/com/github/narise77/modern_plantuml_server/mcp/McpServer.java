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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles Model Context Protocol (MCP) JSON-RPC 2.0 requests.
 */
@Component
public class McpServer {

    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_NAME = "plantuml-mcp-server";
    private static final String SERVER_VERSION = "1.0.0";

    private final McpService mcpService;
    private final ObjectMapper objectMapper;

    public McpServer(McpService mcpService, ObjectMapper objectMapper) {
        this.mcpService = mcpService;
        this.objectMapper = objectMapper;
    }

    /**
     * Process a JSON-RPC request and produce a JSON-RPC response string.
     *
     * @param requestJson incoming JSON-RPC string
     * @return response JSON-RPC string, or null if notification without response
     */
    public String handleMessage(String requestJson) {
        if (requestJson == null) {
            return errorResponse(null, -32700, "Parse error: empty request");
        }
        requestJson = requestJson.trim();
        if (requestJson.startsWith("\uFEFF")) {
            requestJson = requestJson.substring(1).trim();
        }
        if (requestJson.isEmpty()) {
            return errorResponse(null, -32700, "Parse error: empty request");
        }
        try {
            final JsonNode root = objectMapper.readTree(requestJson);
            if (!root.isObject()) {
                return errorResponse(null, -32600, "Invalid Request: expected JSON object");
            }

            final JsonNode idNode = root.get("id");
            final JsonNode methodNode = root.get("method");

            if (methodNode == null || !methodNode.isTextual()) {
                return errorResponse(idNode, -32600, "Invalid Request: method is missing or not a string");
            }

            final String method = methodNode.asText();
            final JsonNode paramsNode = root.get("params");

            // Notification: initialized
            if ("notifications/initialized".equals(method)) {
                return null;
            }

            switch (method) {
                case "initialize":
                    return successResponse(idNode, handleInitialize(paramsNode));
                case "ping":
                    return successResponse(idNode, objectMapper.createObjectNode());
                case "tools/list":
                    return successResponse(idNode, handleToolsList());
                case "tools/call":
                    return successResponse(idNode, handleToolsCall(paramsNode));
                case "resources/list":
                    return successResponse(idNode, handleResourcesList());
                case "resources/read":
                    return successResponse(idNode, handleResourcesRead(paramsNode));
                case "prompts/list":
                    return successResponse(idNode, handlePromptsList());
                case "prompts/get":
                    return successResponse(idNode, handlePromptsGet(paramsNode));
                default:
                    return errorResponse(idNode, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            return errorResponse(null, -32603, "Internal error: " + e.getMessage());
        }
    }

    private ObjectNode handleInitialize(JsonNode params) {
        final ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);

        final ObjectNode capabilities = result.putObject("capabilities");
        capabilities.putObject("tools");
        capabilities.putObject("resources");
        capabilities.putObject("prompts");

        final ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);

        return result;
    }

    private ObjectNode handleToolsList() {
        final ObjectNode result = objectMapper.createObjectNode();
        final ArrayNode tools = result.putArray("tools");

        // 1. render_diagram
        ObjectNode tool1 = tools.addObject();
        tool1.put("name", "render_diagram");
        tool1.put("description", "Render PlantUML source code into SVG, PNG, ASCII text, or Draw.io XML diagram.");
        ObjectNode schema1 = tool1.putObject("inputSchema");
        schema1.put("type", "object");
        ObjectNode props1 = schema1.putObject("properties");
        ObjectNode codeProp1 = props1.putObject("code");
        codeProp1.put("type", "string");
        codeProp1.put("description", "PlantUML diagram code to render");
        ObjectNode formatProp1 = props1.putObject("format");
        formatProp1.put("type", "string");
        formatProp1.put("description", "Output format: 'svg', 'png', 'txt', or 'drawio' (default: 'svg')");
        ArrayNode formatEnum1 = formatProp1.putArray("enum");
        formatEnum1.add("svg").add("png").add("txt").add("drawio");
        schema1.putArray("required").add("code");

        // 2. check_syntax
        ObjectNode tool2 = tools.addObject();
        tool2.put("name", "check_syntax");
        tool2.put("description", "Check the syntax of PlantUML code and report errors without generating image.");
        ObjectNode schema2 = tool2.putObject("inputSchema");
        schema2.put("type", "object");
        ObjectNode props2 = schema2.putObject("properties");
        ObjectNode codeProp2 = props2.putObject("code");
        codeProp2.put("type", "string");
        codeProp2.put("description", "PlantUML diagram code to validate");
        schema2.putArray("required").add("code");

        // 3. encode_diagram
        ObjectNode tool3 = tools.addObject();
        tool3.put("name", "encode_diagram");
        tool3.put("description", "Encode PlantUML code into PlantUML standard URL hash string.");
        ObjectNode schema3 = tool3.putObject("inputSchema");
        schema3.put("type", "object");
        ObjectNode props3 = schema3.putObject("properties");
        ObjectNode codeProp3 = props3.putObject("code");
        codeProp3.put("type", "string");
        codeProp3.put("description", "PlantUML code to encode");
        schema3.putArray("required").add("code");

        // 4. decode_diagram
        ObjectNode tool4 = tools.addObject();
        tool4.put("name", "decode_diagram");
        tool4.put("description", "Decode PlantUML URL hash string back into original source text.");
        ObjectNode schema4 = tool4.putObject("inputSchema");
        schema4.put("type", "object");
        ObjectNode props4 = schema4.putObject("properties");
        ObjectNode encProp4 = props4.putObject("encoded");
        encProp4.put("type", "string");
        encProp4.put("description", "Encoded PlantUML hash string");
        schema4.putArray("required").add("encoded");

        // 5. get_samples
        ObjectNode tool5 = tools.addObject();
        tool5.put("name", "get_samples");
        tool5.put("description", "List available PlantUML sample templates (Activity, Component, Sequence, Mindmap, etc.).");
        ObjectNode schema5 = tool5.putObject("inputSchema");
        schema5.put("type", "object");

        return result;
    }

    private ObjectNode handleToolsCall(JsonNode params) throws IOException {
        if (params == null || !params.isObject()) {
            throw new IllegalArgumentException("params must be an object");
        }
        final String toolName = params.path("name").asText();
        final JsonNode args = params.path("arguments");

        final ObjectNode result = objectMapper.createObjectNode();
        final ArrayNode content = result.putArray("content");

        switch (toolName) {
            case "render_diagram": {
                final String code = args.has("code") ? args.path("code").asText("") : args.path("source").asText("");
                final String format = args.path("format").asText("svg");
                final Map<String, Object> renderRes = mcpService.renderDiagram(code, format);
                if ("drawio".equalsIgnoreCase(format)) {
                    if (renderRes.containsKey("imageData")) {
                        final ObjectNode imgItem = content.addObject();
                        imgItem.put("type", "image");
                        imgItem.put("data", (String) renderRes.get("imageData"));
                        imgItem.put("mimeType", (String) renderRes.get("imageMimeType"));
                    }
                    final ObjectNode textItem = content.addObject();
                    textItem.put("type", "text");
                    textItem.put("text", (String) renderRes.get("data"));
                } else {
                    final boolean isBinary = Boolean.TRUE.equals(renderRes.get("isBinary"));
                    final ObjectNode contentItem = content.addObject();
                    if (isBinary) {
                        contentItem.put("type", "image");
                        contentItem.put("data", (String) renderRes.get("data"));
                        contentItem.put("mimeType", (String) renderRes.get("mimeType"));
                    } else {
                        contentItem.put("type", "text");
                        contentItem.put("text", (String) renderRes.get("data"));
                    }
                }
                break;
            }
            case "check_syntax": {
                final String code = args.has("code") ? args.path("code").asText("") : args.path("source").asText("");
                final Map<String, Object> checkRes = mcpService.checkSyntax(code);
                final ObjectNode contentItem = content.addObject();
                contentItem.put("type", "text");
                contentItem.put("text", objectMapper.writeValueAsString(checkRes));
                break;
            }
            case "encode_diagram": {
                final String code = args.has("code") ? args.path("code").asText("") : args.path("source").asText("");
                final String encoded = mcpService.encode(code);
                final ObjectNode contentItem = content.addObject();
                contentItem.put("type", "text");
                contentItem.put("text", encoded);
                break;
            }
            case "decode_diagram": {
                final String encoded = args.path("encoded").asText("");
                final String decoded = mcpService.decode(encoded);
                final ObjectNode contentItem = content.addObject();
                contentItem.put("type", "text");
                contentItem.put("text", decoded);
                break;
            }
            case "get_samples": {
                final List<Map<String, String>> samples = mcpService.getSamples();
                final ObjectNode contentItem = content.addObject();
                contentItem.put("type", "text");
                contentItem.put("text", objectMapper.writeValueAsString(samples));
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        return result;
    }

    private ObjectNode handleResourcesList() {
        final ObjectNode result = objectMapper.createObjectNode();
        final ArrayNode resources = result.putArray("resources");

        ObjectNode r1 = resources.addObject();
        r1.put("uri", "plantuml://version");
        r1.put("name", "PlantUML Version");
        r1.put("description", "Server and PlantUML engine version information");
        r1.put("mimeType", "text/plain");

        ObjectNode r2 = resources.addObject();
        r2.put("uri", "plantuml://samples");
        r2.put("name", "Sample Diagrams");
        r2.put("description", "Catalog of built-in PlantUML sample templates");
        r2.put("mimeType", "application/json");

        return result;
    }

    private ObjectNode handleResourcesRead(JsonNode params) throws IOException {
        final String uri = params.path("uri").asText("");
        final ObjectNode result = objectMapper.createObjectNode();
        final ArrayNode contents = result.putArray("contents");

        if ("plantuml://version".equals(uri)) {
            ObjectNode item = contents.addObject();
            item.put("uri", uri);
            item.put("mimeType", "text/plain");
            item.put("text", mcpService.getVersion());
        } else if ("plantuml://samples".equals(uri)) {
            ObjectNode item = contents.addObject();
            item.put("uri", uri);
            item.put("mimeType", "application/json");
            item.put("text", objectMapper.writeValueAsString(mcpService.getSamples()));
        } else if (uri.startsWith("plantuml://samples/")) {
            final String name = uri.substring("plantuml://samples/".length());
            for (Map<String, String> s : mcpService.getSamples()) {
                if (name.equalsIgnoreCase(s.get("filename")) || name.equalsIgnoreCase(s.get("name"))) {
                    ObjectNode item = contents.addObject();
                    item.put("uri", uri);
                    item.put("mimeType", "text/vnd.plantuml");
                    item.put("text", s.get("code"));
                    return result;
                }
            }
            throw new IllegalArgumentException("Sample resource not found: " + uri);
        } else {
            throw new IllegalArgumentException("Resource not found: " + uri);
        }

        return result;
    }

    private ObjectNode handlePromptsList() {
        final ObjectNode result = objectMapper.createObjectNode();
        final ArrayNode prompts = result.putArray("prompts");

        ObjectNode p1 = prompts.addObject();
        p1.put("name", "design_diagram");
        p1.put("description", "Generate a clear, well-structured PlantUML diagram from requirements");
        ArrayNode args1 = p1.putArray("arguments");
        ObjectNode a1 = args1.addObject();
        a1.put("name", "description");
        a1.put("description", "Description of what the diagram should represent");
        a1.put("required", true);
        ObjectNode a2 = args1.addObject();
        a2.put("name", "type");
        a2.put("description", "Type of diagram (sequence, class, activity, component, state, etc.)");
        a2.put("required", false);

        ObjectNode p2 = prompts.addObject();
        p2.put("name", "explain_diagram");
        p2.put("description", "Explain and review existing PlantUML diagram architecture");
        ArrayNode args2 = p2.putArray("arguments");
        ObjectNode b1 = args2.addObject();
        b1.put("name", "code");
        b1.put("description", "PlantUML source code to explain");
        b1.put("required", true);

        return result;
    }

    private ObjectNode handlePromptsGet(JsonNode params) {
        final String name = params.path("name").asText("");
        final JsonNode args = params.path("arguments");
        final ObjectNode result = objectMapper.createObjectNode();
        final ArrayNode messages = result.putArray("messages");

        if ("design_diagram".equals(name)) {
            final String desc = args.path("description").asText("");
            final String type = args.path("type").asText("diagram");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            ObjectNode content = msg.putObject("content");
            content.put("type", "text");
            content.put("text", "Please create a well-structured PlantUML " + type + " for the following specification:\n\n"
                + desc + "\n\n"
                + "Requirements:\n"
                + "- Begin with @startuml and end with @enduml (or appropriate start/end tag)\n"
                + "- Include clear labels, participants/classes/states, and descriptive relationships\n"
                + "- Add meaningful comments");
        } else if ("explain_diagram".equals(name)) {
            final String code = args.path("code").asText("");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            ObjectNode content = msg.putObject("content");
            content.put("type", "text");
            content.put("text", "Please analyze the following PlantUML diagram and explain its architecture, components, and flow in detail:\n\n```plantuml\n"
                + code + "\n```");
        } else {
            throw new IllegalArgumentException("Unknown prompt: " + name);
        }

        return result;
    }

    private String successResponse(JsonNode idNode, ObjectNode resultNode) {
        final ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (idNode != null) {
            response.set("id", idNode);
        } else {
            response.putNull("id");
        }
        response.set("result", resultNode);
        return response.toString();
    }

    private String errorResponse(JsonNode idNode, int code, String message) {
        final ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (idNode != null) {
            response.set("id", idNode);
        } else {
            response.putNull("id");
        }
        final ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        return response.toString();
    }
}
