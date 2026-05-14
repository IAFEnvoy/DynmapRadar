package com.iafenvoy.dynmap.radar.config;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.data.MarkerState;
import com.iafenvoy.dynmap.radar.util.CommandUtil;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import xaero.map.WorldMapSession;
import xaero.map.world.MapDimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Client-side commands to configure per-server DynmapRadar settings.
 */
public class DynmapRadarCommands {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = literal("dradar");

            // ======================== Status ========================
            root.then(literal("status").executes(ctx -> showStatus(ctx.getSource())));

            // ======================== Settings ========================
            LiteralArgumentBuilder<FabricClientCommandSource> settings = literal("settings");

            CommandUtil.appendSetting(settings, "url",
                    StringArgumentType.greedyString(),
                    StringArgumentType::getString,
                    cfg -> cfg.dynmapMain,
                    (cfg, v) -> cfg.dynmapMain = v);

            CommandUtil.appendSetting(settings, "mode",
                    StringArgumentType.word(),
                    (ctx, name) -> {
                        String mode = StringArgumentType.getString(ctx, name).toUpperCase();
                        if (!mode.equals("NAME") && !mode.equals("ACCOUNT"))
                            throw new IllegalArgumentException("Invalid mode. Use 'name' or 'account'.");
                        return mode;
                    },
                    cfg -> cfg.nameDisplayMode,
                    (cfg, v) -> cfg.nameDisplayMode = v);

            CommandUtil.appendSetting(settings, "headSize",
                    IntegerArgumentType.integer(8, 64),
                    IntegerArgumentType::getInteger,
                    cfg -> cfg.headSize,
                    (cfg, v) -> cfg.headSize = v);

            CommandUtil.appendSetting(settings, "markerScale",
                    DoubleArgumentType.doubleArg(0.1, 10),
                    DoubleArgumentType::getDouble,
                    cfg -> cfg.markerScale,
                    (cfg, v) -> cfg.markerScale = v);

            CommandUtil.appendSetting(settings, "pointScale",
                    DoubleArgumentType.doubleArg(0.25, 10),
                    DoubleArgumentType::getDouble,
                    cfg -> cfg.pointIconScale,
                    (cfg, v) -> cfg.pointIconScale = v);

            CommandUtil.appendSetting(settings, "pointFollowZoom",
                    BoolArgumentType.bool(),
                    BoolArgumentType::getBool,
                    cfg -> cfg.pointSizeFollowZoom,
                    (cfg, v) -> cfg.pointSizeFollowZoom = v);

            CommandUtil.appendSetting(settings, "headFollowZoom",
                    BoolArgumentType.bool(),
                    BoolArgumentType::getBool,
                    cfg -> cfg.headSizeFollowZoom,
                    (cfg, v) -> cfg.headSizeFollowZoom = v);

            CommandUtil.appendSetting(settings, "showInMinimap",
                    BoolArgumentType.bool(),
                    BoolArgumentType::getBool,
                    cfg -> cfg.markerShowInMinimap,
                    (cfg, v) -> cfg.markerShowInMinimap = v);

            CommandUtil.appendSetting(settings, "minimapShapes",
                    BoolArgumentType.bool(),
                    BoolArgumentType::getBool,
                    cfg -> cfg.minimapShowShapes,
                    (cfg, v) -> cfg.minimapShowShapes = v);

            CommandUtil.appendSetting(settings, "minimapCullRadius",
                    DoubleArgumentType.doubleArg(10, 10000),
                    DoubleArgumentType::getDouble,
                    cfg -> cfg.minimapCullRadius,
                    (cfg, v) -> cfg.minimapCullRadius = v);

            CommandUtil.appendSetting(settings, "waypointColor",
                    IntegerArgumentType.integer(0, 0xFFFFFF),
                    IntegerArgumentType::getInteger,
                    cfg -> cfg.waypointColor,
                    (cfg, v) -> cfg.waypointColor = v);

            CommandUtil.appendSetting(settings, "pointMinScale",
                    DoubleArgumentType.doubleArg(0, 10),
                    DoubleArgumentType::getDouble,
                    cfg -> cfg.pointMinScale,
                    (cfg, v) -> cfg.pointMinScale = v);

            CommandUtil.appendSetting(settings, "interval",
                    IntegerArgumentType.integer(100, 60000),
                    IntegerArgumentType::getInteger,
                    cfg -> cfg.updateInterval,
                    (cfg, v) -> cfg.updateInterval = v);

            root.then(settings);

            // ======================== Layers ========================
            LiteralArgumentBuilder<FabricClientCommandSource> layer = literal("layer");
            // layer world show/hide/list/showAll/reset
            layer.then(buildLayerBranch("world", false));
            // layer minimap show/hide/list/showAll/reset
            layer.then(buildLayerBranch("minimap", true));
            root.then(layer);

            // /dynmapradar dim map <dynmapWorld> <xaeroDimension>
            LiteralArgumentBuilder<FabricClientCommandSource> dim = literal("dim");
            dim.then(literal("list").executes(ctx -> showDims(ctx.getSource())));

            dim.then(literal("map")
                    .then(argument("dynmapWorld", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                        Set<String> worlds = DynmapRadarClient.DATA_FETCHER.getDynmapWorlds();
                                        if (worlds.isEmpty()) return SharedSuggestionProvider.suggest(List.of("world"), builder);
                                        return SharedSuggestionProvider.suggest(worlds, builder);
                                    })
                            .then(argument("xaeroDimension", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        List<String> dims = new ArrayList<>();
                                        WorldMapSession session = WorldMapSession.getCurrentSession();
                                        if (session != null) {
                                            for (MapDimension d : session.getMapProcessor().getMapWorld().getDimensionsList()) {
                                                dims.add(d.getDimId().location().toString());
                                            }
                                        }
                                        if (dims.isEmpty()) {
                                            Minecraft mc = Minecraft.getInstance();
                                            if (mc.level != null)
                                                dims.add(mc.level.dimension().location().toString());
                                        }
                                        return SharedSuggestionProvider.suggest(dims, builder);
                                    })
                                    .executes(ctx -> {
                                        String dw = StringArgumentType.getString(ctx, "dynmapWorld");
                                        String xd = StringArgumentType.getString(ctx, "xaeroDimension");
                                        return dimMap(ctx.getSource(), dw, xd);
                                    }))));

            dim.then(literal("unmap")
                    .then(argument("dynmapWorld", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                    DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping.keySet(), builder))
                            .executes(ctx -> {
                                String dw = StringArgumentType.getString(ctx, "dynmapWorld");
                                return dimUnmap(ctx.getSource(), dw);
                            })));

            dim.then(literal("mapCurrent").executes(ctx -> dimMapCurrent(ctx.getSource())));
            root.then(dim);

            // /dynmapradar reload
            root.then(literal("reload")
                    .executes(ctx -> reload(ctx.getSource())));

            // /dynmapradar clearCache
            root.then(literal("clearCache")
                    .executes(ctx -> clearCache(ctx.getSource())));

            dispatcher.register(root);
        });
    }

    private static int showStatus(FabricClientCommandSource source) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        String key = DynmapRadarClient.CONFIG_MANAGER.getCurrentServerKey();
        source.sendFeedback(Component.translatable("dynmap_radar.status.title", key).withStyle(ChatFormatting.GOLD));
        source.sendFeedback(Component.translatable("dynmap_radar.status.url", cfg.dynmapMain.isEmpty() ? Component.translatable("dynmap_radar.status.not_set") : cfg.dynmapMain));
        source.sendFeedback(Component.translatable("dynmap_radar.status.mode", cfg.nameDisplayMode));
        source.sendFeedback(Component.translatable("dynmap_radar.status.head_size", cfg.headSize));
        source.sendFeedback(Component.translatable("dynmap_radar.status.interval", cfg.updateInterval));
        source.sendFeedback(Component.translatable("dynmap_radar.status.marker_scale", String.format("%.1f", cfg.markerScale)));
        source.sendFeedback(Component.translatable("dynmap_radar.status.point_scale", String.format("%.1f", cfg.pointIconScale)));
        source.sendFeedback(Component.translatable("dynmap_radar.status.point_follow_zoom", cfg.pointSizeFollowZoom));
        source.sendFeedback(Component.translatable("dynmap_radar.status.head_follow_zoom", cfg.headSizeFollowZoom));
        source.sendFeedback(Component.translatable("dynmap_radar.status.show_in_minimap", cfg.markerShowInMinimap));
        source.sendFeedback(Component.translatable("dynmap_radar.status.minimap_shapes", cfg.minimapShowShapes));
        source.sendFeedback(Component.translatable("dynmap_radar.status.cull_radius", String.format("%.0f", cfg.minimapCullRadius)));
        source.sendFeedback(Component.translatable("dynmap_radar.status.waypoint_color", String.format("%06X", cfg.waypointColor)));
        source.sendFeedback(Component.translatable("dynmap_radar.status.point_min_scale", String.format("%.2f", cfg.pointMinScale)));
        source.sendFeedback(Component.translatable("dynmap_radar.status.world_layers", formatLayerList(cfg.worldLayerVisibility, cfg.layerDefaults)));
        source.sendFeedback(Component.translatable("dynmap_radar.status.minimap_layers", formatLayerList(cfg.minimapLayerVisibility, cfg.layerDefaults)));
        if (!cfg.dimensionMapping.isEmpty()) {
            for (Map.Entry<String, String> e : cfg.dimensionMapping.entrySet()) {
                source.sendFeedback(Component.translatable("dynmap_radar.status.dim_mapping", e.getKey(), e.getValue()));
            }
        }
        return 1;
    }

    // ======================== Layer Helpers ========================
    private static LiteralArgumentBuilder<FabricClientCommandSource> buildLayerBranch(String mapName, boolean isMinimap) {
        LiteralArgumentBuilder<FabricClientCommandSource> map = literal(mapName);
        map.then(literal("list").executes(ctx -> showLayers(ctx.getSource(), isMinimap)));
        map.then(literal("show")
                .then(argument("setId", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                DynmapRadarClient.DATA_FETCHER.getMarkerState().sets.keySet(), b))
                        .executes(ctx -> layerShow(ctx.getSource(), StringArgumentType.getString(ctx, "setId"), isMinimap))));
        map.then(literal("hide")
                .then(argument("setId", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                DynmapRadarClient.DATA_FETCHER.getMarkerState().sets.keySet(), b))
                        .executes(ctx -> layerHide(ctx.getSource(), StringArgumentType.getString(ctx, "setId"), isMinimap))));
        map.then(literal("showAll").executes(ctx -> layerShowAll(ctx.getSource(), isMinimap)));
        map.then(literal("reset")
                .then(argument("setId", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                DynmapRadarClient.DATA_FETCHER.getMarkerState().sets.keySet(), b))
                        .executes(ctx -> layerReset(ctx.getSource(), StringArgumentType.getString(ctx, "setId")))));
        return map;
    }

    private static String formatLayerList(Map<String, Boolean> visibility, Map<String, Boolean> defaults) {
        MarkerState ms = DynmapRadarClient.DATA_FETCHER.getMarkerState();
        if (ms.sets.isEmpty()) return Component.translatable("dynmap_radar.status.none").getString();
        List<String> shown = new ArrayList<>();
        List<String> hidden = new ArrayList<>();
        for (String id : ms.sets.keySet()) {
            if (visibility.containsKey(id) ? visibility.get(id) : (defaults.get(id) == null || !defaults.get(id)))
                shown.add(id);
            else
                hidden.add(id);
        }
        return ChatFormatting.GREEN + "[" + String.join(", ", shown) + "] "
                + ChatFormatting.RED + "[" + String.join(", ", hidden) + "]";
    }

    private static int showLayers(FabricClientCommandSource source, boolean isMinimap) {
        MarkerState ms = DynmapRadarClient.DATA_FETCHER.getMarkerState();
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        String titleKey = isMinimap ? "dynmap_radar.layer.minimap_title" : "dynmap_radar.layer.worldmap_title";
        source.sendFeedback(Component.translatable(titleKey).withStyle(ChatFormatting.GOLD));
        for (MarkerState.MarkerSet s : ms.sets.values()) {
            boolean visible = isMinimap ? cfg.isLayerVisibleMinimap(s.id()) : cfg.isLayerVisibleWorldmap(s.id());
            String prefix = visible ? ChatFormatting.GREEN + "[SHOW]" : ChatFormatting.RED + "[HIDE]";
            source.sendFeedback(Component.literal(prefix + " " + s.id() + " - " + s.label()));
        }
        if (ms.sets.isEmpty())
            source.sendFeedback(Component.translatable("dynmap_radar.status.none").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static MutableComponent targetName(boolean isMinimap) {
        return Component.translatable(isMinimap ? "dynmap_radar.target.minimap" : "dynmap_radar.target.worldmap");
    }

    private static int layerShow(FabricClientCommandSource source, String setId, boolean isMinimap) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        if (isMinimap) cfg.setLayerVisibleMinimap(setId, true);
        else cfg.setLayerVisibleWorldmap(setId, true);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.translatable("dynmap_radar.layer.shown", setId, targetName(isMinimap)).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int layerHide(FabricClientCommandSource source, String setId, boolean isMinimap) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        if (isMinimap) cfg.setLayerVisibleMinimap(setId, false);
        else cfg.setLayerVisibleWorldmap(setId, false);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.translatable("dynmap_radar.layer.hidden", setId, targetName(isMinimap)).withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int layerShowAll(FabricClientCommandSource source, boolean isMinimap) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        if (isMinimap) cfg.minimapLayerVisibility.clear();
        else cfg.worldLayerVisibility.clear();
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.translatable("dynmap_radar.layer.show_all", targetName(isMinimap)).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int layerReset(FabricClientCommandSource source, String setId) {
        DynmapRadarClient.CONFIG_MANAGER.getConfig().resetLayer(setId);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.translatable("dynmap_radar.layer.reset_done", setId).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int showDims(FabricClientCommandSource source) {
        Map<String, String> mapping = DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping;
        source.sendFeedback(Component.translatable("dynmap_radar.dim.title").withStyle(ChatFormatting.GOLD));
        String currentXaero = "";
        if (Minecraft.getInstance().level != null) {
            currentXaero = Minecraft.getInstance().level.dimension().location().toString();
        }
        source.sendFeedback(Component.translatable("dynmap_radar.dim.current", currentXaero));
        if (mapping.isEmpty()) {
            source.sendFeedback(Component.translatable("dynmap_radar.dim.no_mapping").withStyle(ChatFormatting.GRAY));
        } else {
            for (Map.Entry<String, String> e : mapping.entrySet()) {
                source.sendFeedback(Component.literal("  " + e.getKey() + " -> " + e.getValue()));
            }
        }
        return 1;
    }

    private static int dimMap(FabricClientCommandSource source, String dynmapWorld, String xaeroDim) {
        // Validate xaeroDim is a valid ResourceLocation
        try {
            ResourceLocation rl = new ResourceLocation(xaeroDim);
        } catch (Exception e) {
            source.sendFeedback(Component.translatable("dynmap_radar.dim.invalid_format").withStyle(ChatFormatting.RED));
            return 0;
        }
        DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping.put(dynmapWorld, xaeroDim);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.translatable("dynmap_radar.dim.mapped", dynmapWorld, xaeroDim).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int dimMapCurrent(FabricClientCommandSource source) {
        if (Minecraft.getInstance().level == null) {
            source.sendFeedback(Component.translatable("dynmap_radar.dim.not_in_world").withStyle(ChatFormatting.RED));
            return 0;
        }
        String current = Minecraft.getInstance().level.dimension().location().toString();
        DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping.put("world", current);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.translatable("dynmap_radar.dim.mapped_current", current).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int dimUnmap(FabricClientCommandSource source, String dynmapWorld) {
        DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping.remove(dynmapWorld);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.translatable("dynmap_radar.dim.unmapped", dynmapWorld).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int reload(FabricClientCommandSource source) {
        DynmapRadarClient.CONFIG_MANAGER.reload();
        source.sendFeedback(Component.translatable("dynmap_radar.reload.done").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int clearCache(FabricClientCommandSource source) {
        DynmapRadarClient.DATA_FETCHER.reset();
        DynmapRadarClient.ICON_MANAGER.clearCache();
        DynmapRadarClient.ICON_MANAGER.clearDiskCache();
        DynmapRadarClient.DATA_FETCHER.doFetch();
        source.sendFeedback(Component.translatable("dynmap_radar.clear_cache.done").withStyle(ChatFormatting.GREEN));
        return 1;
    }
}
