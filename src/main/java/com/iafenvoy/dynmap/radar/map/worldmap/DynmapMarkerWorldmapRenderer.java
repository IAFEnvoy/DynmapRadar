package com.iafenvoy.dynmap.radar.map.worldmap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import com.iafenvoy.dynmap.radar.map.IconManager;
import com.iafenvoy.dynmap.radar.map.MarkerDrawingUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureManager;
import xaero.map.WorldMap;
import xaero.map.WorldMapClientOnly;
import xaero.map.WorldMapSession;
import xaero.map.element.MapElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public class DynmapMarkerWorldmapRenderer extends MapElementRenderer<DynmapMarkerElement, DynmapPlayerElementRenderContext, DynmapMarkerWorldmapRenderer> {
    private final IconManager iconManager;

    public DynmapMarkerWorldmapRenderer(DynmapPlayerElementRenderContext ctx, IconManager iconManager) {
        super(ctx, new DynmapMarkerWorldmapRenderProvider(), new DynmapMarkerWorldmapReader());
        this.iconManager = iconManager;
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }

    @Override
    public void preRender(ElementRenderInfo ri, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp, boolean cv) {
        Minecraft mc = Minecraft.getInstance();
        WorldMapSession session = WorldMapSession.getCurrentSession();
        WorldMapClientOnly wc = WorldMap.worldMapClientOnly;
        MultiBufferSource.BufferSource rb = wc.customVertexConsumers.getRenderTypeBuffers();
        DynmapPlayerElementRenderContext ctx = this.getContext();
        ctx.textBGConsumer = rb.getBuffer(CustomRenderTypes.MAP_ELEMENT_TEXT_BG);
        ctx.uniqueTextureUIObjectRenderer = mp.getRenderer(MultiTextureRenderTypeRendererProvider::defaultTextureBind, i -> {
        }, CustomRenderTypes.GUI_BILINEAR_PREMULTIPLIED);
        if (session != null) {
            ctx.mapDimId = session.getMapProcessor().getMapWorld().getCurrentDimensionId();
            ctx.mapDimDiv = session.getMapProcessor().getMapWorld().getCurrentDimension()
                    .calculateDimDiv(session.getMapProcessor().getWorldDimensionTypeRegistry(), mc.level.dimensionType());
        }
        ctx.cachedScale = ri.scale;
    }

    @Override
    public void postRender(ElementRenderInfo ri, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp, boolean cv) {
        MultiBufferSource.BufferSource rb = WorldMap.worldMapClientOnly.customVertexConsumers.getRenderTypeBuffers();
        mp.draw(this.getContext().uniqueTextureUIObjectRenderer);
        rb.endBatch();
    }

    @Override
    public void renderElementShadow(DynmapMarkerElement e, boolean h, float p, double mx, double mz, ElementRenderInfo ri, GuiGraphics g, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp) {
    }

    @Override
    public boolean shouldRender(ElementRenderLocation l, boolean h) {
        if (l == ElementRenderLocation.WORLD_MAP) return true;
        if (DynmapRadarClient.CONFIG_MANAGER.getConfig().markerShowInMinimap)
            return l == ElementRenderLocation.IN_MINIMAP || l == ElementRenderLocation.OVER_MINIMAP;
        return false;
    }

    @Override
    public boolean shouldRender(int l, boolean h) {
        if (l == ElementRenderLocation.WORLD_MAP.getIndex()) return true;
        if (DynmapRadarClient.CONFIG_MANAGER.getConfig().markerShowInMinimap)
            return l == ElementRenderLocation.IN_MINIMAP.getIndex() || l == ElementRenderLocation.OVER_MINIMAP.getIndex();
        return false;
    }

    @Override
    public boolean renderElement(DynmapMarkerElement e, boolean hovered, double dimDiv, float pt,
                                 double mx, double mz, ElementRenderInfo ri, GuiGraphics g,
                                 MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp) {
        if (e == null) return false;
        double ms = DynmapRadarClient.CONFIG_MANAGER.getConfig().markerScale;
        double ws = ri.scale * ms;
        Font font = Minecraft.getInstance().font;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(mx, mz, 0);
        switch (e.type) {
            case AREA -> MarkerDrawingUtil.drawArea(g, e, ws, ri.scale, font);
            case CIRCLE -> MarkerDrawingUtil.drawCircle(g, e, ws, ri.scale, font);
            case POLYLINE -> MarkerDrawingUtil.drawPoly(g, e, ws, ri.scale, font);
            case POINT -> MarkerDrawingUtil.drawPoint(g, e, ws, font, this.iconManager);
        }
        ps.popPose();
        return false;
    }

    public static DynmapMarkerWorldmapRenderer create(IconManager iconManager) {
        return new DynmapMarkerWorldmapRenderer(new DynmapPlayerElementRenderContext(), iconManager);
    }

    @Override
    public void beforeRender(int l, Minecraft mc, GuiGraphics g, double cx, double cy, double cz, double dd, float br, double sc, double mx, TextureManager tm, Font f, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp, boolean cv) {
        this.preRender(null, bs, mp, cv);
    }

    @Override
    public void afterRender(int l, Minecraft mc, GuiGraphics g, double cx, double cy, double cz, double dd, float br, double sc, double mx, TextureManager tm, Font f, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp, boolean cv) {
        this.postRender(null, bs, mp, cv);
    }

    @Override
    public void renderElementPre(int l, DynmapMarkerElement e, boolean h, Minecraft mc, GuiGraphics g, double cx, double cy, double cz, double dd, float br, double sc, double mx, TextureManager tm, Font f, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp, float pt, double rx, double rz, boolean cv, float os) {
    }

    @Override
    public boolean renderElement(int l, DynmapMarkerElement e, boolean h, Minecraft mc, GuiGraphics g, double cx, double cy, double cz, double dd, float br, double sc, double mx, TextureManager tm, Font f, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp, int ei, double os, float pt, double rx, double rz, boolean cv, float b2) {
        return this.renderElement(e, h, dd, pt, rx, rz, null, g, bs, mp);
    }

    public static DynmapMarkerWorldmapRenderer create() {
        return new DynmapMarkerWorldmapRenderer(new DynmapPlayerElementRenderContext(), new IconManager());
    }
}
