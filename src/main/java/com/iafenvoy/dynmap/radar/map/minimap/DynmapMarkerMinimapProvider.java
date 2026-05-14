package com.iafenvoy.dynmap.radar.map.minimap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.data.MarkerState;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import net.minecraft.client.Minecraft;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.module.MinimapSession;

import java.util.*;

public class DynmapMarkerMinimapProvider extends MinimapElementRenderProvider<DynmapMarkerElement, DynmapPlayerElementRenderContext> {
    private final List<DynmapMarkerElement> elements = new LinkedList<>();
    private Iterator<DynmapMarkerElement> iterator;

    @Override
    public void begin(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        Map<String, String> dimMapping = cfg.dimensionMapping;

        if (dimMapping.isEmpty()) {
            this.iterator = Collections.emptyIterator();
            return;
        }

        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) {
            this.iterator = Collections.emptyIterator();
            return;
        }
        String currentXaeroDim = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.dimension().location().toString() : "";
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
