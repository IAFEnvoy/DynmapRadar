package com.iafenvoy.dynmap.radar.util;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Helper to quickly create "{@code <setting> get/set <value>}" subcommands.
 * Each setting auto-saves the ServerConfig after modification.
 */
public final class CommandUtil {

    private CommandUtil() {
    }

    /**
     * Append a get/set setting node.
     * Usage: {@code /dradar settings <name> get} to read, {@code /dradar settings <name> set <value>} to write.
     */
    public static <T> void appendSetting(
            ArgumentBuilder<FabricClientCommandSource, ?> parent,
            String name,
            ArgumentType<T> type,
            BiFunction<CommandContext<FabricClientCommandSource>, String, T> parser,
            Function<ServerConfig, T> getter,
            BiConsumer<ServerConfig, T> setter
    ) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal(name);
        node.then(literal("get").executes(ctx -> {
            ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
            send(ctx.getSource(), name, "get", String.valueOf(getter.apply(cfg)));
            return 1;
        }));
        node.then(literal("set").then(argument("value", type).executes(ctx -> {
            T value = parser.apply(ctx, "value");
            ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
            setter.accept(cfg, value);
            DynmapRadarClient.CONFIG_MANAGER.save();
            send(ctx.getSource(), name, "set", String.valueOf(value));
            return 1;
        })));
        parent.then(node);
    }

    private static void send(FabricClientCommandSource source, String key, String action, String value) {
        source.sendFeedback(Component.literal("§a" + key + " " + action + " → " + value));
    }
}
