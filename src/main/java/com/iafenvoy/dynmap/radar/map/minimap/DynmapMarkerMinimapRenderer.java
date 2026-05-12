package com.iafenvoy.dynmap.radar.map.minimap;

import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import com.iafenvoy.dynmap.radar.map.IconManager;
import com.iafenvoy.dynmap.radar.map.MarkerDrawingUtil;
import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

public class DynmapMarkerMinimapRenderer extends MinimapElementRenderer<DynmapMarkerElement, DynmapPlayerElementRenderContext> {
    private final IconManager iconManager;
    private MultiBufferSource.BufferSource bufferSource;

    public DynmapMarkerMinimapRenderer(IconManager iconManager) {
        super(new DynmapMarkerMinimapReader(), new DynmapMarkerMinimapProvider(), new DynmapPlayerElementRenderContext());
        this.iconManager = iconManager;
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public boolean shouldRender(MinimapElementRenderLocation loc) {
        if (loc == MinimapElementRenderLocation.WORLD_MAP) return true;
        if (DynmapRadarClient.CONFIG_MANAGER.getConfig().markerShowInMinimap)
            return loc == MinimapElementRenderLocation.IN_MINIMAP ||
                    loc == MinimapElementRenderLocation.OVER_MINIMAP;
        return false;
    }

    @Override
    public void preRender(MinimapElementRenderInfo ri, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp) {
        this.bufferSource = bs;
        this.getContext().cameraX = ri.renderPos.x;
        this.getContext().cameraZ = ri.renderPos.z;
    }

    @Override
    public void postRender(MinimapElementRenderInfo ri, MultiBufferSource.BufferSource bs, MultiTextureRenderTypeRendererProvider mp) {
        if (this.bufferSource != null) {
            this.bufferSource.endBatch();
            this.bufferSource = null;
        }
    }

    @Override
    public boolean renderElement(DynmapMarkerElement e, boolean hovered, boolean cave, double dimDiv, float pt,
                                 double mx, double mz, MinimapElementRenderInfo ri, GuiGraphics g,
                                 MultiBufferSource.BufferSource bs) {
        if (e == null) return false;
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        double s = ri.backgroundCoordinateScale * cfg.markerScale;
        Font font = Minecraft.getInstance().font;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(mx, mz, 0);
        switch (e.type) {
            case AREA -> MarkerDrawingUtil.drawArea(g, e, s, ri.backgroundCoordinateScale, font);
            case CIRCLE -> MarkerDrawingUtil.drawCircle(g, e, s, ri.backgroundCoordinateScale, font);
            case POLYLINE -> MarkerDrawingUtil.drawPoly(g, e, s, ri.backgroundCoordinateScale, font);
            case POINT -> MarkerDrawingUtil.drawPoint(g, e, s, font, this.iconManager);
        }
        ps.popPose();
        return false;
    }
}
