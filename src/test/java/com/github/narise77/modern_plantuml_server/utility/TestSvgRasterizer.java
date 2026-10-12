package com.github.narise77.modern_plantuml_server.utility;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSvgRasterizer {

    private static final String SAMPLE_SVG =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"120\" height=\"80\" viewBox=\"0 0 120 80\">"
        + "<rect width=\"120\" height=\"80\" fill=\"#336699\" />"
        + "<circle cx=\"60\" cy=\"40\" r=\"30\" fill=\"#ffffff\" />"
        + "</svg>";

    @Test
    public void testRenderToImage() {
        BufferedImage image = SvgRasterizer.renderToImage(SAMPLE_SVG);
        Assertions.assertNotNull(image);
        Assertions.assertEquals(120, image.getWidth());
        Assertions.assertEquals(80, image.getHeight());
    }

    @Test
    public void testRenderToPng() throws IOException {
        byte[] pngBytes = SvgRasterizer.renderToPng(SAMPLE_SVG);
        Assertions.assertNotNull(pngBytes);
        Assertions.assertTrue(pngBytes.length > 0);

        // Verify PNG magic bytes: 0x89, 'P', 'N', 'G'
        Assertions.assertEquals((byte) 0x89, pngBytes[0]);
        Assertions.assertEquals((byte) 'P', pngBytes[1]);
        Assertions.assertEquals((byte) 'N', pngBytes[2]);
        Assertions.assertEquals((byte) 'G', pngBytes[3]);
    }

    @Test
    public void testNullAndEmptyHandling() {
        Assertions.assertNull(SvgRasterizer.renderToImage((String) null));
        Assertions.assertNull(SvgRasterizer.renderToImage("   "));
        Assertions.assertThrows(IOException.class, () -> SvgRasterizer.renderToPng(null));
    }
}
