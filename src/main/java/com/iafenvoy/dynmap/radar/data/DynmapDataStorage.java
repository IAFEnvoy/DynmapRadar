package com.iafenvoy.dynmap.radar.data;

import com.iafenvoy.dynmap.radar.map.DynmapPlayerElement;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Central thread-safe data storage for all Dynmap data.
 * 
 * All mutable state is owned by the fetcher thread which builds new
 * data off-screen, then atomically swaps the snapshot.
 * Render/command threads read the snapshot without any locking risk.
 */
public class DynmapDataStorage {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // All fields are volatile immutable collections - no CME possible on read
    private volatile List<DynmapPlayerData> players = List.of();
    private volatile Set<String> dynmapWorlds = Set.of();
    private volatile Map<String, MarkerState.MarkerSet> markerSets = Map.of();
    private volatile Map<String, List<MarkerState.PointMarker>> pointMarkers = Map.of();
    private volatile Map<String, List<MarkerState.AreaMarker>> areaMarkers = Map.of();
    private volatile Map<String, List<MarkerState.PolyLineMarker>> lineMarkers = Map.of();
    private volatile Map<String, List<MarkerState.CircleMarker>> circleMarkers = Map.of();

    // ======================== Atomic read ========================

    public List<DynmapPlayerData> getPlayers() { return players; }
    public Set<String> getDynmapWorlds() { return dynmapWorlds; }
    public Map<String, MarkerState.MarkerSet> getMarkerSets() { return markerSets; }
    public Map<String, List<MarkerState.PointMarker>> getPointMarkers() { return pointMarkers; }
    public Map<String, List<MarkerState.AreaMarker>> getAreaMarkers() { return areaMarkers; }
    public Map<String, List<MarkerState.PolyLineMarker>> getLineMarkers() { return lineMarkers; }
    public Map<String, List<MarkerState.CircleMarker>> getCircleMarkers() { return circleMarkers; }

    // ======================== Atomic write ========================

    public void updatePlayers(List<DynmapPlayerData> p) {
        lock.writeLock().lock();
        try { this.players = List.copyOf(p); }
        finally { lock.writeLock().unlock(); }
    }

    public void updateDynmapWorlds(Set<String> w) {
        lock.writeLock().lock();
        try { this.dynmapWorlds = Set.copyOf(w); }
        finally { lock.writeLock().unlock(); }
    }

    /** Atomically swap all marker data from maps built in the fetcher thread. */
    public void updateAllMarkers(
            Map<String, MarkerState.MarkerSet> newSets,
            Map<String, List<MarkerState.PointMarker>> newPoints,
            Map<String, List<MarkerState.AreaMarker>> newAreas,
            Map<String, List<MarkerState.PolyLineMarker>> newLines,
            Map<String, List<MarkerState.CircleMarker>> newCircles) {
        lock.writeLock().lock();
        try {
            this.markerSets = Collections.unmodifiableMap(new LinkedHashMap<>(newSets));
            this.pointMarkers = freezeMarkerMap(newPoints);
            this.areaMarkers = freezeMarkerMap(newAreas);
            this.lineMarkers = freezeMarkerMap(newLines);
            this.circleMarkers = freezeMarkerMap(newCircles);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Reset all data to empty (called on disconnect). */
    public void clear() {
        lock.writeLock().lock();
        try {
            this.players = List.of();
            this.dynmapWorlds = Set.of();
            this.markerSets = Map.of();
            this.pointMarkers = Map.of();
            this.areaMarkers = Map.of();
            this.lineMarkers = Map.of();
            this.circleMarkers = Map.of();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Deep-freeze a mutable map into an unmodifiable one with unmodifiable lists. */
    private static <T> Map<String, List<T>> freezeMarkerMap(Map<String, List<T>> src) {
        Map<String, List<T>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<T>> e : src.entrySet()) {
            out.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }
}
