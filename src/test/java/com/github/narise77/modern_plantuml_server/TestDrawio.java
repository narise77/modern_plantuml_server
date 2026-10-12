package com.github.narise77.modern_plantuml_server;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.narise77.modern_plantuml_server.utils.TestUtils;
import com.github.narise77.modern_plantuml_server.utils.WebappTestCase;

public class TestDrawio extends WebappTestCase {

    @Test
    public void testSimpleDrawioDiagram() throws IOException {
        final URL url = new URL(getServerUrl() + "/drawio/" + TestUtils.SEQBOB);
        final URLConnection conn = url.openConnection();
        Assertions.assertTrue(conn.getContentType().toLowerCase().contains("application/xml"),
            "Response content type is not application/xml");
        String content = getContentText(conn);
        Assertions.assertTrue(content.contains("<mxfile"), "Missing mxfile root tag");
        Assertions.assertTrue(content.contains("<mxGraphModel"), "Missing mxGraphModel tag");
        // Verify native Draw.io elements are used rather than a single embedded base64 image
        Assertions.assertFalse(content.contains("data:image/svg+xml;base64,"), "Should not contain embedded svg image");
        Assertions.assertTrue(content.contains("vertex=\"1\""), "Missing vertex elements");
        Assertions.assertTrue(content.contains("value=\"Bob\""), "Missing Bob text cell");
        Assertions.assertTrue(content.contains("value=\"Alice\""), "Missing Alice text cell");
    }

    @Test
    public void testPostedDrawioDiagram() throws IOException {
        final URL url = new URL(getServerUrl() + "/drawio");
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-type", "text/plain");
        try (final OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
            writer.write("@startuml\nBob -> Alice : hello\n@enduml");
            writer.flush();
        }
        Assertions.assertEquals(200, conn.getResponseCode(), "Bad HTTP status received");
        String content = getContentText(conn);
        Assertions.assertTrue(content.contains("<mxfile"), "Missing mxfile root tag");
        Assertions.assertTrue(content.contains("value=\"hello\""), "Missing hello text cell");
        Assertions.assertTrue(content.contains("<mxGeometry"), "Missing mxGeometry elements");
    }
}
