package com.iafenvoy.dynmap.radar.data;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed update from the Dynmap /up/world/* endpoint.
 * Handles the fact that "x", "y", "z" fields can be either scalar or array.
 */
public class DynmapUpdate {
    // Common fields
    public String type;

    // Marker component messages use "ctype" instead
    public String ctype;
    public String msg;

    // Marker common
    public String id;
    public String label;
    public String set;
    public String desc;
    public int minzoom;
    public int maxzoom;
    public boolean markup;

    // Point marker (scalar, Gson fills these)
    public double x;
    public double y;
    public double z;
    public String icon;
    public String dim;

    // Area / polyline / circle markers (arrays parsed from raw JSON)
    public transient double[] xArr;
    public transient double[] yArr;
    public transient double[] zArr;
    public int weight;
    public double opacity;
    public String color;
    public double fillopacity;
    public String fillcolor;
    public double ytop;
    public double ybottom;

    // Circle marker
    public double xr;
    public double zr;

    // Marker set
    public int layerprio;
    public boolean showlabels = true;
    public boolean hide;

    // Chat
    public String source;
    public String playerName;
    public String message;
    public String account;
    public String channel;

    // Tile
    public String name;
    public long timestamp;

    // --- Manual field extraction from raw JSON ---

    public static DynmapUpdate fromJsonElement(JsonElement elem) {
        DynmapUpdate u = new Gson().fromJson(elem, DynmapUpdate.class);
        if (u == null) return null;
        JsonObject obj = elem.getAsJsonObject();
        u.xArr = parseDoubleArray(obj, "x");
        u.yArr = parseDoubleArray(obj, "y");
        u.zArr = parseDoubleArray(obj, "z");
        return u;
    }

    public static List<DynmapUpdate> parseUpdates(JsonElement updatesElem) {
        List<DynmapUpdate> list = new ArrayList<>();
        if (updatesElem == null || !updatesElem.isJsonArray()) return list;
        for (JsonElement e : updatesElem.getAsJsonArray()) {
            DynmapUpdate u = fromJsonElement(e);
            if (u != null) list.add(u);
        }
        return list;
    }

    private static double[] parseDoubleArray(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonArray()) return null;
        JsonArray arr = el.getAsJsonArray();
        double[] result = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.get(i).getAsDouble();
        }
        return result;
    }

    // --- Helpers ---

    public boolean isMarkerUpdate() {
        return "markers".equals(this.ctype) && "markerupdated".equals(this.msg);
    }

    public boolean isMarkerDelete() {
        return "markers".equals(this.ctype) && "markerdeleted".equals(this.msg);
    }

    public boolean isAreaUpdate() {
        return "markers".equals(this.ctype) && "areaupdated".equals(this.msg);
    }

    public boolean isAreaDelete() {
        return "markers".equals(this.ctype) && "areadeleted".equals(this.msg);
    }

    public boolean isLineUpdate() {
        return "markers".equals(this.ctype) && "lineupdated".equals(this.msg);
    }

    public boolean isLineDelete() {
        return "markers".equals(this.ctype) && "linedeleted".equals(this.msg);
    }

    public boolean isCircleUpdate() {
        return "markers".equals(this.ctype) && "circleupdated".equals(this.msg);
    }

    public boolean isCircleDelete() {
        return "markers".equals(this.ctype) && "circledeleted".equals(this.msg);
    }

    public boolean isSetUpdate() {
        return "markers".equals(this.ctype) && "setupdated".equals(this.msg);
    }

    public boolean isSetDelete() {
        return "markers".equals(this.ctype) && "setdeleted".equals(this.msg);
    }

    public boolean isTile() {
        return "tile".equals(this.type);
    }

    public boolean isChat() {
        return "chat".equals(this.type);
    }

    public int parsedColor() {
        if (this.color == null || this.color.isEmpty()) return 0xFFFF0000;
        try {
            if (this.color.startsWith("#")) return 0xFF000000 | Integer.parseInt(this.color.substring(1), 16);
            return 0xFF000000 | Integer.parseInt(this.color, 16);
        } catch (NumberFormatException e) {
            return 0xFFFF0000;
        }
    }

    public int parsedFillColor() {
        if (this.fillcolor == null || this.fillcolor.isEmpty()) return this.parsedColor();
        try {
            if (this.fillcolor.startsWith("#")) return 0xFF000000 | Integer.parseInt(this.fillcolor.substring(1), 16);
            return 0xFF000000 | Integer.parseInt(this.fillcolor, 16);
        } catch (NumberFormatException e) {
            return this.parsedColor();
        }
    }
}

