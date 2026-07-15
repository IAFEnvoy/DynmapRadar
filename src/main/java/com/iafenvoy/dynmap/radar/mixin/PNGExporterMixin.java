package com.iafenvoy.dynmap.radar.mixin;

import com.iafenvoy.dynmap.radar.map.ExportRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xaero.map.file.export.PNGExporter;

import java.awt.image.BufferedImage;

@Mixin(value = PNGExporter.class, remap = false)
public class PNGExporterMixin {

    @ModifyArgs(method = "export",
                at = @At(value = "INVOKE",
                         target = "Lxaero/map/file/export/PNGExporter;saveImage(Ljava/awt/image/BufferedImage;Ljava/nio/file/Path;Ljava/lang/String;Ljava/lang/String;)Lxaero/map/file/export/PNGExportResultType;"),
                remap = false)
    private void drawMarkers(Args args) {
        BufferedImage image = args.get(0);
        // Xaero passes the export origin as the fourth saveImage argument
        // ("_x<worldX>_z<worldZ>"). The third argument is null for a full export.
        String origin = args.get(3);
        if (image != null) {
            ExportRenderHelper.renderMarkers(image, origin);
        }
    }
}
