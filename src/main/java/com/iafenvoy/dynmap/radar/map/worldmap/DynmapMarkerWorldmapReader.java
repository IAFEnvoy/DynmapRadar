package com.iafenvoy.dynmap.radar.map.worldmap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.map.DynmapMarkerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.element.MapElementReader;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.teleport.MapTeleporter;

import java.util.ArrayList;

public class DynmapMarkerWorldmapReader extends MapElementReader<DynmapMarkerElement, DynmapPlayerElementRenderContext, DynmapMarkerWorldmapRenderer> {

    @Override
    public boolean isHidden(DynmapMarkerElement e, DynmapPlayerElementRenderContext c) {
        if (e.type == DynmapMarkerElement.Type.POINT) {
            double minScale = DynmapRadarClient.CONFIG_MANAGER.getConfig().pointMinScale;
            if (minScale > 0 && c.cachedScale < minScale) return true;
        }
        return false;
    }

    @Override
    public double getRenderX(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return e.x();
    }

    @Override
    public double getRenderZ(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return e.z();
    }

    @Override
    public boolean isInteractable(ElementRenderLocation location, DynmapMarkerElement element) {
        return element.type == DynmapMarkerElement.Type.POINT;
    }

    @Override
    public int getInteractionBoxLeft(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return (int) (Math.floor(e.minX) * c.cachedScale - 50);
    }

    @Override
    public int getInteractionBoxRight(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return (int) (Math.ceil(e.maxX) * c.cachedScale + 50);
    }

    @Override
    public int getInteractionBoxTop(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return (int) (Math.floor(e.minZ) * c.cachedScale - 50);
    }

    @Override
    public int getInteractionBoxBottom(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return (int) (Math.ceil(e.maxZ) * c.cachedScale + 50);
    }

    @Override
    public int getRenderBoxLeft(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return (int) (Math.floor(e.minX) * c.cachedScale - 50);
    }

    @Override
    public int getRenderBoxRight(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return (int) (Math.ceil(e.maxX) * c.cachedScale + 50);
    }

    @Override
    public int getRenderBoxTop(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return (int) (Math.floor(e.minZ) * c.cachedScale - 50);
    }

    @Override
    public int getRenderBoxBottom(DynmapMarkerElement e, DynmapPlayerElementRenderContext c, float p) {
        return (int) (Math.ceil(e.maxZ) * c.cachedScale + 50);
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

    @Override
    public boolean isRightClickValid(DynmapMarkerElement e) {
        return true;
    }

    @Override
    public ArrayList<RightClickOption> getRightClickOptions(DynmapMarkerElement e, IRightClickableElement r) {
        ArrayList<RightClickOption> opts = new ArrayList<>();
        if (e.type != DynmapMarkerElement.Type.POINT) return opts;
        // Title (not clickable)
        opts.add(new RightClickOption("dynmap_radar.right_click.title", 0, r) {
            @Override
            public void onAction(Screen screen) {
            }

            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public String getDisplayName() {
                String label = e.label != null ? e.label : e.id;
                return label != null ? label : I18n.get("dynmap_radar.right_click.title");
            }
        });
        // Copy label
        opts.add(new RightClickOption("dynmap_radar.copy_name", 1, r) {
            @Override
            public void onAction(Screen screen) {
                Minecraft.getInstance().keyboardHandler.setClipboard(e.label != null ? e.label : "");
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.copy_name");
            }
        });
        // Copy coords
        opts.add(new RightClickOption("dynmap_radar.copy_coords", 2, r) {
            @Override
            public void onAction(Screen screen) {
                Minecraft.getInstance().keyboardHandler.setClipboard(
                        (int) e.x() + " " + (int) e.y() + " " + (int) e.z());
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.copy_coords");
            }
        });
        // Copy full info
        opts.add(new RightClickOption("dynmap_radar.copy_full", 3, r) {
            @Override
            public void onAction(Screen screen) {
                StringBuilder sb = new StringBuilder();
                sb.append(e.label != null ? e.label : e.id);
                sb.append(" [").append((int) e.x()).append(", ").append((int) e.y()).append(", ").append((int) e.z()).append("]");
                if (e.desc != null && !e.desc.isEmpty()) {
                    sb.append(" ").append(e.desc.replaceAll("<[^>]+>", ""));
                }
                Minecraft.getInstance().keyboardHandler.setClipboard(sb.toString());
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.copy_full");
            }
        });
        // Teleport (uses Xaero's internal teleporter)
        opts.add(new RightClickOption("dynmap_radar.teleport", 4, r) {
            @Override
            public void onAction(Screen screen) {
                WorldMapSession session = WorldMapSession.getCurrentSession();
                if (session == null) return;
                new MapTeleporter().teleport(screen, session.getMapProcessor().getMapWorld(),
                        (int) e.x(), (int) e.y(), (int) e.z(), null);
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.teleport");
            }
        });
        // Create waypoint
        opts.add(new RightClickOption("dynmap_radar.create_waypoint", 5, r) {
            @Override
            public void onAction(Screen screen) {
                MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
                MinimapWorld world = session.getWorldManager().getCurrentWorld();
                if (world == null) return;
                String label = e.label != null && !e.label.isEmpty() ? e.label : "Dynmap Marker";
                String initials = label.length() >= 2 ? label.substring(0, 2).toUpperCase() : "DM";
                int color = DynmapRadarClient.CONFIG_MANAGER.getConfig().waypointColor;
                world.getCurrentWaypointSet().add(new Waypoint((int) e.x(), (int) e.y(), (int) e.z(), label, initials, color));
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.create_waypoint");
            }
        });
        return opts;
    }
}

