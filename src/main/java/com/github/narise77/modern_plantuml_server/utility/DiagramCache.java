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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-safe LRU in-memory cache for rendered diagram byte arrays.
 * Bounded by maximum entries and maximum total payload size in bytes to prevent memory exhaustion.
 */
public final class DiagramCache {

    private static final int DEFAULT_MAX_ENTRIES = 500;
    private static final long DEFAULT_MAX_WEIGHT_BYTES = 64 * 1024 * 1024L; // 64 MB

    private final int maxEntries;
    private final long maxWeightBytes;
    private long currentWeightBytes = 0;

    private final Map<String, byte[]> cache;

    public static final DiagramCache INSTANCE = new DiagramCache(DEFAULT_MAX_ENTRIES, DEFAULT_MAX_WEIGHT_BYTES);

    public DiagramCache(int maxEntries, long maxWeightBytes) {
        this.maxEntries = maxEntries;
        this.maxWeightBytes = maxWeightBytes;
        this.cache = new LinkedHashMap<String, byte[]>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                if (size() > DiagramCache.this.maxEntries || currentWeightBytes > DiagramCache.this.maxWeightBytes) {
                    currentWeightBytes -= eldest.getValue().length;
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Compute a SHA-256 cache key from diagram parameters.
     *
     * @param uml textual diagram source
     * @param format output format name
     * @param idx diagram index
     * @param dpi diagram DPI (or null/empty)
     * @return hex encoded key string
     */
    public static String computeKey(String uml, String format, int idx, String dpi) {
        final String combined = (uml != null ? uml : "")
            + "\u0000" + (format != null ? format : "")
            + "\u0000" + idx
            + "\u0000" + (dpi != null ? dpi : "");
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(combined.hashCode());
        }
    }

    /**
     * Retrieve cached image bytes.
     *
     * @param key cache key
     * @return cached byte array, or null if miss
     */
    public synchronized byte[] get(String key) {
        if (key == null) {
            return null;
        }
        final byte[] bytes = cache.get(key);
        return bytes != null ? bytes.clone() : null;
    }

    /**
     * Store diagram byte array in cache.
     *
     * @param key cache key
     * @param data rendered byte array
     */
    public synchronized void put(String key, byte[] data) {
        if (key == null || data == null) {
            return;
        }
        // Don't cache exceptionally large individual diagrams (e.g. > 8MB)
        if (data.length > 8 * 1024 * 1024) {
            return;
        }
        final byte[] prev = cache.put(key, data.clone());
        if (prev != null) {
            currentWeightBytes -= prev.length;
        }
        currentWeightBytes += data.length;

        // Evict if exceeding max weight
        while (currentWeightBytes > maxWeightBytes && !cache.isEmpty()) {
            final Map.Entry<String, byte[]> eldest = cache.entrySet().iterator().next();
            currentWeightBytes -= eldest.getValue().length;
            cache.remove(eldest.getKey());
        }
    }

    /**
     * Clear all cached items.
     */
    public synchronized void clear() {
        cache.clear();
        currentWeightBytes = 0;
    }

    /**
     * Return current number of cached items.
     */
    public synchronized int size() {
        return cache.size();
    }

    /**
     * Return current weight in bytes.
     */
    public synchronized long getCurrentWeightBytes() {
        return currentWeightBytes;
    }
}
