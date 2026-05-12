package com.iafenvoy.dynmap.radar.map.minimap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import net.minecraft.client.Minecraft;
import xaero.hud.minimap.element.render.MinimapElementReader;

public class DynmapPlayerMinimapReader extends MinimapElementReader<DynmapPlayerElement, DynmapPlayerElementRenderContext> {

    @Override
    public boolean isHidden(DynmapPlayerElement e, DynmapPlayerElementRenderContext c) {
        double dx = e.getX() - c.cameraX, dz = e.getZ() - c.cameraZ;
        double r = DynmapRadarClient.CONFIG_MANAGER.getConfig().minimapCullRadius;
        return dx * dx + dz * dz > r * r;
    }

    @Override
    public double getRenderX(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return e.getX();
    }

    @Override
    public double getRenderY(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return e.getY();
    }

    @Override
    public double getRenderZ(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return e.getZ();
    }

    @Override
    public int getInteractionBoxLeft(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return -16;
    }

    @Override
    public int getInteractionBoxRight(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return 16;
    }

    @Override
    public int getInteractionBoxTop(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return -16;
    }

    @Override
    public int getInteractionBoxBottom(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return 4;
    }

    @Override
    public int getRenderBoxLeft(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return -16;
    }

    @Override
    public int getRenderBoxRight(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return 16;
    }

    @Override
    public int getRenderBoxTop(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return -16;
    }

    @Override
    public int getRenderBoxBottom(DynmapPlayerElement e, DynmapPlayerElementRenderContext c, float p) {
        return 4;
    }

    @Override
    public int getLeftSideLength(DynmapPlayerElement e, Minecraft mc) {
        return 0;
    }

    @Override
    public String getMenuName(DynmapPlayerElement e) {
        return e.displayName();
    }

    @Override
    public String getFilterName(DynmapPlayerElement e) {
        return e.displayName();
    }

    @Override
    public int getMenuTextFillLeftPadding(DynmapPlayerElement e) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(DynmapPlayerElement e) {
        return 0;
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return false;
    }
}
