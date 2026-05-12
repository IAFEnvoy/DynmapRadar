package com.iafenvoy.dynmap.radar.map;

import com.iafenvoy.dynmap.radar.data.MarkerState;
import net.minecraft.world.phys.Vec3;

/**
 * Wraps a single marker for per-element rendering, matching the player renderer pattern.
 */
public class DynmapMarkerElement {
    public enum Type {AREA, CIRCLE, POLYLINE, POINT, SET}

    public final Type type;
    public final String setId;
    // Geometry anchor
    public final Vec3 pos;
    public final String icon;
    // Area / Polyline vertices
    public final double[] xArr, yArr, zArr;
    public final double ytop, ybottom;
    // Circle
    public final double xr, zr;
    // Common
    public final String id, label, desc;
    public final int minzoom, maxzoom;
    public final int weight;
    public final double opacity, fillopacity;
    public final int color, fillcolor;
    // Precomputed axis-aligned bounding box (world-space)
    public final double minX, minZ, maxX, maxZ;

    private DynmapMarkerElement(Type t, String setId, Vec3 pos, String id, String label,
                                String icon, String desc, int minz, int maxz,
                                double[] xArr, double[] zArr, double[] yArr,
                                double ytop, double ybottom, double xr, double zr,
                                int weight, double opacity, int color, double fillopacity, int fillcolor) {
        this.type = t;
        this.setId = setId;
        this.pos = pos;
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.desc = desc;
        this.minzoom = minz;
        this.maxzoom = maxz;
        this.xArr = xArr;
        this.zArr = zArr;
        this.yArr = yArr;
        this.ytop = ytop;
        this.ybottom = ybottom;
        this.xr = xr;
        this.zr = zr;
        this.weight = weight;
        this.opacity = opacity;
        this.color = color;
        this.fillopacity = fillopacity;
        this.fillcolor = fillcolor;
        double[] mm = computeMinMax(t, pos.x, pos.z, xArr, zArr, xr, zr);
        this.minX = mm[0] - this.x();
        this.minZ = mm[1] - this.z();
        this.maxX = mm[2] - this.x();
        this.maxZ = mm[3] - this.z();
    }

    private static double[] computeMinMax(Type t, double ax, double az,
                                          double[] xArr, double[] zArr, double xr, double zr) {
        if (t == Type.POINT) return new double[]{ax - 1, az - 1, ax + 1, az + 1};
        double mnx = ax, mxX = ax, mnz = az, mxz = az;
        if (xArr != null && xArr.length > 0) {
            for (int i = 0; i < xArr.length; i++) {
                if (xArr[i] < mnx) mnx = xArr[i];
                if (xArr[i] > mxX) mxX = xArr[i];
                if (zArr[i] < mnz) mnz = zArr[i];
                if (zArr[i] > mxz) mxz = zArr[i];
            }
        }
        if (xr > 0) {
            mnx = ax - xr;
            mxX = ax + xr;
        }
        if (zr > 0) {
            mnz = az - zr;
            mxz = az + zr;
        }
        return new double[]{mnx, mnz, mxX, mxz};
    }

    // Convenience getters
    public double x() {
        return pos.x;
    }

    public double y() {
        return pos.y;
    }

    public double z() {
        return pos.z;
    }

    // Factory methods
    public static DynmapMarkerElement fromPoint(String setId, MarkerState.PointMarker p) {
        return new DynmapMarkerElement(Type.POINT, setId, new Vec3(p.x(), p.y(), p.z()), p.id(), p.label(), p.icon(), p.desc(), p.minzoom(), p.maxzoom(), null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static DynmapMarkerElement fromArea(String setId, MarkerState.AreaMarker a) {
        return new DynmapMarkerElement(Type.AREA, setId, new Vec3(a.xArr()[0], a.ytop(), a.zArr()[0]), a.id(), a.label(), null, a.desc(), a.minzoom(), a.maxzoom(), a.xArr(), a.zArr(), null, a.ytop(), a.ybottom(), 0, 0, a.weight(), a.opacity(), a.color(), a.fillopacity(), a.fillcolor());
    }

    public static DynmapMarkerElement fromCircle(String setId, MarkerState.CircleMarker c) {
        return new DynmapMarkerElement(Type.CIRCLE, setId, new Vec3(c.x(), c.y(), c.z()), c.id(), c.label(), null, c.desc(), c.minzoom(), c.maxzoom(), null, null, null, 0, 0, c.xr(), c.zr(), c.weight(), c.opacity(), c.color(), c.fillopacity(), c.fillcolor());
    }

    public static DynmapMarkerElement fromPolyLine(String setId, MarkerState.PolyLineMarker l) {
        double y = l.yArr() != null && l.yArr().length > 0 ? l.yArr()[0] : 64;
        return new DynmapMarkerElement(Type.POLYLINE, setId, new Vec3(l.xArr()[0], y, l.zArr()[0]), l.id(), l.label(), null, l.desc(), l.minzoom(), l.maxzoom(), l.xArr(), l.zArr(), l.yArr(), 0, 0, 0, 0, l.weight(), l.opacity(), l.color(), 0, 0);
    }
}
