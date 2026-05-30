package com.iafenvoy.dynmap.radar.data;

import com.google.gson.JsonArray;

import java.util.List;

/**
 * Full response from the Dynmap /up/world/{world}/{timestamp} API.
 * updates field is a raw JsonArray because entries have polymorphic shapes.
 */
public class DynmapApiResponse {
    public List<DynmapPlayerData> players;
    public JsonArray updates;
}
