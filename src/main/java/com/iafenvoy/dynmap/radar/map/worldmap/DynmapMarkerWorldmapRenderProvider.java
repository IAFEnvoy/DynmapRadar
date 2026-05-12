package com.iafenvoy.dynmap.radar.map.worldmap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.data.MarkerState;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.render.ElementRenderLocation;

import java.util.Iterator;
import java.util.List;

public class DynmapMarkerWorldmapRenderProvider extends MapElementRenderProvider<DynmapMarkerElement, DynmapPlayerElementRenderContext> {
    private Iterator<DynmapMarkerElement> iterator;

    @Override
    public void begin(int l, DynmapPlayerElementRenderContext ctx) {
        MarkerState state = DynmapRadarClient.DATA_FETCHER.getMarkerState();
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        this.iterator = (state != null ? state.collectElements(cfg::isLayerVisibleWorldmap, false) : List.<DynmapMarkerElement>of()).iterator();
    }

    @Override
    public boolean hasNext(int l, DynmapPlayerElementRenderContext ctx) {
        return this.iterator != null && this.iterator.hasNext();
    }

    @Override
    public DynmapMarkerElement getNext(int l, DynmapPlayerElementRenderContext ctx) {
        return this.iterator.next();
    }

    @Override
    public void end(int l, DynmapPlayerElementRenderContext ctx) {
        this.iterator = null;
    }

    @Override
    public void begin(ElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        this.begin(0, ctx);
    }

    @Override
    public boolean hasNext(ElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        return this.hasNext(0, ctx);
    }

    @Override
    public DynmapMarkerElement getNext(ElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        return this.getNext(0, ctx);
    }

    @Override
    public void end(ElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        this.end(0, ctx);
    }
}
