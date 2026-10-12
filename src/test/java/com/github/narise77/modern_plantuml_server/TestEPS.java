package com.github.narise77.modern_plantuml_server;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.narise77.modern_plantuml_server.utils.TestUtils;
import com.github.narise77.modern_plantuml_server.utils.WebappTestCase;


public class TestEPS extends WebappTestCase {

    /**
     * Verifies the generation of the EPS for the Bob -> Alice sample
     */
    @Test
    public void testSimpleSequenceDiagram() throws IOException {
        final URL url = new URL(getServerUrl() + "/eps/" + TestUtils.SEQBOB);
        final URLConnection conn = url.openConnection();
        // Analyze response
        // Verifies the Content-Type header
        Assertions.assertEquals(
            "application/postscript",
            conn.getContentType().toLowerCase(),
            "Response content type is not EPS"
        );
        // Get the content and verify its size
        String diagram = getContentText(conn);
        int diagramLen = diagram.length();
        Assertions.assertTrue(diagramLen > 7000);
        Assertions.assertTrue(diagramLen < 10000);
    }

}
