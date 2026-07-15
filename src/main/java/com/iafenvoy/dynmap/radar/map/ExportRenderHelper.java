package com.iafenvoy.dynmap.radar.map;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.config.ServerConfig;
import com.iafenvoy.dynmap.radar.data.DynmapDataStorage;
import com.iafenvoy.dynmap.radar.data.MarkerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders Dynmap markers into the BufferedImage BEFORE Xaero writes it to disk.
 * Called via Mixin at the saveImage INVOKE inside PNGExporter.export().
 * Export world coordinates are read from Xaero's saveImage suffix
 * (e.g. "_x-1024_z-1024"). The suffix contains the world coordinate at the
 * top-left of the exported area; the image uses one pixel per map block at the
 * default export scale.
 */
public final class ExportRenderHelper {
    private static final Logger LOG = LoggerFactory.getLogger("dynmap_radar_export");
    private static final Pattern EXPORT_ORIGIN_PATTERN = Pattern.compile("_x(-?\\d+)_z(-?\\d+)");

    private ExportRenderHelper() {}

    public static void renderMarkers(BufferedImage image, String exportOrigin) {
        ServerConfig cfg = DynmapRadarClient.CONFIG_MANAGER.getConfig();
        if (!cfg.exportWithMap) return;

        DynmapDataStorage storage = DynmapRadarClient.DATA_FETCHER.getStorage();
        List<DynmapMarkerElement> elements = MarkerState.collectElements(
                storage.getMarkerSets(), storage.getPointMarkers(),
                storage.getAreaMarkers(), storage.getLineMarkers(),
                storage.getCircleMarkers(), cfg::isLayerVisibleWorldmap, false);
        if (elements.isEmpty()) return;

        Matcher originMatcher = exportOrigin == null ? null : EXPORT_ORIGIN_PATTERN.matcher(exportOrigin);
        if (originMatcher == null || !originMatcher.find()) {
            LOG.warn("Cannot render Dynmap markers: Xaero did not provide an export origin ({})", exportOrigin);
            return;
        }

        int iw = image.getWidth(), ih = image.getHeight();
        double left = Integer.parseInt(originMatcher.group(1));
        double top = Integer.parseInt(originMatcher.group(2)) + ih;
        double right = left + iw;
        double bottom = top - ih;

        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (DynmapMarkerElement e : elements) {
                switch (e.type) {
                    case AREA -> drawArea(g, e, left, right, top, bottom, iw, ih);
                    case CIRCLE -> drawCircle(g, e, left, right, top, bottom, iw, ih);
                    case POLYLINE -> drawPolyline(g, e, left, right, top, bottom, iw, ih);
                    case POINT -> drawPoint(g, e, left, right, top, bottom, iw, ih);
                }
            }
        } finally { g.dispose(); }
    }

    private static int[] proj(double wx, double wz, double l, double r, double t, double b, int iw, int ih) {
        // Xaero's exported PNG already has increasing world Z mapped downward.
        return new int[]{(int) Math.round((wx - l) / (r - l) * iw), (int) Math.round((wz - b) / (t - b) * ih)};
    }
    private static Color argb(int hex, int a) { return new Color((hex >> 16) & 0xFF, (hex >> 8) & 0xFF, hex & 0xFF, Math.min(255, Math.max(0, a))); }

    private static void drawArea(Graphics2D g, DynmapMarkerElement e, double l, double r, double t, double b, int iw, int ih) {
        if (e.xArr == null || e.zArr == null || e.xArr.length < 2) return;
        int n = e.xArr.length; int[] px = new int[n], pz = new int[n];
        for (int i = 0; i < n; i++) { int[] v = proj(e.xArr[i], e.zArr[i], l, r, t, b, iw, ih); px[i] = v[0]; pz[i] = v[1]; }
        int fa = (int)(e.fillopacity * 255);
        if (fa > 0) { g.setColor(argb(e.fillcolor, fa)); if (n == 2) { int x0 = Math.min(px[0], px[1]), z0 = Math.min(pz[0], pz[1]); g.fillRect(x0, z0, Math.max(1, Math.abs(px[1]-px[0]) + 1), Math.max(1, Math.abs(pz[1]-pz[0]) + 1)); } else g.fillPolygon(new Polygon(px, pz, n)); }
        int la = (int)(e.opacity * 255);
        if (la > 0) { g.setColor(argb(e.color, la)); g.setStroke(new BasicStroke(Math.max(1f, (float)e.weight), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); if (n == 2) g.drawRect(Math.min(px[0], px[1]), Math.min(pz[0], pz[1]), Math.abs(px[1]-px[0]), Math.abs(pz[1]-pz[0])); else g.drawPolygon(new Polygon(px, pz, n)); }
    }

    private static void drawCircle(Graphics2D g, DynmapMarkerElement e, double l, double r, double t, double b, int iw, int ih) {
        int[] c = proj(e.x(), e.z(), l, r, t, b, iw, ih), ex = proj(e.x()+e.xr, e.z(), l, r, t, b, iw, ih), ez = proj(e.x(), e.z()+e.zr, l, r, t, b, iw, ih);
        double rx = Math.abs(ex[0]-c[0]), rz = Math.abs(ez[1]-c[1]); if (rx <= 0 || rz <= 0) return;
        double cx = c[0]-rx, cz = c[1]-rz;
        int fa = (int)(e.fillopacity*255); if (fa > 0) { g.setColor(argb(e.fillcolor, fa)); g.fill(new Ellipse2D.Double(cx, cz, rx*2, rz*2)); }
        int la = (int)(e.opacity*255); if (la > 0) { g.setColor(argb(e.color, la)); g.setStroke(new BasicStroke(Math.max(1f, (float)e.weight), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(new Ellipse2D.Double(cx, cz, rx*2, rz*2)); }
    }

    private static void drawPolyline(Graphics2D g, DynmapMarkerElement e, double l, double r, double t, double b, int iw, int ih) {
        if (e.xArr == null || e.zArr == null || e.xArr.length < 2) return;
        int n = e.xArr.length; int[] px = new int[n], pz = new int[n];
        for (int i = 0; i < n; i++) { int[] v = proj(e.xArr[i], e.zArr[i], l, r, t, b, iw, ih); px[i] = v[0]; pz[i] = v[1]; }
        int la = (int)(e.opacity*255); if (la > 0) { g.setColor(argb(e.color, la)); g.setStroke(new BasicStroke(Math.max(1f, (float)e.weight), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); Path2D p = new Path2D.Float(); p.moveTo(px[0], pz[0]); for (int i = 1; i < n; i++) p.lineTo(px[i], pz[i]); g.draw(p); }
    }

    private static void drawPoint(Graphics2D g, DynmapMarkerElement e, double l, double r, double t, double b, int iw, int ih) {
        int[] v = proj(e.x(), e.z(), l, r, t, b, iw, ih); int cx = v[0], cz = v[1];
        g.setColor(new Color(0xFF, 0xFF, 0xFF, 200)); g.fillOval(cx-4, cz-4, 8, 8);
        String label = e.label;
        if (label != null && !label.isEmpty()) { label = label.replaceAll("<[^>]+>", "").replace("<br/>", " "); g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10)); g.drawString(label, cx-g.getFontMetrics().stringWidth(label)/2, cz-6); }
    }
}
