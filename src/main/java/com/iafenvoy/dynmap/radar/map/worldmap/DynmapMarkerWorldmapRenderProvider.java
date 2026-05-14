package com.iafenvoy.dynmap.radar.map.worldmap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.data.MarkerState;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import xaero.map.WorldMapSession;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.render.ElementRenderLocation;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DynmapMarkerWorldmapRenderProvider extends MapElementRenderProvider<DynmapMarkerElement, DynmapPlayerElementRenderContext> {
    private Iterator<DynmapMarkerElement> iterator;

    @Override
    public void begin(int l, DynmapPlayerElementRenderContext ctx) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        Map<String, String> dimMapping = cfg.dimensionMapping;

        // If no dimension mapping is configured, don't render anything
        if (dimMapping.isEmpty()) {
            this.iterator = Collections.emptyIterator();
            return;
        }

        // Check if current Xaero dimension matches any mapping
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            this.iterator = Collections.emptyIterator();
            return;
        }
        String currentXaeroDim = session.getMapProcessor().getMapWorld().getCurrentDimensionId().location().toString();
        boolean matches = false;
        for (String mappedDim : dimMapping.values()) {
            if (mappedDim.equals(currentXaeroDim)) {
                matches = true;
                break;
            }
        }
        if (!matches) {
            this.iterator = Collections.emptyIterator();
            return;
        }

        MarkerState state = DynmapRadarClient.DATA_FETCHER.getMarkerState();
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
