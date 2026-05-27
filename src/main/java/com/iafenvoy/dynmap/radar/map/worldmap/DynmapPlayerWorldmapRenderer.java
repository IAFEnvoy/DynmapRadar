package com.iafenvoy.dynmap.radar.map.worldmap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.data.DynmapDataStorage;
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
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import xaero.map.WorldMap;
import xaero.map.WorldMapClientOnly;
import xaero.map.WorldMapSession;
import xaero.map.element.MapElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.CustomVertexConsumers;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Renders Dynmap players on the Xaero world map.
 * Skin rendering follows the same logic as Minecraft's PlayerTabOverlay:
 * - Uses PlayerInfo.getSkinLocation() for skin texture
 * - Same UV coordinates: face at (8,8), hat at (40,8), both 8x8
 * Head is rendered on top, name displayed below.
 */
public class DynmapPlayerWorldmapRenderer extends MapElementRenderer<DynmapPlayerElement, DynmapPlayerElementRenderContext, DynmapPlayerWorldmapRenderer> {

    public DynmapPlayerWorldmapRenderer(DynmapPlayerElementRenderContext context,
                                        DynmapPlayerWorldmapRenderProvider provider,
                                        DynmapPlayerWorldmapReader reader) {
        super(context, provider, reader);
    }

    // =================== Provider/Reader ===================

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }

    // =================== Per-frame pre/post render ===================

    @Override
    public void preRender(ElementRenderInfo renderInfo, MultiBufferSource.BufferSource bufferSource,
                          MultiTextureRenderTypeRendererProvider multiTexProvider, boolean cave) {
        Minecraft mc = Minecraft.getInstance();
        WorldMapSession session = WorldMapSession.getCurrentSession();
        WorldMapClientOnly wmClient = WorldMap.worldMapClientOnly;
        CustomVertexConsumers cvc = wmClient.customVertexConsumers;
        MultiBufferSource.BufferSource renderTypeBuffers = cvc.getRenderTypeBuffers();

        DynmapPlayerElementRenderContext ctx = this.getContext();

        ctx.textBGConsumer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_ELEMENT_TEXT_BG);

        ctx.uniqueTextureUIObjectRenderer = multiTexProvider.getRenderer(
                MultiTextureRenderTypeRendererProvider::defaultTextureBind,
                i -> {
                },
                CustomRenderTypes.GUI_BILINEAR_PREMULTIPLIED);

        if (session != null) {
            MapWorld mapWorld = session.getMapProcessor().getMapWorld();
            ctx.mapDimId = mapWorld.getCurrentDimensionId();

            MapDimension dim = mapWorld.getCurrentDimension();
            ctx.mapDimDiv = dim.calculateDimDiv(
                    session.getMapProcessor().getWorldDimensionTypeRegistry(),
                    mc.level.dimensionType());
        }
    }

    @Override
    public void postRender(ElementRenderInfo renderInfo, MultiBufferSource.BufferSource bufferSource,
                           MultiTextureRenderTypeRendererProvider multiTexProvider, boolean cave) {
        WorldMapClientOnly wmClient = WorldMap.worldMapClientOnly;
        CustomVertexConsumers cvc = wmClient.customVertexConsumers;
        MultiBufferSource.BufferSource renderTypeBuffers = cvc.getRenderTypeBuffers();

        DynmapPlayerElementRenderContext ctx = this.getContext();
        multiTexProvider.draw(ctx.uniqueTextureUIObjectRenderer);
        renderTypeBuffers.endBatch();
    }

    @Override
    public void renderElementShadow(DynmapPlayerElement element, boolean hovered, float partialTicks,
                                    double mouseX, double mouseZ, ElementRenderInfo renderInfo,
                                    GuiGraphics guiGraphics, MultiBufferSource.BufferSource bufferSource,
                                    MultiTextureRenderTypeRendererProvider multiTexProvider) {
    }

    // =================== renderElement ===================

    @Override
    public boolean renderElement(DynmapPlayerElement element, boolean hovered, double dimensionDiv,
                                 float partialTicks, double mouseX, double mouseZ,
                                 ElementRenderInfo renderInfo, GuiGraphics guiGraphics,
                                 MultiBufferSource.BufferSource bufferSource,
                                 MultiTextureRenderTypeRendererProvider multiTexProvider) {
        if (element == null) return false;

        PoseStack poseStack = guiGraphics.pose();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        String displayName = element.displayName();

        poseStack.pushPose();

        // --- Translate to player position ---
        poseStack.translate(mouseX, mouseZ, 0.0d);

        // --- Player head icon (using vanilla PlayerFaceRenderer) ---
        int headSize = DynmapRadarClient.CONFIG_MANAGER.getConfig().headSize;
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        int effectiveHeadSize = cfg.headSizeFollowZoom ? (int) (headSize * renderInfo.scale) : headSize;
        effectiveHeadSize = Math.max(4, effectiveHeadSize);
        int halfSize = effectiveHeadSize / 2;
        ResourceLocation skin = this.getPlayerSkin(element);
        PlayerFaceRenderer.draw(guiGraphics, skin, -halfSize, -halfSize, effectiveHeadSize);

        // --- Name below head (white text on dark background, 2x scale) ---
        int nameWidth = font.width(displayName);
        // Scale text rendering by 2x so it's twice as large
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 1.0f);
        int nameX = -nameWidth / 2;
        int nameY = (halfSize + 2) / 2; // compensate for the 2x scale

        font.drawInBatch(displayName,
                (float) nameX, (float) nameY,
                -1, false,
                poseStack.last().pose(), bufferSource,
                Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        poseStack.popPose();
        return true;
    }

    /**
     * Get player skin using the same approach as PlayerTabOverlay:
     * 1. Try connection.getPlayerInfo(uuid) → PlayerInfo.getSkinLocation()
     * 2. Fall back to Minecraft's default skin (Steve/Alex based on UUID)
     */
    private ResourceLocation getPlayerSkin(DynmapPlayerElement element) {
        return Optional.of(Minecraft.getInstance())
                .map(Minecraft::getConnection)
                .map(x -> x.getPlayerInfo(element.getAccount()))
                .map(PlayerInfo::getSkinLocation)
                .orElse(DefaultPlayerSkin.getDefaultSkin(UUID.nameUUIDFromBytes(("OfflinePlayer:" + element.getAccount()).getBytes(StandardCharsets.UTF_8))));
    }

    // =================== shouldRender ===================

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean hovered) {
        if (location == ElementRenderLocation.WORLD_MAP) return true;
        if (DynmapRadarClient.CONFIG_MANAGER.getConfig().markerShowInMinimap)
            return location == ElementRenderLocation.IN_MINIMAP || location == ElementRenderLocation.OVER_MINIMAP;
        return false;
    }

    @Override
    public boolean shouldRender(int location, boolean hovered) {
        if (location == ElementRenderLocation.WORLD_MAP.getIndex()) return true;
        if (DynmapRadarClient.CONFIG_MANAGER.getConfig().markerShowInMinimap)
            return location == ElementRenderLocation.IN_MINIMAP.getIndex() || location == ElementRenderLocation.OVER_MINIMAP.getIndex();
        return false;
    }

    // =================== Delegates ===================

    @Override
    public void beforeRender(int location, Minecraft mc, GuiGraphics guiGraphics,
                             double cameraX, double cameraY, double cameraZ, double dimensionDiv,
                             float brightness, double scale, double mouseX,
                             TextureManager textureManager, Font font,
                             MultiBufferSource.BufferSource bufferSource,
                             MultiTextureRenderTypeRendererProvider multiTexProvider, boolean cave) {
        this.preRender(null, bufferSource, multiTexProvider, cave);
    }

    @Override
    public void afterRender(int location, Minecraft mc, GuiGraphics guiGraphics,
                            double cameraX, double cameraY, double cameraZ, double dimensionDiv,
                            float brightness, double scale, double mouseX,
                            TextureManager textureManager, Font font,
                            MultiBufferSource.BufferSource bufferSource,
                            MultiTextureRenderTypeRendererProvider multiTexProvider, boolean cave) {
        this.postRender(null, bufferSource, multiTexProvider, cave);
    }

    @Override
    public void renderElementPre(int location, DynmapPlayerElement element, boolean hovered,
                                 Minecraft mc, GuiGraphics guiGraphics,
                                 double cameraX, double cameraY, double cameraZ, double dimensionDiv,
                                 float brightness, double scale, double mouseX,
                                 TextureManager textureManager, Font font,
                                 MultiBufferSource.BufferSource bufferSource,
                                 MultiTextureRenderTypeRendererProvider multiTexProvider,
                                 float partialTicks, double renderX, double renderZ,
                                 boolean cave, float optionalScale) {
        this.renderElementShadow(element, hovered, partialTicks, renderX, renderZ, null,
                guiGraphics, bufferSource, multiTexProvider);
    }

    @Override
    public boolean renderElement(int location, DynmapPlayerElement element, boolean hovered,
                                 Minecraft mc, GuiGraphics guiGraphics,
                                 double cameraX, double cameraY, double cameraZ, double dimensionDiv,
                                 float brightness, double scale, double mouseX,
                                 TextureManager textureManager, Font font,
                                 MultiBufferSource.BufferSource bufferSource,
                                 MultiTextureRenderTypeRendererProvider multiTexProvider,
                                 int elementIndex, double optionalScale, float partialTicks,
                                 double renderX, double renderZ, boolean cave, float boxScale) {
        return this.renderElement(element, hovered, dimensionDiv, partialTicks, renderX, renderZ,
                null, guiGraphics, bufferSource, multiTexProvider);
    }

    // =================== Factory ===================

    public static DynmapPlayerWorldmapRenderer create(DynmapDataStorage storage) {
        DynmapPlayerElementRenderContext context = new DynmapPlayerElementRenderContext();
        DynmapPlayerWorldmapRenderProvider provider = new DynmapPlayerWorldmapRenderProvider(storage);
        DynmapPlayerWorldmapReader reader = new DynmapPlayerWorldmapReader();
        return new DynmapPlayerWorldmapRenderer(context, provider, reader);
    }
}
