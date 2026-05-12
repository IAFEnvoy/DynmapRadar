package com.iafenvoy.dynmap.radar.map;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.util.HttpUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared icon cache and async download manager.
 * One instance shared between WorldMap and Minimap renderers.
 */
public class IconManager {
    private final ConcurrentHashMap<String, ResourceLocation> iconCache = new ConcurrentHashMap<>();
    private final Set<String> pendingDownloads = ConcurrentHashMap.newKeySet();

    public ResourceLocation get(String iconName) {
        return this.iconCache.get(iconName);
    }

    public boolean isCached(String iconName) {
        return this.iconCache.containsKey(iconName);
    }

    public void ensureDownloading(String iconName) {
        if (this.iconCache.containsKey(iconName)) return;
        if (!this.pendingDownloads.add(iconName)) return;
        String clean = iconName.replace("markers/", "").replace(".png", "");
        String base = DynmapRadarClient.CONFIG_MANAGER.getConfig().dynmapMain.trim();
        if (base.isEmpty()) {
            this.pendingDownloads.remove(iconName);
            return;
        }
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String url = base + "/tiles/_markers_/" + clean + ".png";
        ResourceLocation loc = new ResourceLocation(DynmapRadarClient.MOD_ID, "marker_icons/" + clean);
        File cacheFile = new File("./config/dynmap_radar/cache/" + clean + ".png");
        cacheFile.getParentFile().mkdirs();
        CompletableFuture.runAsync(() -> {
            try {
                NativeImage img = this.downloadIcon(cacheFile, url);
                if (img != null) {
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(img));
                        this.iconCache.put(iconName, loc);
                    });
                }
            } catch (Exception ex) {
                DynmapRadarClient.LOGGER.warn("Failed to load marker icon: {}", clean, ex);
            } finally {
                this.pendingDownloads.remove(iconName);
            }
        });
    }

    private NativeImage downloadIcon(File cacheFile, String url) {
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try (InputStream in = new FileInputStream(cacheFile)) {
                return NativeImage.read(NativeImage.Format.RGBA, in);
            } catch (Exception e) {
                cacheFile.delete();
            }
        }
        byte[] data = HttpUtil.getBytes(url);
        if (data == null) return null;
        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            fos.write(data);
        } catch (Exception ignored) {
        }
        try {
            return NativeImage.read(new ByteArrayInputStream(data));
        } catch (Exception e) {
            cacheFile.delete();
        }
        return null;
    }

    public void clearCache() {
        this.iconCache.clear();
        this.pendingDownloads.clear();
    }

    public void clearDiskCache() {
        File cacheDir = new File("./config/dynmap_radar/cache");
        if (cacheDir.isDirectory()) {
            try {
                Files.walk(cacheDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (Exception ignored) {
            }
        }
    }
}
