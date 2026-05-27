package com.iafenvoy.dynmap.radar.data;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;

import java.util.*;
import java.util.function.Predicate;

/**
 * Pure data records for Dynmap markers. Mutable state management moved to DynmapDataStorage.
 */
public class MarkerState {

    /**
     * Build a renderable element list from immutable snapshot maps. Thread-safe: reads only.
     */
    public static List<DynmapMarkerElement> collectElements(
            Map<String, MarkerSet> sets,
            Map<String, List<PointMarker>> pointMarkers,
            Map<String, List<AreaMarker>> areaMarkers,
            Map<String, List<PolyLineMarker>> lineMarkers,
            Map<String, List<CircleMarker>> circleMarkers,
            Predicate<String> layerFilter, boolean minimap) {
        List<DynmapMarkerElement> elements = new ArrayList<>();
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        boolean shapes = !minimap || cfg.minimapShowShapes;
        for (MarkerSet set : sets.values()) {
            if (!layerFilter.test(set.id())) continue;
            if (shapes) {
                areaMarkers.getOrDefault(set.id(), List.of()).stream().filter(Objects::nonNull).map(a -> DynmapMarkerElement.fromArea(set.id(), a)).forEach(elements::add);
                circleMarkers.getOrDefault(set.id(), List.of()).stream().filter(Objects::nonNull).map(c -> DynmapMarkerElement.fromCircle(set.id(), c)).forEach(elements::add);
                lineMarkers.getOrDefault(set.id(), List.of()).stream().filter(Objects::nonNull).map(lm -> DynmapMarkerElement.fromPolyLine(set.id(), lm)).forEach(elements::add);
            }
        }
        for (MarkerSet set : sets.values()) {
            if (!layerFilter.test(set.id())) continue;
            pointMarkers.getOrDefault(set.id(), List.of()).stream().filter(Objects::nonNull).map(p -> DynmapMarkerElement.fromPoint(set.id(), p)).forEach(elements::add);
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
