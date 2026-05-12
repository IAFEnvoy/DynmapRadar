package com.iafenvoy.dynmap.radar.data;

import com.google.gson.*;
import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DynmapDataFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynmapRadarClient.MOD_ID + "_fetcher");
    private static final Gson GSON = new GsonBuilder().create();

    private List<DynmapPlayerData> currentPlayers = Collections.emptyList();
    private final MarkerState markerState = new MarkerState();
    private long lastTimestamp = 0;
    private long lastConfigHash = -1;
    private String currentDynmapUrl = "";
    private boolean initialFetchDone = false;

    public MarkerState getMarkerState() {
        return this.markerState;
    }

    public List<DynmapPlayerData> getCurrentPlayers() {
        return this.currentPlayers;
    }

    /**
     * Reset all state when disconnecting from a server.
     */
    public void reset() {
        this.currentPlayers = Collections.emptyList();
        this.markerState.clear();
        this.lastTimestamp = 0;
        this.lastConfigHash = -1;
        this.currentDynmapUrl = "";
        this.initialFetchDone = false;
    }

    public CompletableFuture<Void> doFetch() {
        return CompletableFuture.runAsync(() -> {
            ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
            String url = cfg.dynmapMain.trim();
            if (url.isEmpty()) return;
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

            // Detect URL change → reset
            if (!url.equals(this.currentDynmapUrl)) {
                this.currentDynmapUrl = url;
                this.lastTimestamp = 0;
                this.lastConfigHash = -1;
                this.initialFetchDone = false;
                this.currentPlayers = Collections.emptyList();
                // Reset marker state and load initial markers
                this.markerState.sets.clear();
                this.markerState.pointMarkers.clear();
                this.markerState.areaMarkers.clear();
                this.markerState.lineMarkers.clear();
                this.markerState.circleMarkers.clear();
                this.fetchInitialMarkers(url);
            }

            try {
                long now = System.currentTimeMillis();
                long since = this.lastTimestamp > 0 ? this.lastTimestamp : now - 1000;

                // 1. Fetch configuration (only on hash change or first fetch)
                try {
                    int newHash = this.fetchConfigAndGetHash(url);
                    if (newHash != this.lastConfigHash) {
                        this.lastConfigHash = newHash;
                        this.initialFetchDone = false; // force full player re-fetch
                    }
                } catch (Exception e) {
                    LOGGER.debug("Config fetch failed (non-fatal): {}", e.getMessage());
                }

                // 2. Fetch world updates
                String updateUrl = url + "/up/world/world/" + since;
                String json = HttpUtil.getString(updateUrl);
                if (json != null) {
                    DynmapApiResponse resp = GSON.fromJson(json, DynmapApiResponse.class);
                    if (resp != null) {
                        // Players: on initial fetch, take all. On update, merge.
                        if (this.initialFetchDone) {
                            this.mergePlayers(resp.players);
                        } else {
                            if (resp.players != null) {
                                this.currentPlayers = new ArrayList<>();
                                for (DynmapPlayerData p : resp.players) {
                                    if ("player".equals(p.type) && "world".equals(p.world))
                                        this.currentPlayers.add(p);
                                }
                            }
                            this.initialFetchDone = true;
                        }

                        // Process marker/area/line/circle updates
                        if (resp.updates != null) {
                            for (DynmapUpdate up : DynmapUpdate.parseUpdates(resp.updates)) {
                                if (up.isTile()) continue;
                                this.markerState.applyUpdate(up);
                            }
                        }
                    }
                }
                this.lastTimestamp = now;
            } catch (Exception e) {
                LOGGER.warn("Fetch error: {}", e.getMessage());
            }
        });
    }

    /**
     * Fetch config and return confighash.
     */
    private int fetchConfigAndGetHash(String baseUrl) {
        String json = HttpUtil.getString(baseUrl + "/up/configuration");
        if (json != null) {
            JsonObject cfg = JsonParser.parseString(json).getAsJsonObject();
            if (cfg.has("confighash")) {
                return cfg.get("confighash").getAsInt();
            }
        }
        return -1;
    }

    /**
     * Merge incremental player list: add/update by account, remove players no longer present.
     */
    private void mergePlayers(List<DynmapPlayerData> incoming) {
        if (incoming == null || incoming.isEmpty()) return;
        List<DynmapPlayerData> merged = new ArrayList<>(this.currentPlayers);
        for (DynmapPlayerData inc : incoming) {
            if (!"player".equals(inc.type) || !"world".equals(inc.world)) continue;
            boolean found = false;
            for (int i = 0; i < merged.size(); i++) {
                if (merged.get(i).account.equals(inc.account)) {
                    merged.set(i, inc); // update position
                    found = true;
                    break;
                }
            }
            if (!found) merged.add(inc);
        }
        this.currentPlayers = merged;
    }

    /**
     * Fetch the full marker snapshot from tiles/_markers_/marker_world.json on first connect.
     */
    private void fetchInitialMarkers(String baseUrl) {
        String json = HttpUtil.getString(baseUrl + "/tiles/_markers_/marker_world.json", 5000, 10000);
        if (json == null) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            // marker_world.json has a "sets" object at top level
            if (root.has("sets")) {
                JsonObject sets = root.getAsJsonObject("sets");
                for (String setId : sets.keySet()) {
                    JsonObject setObj = sets.getAsJsonObject(setId);
                    String label = setObj.has("label") ? setObj.get("label").getAsString() : setId;
                    int prio = setObj.has("layerprio") ? setObj.get("layerprio").getAsInt() : 0;
                    int minzoom = setObj.has("minzoom") ? setObj.get("minzoom").getAsInt() : -1;
                    int maxzoom = setObj.has("maxzoom") ? setObj.get("maxzoom").getAsInt() : -1;
                    boolean showlabels = !setObj.has("showlabels") || setObj.get("showlabels").getAsBoolean();
                    boolean hide = setObj.has("hide") && setObj.get("hide").getAsBoolean();
                    this.markerState.sets.put(setId, new MarkerState.MarkerSet(setId, label, prio, minzoom, maxzoom, showlabels, hide));
                    // Record Dynmap default
                    DynmapRadarClient.CONFIG_MANAGER.getConfig().recordLayerDefault(setId, hide);

                    // Process markers array within the set
                    if (setObj.has("markers")) {
                        JsonObject markersObj = setObj.getAsJsonObject("markers");
                        for (String markerId : markersObj.keySet()) {
                            JsonObject m = markersObj.getAsJsonObject(markerId);
                            String mlabel = m.has("label") ? m.get("label").getAsString() : markerId;
                            double mx = m.has("x") ? m.get("x").getAsDouble() : 0;
                            double my = m.has("y") ? m.get("y").getAsDouble() : 64;
                            double mz = m.has("z") ? m.get("z").getAsDouble() : 0;
                            String icon = m.has("icon") ? m.get("icon").getAsString() : "default";
                            String desc = m.has("desc") ? m.get("desc").getAsString() : null;
                            int mminz = m.has("minzoom") ? m.get("minzoom").getAsInt() : -1;
                            int mmaxz = m.has("maxzoom") ? m.get("maxzoom").getAsInt() : -1;
                            MarkerState.PointMarker pm = new MarkerState.PointMarker(markerId, mlabel, mx, my, mz, icon, desc, mminz, mmaxz);
                            this.markerState.pointMarkers.computeIfAbsent(setId, k -> new ArrayList<>()).add(pm);
                        }
                    }

                    // Process areas
                    if (setObj.has("areas")) {
                        JsonObject areasObj = setObj.getAsJsonObject("areas");
                        for (String areaId : areasObj.keySet()) {
                            JsonObject a = areasObj.getAsJsonObject(areaId);
                            String alabel = a.has("label") ? a.get("label").getAsString() : areaId;
                            double ytop = a.has("ytop") ? a.get("ytop").getAsDouble() : 64;
                            double ybottom = a.has("ybottom") ? a.get("ybottom").getAsDouble() : 64;
                            int weight = a.has("weight") ? a.get("weight").getAsInt() : 3;
                            double opacity = a.has("opacity") ? a.get("opacity").getAsDouble() : 0.8;
                            String color = a.has("color") ? a.get("color").getAsString() : "#FF0000";
                            double fillopacity = a.has("fillopacity") ? a.get("fillopacity").getAsDouble() : 0.35;
                            String fillcolor = a.has("fillcolor") ? a.get("fillcolor").getAsString() : "#FF0000";
                            String desc = a.has("desc") ? a.get("desc").getAsString() : null;
                            int aminz = a.has("minzoom") ? a.get("minzoom").getAsInt() : -1;
                            int amaxz = a.has("maxzoom") ? a.get("maxzoom").getAsInt() : -1;
                            double[] ax = parseDoubleArray(a, "x");
                            double[] az = parseDoubleArray(a, "z");
                            if (ax != null && az != null) {
                                int ci = parseHexColor(color);
                                int fi = parseHexColor(fillcolor);
                                MarkerState.AreaMarker am = new MarkerState.AreaMarker(areaId, alabel, ax, az, ytop, ybottom, weight, opacity, ci, fillopacity, fi, desc, aminz, amaxz);
                                this.markerState.areaMarkers.computeIfAbsent(setId, k -> new ArrayList<>()).add(am);
                            }
                        }
                    }

                    // Process lines
                    if (setObj.has("lines")) {
                        JsonObject linesObj = setObj.getAsJsonObject("lines");
                        for (String lineId : linesObj.keySet()) {
                            JsonObject l = linesObj.getAsJsonObject(lineId);
                            String llabel = l.has("label") ? l.get("label").getAsString() : lineId;
                            int weight = l.has("weight") ? l.get("weight").getAsInt() : 3;
                            double opacity = l.has("opacity") ? l.get("opacity").getAsDouble() : 0.8;
                            String color = l.has("color") ? l.get("color").getAsString() : "#FF0000";
                            String desc = l.has("desc") ? l.get("desc").getAsString() : null;
                            int lminz = l.has("minzoom") ? l.get("minzoom").getAsInt() : -1;
                            int lmaxz = l.has("maxzoom") ? l.get("maxzoom").getAsInt() : -1;
                            double[] lx = parseDoubleArray(l, "x");
                            double[] ly = parseDoubleArray(l, "y");
                            double[] lz = parseDoubleArray(l, "z");
                            if (lx != null && lz != null) {
                                if (ly == null) {
                                    ly = new double[lx.length];
                                    Arrays.fill(ly, 64);
                                }
                                int ci = parseHexColor(color);
                                MarkerState.PolyLineMarker lm = new MarkerState.PolyLineMarker(lineId, llabel, lx, ly, lz, weight, opacity, ci, desc, lminz, lmaxz);
                                this.markerState.lineMarkers.computeIfAbsent(setId, k -> new ArrayList<>()).add(lm);
                            }
                        }
                    }

                    // Process circles
                    if (setObj.has("circles")) {
                        JsonObject circlesObj = setObj.getAsJsonObject("circles");
                        for (String circleId : circlesObj.keySet()) {
                            JsonObject c = circlesObj.getAsJsonObject(circleId);
                            String clabel = c.has("label") ? c.get("label").getAsString() : circleId;
                            double cx = c.has("x") ? c.get("x").getAsDouble() : 0;
                            double cy = c.has("y") ? c.get("y").getAsDouble() : 64;
                            double cz = c.has("z") ? c.get("z").getAsDouble() : 0;
                            double xr = c.has("xr") ? c.get("xr").getAsDouble() : 5;
                            double zr = c.has("zr") ? c.get("zr").getAsDouble() : 5;
                            int weight = c.has("weight") ? c.get("weight").getAsInt() : 3;
                            double opacity = c.has("opacity") ? c.get("opacity").getAsDouble() : 0.8;
                            String color = c.has("color") ? c.get("color").getAsString() : "#FF0000";
                            double fillopacity = c.has("fillopacity") ? c.get("fillopacity").getAsDouble() : 0.35;
                            String fillcolor = c.has("fillcolor") ? c.get("fillcolor").getAsString() : "#FF0000";
                            String desc = c.has("desc") ? c.get("desc").getAsString() : null;
                            int cminz = c.has("minzoom") ? c.get("minzoom").getAsInt() : -1;
                            int cmaxz = c.has("maxzoom") ? c.get("maxzoom").getAsInt() : -1;
                            int ci = parseHexColor(color);
                            int fi = parseHexColor(fillcolor);
                            MarkerState.CircleMarker cm = new MarkerState.CircleMarker(circleId, clabel, cx, cy, cz, xr, zr, weight, opacity, ci, fillopacity, fi, desc, cminz, cmaxz);
                            this.markerState.circleMarkers.computeIfAbsent(setId, k -> new ArrayList<>()).add(cm);
                        }
                    }
                }
            }
            LOGGER.info("Loaded {} marker sets from initial config", this.markerState.sets.size());
        } catch (
                Exception e) {
            LOGGER.warn("Failed to fetch initial markers: {}", e.getMessage());
        }
    }

    private static double[] parseDoubleArray(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        JsonElement el = obj.get(key);
        if (!el.isJsonArray()) return null;
        JsonArray arr = el.getAsJsonArray();
        double[] result = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++)
            result[i] = arr.get(i).getAsDouble();
        return result;
    }

    private static int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0xFFFF0000;
        if (hex.startsWith("#")) hex = hex.substring(1);
        try {
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFF0000;
        }
    }
}
