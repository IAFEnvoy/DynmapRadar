package com.iafenvoy.dynmap.radar.map;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Shared static drawing utilities for Dynmap markers.
 * Used by both WorldMap and Minimap renderers.
 */
public final class MarkerDrawingUtil {

    private MarkerDrawingUtil() {
    }

    public static void drawArea(GuiGraphics g, DynmapMarkerElement e, double s, double mapScale, Font font) {
        if (e.xArr == null || e.zArr == null || e.xArr.length < 2) return;
        int n = e.xArr.length;
        int[] x = new int[n], z = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = (int) ((e.xArr[i] - e.x()) * s);
            z[i] = (int) ((e.zArr[i] - e.z()) * s);
        }
        int fillAlpha = (int) (e.fillopacity * 255);
        int fillARGB = ((fillAlpha & 0xFF) << 24) | (e.fillcolor & 0x00FFFFFF);
        int lineAlpha = (int) (e.opacity * 255);
        int lineARGB = ((lineAlpha & 0xFF) << 24) | (e.color & 0x00FFFFFF);
        int tw = Math.max(1, (int) (e.weight * s / mapScale));
        if (n == 2) {
            if (fillAlpha != 0) g.fill(x[0], z[0], x[1] + 1, z[1] + 1, fillARGB);
        } else {
            if (fillAlpha != 0) {
                for (int i = 0; i < n - 1; i++) g.fill(x[0], z[0], x[i + 1], z[i + 1], fillARGB);
            }
        }
        Matrix4f mat = g.pose().last().pose();
        BufferBuilder bb = beginBatch();
        if (n == 2) {
            thickLineQuad(bb, mat, x[0], z[0], x[1], z[0], lineARGB, tw);
            thickLineQuad(bb, mat, x[1], z[0], x[1], z[1], lineARGB, tw);
            thickLineQuad(bb, mat, x[1], z[1], x[0], z[1], lineARGB, tw);
            thickLineQuad(bb, mat, x[0], z[1], x[0], z[0], lineARGB, tw);
        } else {
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                thickLineQuad(bb, mat, x[i], z[i], x[j], z[j], lineARGB, tw);
            }
        }
        endBatch();
    }

    public static void drawCircle(GuiGraphics g, DynmapMarkerElement e, double s, double mapScale, Font font) {
        int rw = (int) (e.xr * s), rh = (int) (e.zr * s);
        if (rw <= 0 || rh <= 0) return;
        int fillAlpha = (int) (e.fillopacity * 255);
        int fillARGB = ((fillAlpha & 0xFF) << 24) | (e.fillcolor & 0x00FFFFFF);
        for (int dy = -rh; dy <= rh; dy++) {
            double t = dy / (double) rh;
            if (Math.abs(t) > 1) continue;
            int hw = (int) (rw * Math.sin(Math.acos(t)));
            g.fill(-hw, dy, hw + 1, dy + 1, fillARGB);
        }
        int lineAlpha = (int) (e.opacity * 255);
        int lineARGB = ((lineAlpha & 0xFF) << 24) | (e.color & 0x00FFFFFF);
        int tw = Math.max(1, (int) (e.weight * s / mapScale));
        Matrix4f mat = g.pose().last().pose();
        BufferBuilder bb = beginBatch();
        for (int i = 0; i < 36; i++) {
            double a1 = Math.PI * 2 * i / 36, a2 = Math.PI * 2 * (i + 1) / 36;
            int x1 = (int) (rw * Math.cos(a1)), z1 = (int) (rh * Math.sin(a1));
            int x2 = (int) (rw * Math.cos(a2)), z2 = (int) (rh * Math.sin(a2));
            thickLineQuad(bb, mat, x1, z1, x2, z2, lineARGB, tw);
        }
        endBatch();
    }

    public static void drawPoly(GuiGraphics g, DynmapMarkerElement e, double s, double mapScale, Font font) {
        if (e.xArr == null || e.zArr == null || e.xArr.length < 2) return;
        int n = e.xArr.length;
        int lineAlpha = (int) (e.opacity * 255);
        int lineARGB = ((lineAlpha & 0xFF) << 24) | (e.color & 0x00FFFFFF);
        int tw = Math.max(1, (int) (e.weight * s / mapScale));
        Matrix4f mat = g.pose().last().pose();
        BufferBuilder bb = beginBatch();
        for (int i = 0; i < n - 1; i++) {
            int x1 = (int) ((e.xArr[i] - e.x()) * s), z1 = (int) ((e.zArr[i] - e.z()) * s);
            int x2 = (int) ((e.xArr[i + 1] - e.x()) * s), z2 = (int) ((e.zArr[i + 1] - e.z()) * s);
            thickLineQuad(bb, mat, x1, z1, x2, z2, lineARGB, tw);
        }
        endBatch();
    }

    private static BufferBuilder beginBatch() {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        return bb;
    }

    private static void endBatch() {
        BufferUploader.drawWithShader(Tesselator.getInstance().getBuilder().end());
    }

    /**
     * Write 4 perp-offset vertices for a thick line segment to the current batch.
     */
    private static void thickLineQuad(BufferBuilder bb, Matrix4f mat,
                                      int x1, int y1, int x2, int y2, int color, int tw) {
        if (tw < 1) tw = 1;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        int dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt((double) dx * dx + (double) dy * dy);
        if (len < 0.5) return;
        float nx = (float) (-dy / len * tw * 0.5);
        float ny = (float) (dx / len * tw * 0.5);
        // CW winding for front-face (Y-down coordinate, so negate Y later in pose already handles it)
        bb.vertex(mat, x1 + nx, y1 + ny, 0).color(r, g, b, a).endVertex();
        bb.vertex(mat, x2 + nx, y2 + ny, 0).color(r, g, b, a).endVertex();
        bb.vertex(mat, x2 - nx, y2 - ny, 0).color(r, g, b, a).endVertex();
        bb.vertex(mat, x1 - nx, y1 - ny, 0).color(r, g, b, a).endVertex();
    }

    public static void drawPoint(GuiGraphics g, DynmapMarkerElement e, double s, Font font, IconManager iconManager) {
        String in = e.icon != null && !e.icon.isEmpty() ? e.icon : "default";
        iconManager.ensureDownloading(in);
        ResourceLocation tex = iconManager.get(in);
        PoseStack ps = g.pose();
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        double pscale = cfg.pointSizeFollowZoom ? s * cfg.pointIconScale : cfg.markerScale * cfg.pointIconScale;
        ps.pushPose();
        ps.scale((float) pscale, (float) pscale, 1);
        ps.translate(-4, 4, 0); // Center the icon on the point
        if (tex != null) g.blit(tex, 0, -8, 8, 8, 0, 0, 16, 16, 16, 16);
        drawLabel(g, font, e.label);
        ps.popPose();
    }

    public static void drawLabel(GuiGraphics g, Font font, String label) {
        if (label == null || label.isEmpty()) return;
        String[] lines = label.replaceAll("<br\\s*/?>", "\n").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                g.drawString(font, line, 10, -9 + i * 10, 0xFFFFFFFF, false);
            }
        }
    }
}

