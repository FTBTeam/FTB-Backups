package dev.ftb.mods.ftbbackups.client;

import dev.ftb.mods.ftbbackups.FTBBackups;
import dev.ftb.mods.ftbbackups.FTBBackupsClientConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BackupOverlayLayer implements LayeredDraw.Layer {
    public static final ResourceLocation ID = FTBBackups.id("overlay");

    private int prevProgress;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!FTBBackupsClientConfig.SHOW_OVERLAY.get()) {
            return;
        }

        BackupsClient.BackupProgress progress = BackupsClient.getBackupProgress();
        if (!progress.inProgress()) {
            return;
        }

        Component line1 = progress.finished() ?
                Component.translatable("ftbbackups3.lang.finished") :
                Component.translatable("ftbbackups3.lang.already_running");
        Component line2 = BackupsClient.progressMessage();

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int width = mc.getWindow().getGuiScaledWidth() / 4;

        int progressBarHeight = font.lineHeight + 4;
        int height = font.lineHeight + progressBarHeight + 8;
        float scale = 1f;

        int insetX = FTBBackupsClientConfig.OVERLAY_INSET_X.get();
        int insetY = FTBBackupsClientConfig.OVERLAY_INSET_Y.get();
        var pos = FTBBackupsClientConfig.OVERLAY_POS.get().getPanelPos(
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                (int) (width * scale), (int) (height * scale),
                insetX, insetY
        );

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(pos.x(), pos.y(), 100);
        guiGraphics.pose().scale(scale, scale, 1F);

        // panel background
        Color4I.rgba(0x80200020).draw(guiGraphics, -2, -2, width + 4, height + 4);
        GuiHelper.drawHollowRect(guiGraphics, -2, -2, width + 4, height + 4, Color4I.rgba(0x80400040), false);

        guiGraphics.drawString(font, line1, 2, 2, 0xFFFAF9F6, false);

        int pw = (int) ((width - 4) * Mth.lerp(deltaTracker.getGameTimeDeltaTicks(), prevProgress, progress.current()) / progress.total());
        int sy = font.lineHeight + 4;
        guiGraphics.fill(2, sy - 1, width - 3, sy + progressBarHeight + 1, 0xC00060E0);
        guiGraphics.fill(3, sy, pw, sy + progressBarHeight, 0xC00040A0);

        guiGraphics.drawString(font, line2, 14, 2 + font.lineHeight + 5, 0xFFFAF9F6, false);

        guiGraphics.pose().popPose();

        prevProgress = progress.current();
    }
}
