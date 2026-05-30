package com.iafenvoy.dynmap.radar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.iafenvoy.dynmap.radar.DynmapRadarClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-server configuration stored as JSON in the dynmap_radar folder.
 */
public class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("dynmapMain")
    public String dynmapMain = "";

    @SerializedName("nameDisplayMode")
    public String nameDisplayMode = "NAME";

    @SerializedName("headSize")
    public int headSize = 24;

    @SerializedName("updateInterval")
    public int updateInterval = 1000;

    /**
     * World-map layer visibility: setId -> shown (true) / hidden (false).
     * If a layer is absent from this map, Dynmap's default (hide flag) is used.
     */
    @SerializedName("worldLayerVisibility")
    public Map<String, Boolean> worldLayerVisibility = new LinkedHashMap<>();

    /**
     * Minimap layer visibility: same as above but for minimap.
     */
    @SerializedName("minimapLayerVisibility")
    public Map<String, Boolean> minimapLayerVisibility = new LinkedHashMap<>();

    /**
     * Dynmap server defaults for each layer (the hide flag from marker_world.json).
     */
    @SerializedName("layerDefaults")
    public Map<String, Boolean> layerDefaults = new LinkedHashMap<>();

    /**
     * Dimension mapping: dynmap world name -> Xaero dimension ResourceLocation.toString()
     */
    @SerializedName("dimensionMapping")
    public Map<String, String> dimensionMapping = new LinkedHashMap<>();

    @SerializedName("markerScale")
    public double markerScale = 2.0;

    @SerializedName("pointIconScale")
    public double pointIconScale = 1.0;

    @SerializedName("pointSizeFollowZoom")
    public boolean pointSizeFollowZoom = true;

    @SerializedName("headSizeFollowZoom")
    public boolean headSizeFollowZoom = false;

    @SerializedName("markerShowInMinimap")
    public boolean markerShowInMinimap = false;

    @SerializedName("minimapShowShapes")
    public boolean minimapShowShapes = false;

    @SerializedName("minimapCullRadius")
    public double minimapCullRadius = 200;

    @SerializedName("waypointColor")
    public int waypointColor = 0xFFFFFF;

    @SerializedName("pointMinScale")
    public double pointMinScale = 0;

    @SerializedName("enabled")
    public boolean enabled = true;

    public ServerConfig() {
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ServerConfig fromJson(String json) {
        return GSON.fromJson(json, ServerConfig.class);
    }

    public static ServerConfig defaults() {
        return new ServerConfig();
    }

    public boolean isNameMode() {
        return "NAME".equals(this.nameDisplayMode);
    }

    /**
     * Check if a layer is visible on the world map.
     */
    public boolean isLayerVisibleWorldmap(String setId) {
        return this.isLayerVisibleFromMap(this.worldLayerVisibility, setId);
    }

    /**
     * Check if a layer is visible on the minimap.
     */
    public boolean isLayerVisibleMinimap(String setId) {
        return this.isLayerVisibleFromMap(this.minimapLayerVisibility, setId);
    }

    private boolean isLayerVisibleFromMap(Map<String, Boolean> visibility, String setId) {
        if (visibility.containsKey(setId)) return visibility.get(setId);
        // Not configured yet: use Dynmap default (hide==true means hidden by default)
        Boolean hide = this.layerDefaults.get(setId);
        return hide == null || !hide;
    }

    /**
     * Record a new layer's default visibility from Dynmap.
     * Auto-saves the config so defaults persist across disconnects.
     */
    public void recordLayerDefault(String setId, boolean hide) {
        if (this.layerDefaults.containsKey(setId)) return;
        this.layerDefaults.put(setId, hide);
        DynmapRadarClient.CONFIG_MANAGER.save();
    }

    /**
     * Set a layer's visibility on the world map.
     */
    public void setLayerVisibleWorldmap(String setId, boolean shown) {
        this.worldLayerVisibility.put(setId, shown);
    }

    /**
     * Set a layer's visibility on the minimap.
     */
    public void setLayerVisibleMinimap(String setId, boolean shown) {
        this.minimapLayerVisibility.put(setId, shown);
    }

    /**
     * Reset a layer to Dynmap defaults (remove from both visibility maps).
     */
    public void resetLayer(String setId) {
        this.worldLayerVisibility.remove(setId);
        this.minimapLayerVisibility.remove(setId);
    }

    /**
     * Show all layers on both maps (clear all visibility entries = use defaults).
     */
    public void showAllLayers() {
        this.worldLayerVisibility.clear();
        this.minimapLayerVisibility.clear();
    }
}
