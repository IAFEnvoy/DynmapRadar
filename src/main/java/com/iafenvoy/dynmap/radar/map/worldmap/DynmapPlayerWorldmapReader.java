package com.iafenvoy.dynmap.radar.map.worldmap;

import com.iafenvoy.dynmap.radar.DynmapRadarClient;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElement;
import com.iafenvoy.dynmap.radar.map.DynmapPlayerElementRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.WorldMapSession;
import xaero.map.element.MapElementReader;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.teleport.MapTeleporter;

import java.util.ArrayList;

/**
 * Reader for Dynmap player elements - provides position, hitbox, and menu info.
 */
public class DynmapPlayerWorldmapReader extends MapElementReader<DynmapPlayerElement, DynmapPlayerElementRenderContext, DynmapPlayerWorldmapRenderer> {

    @Override
    public boolean isHidden(DynmapPlayerElement element, DynmapPlayerElementRenderContext context) {
        return false;
    }

    @Override
    public double getRenderX(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return element.getX();
    }

    @Override
    public double getRenderZ(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return element.getZ();
    }

    @Override
    public int getInteractionBoxLeft(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return -8;
    }

    @Override
    public int getInteractionBoxRight(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return 8;
    }

    @Override
    public int getInteractionBoxTop(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return -16;
    }

    @Override
    public int getInteractionBoxBottom(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return 4;
    }

    @Override
    public int getRenderBoxLeft(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return -8;
    }

    @Override
    public int getRenderBoxRight(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return 8;
    }

    @Override
    public int getRenderBoxTop(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return -16;
    }

    @Override
    public int getRenderBoxBottom(DynmapPlayerElement element, DynmapPlayerElementRenderContext context, float partialTicks) {
        return 4;
    }

    @Override
    public int getLeftSideLength(DynmapPlayerElement element, Minecraft mc) {
        String name = element.displayName();
        return mc.font.width(name) / 2 + 4;
    }

    @Override
    public String getMenuName(DynmapPlayerElement element) {
        return element.displayName();
    }

    @Override
    public String getFilterName(DynmapPlayerElement element) {
        return element.displayName();
    }

    @Override
    public int getMenuTextFillLeftPadding(DynmapPlayerElement element) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(DynmapPlayerElement element) {
        return 0xFF000000;
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return false;
    }

    @Override
    public boolean isRightClickValid(DynmapPlayerElement element) {
        return true;
    }

    @Override
    public ArrayList<RightClickOption> getRightClickOptions(DynmapPlayerElement element, IRightClickableElement rightClickable) {
        ArrayList<RightClickOption> opts = new ArrayList<>();
        // Title (not clickable)
        opts.add(new RightClickOption("dynmap_radar.right_click.title", 0, rightClickable) {
            @Override
            public void onAction(Screen screen) {
            }

            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public String getDisplayName() {
                return element.displayName();
            }
        });
        // Copy name
        opts.add(new RightClickOption("dynmap_radar.copy_name", 1, rightClickable) {
            @Override
            public void onAction(Screen screen) {
                Minecraft.getInstance().keyboardHandler.setClipboard(element.displayName());
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.copy_name");
            }
        });
        // Copy coords
        opts.add(new RightClickOption("dynmap_radar.copy_coords", 2, rightClickable) {
            @Override
            public void onAction(Screen screen) {
                Minecraft.getInstance().keyboardHandler.setClipboard(
                        (int) element.getX() + " " + (int) element.getY() + " " + (int) element.getZ());
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.copy_coords");
            }
        });
        // Teleport
        opts.add(new RightClickOption("dynmap_radar.teleport", 3, rightClickable) {
            @Override
            public void onAction(Screen screen) {
                WorldMapSession session = WorldMapSession.getCurrentSession();
                if (session == null) return;
                new MapTeleporter().teleport(screen, session.getMapProcessor().getMapWorld(),
                        (int) element.getX(), (int) element.getY(), (int) element.getZ(), null);
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.teleport");
            }
        });
        // Create waypoint
        opts.add(new RightClickOption("dynmap_radar.create_waypoint", 4, rightClickable) {
            @Override
            public void onAction(Screen screen) {
                MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
                MinimapWorld world = session.getWorldManager().getCurrentWorld();
                if (world == null) return;
                String name = element.displayName();
                String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
                int color = DynmapRadarClient.CONFIG_MANAGER.getConfig().waypointColor;
                world.getCurrentWaypointSet().add(new Waypoint((int) element.getX(), (int) element.getY(), (int) element.getZ(), name, initials, color));
            }

            @Override
            public String getDisplayName() {
                return I18n.get("dynmap_radar.right_click.create_waypoint");
            }
        });
        return opts;
    }
}
