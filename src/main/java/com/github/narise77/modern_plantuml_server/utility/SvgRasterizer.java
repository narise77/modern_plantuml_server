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
package com.github.narise77.modern_plantuml_server.utility;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;

/**
 * Lightweight SVG rasterizer using JSVG (pure Java, zero dependencies, GraalVM friendly).
 */
public final class SvgRasterizer {

    private SvgRasterizer() {
        // utility class
    }

    /**
     * Render SVG input stream to BufferedImage.
     *
     * @param in SVG input stream
     * @return rendered BufferedImage, or null if loading failed
     */
    public static BufferedImage renderToImage(InputStream in) {
        if (in == null) {
            return null;
        }
        final SVGLoader loader = new SVGLoader();
        final SVGDocument document = loader.load(in, null, LoaderContext.createDefault());
        if (document == null) {
            return null;
        }

        final FloatSize size = document.size();
        final int width = Math.max(1, (int) Math.ceil(size.width));
        final int height = Math.max(1, (int) Math.ceil(size.height));

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            document.render((javax.swing.JComponent) null, g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Render SVG string content to BufferedImage.
     *
     * @param svgContent SVG XML text
     * @return rendered BufferedImage, or null if loading failed
     */
    public static BufferedImage renderToImage(String svgContent) {
        if (svgContent == null || svgContent.trim().isEmpty()) {
            return null;
        }
        final byte[] bytes = svgContent.getBytes(StandardCharsets.UTF_8);
        return renderToImage(new ByteArrayInputStream(bytes));
    }

    /**
     * Convert SVG string content to PNG byte array.
     *
     * @param svgContent SVG XML text
     * @return PNG bytes
     * @throws IOException on encoding error
     */
    public static byte[] renderToPng(String svgContent) throws IOException {
        final BufferedImage img = renderToImage(svgContent);
        if (img == null) {
            throw new IOException("Failed to parse and render SVG content");
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
