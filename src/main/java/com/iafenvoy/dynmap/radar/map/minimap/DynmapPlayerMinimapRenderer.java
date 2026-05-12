package com.iafenvoy.dynmap.radar.map.minimap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class DynmapPlayerMinimapRenderer extends MinimapElementRenderer<DynmapPlayerElement, DynmapPlayerElementRenderContext> {
    private MultiBufferSource.BufferSource bufferSource;

    public DynmapPlayerMinimapRenderer() {
        super(new DynmapPlayerMinimapReader(), new DynmapPlayerMinimapProvider(), new DynmapPlayerElementRenderContext());
    }

    @Override
    public int getOrder() {
        return 200;
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
    public boolean renderElement(DynmapPlayerElement e, boolean hovered, boolean cave, double dimDiv, float pt,
                                 double mx, double mz, MinimapElementRenderInfo ri, GuiGraphics g,
                                 MultiBufferSource.BufferSource bs) {
        if (e == null) return false;
        PoseStack ps = g.pose();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ps.pushPose();
        ps.translate(mx, mz, 0);

        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        int headSize = cfg.headSize;
        int effectiveHeadSize = cfg.headSizeFollowZoom ? (int) (headSize * ri.backgroundCoordinateScale) : headSize;
        effectiveHeadSize = Math.max(4, effectiveHeadSize);
        int halfSize = effectiveHeadSize / 2;
        ResourceLocation skin = getPlayerSkin(e);
        PlayerFaceRenderer.draw(g, skin, -halfSize, -halfSize, effectiveHeadSize);

        String displayName = e.displayName();
        int nameWidth = font.width(displayName);
        ps.pushPose();
        ps.scale(2.0f, 2.0f, 1.0f);
        font.drawInBatch(displayName, (float) (-nameWidth / 2), (float) ((halfSize + 2) / 2),
                -1, false, ps.last().pose(), this.bufferSource != null ? this.bufferSource : bs,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        ps.popPose();

        ps.popPose();
        return true;
    }

    private static ResourceLocation getPlayerSkin(DynmapPlayerElement element) {
        return Optional.of(Minecraft.getInstance())
                .map(Minecraft::getConnection)
                .map(x -> x.getPlayerInfo(element.getAccount()))
                .map(PlayerInfo::getSkinLocation)
                .orElse(DefaultPlayerSkin.getDefaultSkin(
                        UUID.nameUUIDFromBytes(("OfflinePlayer:" + element.getAccount()).getBytes(StandardCharsets.UTF_8))));
    }
}
