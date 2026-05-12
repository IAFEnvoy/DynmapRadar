package com.iafenvoy.dynmap.radar.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Thin wrapper around HttpURLConnection for GET requests with URL logging.
 */
public final class HttpUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("dynmap_radar_http");
    private static final String UA = "DynmapRadar/1.0";

    private HttpUtil() {
    }

    /**
     * GET a URL and return the response body as a String. Returns null on failure.
     */
    public static String getString(String url) {
        return getString(url, 5000, 5000);
    }

    /**
     * GET with configurable timeouts.
     */
    public static String getString(String url, int connectTimeout, int readTimeout) {
        LOGGER.debug("GET {}", url);
        // 1. 直接调用已有的字节方法
        byte[] bytes = getBytes(url, connectTimeout, readTimeout, Optional.of("application/json"));
        if (bytes == null) {
            return null;
        }
        // 2. 字节转字符串
        String result = new String(bytes, StandardCharsets.UTF_8);
        LOGGER.debug("GET {} -> {} bytes (string)", url, bytes.length);
        return result;
    }

    /**
     * GET a URL and return raw bytes. Returns null on failure.
     */
    public static byte[] getBytes(String url) {
        return getBytes(url, 5000, 10000, Optional.empty());
    }

    /**
     * GET raw bytes with configurable timeouts.
     */
    public static byte[] getBytes(String url, int connectTimeout, int readTimeout, Optional<String> acceptHeader) {
        LOGGER.debug("GET {}", url);
        try {
            HttpURLConnection conn = openConnection(url, connectTimeout, readTimeout);
            // 只有getString需要加这个header
            acceptHeader.ifPresent(h -> conn.setRequestProperty("Accept", h));

            int code = conn.getResponseCode();
            if (code == 200) {
                try (InputStream in = conn.getInputStream()) {
                    byte[] data = in.readAllBytes();
                    LOGGER.debug("GET {} -> {} bytes", url, data.length);
                    return data;
                } finally {
                    conn.disconnect();
                }
            } else {
                LOGGER.debug("GET {} -> HTTP {}", url, code);
                return null;
            }
        } catch (Exception e) {
            LOGGER.debug("GET {} failed: {}", url, e.getMessage());
            return null;
        }
    }

    private static HttpURLConnection openConnection(String url, int connectTimeout, int readTimeout) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setRequestProperty("User-Agent", UA);
        return conn;
    }
}
