package dev.ftb.mods.ftbbackups.client;

import dev.ftb.mods.ftbbackups.FTBBackups;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

public class BackupOverlayLayer implements LayeredDraw.Layer {
    public static final ResourceLocation ID = FTBBackups.id("overlay");

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        // NO-OP
    }
}
