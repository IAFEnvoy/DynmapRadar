package com.iafenvoy.dynmap.radar.map.worldmap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.data.DynmapDataFetcher;
import com.iafenvoy.dynmap.radar.data.DynmapPlayerData;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.render.ElementRenderLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Provides Dynmap player elements to the Xaero rendering pipeline.
 * Filters out players that are already loaded by the client.
 */
public class DynmapPlayerWorldmapRenderProvider extends MapElementRenderProvider<DynmapPlayerElement, DynmapPlayerElementRenderContext> {
    private final DynmapDataFetcher dataFetcher;
    private final List<DynmapPlayerElement> currentElements = new ArrayList<>();
    private Iterator<DynmapPlayerElement> iterator;
    private int currentLocation = -1;

    public DynmapPlayerWorldmapRenderProvider(DynmapDataFetcher dataFetcher) {
        this.dataFetcher = dataFetcher;
    }

    @Override
    public void begin(int location, DynmapPlayerElementRenderContext context) {
        this.currentLocation = location;
        this.refreshElements();
        this.iterator = this.currentElements.iterator();
    }

    @Override
    public boolean hasNext(int location, DynmapPlayerElementRenderContext context) {
        return this.iterator != null && this.iterator.hasNext();
    }

    @Override
    public DynmapPlayerElement getNext(int location, DynmapPlayerElementRenderContext context) {
        return this.iterator.next();
    }

    @Override
    public void end(int location, DynmapPlayerElementRenderContext context) {
        this.iterator = null;
        this.currentLocation = -1;
    }

    @Override
    public void begin(ElementRenderLocation location, DynmapPlayerElementRenderContext context) {
        this.begin(location.getIndex(), context);
    }

    @Override
    public boolean hasNext(ElementRenderLocation location, DynmapPlayerElementRenderContext context) {
        return this.hasNext(location.getIndex(), context);
    }

    @Override
    public DynmapPlayerElement getNext(ElementRenderLocation location, DynmapPlayerElementRenderContext context) {
        return this.getNext(location.getIndex(), context);
    }

    @Override
    public void end(ElementRenderLocation location, DynmapPlayerElementRenderContext context) {
        this.end(location.getIndex(), context);
    }

    /**
     * Refresh the element list from the latest fetched data,
     * excluding players that are loaded locally.
     */
    private void refreshElements() {
        List<DynmapPlayerData> players = this.dataFetcher.getCurrentPlayers();
        this.currentElements.clear();

        // Collect names of locally loaded players to exclude them
        Set<String> localPlayerNames = new HashSet<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (Player player : mc.level.players()) {
                localPlayerNames.add(player.getGameProfile().getName().toLowerCase());
                localPlayerNames.add(player.getName().getString().toLowerCase());
            }
        }

        // Build display name based on config
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();

        for (DynmapPlayerData p : players) {
            // Skip if this account matches a locally loaded player
            String accountLower = p.account.toLowerCase();
            String nameLower = p.name.toLowerCase();
            if (localPlayerNames.contains(accountLower) || localPlayerNames.contains(nameLower)) {
                continue;
            }

            String displayName = cfg.isNameMode() ? p.name : p.account;

            this.currentElements.add(new DynmapPlayerElement(p, displayName));
        }
    }
}
