package com.iafenvoy.dynmap.radar.map.minimap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.data.DynmapDataStorage;
import com.iafenvoy.dynmap.radar.data.DynmapPlayerData;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.module.MinimapSession;

import java.util.*;

public class DynmapPlayerMinimapProvider extends MinimapElementRenderProvider<DynmapPlayerElement, DynmapPlayerElementRenderContext> {
    private final List<DynmapPlayerElement> elements = new ArrayList<>();
    private Iterator<DynmapPlayerElement> iterator;

    @Override
    public void begin(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        this.elements.clear();

        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        if (!cfg.enabled) {
            this.iterator = Collections.emptyIterator();
            return;
        }
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

        // Exclude locally-loaded players (minimap already renders them natively)
        Set<String> localNames = new HashSet<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) for (Player p : mc.level.players()) {
            localNames.add(p.getGameProfile().getName().toLowerCase());
            localNames.add(p.getName().getString().toLowerCase());
        }
        DynmapDataStorage storage = DynmapRadarClient.DATA_FETCHER.getStorage();
        for (DynmapPlayerData data : storage.getPlayers()) {
            if (localNames.contains(data.account.toLowerCase()) || localNames.contains(data.name.toLowerCase()))
                continue;
            String name = "ACCOUNT".equalsIgnoreCase(cfg.nameDisplayMode) ? data.account : data.name;
            this.elements.add(new DynmapPlayerElement(data, name));
        }
        this.iterator = this.elements.iterator();
    }

    @Override
    public boolean hasNext(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        return this.iterator != null && this.iterator.hasNext();
    }

    @Override
    public DynmapPlayerElement getNext(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        return this.iterator.next();
    }

    @Override
    public void end(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        this.iterator = null;
        this.elements.clear();
    }
}
