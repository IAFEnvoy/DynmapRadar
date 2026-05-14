package com.iafenvoy.dynmap.radar.data;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Holds all the live marker state for rendering.
 * All marker list maps are volatile to support atomic double-buffered refresh.
 * Lists inside the maps are always CopyOnWriteArrayList for safe cross-thread iteration.
 */
public class MarkerState {
    public final Map<String, MarkerSet> sets = new ConcurrentHashMap<>();
    public volatile Map<String, List<PointMarker>> pointMarkers = new ConcurrentHashMap<>();
    public volatile Map<String, List<AreaMarker>> areaMarkers = new ConcurrentHashMap<>();
    public volatile Map<String, List<PolyLineMarker>> lineMarkers = new ConcurrentHashMap<>();
    public volatile Map<String, List<CircleMarker>> circleMarkers = new ConcurrentHashMap<>();

    public void clear() {
        this.sets.clear();
        this.pointMarkers = new ConcurrentHashMap<>();
        this.areaMarkers = new ConcurrentHashMap<>();
        this.lineMarkers = new ConcurrentHashMap<>();
        this.circleMarkers = new ConcurrentHashMap<>();
    }

    /**
     * Atomically replace all marker data. Used by periodic full refresh
     * to avoid flicker and ConcurrentModificationException.
     */
    public void swapMarkerData(Map<String, List<PointMarker>> newPoints,
                               Map<String, List<AreaMarker>> newAreas,
                               Map<String, List<PolyLineMarker>> newLines,
                               Map<String, List<CircleMarker>> newCircles) {
        this.pointMarkers = newPoints;
        this.areaMarkers = newAreas;
        this.lineMarkers = newLines;
        this.circleMarkers = newCircles;
    }

    public void applyUpdate(DynmapUpdate up) {
        if (up.isSetUpdate()) {
            boolean hide = up.hide; // Dynmap may send hide flag in set updates
            this.sets.put(up.id, new MarkerSet(up.id, up.label, up.layerprio, up.minzoom, up.maxzoom, up.showlabels, hide));
            // Record default for this layer
            DynmapRadarClient.CONFIG_MANAGER.getConfig().recordLayerDefault(up.id, hide);
        } else if (up.isSetDelete()) {
            this.sets.remove(up.id);
            // Also clear all markers belonging to this set
            this.pointMarkers.remove(up.id);
            this.areaMarkers.remove(up.id);
            this.lineMarkers.remove(up.id);
            this.circleMarkers.remove(up.id);
        } else if (up.isMarkerUpdate()) {
            PointMarker m = new PointMarker(up.id, up.label, up.x, up.y, up.z, up.icon, up.desc, up.minzoom, up.maxzoom);
            this.pointMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).removeIf(p -> p.id.equals(up.id));
            this.pointMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).add(m);
        } else if (up.isMarkerDelete()) {
            List<PointMarker> list = this.pointMarkers.get(up.set);
            if (list != null) list.removeIf(p -> p.id.equals(up.id));
        } else if (up.isAreaUpdate()) {
            AreaMarker m = new AreaMarker(up.id, up.label, up.xArr, up.zArr, up.ytop, up.ybottom,
                    up.weight, up.opacity, up.parsedColor(), up.fillopacity, up.parsedFillColor(), up.desc, up.minzoom, up.maxzoom);
            this.areaMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).removeIf(a -> a.id.equals(up.id));
            this.areaMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).add(m);
        } else if (up.isAreaDelete()) {
            List<AreaMarker> list = this.areaMarkers.get(up.set);
            if (list != null) list.removeIf(a -> a.id.equals(up.id));
        } else if (up.isLineUpdate()) {
            PolyLineMarker m = new PolyLineMarker(up.id, up.label, up.xArr, up.yArr, up.zArr,
                    up.weight, up.opacity, up.parsedColor(), up.desc, up.minzoom, up.maxzoom);
            this.lineMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).removeIf(l -> l.id.equals(up.id));
            this.lineMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).add(m);
        } else if (up.isLineDelete()) {
            List<PolyLineMarker> list = this.lineMarkers.get(up.set);
            if (list != null) list.removeIf(l -> l.id.equals(up.id));
        } else if (up.isCircleUpdate()) {
            CircleMarker m = new CircleMarker(up.id, up.label, up.x, up.y, up.z, up.xr, up.zr,
                    up.weight, up.opacity, up.parsedColor(), up.fillopacity, up.parsedFillColor(), up.desc, up.minzoom, up.maxzoom);
            this.circleMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).removeIf(c -> c.id.equals(up.id));
            this.circleMarkers.computeIfAbsent(up.set, k -> new CopyOnWriteArrayList<>()).add(m);
        } else if (up.isCircleDelete()) {
            List<CircleMarker> list = this.circleMarkers.get(up.set);
            if (list != null) list.removeIf(c -> c.id.equals(up.id));
        }
    }

    public List<DynmapMarkerElement> collectElements(Predicate<String> layerFilter, boolean minimap) {
        List<DynmapMarkerElement> elements = new LinkedList<>();
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        boolean shapes = !minimap || cfg.minimapShowShapes;
        for (MarkerState.MarkerSet set : this.sets.values()) {
            if (!layerFilter.test(set.id())) continue;
            if (shapes) {
                this.areaMarkers.getOrDefault(set.id(), List.of()).stream().filter(Objects::nonNull).map(a -> DynmapMarkerElement.fromArea(set.id(), a)).forEach(elements::add);
                this.circleMarkers.getOrDefault(set.id(), List.of()).stream().filter(Objects::nonNull).map(c -> DynmapMarkerElement.fromCircle(set.id(), c)).forEach(elements::add);
                this.lineMarkers.getOrDefault(set.id(), List.of()).stream().filter(Objects::nonNull).map(lm -> DynmapMarkerElement.fromPolyLine(set.id(), lm)).forEach(elements::add);
            }
        }
        // Points go last so they render on top
        for (MarkerState.MarkerSet set : this.sets.values()) {
            if (!layerFilter.test(set.id())) continue;
            this.pointMarkers.getOrDefault(set.id(), List.of()).stream().filter(Objects::nonNull).map(p -> DynmapMarkerElement.fromPoint(set.id(), p)).forEach(elements::add);
        }
        return elements;
    }

    public record MarkerSet(String id, String label, int layerprio, int minzoom, int maxzoom, boolean showlabels,
                            boolean hide) {
    }

    public record PointMarker(String id, String label, double x, double y, double z, String icon, String desc,
                              int minzoom, int maxzoom) {
    }

    public record AreaMarker(String id, String label, double[] xArr, double[] zArr, double ytop, double ybottom,
                             int weight, double opacity, int color, double fillopacity, int fillcolor, String desc,
                             int minzoom, int maxzoom) {
    }

    public record PolyLineMarker(String id, String label, double[] xArr, double[] yArr, double[] zArr, int weight,
                                 double opacity, int color, String desc, int minzoom, int maxzoom) {
    }

    public record CircleMarker(String id, String label, double x, double y, double z, double xr, double zr,
                               int weight,
                               double opacity, int color, double fillopacity, int fillcolor, String desc,
                               int minzoom,
                               int maxzoom) {
    }
}
