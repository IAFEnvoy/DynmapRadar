package com.iafenvoy.dynmap.radar;

import com.iafenvoy.dynmap.radar.config.DynmapRadarCommands;
import com.iafenvoy.dynmap.radar.config.ServerConfigManager;
import com.iafenvoy.dynmap.radar.data.DynmapDataFetcher;
import com.iafenvoy.dynmap.radar.map.IconManager;
import com.iafenvoy.dynmap.radar.map.worldmap.DynmapMarkerWorldmapRenderer;
import com.iafenvoy.dynmap.radar.map.worldmap.DynmapPlayerWorldmapRenderer;
import com.iafenvoy.dynmap.radar.map.minimap.DynmapMarkerMinimapRenderer;
import com.iafenvoy.dynmap.radar.map.minimap.DynmapPlayerMinimapRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.HudMod;
import xaero.hud.minimap.Minimap;
import xaero.map.WorldMap;

public class DynmapRadarClient implements ClientModInitializer {
    public static final DynmapDataFetcher DATA_FETCHER = new DynmapDataFetcher();
    public static final ServerConfigManager CONFIG_MANAGER = new ServerConfigManager();
    public static final String MOD_ID = "dynmap_radar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final IconManager ICON_MANAGER = new IconManager();
    private static DynmapPlayerWorldmapRenderer playerRenderer;
    public static DynmapMarkerWorldmapRenderer markerRenderer;
    private static DynmapPlayerMinimapRenderer playerMinimapRenderer;
    private static DynmapMarkerMinimapRenderer markerMinimapRenderer;
    private static long lastUpdateTick = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("DynmapRadar client initializing...");
        CONFIG_MANAGER.reload();
        DynmapRadarCommands.register();

        // Create WorldMap renderers — both use shared IconManager
        playerRenderer = DynmapPlayerWorldmapRenderer.create(DATA_FETCHER.getStorage());
        markerRenderer = DynmapMarkerWorldmapRenderer.create(ICON_MANAGER);

        // Create minimap renderers — marker shares IconManager
        playerMinimapRenderer = new DynmapPlayerMinimapRenderer();
        markerMinimapRenderer = new DynmapMarkerMinimapRenderer(ICON_MANAGER);

        ClientTickEvents.START_CLIENT_TICK.register(this::onClientTick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("Disconnected from server, clearing all map data...");
            DATA_FETCHER.reset();
            ICON_MANAGER.clearCache();
            ICON_MANAGER.clearDiskCache();
            lastUpdateTick = 0;
        });
    }

    private void onClientTick(Minecraft client) {
        // Wait for Xaero to be loaded
        if (!WorldMap.loaded || WorldMap.mapElementRenderHandler == null) {
            return;
        }

        // Register our renderers once Xaero is loaded
        if (!isRendererRegistered) {
            // WorldMap: register the WorldMap-native renderers
            WorldMap.mapElementRenderHandler.add(playerRenderer);
            WorldMap.mapElementRenderHandler.add(markerRenderer);

            // Minimap: register minimap renderers
            Minimap minimap = HudMod.INSTANCE.getMinimap();
            if (minimap != null && minimap.getOverMapRendererHandler() != null) {
                minimap.getOverMapRendererHandler().add(playerMinimapRenderer);
                minimap.getOverMapRendererHandler().add(markerMinimapRenderer);
            }

            isRendererRegistered = true;
            LOGGER.info("DynmapRadar renderers registered with Xaero WorldMap & Minimap");
        }

        // Reload config on server change
        CONFIG_MANAGER.reload();

        // Periodic data fetching
        long now = System.currentTimeMillis();
        int interval = CONFIG_MANAGER.getConfig().updateInterval;
        if (now - lastUpdateTick >= interval && client.level != null) {
            lastUpdateTick = now;
            DATA_FETCHER.doFetch();
        }
    }

    private static boolean isRendererRegistered = false;
}
