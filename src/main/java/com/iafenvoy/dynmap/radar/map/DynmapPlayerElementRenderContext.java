package com.iafenvoy.dynmap.radar.map;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;

/**
 * Render context for Dynmap player/marker elements.
 */
public class DynmapPlayerElementRenderContext {
    public double mapDimDiv;
    public VertexConsumer textBGConsumer;
    public MultiTextureRenderTypeRenderer uniqueTextureUIObjectRenderer;
    public ResourceKey<Level> mapDimId;
    public double cachedScale;
    /** Camera position for minimap distance culling. Set by preRender. */
    public double cameraX, cameraZ;
}
