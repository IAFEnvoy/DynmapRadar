package com.iafenvoy.dynmap.radar.map.minimap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.data.MarkerState;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DynmapMarkerMinimapProvider extends MinimapElementRenderProvider<DynmapMarkerElement, DynmapPlayerElementRenderContext> {
    private final List<DynmapMarkerElement> elements = new LinkedList<>();
    private Iterator<DynmapMarkerElement> iterator;

    @Override
    public void begin(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        MarkerState state = DynmapRadarClient.DATA_FETCHER.getMarkerState();
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        this.iterator = (state != null ? state.collectElements(cfg::isLayerVisibleMinimap, true) : List.<DynmapMarkerElement>of()).iterator();
    }

    @Override
    public boolean hasNext(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        return this.iterator != null && this.iterator.hasNext();
    }

    @Override
    public DynmapMarkerElement getNext(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        return this.iterator.next();
    }

    @Override
    public void end(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        this.iterator = null;
    }
}
