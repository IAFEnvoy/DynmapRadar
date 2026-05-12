package com.iafenvoy.dynmap.radar.map.minimap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import net.minecraft.client.Minecraft;
import xaero.hud.minimap.element.render.MinimapElementReader;

public class DynmapMarkerMinimapReader extends MinimapElementReader<DynmapMarkerElement, DynmapPlayerElementRenderContext> {

    @Override
    public boolean isHidden(DynmapMarkerElement e, DynmapPlayerElementRenderContext c) {
        double r = DynmapRadarClient.CONFIG_MANAGER.getConfig().minimapCullRadius;
        double wx = e.x(), wz = e.z();
        double wMinX = wx + e.minX, wMaxX = wx + e.maxX;
        double wMinZ = wz + e.minZ, wMaxZ = wz + e.maxZ;
        double cx = Math.max(wMinX, Math.min(c.cameraX, wMaxX));
        double cz = Math.max(wMinZ, Math.min(c.cameraZ, wMaxZ));
        double dx = cx - c.cameraX, dz = cz - c.cameraZ;
        return dx * dx + dz * dz > r * r;
    }

    @Override
    public double getRenderX(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return e.x();
    }

    @Override
    public double getRenderY(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return e.y();
    }

    @Override
    public double getRenderZ(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return e.z();
    }

    @Override
    public int getInteractionBoxLeft(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return -16;
    }

    @Override
    public int getInteractionBoxRight(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return 16;
    }

    @Override
    public int getInteractionBoxTop(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return -16;
    }

    @Override
    public int getInteractionBoxBottom(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return 4;
    }

    @Override
    public int getRenderBoxLeft(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return -16;
    }

    @Override
    public int getRenderBoxRight(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return 16;
    }

    @Override
    public int getRenderBoxTop(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return -16;
    }

    @Override
    public int getRenderBoxBottom(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return 4;
    }

    @Override
    public int getLeftSideLength(DynmapMarkerElement e, Minecraft mc) {
        return 0;
    }

    @Override
    public String getMenuName(DynmapMarkerElement e) {
        return e.label != null ? e.label : "";
    }

    @Override
    public String getFilterName(DynmapMarkerElement e) {
        return e.label != null ? e.label : "";
    }

    @Override
    public int getMenuTextFillLeftPadding(DynmapMarkerElement e) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(DynmapMarkerElement e) {
        return 0;
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return false;
    }
}

