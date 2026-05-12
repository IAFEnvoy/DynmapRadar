package com.iafenvoy.dynmap.radar.map;

import com.iafenvoy.dynmap.radar.data.DynmapPlayerData;

/**
 * Map element representing a Dynmap player on the world map.
 */
public record DynmapPlayerElement(DynmapPlayerData data, String displayName) {

    public String getAccount() {
        return this.data != null ? this.data.account : "";
    }

    public String getName() {
        return this.data != null ? this.data.name : "";
    }

    public double getX() {
        return this.data != null ? this.data.x : 0;
    }

    public double getY() {
        return this.data != null ? this.data.y : 0;
    }

    public double getZ() {
        return this.data != null ? this.data.z : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DynmapPlayerElement that)) return false;
        if (this.data == null || that.data == null) return false;
        return this.data.account.equals(that.data.account);
    }

    @Override
    public int hashCode() {
        return this.data != null ? this.data.account.hashCode() : 0;
    }
}
