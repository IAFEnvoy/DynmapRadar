package com.iafenvoy.dynmap.radar.map.minimap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.data.DynmapPlayerData;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DynmapPlayerMinimapProvider extends MinimapElementRenderProvider<DynmapPlayerElement, DynmapPlayerElementRenderContext> {
    private final List<DynmapPlayerElement> elements = new ArrayList<>();
    private Iterator<DynmapPlayerElement> iterator;

    @Override
    public void begin(MinimapElementRenderLocation l, DynmapPlayerElementRenderContext ctx) {
        this.elements.clear();
        // Exclude locally-loaded players (minimap already renders them natively)
        Set<String> localNames = new HashSet<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) for (Player p : mc.level.players()) {
            localNames.add(p.getGameProfile().getName().toLowerCase());
            localNames.add(p.getName().getString().toLowerCase());
        }
        for (DynmapPlayerData data : DynmapRadarClient.DATA_FETCHER.getCurrentPlayers()) {
            if (localNames.contains(data.account.toLowerCase()) || localNames.contains(data.name.toLowerCase()))
                continue;
            String name = "ACCOUNT".equalsIgnoreCase(DynmapRadarClient.CONFIG_MANAGER.getConfig().nameDisplayMode)
                    ? data.account : data.name;
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
