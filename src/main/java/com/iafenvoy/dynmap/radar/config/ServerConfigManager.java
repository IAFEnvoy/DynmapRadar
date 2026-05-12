package com.iafenvoy.dynmap.radar.config;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Manages per-server config loading/saving.
 * Configs are stored at: .minecraft/dynmap_radar/<escaped_server_ip>.json
 * On singleplayer/offline, falls back to "singleplayer.json".
 */
public class ServerConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynmapRadarClient.MOD_ID + "_config");
    private static final Path CONFIG_DIR = Paths.get("dynmap_radar");

    private ServerConfig currentConfig = ServerConfig.defaults();
    private String currentServerKey = "";

    public ServerConfig getConfig() {
        return this.currentConfig;
    }

    /**
     * Reload config for the currently connected server.
     * Call this when joining a server/world.
     */
    public void reload() {
        String key = this.detectServerKey();
        if (!Objects.equals(key, this.currentServerKey)) {
            this.currentServerKey = key;
            this.currentConfig = this.load(key);
            LOGGER.info("Loaded config for server: {} -> dynmapMain={}", key, this.currentConfig.dynmapMain);
        }
    }

    /**
     * Save current config to disk.
     */
    public void save() {
        this.save(this.currentServerKey, this.currentConfig);
    }

    private String detectServerKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) {
            return "singleplayer";
        }
        ServerData server = mc.getCurrentServer();
        if (server != null && !server.ip.isEmpty()) {
            return sanitizeFileName(server.ip);
        }
        return "singleplayer";
    }

    public String getCurrentServerKey() {
        return this.currentServerKey;
    }

    private ServerConfig load(String key) {
        Path path = this.getConfigPath(key);
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                return ServerConfig.fromJson(json);
            } catch (IOException e) {
                LOGGER.warn("Failed to load config {}: {}", path, e.getMessage());
            }
        }
        return ServerConfig.defaults();
    }

    private void save(String key, ServerConfig config) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Path path = this.getConfigPath(key);
            Files.writeString(path, config.toJson(), StandardCharsets.UTF_8);
            LOGGER.info("Saved config for {} to {}", key, path);
        } catch (IOException e) {
            LOGGER.error("Failed to save config {}: {}", key, e.getMessage());
        }
    }

    private Path getConfigPath(String key) {
        return CONFIG_DIR.resolve(key + ".json");
    }

    private static String sanitizeFileName(String ip) {
        return ip.replace(':', '_').replace('/', '_').replace('\\', '_')
                .replace('*', '_').replace('?', '_').replace('"', '_')
                .replace('<', '_').replace('>', '_').replace('|', '_');
    }
}
