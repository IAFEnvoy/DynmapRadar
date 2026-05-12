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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                            .then(argument("xaeroDimension", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        List<String> dims = new ArrayList<>();
                                        Minecraft mc = Minecraft.getInstance();
                                        if (mc.level != null) {
                                            dims.add(mc.level.dimension().location().toString());
                                        }
                                        dims.add("minecraft:overworld");
                                        dims.add("minecraft:the_nether");
                                        dims.add("minecraft:the_end");
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
        source.sendFeedback(Component.literal("=== DynmapRadar [" + key + "] ===").withStyle(ChatFormatting.GOLD));
        source.sendFeedback(Component.literal("URL: " + (cfg.dynmapMain.isEmpty() ? "<not set>" : cfg.dynmapMain)));
        source.sendFeedback(Component.literal("Display mode: " + cfg.nameDisplayMode));
        source.sendFeedback(Component.literal("Head size: " + cfg.headSize));
        source.sendFeedback(Component.literal("Update interval: " + cfg.updateInterval + "ms"));
        source.sendFeedback(Component.literal("Marker scale: " + String.format("%.1f", cfg.markerScale)));
        source.sendFeedback(Component.literal("Point icon scale: " + String.format("%.1f", cfg.pointIconScale)));
        source.sendFeedback(Component.literal("Point follow zoom: " + cfg.pointSizeFollowZoom));
        source.sendFeedback(Component.literal("Head follow zoom: " + cfg.headSizeFollowZoom));
        source.sendFeedback(Component.literal("Show markers in minimap: " + cfg.markerShowInMinimap));
        source.sendFeedback(Component.literal("Minimap show shapes: " + cfg.minimapShowShapes));
        source.sendFeedback(Component.literal("Minimap cull radius: " + String.format("%.0f", cfg.minimapCullRadius)));
        source.sendFeedback(Component.literal("Waypoint color: #" + String.format("%06X", cfg.waypointColor)));
        source.sendFeedback(Component.literal("World layers: " + formatLayerList(cfg.worldLayerVisibility, cfg.layerDefaults)));
        source.sendFeedback(Component.literal("Minimap layers: " + formatLayerList(cfg.minimapLayerVisibility, cfg.layerDefaults)));
        if (!cfg.dimensionMapping.isEmpty()) {
            for (Map.Entry<String, String> e : cfg.dimensionMapping.entrySet()) {
                source.sendFeedback(Component.literal("Dimension: " + e.getKey() + " -> " + e.getValue()));
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
        if (ms.sets.isEmpty()) return "<none>";
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
        String title = isMinimap ? "--- Minimap Marker Sets ---" : "--- World Map Marker Sets ---";
        source.sendFeedback(Component.literal(title).withStyle(ChatFormatting.GOLD));
        for (MarkerState.MarkerSet s : ms.sets.values()) {
            boolean visible = isMinimap ? cfg.isLayerVisibleMinimap(s.id()) : cfg.isLayerVisibleWorldmap(s.id());
            String prefix = visible ? ChatFormatting.GREEN + "[SHOW]" : ChatFormatting.RED + "[HIDE]";
            source.sendFeedback(Component.literal(prefix + " " + s.id() + " - " + s.label()));
        }
        if (ms.sets.isEmpty())
            source.sendFeedback(Component.literal("<none>").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int layerShow(FabricClientCommandSource source, String setId, boolean isMinimap) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        if (isMinimap) cfg.setLayerVisibleMinimap(setId, true);
        else cfg.setLayerVisibleWorldmap(setId, true);
        DynmapRadarClient.CONFIG_MANAGER.save();
        String target = isMinimap ? "minimap" : "world";
        source.sendFeedback(Component.literal("Layer '" + setId + "' is now SHOWN on " + target + ".").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int layerHide(FabricClientCommandSource source, String setId, boolean isMinimap) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        if (isMinimap) cfg.setLayerVisibleMinimap(setId, false);
        else cfg.setLayerVisibleWorldmap(setId, false);
        DynmapRadarClient.CONFIG_MANAGER.save();
        String target = isMinimap ? "minimap" : "world";
        source.sendFeedback(Component.literal("Layer '" + setId + "' is now HIDDEN on " + target + ".").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int layerShowAll(FabricClientCommandSource source, boolean isMinimap) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        if (isMinimap) cfg.minimapLayerVisibility.clear();
        else cfg.worldLayerVisibility.clear();
        DynmapRadarClient.CONFIG_MANAGER.save();
        String target = isMinimap ? "minimap" : "world";
        source.sendFeedback(Component.literal("All layers are now SHOWN on " + target + ".").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int layerReset(FabricClientCommandSource source, String setId) {
        DynmapRadarClient.CONFIG_MANAGER.getConfig().resetLayer(setId);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.literal("Layer '" + setId + "' reset to Dynmap defaults.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int showDims(FabricClientCommandSource source) {
        Map<String, String> mapping = DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping;
        source.sendFeedback(Component.literal("--- Dimension Mapping ---").withStyle(ChatFormatting.GOLD));
        String currentXaero = "";
        if (Minecraft.getInstance().level != null) {
            currentXaero = Minecraft.getInstance().level.dimension().location().toString();
        }
        source.sendFeedback(Component.literal("Current Xaero dimension: " + currentXaero));
        if (mapping.isEmpty()) {
            source.sendFeedback(Component.literal("<no mappings>").withStyle(ChatFormatting.GRAY));
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
            source.sendFeedback(Component.literal("Invalid dimension format. Use namespace:path (e.g. minecraft:overworld)").withStyle(ChatFormatting.RED));
            return 0;
        }
        DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping.put(dynmapWorld, xaeroDim);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.literal("Mapped dynmap world '" + dynmapWorld + "' -> '" + xaeroDim + "'").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int dimMapCurrent(FabricClientCommandSource source) {
        if (Minecraft.getInstance().level == null) {
            source.sendFeedback(Component.literal("Not in a world.").withStyle(ChatFormatting.RED));
            return 0;
        }
        String current = Minecraft.getInstance().level.dimension().location().toString();
        DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping.put("world", current);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.literal("Mapped dynmap 'world' -> '" + current + "'").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int dimUnmap(FabricClientCommandSource source, String dynmapWorld) {
        DynmapRadarClient.CONFIG_MANAGER.getConfig().dimensionMapping.remove(dynmapWorld);
        DynmapRadarClient.CONFIG_MANAGER.save();
        source.sendFeedback(Component.literal("Removed mapping for dynmap world '" + dynmapWorld + "'").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int reload(FabricClientCommandSource source) {
        DynmapRadarClient.CONFIG_MANAGER.reload();
        source.sendFeedback(Component.literal("Config reloaded.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int clearCache(FabricClientCommandSource source) {
        DynmapRadarClient.DATA_FETCHER.reset();
        DynmapRadarClient.ICON_MANAGER.clearCache();
        DynmapRadarClient.ICON_MANAGER.clearDiskCache();
        DynmapRadarClient.DATA_FETCHER.doFetch();
        source.sendFeedback(Component.literal("All cached data cleared. Re-fetching...").withStyle(ChatFormatting.GREEN));
        return 1;
    }
}
