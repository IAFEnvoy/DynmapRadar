package com.iafenvoy.dynmap.radar.data;

import com.google.gson.*;
import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DynmapDataFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynmapRadarClient.MOD_ID + "_fetcher");
    private static final Gson GSON = new GsonBuilder().create();
    private static final int FULL_REFRESH_INTERVAL = 30;

    private final DynmapDataStorage storage = new DynmapDataStorage();

    // Fetcher-only local state (only accessed from async fetch thread)
    private final Map<String, MarkerState.MarkerSet> localSets = new LinkedHashMap<>();
    private Map<String, List<MarkerState.PointMarker>> localPointMarkers = new ConcurrentHashMap<>();
    private Map<String, List<MarkerState.AreaMarker>> localAreaMarkers = new ConcurrentHashMap<>();
    private Map<String, List<MarkerState.PolyLineMarker>> localLineMarkers = new ConcurrentHashMap<>();
    private Map<String, List<MarkerState.CircleMarker>> localCircleMarkers = new ConcurrentHashMap<>();
    private Set<String> localDynmapWorlds = new HashSet<>();
    private long lastTimestamp = 0;
    private long lastConfigHash = -1;
    private String currentDynmapUrl = "";
    private boolean initialFetchDone = false;
    private int fetchCount = 0;

    public DynmapDataStorage getStorage() {
        return this.storage;
    }

    /**
     * Reset all state when disconnecting.
     */
    public void reset() {
        this.localSets.clear();
        this.localPointMarkers = new ConcurrentHashMap<>();
        this.localAreaMarkers = new ConcurrentHashMap<>();
        this.localLineMarkers = new ConcurrentHashMap<>();
        this.localCircleMarkers = new ConcurrentHashMap<>();
        this.localDynmapWorlds = new HashSet<>();
        this.lastTimestamp = 0;
        this.lastConfigHash = -1;
        this.currentDynmapUrl = "";
        this.initialFetchDone = false;
        this.fetchCount = 0;
        this.storage.clear();
    }

    public CompletableFuture<Void> doFetch() {
        return CompletableFuture.runAsync(() -> {
            ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
            String url = cfg.dynmapMain.trim();
            if (url.isEmpty()) return;
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

            if (!url.equals(this.currentDynmapUrl)) {
                this.currentDynmapUrl = url;
                this.lastTimestamp = 0;
                this.lastConfigHash = -1;
                this.initialFetchDone = false;
                this.fetchCount = 0;
                this.localSets.clear();
                this.localPointMarkers = new ConcurrentHashMap<>();
                this.localAreaMarkers = new ConcurrentHashMap<>();
                this.localLineMarkers = new ConcurrentHashMap<>();
                this.localCircleMarkers = new ConcurrentHashMap<>();
                this.localDynmapWorlds = new HashSet<>();
                this.storage.clear();
                this.fetchInitialMarkers(url);
                this.pushAllMarkersToStorage();
            }

            try {
                long now = System.currentTimeMillis();
                long since = this.lastTimestamp > 0 ? this.lastTimestamp : now - 1000;

                try {
                    int newHash = this.fetchConfigAndGetHash(url);
                    if (newHash != this.lastConfigHash) {
                        this.lastConfigHash = newHash;
                        this.initialFetchDone = false;
                    }
                } catch (Exception e) {
                    LOGGER.debug("Config fetch failed (non-fatal): {}", e.getMessage());
                }

                String updateUrl = url + "/up/world/world/" + since;
                String json = HttpUtil.getString(updateUrl);
                if (json != null) {
                    DynmapApiResponse resp = GSON.fromJson(json, DynmapApiResponse.class);
                    if (resp != null) {
                        if (resp.players != null) {
                            List<DynmapPlayerData> filtered = new ArrayList<>();
                            for (DynmapPlayerData p : resp.players) {
                                if ("player".equals(p.type) && "world".equals(p.world))
                                    filtered.add(p);
                            }
                            this.storage.updatePlayers(filtered);
                        }
                        this.initialFetchDone = true;

                        if (resp.updates != null) {
                            for (DynmapUpdate up : DynmapUpdate.parseUpdates(resp.updates)) {
                                if (up.isTile()) continue;
                                this.applyUpdateLocal(up);
                            }
                            this.pushAllMarkersToStorage();
                        }

                        this.fetchCount++;
                        if (this.fetchCount % FULL_REFRESH_INTERVAL == 0) {
                            this.refreshMarkersFromFullSnapshot(url);
                        }
                    }
                }
                this.lastTimestamp = now;
            } catch (Exception e) {
                LOGGER.warn("Fetch error: {}", e.getMessage());
            }
        });
    }

    // ======================== Local state mutations (fetcher thread only) ========================

    private void applyUpdateLocal(DynmapUpdate up) {
        if (up.isSetUpdate()) {
            this.localSets.put(up.id, new MarkerState.MarkerSet(up.id, up.label, up.layerprio, up.minzoom, up.maxzoom, up.showlabels, up.hide));
            DynmapRadarClient.CONFIG_MANAGER.getConfig().recordLayerDefault(up.id, up.hide);
        } else if (up.isSetDelete()) {
            this.localSets.remove(up.id);
            this.localPointMarkers.remove(up.id);
            this.localAreaMarkers.remove(up.id);
            this.localLineMarkers.remove(up.id);
            this.localCircleMarkers.remove(up.id);
        } else if (up.isMarkerUpdate()) {
            MarkerState.PointMarker m = new MarkerState.PointMarker(up.id, up.label, up.x, up.y, up.z, up.icon, up.desc, up.minzoom, up.maxzoom);
            this.localPointMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).removeIf(p -> p.id().equals(up.id));
            this.localPointMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).add(m);
        } else if (up.isMarkerDelete()) {
            List<MarkerState.PointMarker> list = this.localPointMarkers.get(up.set);
            if (list != null) list.removeIf(p -> p.id().equals(up.id));
        } else if (up.isAreaUpdate()) {
            MarkerState.AreaMarker m = new MarkerState.AreaMarker(up.id, up.label, up.xArr, up.zArr, up.ytop, up.ybottom,
                    up.weight, up.opacity, up.parsedColor(), up.fillopacity, up.parsedFillColor(), up.desc, up.minzoom, up.maxzoom);
            this.localAreaMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).removeIf(a -> a.id().equals(up.id));
            this.localAreaMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).add(m);
        } else if (up.isAreaDelete()) {
            List<MarkerState.AreaMarker> list = this.localAreaMarkers.get(up.set);
            if (list != null) list.removeIf(a -> a.id().equals(up.id));
        } else if (up.isLineUpdate()) {
            MarkerState.PolyLineMarker m = new MarkerState.PolyLineMarker(up.id, up.label, up.xArr, up.yArr, up.zArr,
                    up.weight, up.opacity, up.parsedColor(), up.desc, up.minzoom, up.maxzoom);
            this.localLineMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).removeIf(l -> l.id().equals(up.id));
            this.localLineMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).add(m);
        } else if (up.isLineDelete()) {
            List<MarkerState.PolyLineMarker> list = this.localLineMarkers.get(up.set);
            if (list != null) list.removeIf(l -> l.id().equals(up.id));
        } else if (up.isCircleUpdate()) {
            MarkerState.CircleMarker m = new MarkerState.CircleMarker(up.id, up.label, up.x, up.y, up.z, up.xr, up.zr,
                    up.weight, up.opacity, up.parsedColor(), up.fillopacity, up.parsedFillColor(), up.desc, up.minzoom, up.maxzoom);
            this.localCircleMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).removeIf(c -> c.id().equals(up.id));
            this.localCircleMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).add(m);
        } else if (up.isCircleDelete()) {
            List<MarkerState.CircleMarker> list = this.localCircleMarkers.get(up.set);
            if (list != null) list.removeIf(c -> c.id().equals(up.id));
        }
    }

    // ======================== Push to storage ========================

    private void pushAllMarkersToStorage() {
        this.storage.updateAllMarkers(
                new LinkedHashMap<>(this.localSets),
                new ConcurrentHashMap<>(this.localPointMarkers),
                new ConcurrentHashMap<>(this.localAreaMarkers),
                new ConcurrentHashMap<>(this.localLineMarkers),
                new ConcurrentHashMap<>(this.localCircleMarkers));
    }

    // ======================== Config & initial markers ========================

    private int fetchConfigAndGetHash(String baseUrl) {
        String json = HttpUtil.getString(baseUrl + "/up/configuration");
        if (json != null) {
            JsonObject cfg = JsonParser.parseString(json).getAsJsonObject();
            if (cfg.has("worlds") && cfg.get("worlds").isJsonArray()) {
                JsonArray worldsArr = cfg.getAsJsonArray("worlds");
                Set<String> worlds = new HashSet<>();
                for (JsonElement w : worldsArr) {
                    if (w.isJsonObject() && w.getAsJsonObject().has("name"))
                        worlds.add(w.getAsJsonObject().get("name").getAsString());
                }
                this.localDynmapWorlds = worlds;
                this.storage.updateDynmapWorlds(Set.copyOf(worlds));
            }
            if (cfg.has("confighash")) return cfg.get("confighash").getAsInt();
        }
        return -1;
    }

    private void refreshMarkersFromFullSnapshot(String baseUrl) {
        Map<String, List<MarkerState.PointMarker>> newPoints = new ConcurrentHashMap<>();
        Map<String, List<MarkerState.AreaMarker>> newAreas = new ConcurrentHashMap<>();
        Map<String, List<MarkerState.PolyLineMarker>> newLines = new ConcurrentHashMap<>();
        Map<String, List<MarkerState.CircleMarker>> newCircles = new ConcurrentHashMap<>();
        this.fetchInitialMarkersIntoMaps(baseUrl, newPoints, newAreas, newLines, newCircles);
        this.localPointMarkers = newPoints;
        this.localAreaMarkers = newAreas;
        this.localLineMarkers = newLines;
        this.localCircleMarkers = newCircles;
        this.pushAllMarkersToStorage();
    }

    private void fetchInitialMarkers(String baseUrl) {
        this.fetchInitialMarkersIntoMaps(baseUrl, this.localPointMarkers, this.localAreaMarkers, this.localLineMarkers, this.localCircleMarkers);
    }

    private void fetchInitialMarkersIntoMaps(String baseUrl,
                                             Map<String, List<MarkerState.PointMarker>> pointsOut,
                                             Map<String, List<MarkerState.AreaMarker>> areasOut,
                                             Map<String, List<MarkerState.PolyLineMarker>> linesOut,
                                             Map<String, List<MarkerState.CircleMarker>> circlesOut) {
        String json = HttpUtil.getString(baseUrl + "/tiles/_markers_/marker_world.json", 5000, 10000);
        if (json == null) return;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
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
                    this.localSets.put(setId, new MarkerState.MarkerSet(setId, label, prio, minzoom, maxzoom, showlabels, hide));
                    DynmapRadarClient.CONFIG_MANAGER.getConfig().recordLayerDefault(setId, hide);

                    if (setObj.has("markers")) {
                        JsonObject markersObj = setObj.getAsJsonObject("markers");
                        for (String markerId : markersObj.keySet()) {
                            JsonObject m = markersObj.getAsJsonObject(markerId);
                            pointsOut.computeIfAbsent(setId, k -> new CopyOnWriteArrayList<>()).add(
                                    new MarkerState.PointMarker(markerId,
                                            m.has("label") ? m.get("label").getAsString() : markerId,
                                            m.has("x") ? m.get("x").getAsDouble() : 0,
                                            m.has("y") ? m.get("y").getAsDouble() : 64,
                                            m.has("z") ? m.get("z").getAsDouble() : 0,
                                            m.has("icon") ? m.get("icon").getAsString() : "default",
                                            m.has("desc") ? m.get("desc").getAsString() : null,
                                            m.has("minzoom") ? m.get("minzoom").getAsInt() : -1,
                                            m.has("maxzoom") ? m.get("maxzoom").getAsInt() : -1));
                        }
                    }

                    if (setObj.has("areas")) {
                        JsonObject areasObj = setObj.getAsJsonObject("areas");
                        for (String areaId : areasObj.keySet()) {
                            JsonObject a = areasObj.getAsJsonObject(areaId);
                            double[] ax = parseDoubleArray(a, "x");
                            double[] az = parseDoubleArray(a, "z");
                            if (ax != null && az != null) {
                                areasOut.computeIfAbsent(setId, k -> new CopyOnWriteArrayList<>()).add(
                                        new MarkerState.AreaMarker(areaId,
                                                a.has("label") ? a.get("label").getAsString() : areaId,
                                                ax, az,
                                                a.has("ytop") ? a.get("ytop").getAsDouble() : 64,
                                                a.has("ybottom") ? a.get("ybottom").getAsDouble() : 64,
                                                a.has("weight") ? a.get("weight").getAsInt() : 3,
                                                a.has("opacity") ? a.get("opacity").getAsDouble() : 0.8,
                                                parseHexColor(a.has("color") ? a.get("color").getAsString() : "#FF0000"),
                                                a.has("fillopacity") ? a.get("fillopacity").getAsDouble() : 0.35,
                                                parseHexColor(a.has("fillcolor") ? a.get("fillcolor").getAsString() : "#FF0000"),
                                                a.has("desc") ? a.get("desc").getAsString() : null,
                                                a.has("minzoom") ? a.get("minzoom").getAsInt() : -1,
                                                a.has("maxzoom") ? a.get("maxzoom").getAsInt() : -1));
                            }
                        }
                    }

                    if (setObj.has("lines")) {
                        JsonObject linesObj = setObj.getAsJsonObject("lines");
                        for (String lineId : linesObj.keySet()) {
                            JsonObject l = linesObj.getAsJsonObject(lineId);
                            double[] lx = parseDoubleArray(l, "x");
                            double[] lz = parseDoubleArray(l, "z");
                            if (lx != null && lz != null) {
                                double[] ly = parseDoubleArray(l, "y");
                                if (ly == null) {
                                    ly = new double[lx.length];
                                    Arrays.fill(ly, 64);
                                }
                                linesOut.computeIfAbsent(setId, k -> new CopyOnWriteArrayList<>()).add(
                                        new MarkerState.PolyLineMarker(lineId,
                                                l.has("label") ? l.get("label").getAsString() : lineId,
                                                lx, ly, lz,
                                                l.has("weight") ? l.get("weight").getAsInt() : 3,
                                                l.has("opacity") ? l.get("opacity").getAsDouble() : 0.8,
                                                parseHexColor(l.has("color") ? l.get("color").getAsString() : "#FF0000"),
                                                l.has("desc") ? l.get("desc").getAsString() : null,
                                                l.has("minzoom") ? l.get("minzoom").getAsInt() : -1,
                                                l.has("maxzoom") ? l.get("maxzoom").getAsInt() : -1));
                            }
                        }
                    }

                    if (setObj.has("circles")) {
                        JsonObject circlesObj = setObj.getAsJsonObject("circles");
                        for (String circleId : circlesObj.keySet()) {
                            JsonObject c = circlesObj.getAsJsonObject(circleId);
                            circlesOut.computeIfAbsent(setId, k -> new CopyOnWriteArrayList<>()).add(
                                    new MarkerState.CircleMarker(circleId,
                                            c.has("label") ? c.get("label").getAsString() : circleId,
                                            c.has("x") ? c.get("x").getAsDouble() : 0,
                                            c.has("y") ? c.get("y").getAsDouble() : 64,
                                            c.has("z") ? c.get("z").getAsDouble() : 0,
                                            c.has("xr") ? c.get("xr").getAsDouble() : 5,
                                            c.has("zr") ? c.get("zr").getAsDouble() : 5,
                                            c.has("weight") ? c.get("weight").getAsInt() : 3,
                                            c.has("opacity") ? c.get("opacity").getAsDouble() : 0.8,
                                            parseHexColor(c.has("color") ? c.get("color").getAsString() : "#FF0000"),
                                            c.has("fillopacity") ? c.get("fillopacity").getAsDouble() : 0.35,
                                            parseHexColor(c.has("fillcolor") ? c.get("fillcolor").getAsString() : "#FF0000"),
                                            c.has("desc") ? c.get("desc").getAsString() : null,
                                            c.has("minzoom") ? c.get("minzoom").getAsInt() : -1,
                                            c.has("maxzoom") ? c.get("maxzoom").getAsInt() : -1));
                        }
                    }
                }
            }
            LOGGER.info("Loaded {} marker sets from initial config", this.localSets.size());
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch initial markers: {}", e.getMessage());
        }
    }

    // ======================== JSON helpers ========================

    private static double[] parseDoubleArray(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        JsonElement el = obj.get(key);
        if (!el.isJsonArray()) return null;
        JsonArray arr = el.getAsJsonArray();
        double[] result = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) result[i] = arr.get(i).getAsDouble();
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
