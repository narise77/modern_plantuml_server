package com.github.narise77.modern_plantuml_server.mcp;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.narise77.modern_plantuml_server.utility.SampleLoader;

public class TestMcpServer {

    private McpServer mcpServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        SampleLoader sampleLoader = new SampleLoader();
        sampleLoader.loadSamples();
        McpService mcpService = new McpService(sampleLoader);
        objectMapper = new ObjectMapper();
        mcpServer = new McpServer(mcpService, objectMapper);
    }

    @Test
    public void testInitialize() throws Exception {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        String response = mcpServer.handleMessage(request);
        Assertions.assertNotNull(response);

        JsonNode root = objectMapper.readTree(response);
        Assertions.assertEquals("2.0", root.path("jsonrpc").asText());
        Assertions.assertEquals(1, root.path("id").asInt());

        JsonNode result = root.path("result");
        Assertions.assertEquals("2024-11-05", result.path("protocolVersion").asText());
        Assertions.assertTrue(result.path("capabilities").has("tools"));
        Assertions.assertTrue(result.path("capabilities").has("resources"));
        Assertions.assertTrue(result.path("capabilities").has("prompts"));
        Assertions.assertEquals("plantuml-mcp-server", result.path("serverInfo").path("name").asText());
    }

    @Test
    public void testToolsList() throws Exception {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}";
        String response = mcpServer.handleMessage(request);
        Assertions.assertNotNull(response);

        JsonNode root = objectMapper.readTree(response);
        JsonNode tools = root.path("result").path("tools");
        Assertions.assertTrue(tools.isArray());
        Assertions.assertEquals(5, tools.size());

        boolean hasRender = false;
        boolean hasCheck = false;
        boolean hasEncode = false;
        boolean hasDecode = false;
        boolean hasSamples = false;

        for (JsonNode tool : tools) {
            String name = tool.path("name").asText();
            if ("render_diagram".equals(name)) hasRender = true;
            if ("check_syntax".equals(name)) hasCheck = true;
            if ("encode_diagram".equals(name)) hasEncode = true;
            if ("decode_diagram".equals(name)) hasDecode = true;
            if ("get_samples".equals(name)) hasSamples = true;
        }

        Assertions.assertTrue(hasRender && hasCheck && hasEncode && hasDecode && hasSamples);
    }

    @Test
    public void testToolCallRenderSvg() throws Exception {
        String request = "{"
            + "\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
            + "\"params\":{"
            + "  \"name\":\"render_diagram\","
            + "  \"arguments\":{\"code\":\"@startuml\\nBob -> Alice : hello\\n@enduml\",\"format\":\"svg\"}"
            + "}}";
        String response = mcpServer.handleMessage(request);
        Assertions.assertNotNull(response);

        JsonNode root = objectMapper.readTree(response);
        JsonNode content = root.path("result").path("content").get(0);
        Assertions.assertEquals("text", content.path("type").asText());
        String svg = content.path("text").asText();
        Assertions.assertTrue(svg.contains("<svg"), "Output should be SVG XML");
        Assertions.assertTrue(svg.contains("Bob"), "Output should contain participant");
    }

    @Test
    public void testToolCallRenderPng() throws Exception {
        String request = "{"
            + "\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\","
            + "\"params\":{"
            + "  \"name\":\"render_diagram\","
            + "  \"arguments\":{\"code\":\"@startuml\\nBob -> Alice : hello\\n@enduml\",\"format\":\"png\"}"
            + "}}";
        String response = mcpServer.handleMessage(request);
        Assertions.assertNotNull(response);

        JsonNode root = objectMapper.readTree(response);
        JsonNode content = root.path("result").path("content").get(0);
        Assertions.assertEquals("image", content.path("type").asText());
        Assertions.assertEquals("image/png", content.path("mimeType").asText());
        Assertions.assertFalse(content.path("data").asText().isEmpty(), "Should contain base64 image data");
    }

    @Test
    public void testToolCallRenderDrawio() throws Exception {
        String request = "{"
            + "\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"tools/call\","
            + "\"params\":{"
            + "  \"name\":\"render_diagram\","
            + "  \"arguments\":{\"code\":\"@startuml\\nBob -> Alice : hello\\n@enduml\",\"format\":\"drawio\"}"
            + "}}";
        String response = mcpServer.handleMessage(request);
        Assertions.assertNotNull(response);

        JsonNode root = objectMapper.readTree(response);
        JsonNode contentArr = root.path("result").path("content");
        Assertions.assertTrue(contentArr.size() >= 2, "Should return image and text content items");

        JsonNode imgItem = contentArr.get(0);
        Assertions.assertEquals("image", imgItem.path("type").asText());
        Assertions.assertEquals("image/png", imgItem.path("mimeType").asText());
        Assertions.assertFalse(imgItem.path("data").asText().isEmpty(), "Image data should not be empty");

        JsonNode textItem = contentArr.get(1);
        Assertions.assertEquals("text", textItem.path("type").asText());
        String xml = textItem.path("text").asText();
        Assertions.assertTrue(xml.contains("<mxfile"), "Should contain mxfile");
        Assertions.assertTrue(xml.contains("<mxGraphModel"), "Should contain mxGraphModel");
        Assertions.assertTrue(xml.contains("value=\"Bob\""), "Should contain Bob element");
        Assertions.assertFalse(xml.contains("data:image/svg+xml;base64,"), "Should not be embedded image");
    }

    @Test
    public void testToolCallCheckSyntax() throws Exception {
        // Correct diagram
        String validReq = "{"
            + "\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\","
            + "\"params\":{"
            + "  \"name\":\"check_syntax\","
            + "  \"arguments\":{\"code\":\"@startuml\\nBob -> Alice : hello\\n@enduml\"}"
            + "}}";
        String validRes = mcpServer.handleMessage(validReq);
        JsonNode validNode = objectMapper.readTree(validRes);
        String validJson = validNode.path("result").path("content").get(0).path("text").asText();
        Assertions.assertTrue(validJson.contains("\"valid\":true"));

        // Invalid diagram
        String invalidReq = "{"
            + "\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\","
            + "\"params\":{"
            + "  \"name\":\"check_syntax\","
            + "  \"arguments\":{\"code\":\"@startuml\\nBob -\\n@enduml\"}"
            + "}}";
        String invalidRes = mcpServer.handleMessage(invalidReq);
        JsonNode invalidNode = objectMapper.readTree(invalidRes);
        String invalidJson = invalidNode.path("result").path("content").get(0).path("text").asText();
        Assertions.assertTrue(invalidJson.contains("\"valid\":false"));
    }

    @Test
    public void testEncodeAndDecodeDiagram() throws Exception {
        String code = "@startuml\nBob -> Alice : hello\n@enduml";
        String encReq = "{"
            + "\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\","
            + "\"params\":{"
            + "  \"name\":\"encode_diagram\","
            + "  \"arguments\":{\"code\":\"" + code.replace("\n", "\\n") + "\"}"
            + "}}";
        String encRes = mcpServer.handleMessage(encReq);
        JsonNode encNode = objectMapper.readTree(encRes);
        String hash = encNode.path("result").path("content").get(0).path("text").asText();
        Assertions.assertFalse(hash.isEmpty());

        String decReq = "{"
            + "\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"tools/call\","
            + "\"params\":{"
            + "  \"name\":\"decode_diagram\","
            + "  \"arguments\":{\"encoded\":\"" + hash + "\"}"
            + "}}";
        String decRes = mcpServer.handleMessage(decReq);
        JsonNode decNode = objectMapper.readTree(decRes);
        String decodedCode = decNode.path("result").path("content").get(0).path("text").asText();
        Assertions.assertEquals(code, decodedCode);
    }

    @Test
    public void testResourcesAndPrompts() throws Exception {
        // Resources list
        String resListReq = "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"resources/list\"}";
        String resListRes = mcpServer.handleMessage(resListReq);
        JsonNode resListNode = objectMapper.readTree(resListRes);
        Assertions.assertTrue(resListNode.path("result").path("resources").size() >= 2);

        // Prompts list
        String promptListReq = "{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"prompts/list\"}";
        String promptListRes = mcpServer.handleMessage(promptListReq);
        JsonNode promptListNode = objectMapper.readTree(promptListRes);
        Assertions.assertEquals(2, promptListNode.path("result").path("prompts").size());

        // Unknown method
        String unknownReq = "{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"unknown/method\"}";
        String unknownRes = mcpServer.handleMessage(unknownReq);
        JsonNode unknownNode = objectMapper.readTree(unknownRes);
        Assertions.assertEquals(-32601, unknownNode.path("error").path("code").asInt());
    }
}
