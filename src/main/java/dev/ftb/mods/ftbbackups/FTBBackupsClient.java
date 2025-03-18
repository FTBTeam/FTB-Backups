package dev.ftb.mods.ftbbackups;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.NeoForge;

public class FTBBackupsClient {
    private static int currentBackupFile = 0;
    private static int totalBackupFiles = 0;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(FTBBackupsClient::onClientDisconnected);
        NeoForge.EVENT_BUS.addListener(FTBBackupsClient::onDebugInfoEvent);
    }

    public static void setBackupProgress(int current, int total) {
        currentBackupFile = current;
        totalBackupFiles = total;
    }

	private static void onClientDisconnected(ClientPlayerNetworkEvent.LoggingOut ignoredEvent) {
		currentBackupFile = 0;
		totalBackupFiles = 0;
	}

    private static void onDebugInfoEvent(CustomizeGuiOverlayEvent.DebugText event) {
        if (!Minecraft.getInstance().options.reducedDebugInfo().get()
                && totalBackupFiles > 0
                && totalBackupFiles > currentBackupFile)
        {
            event.getLeft().add(ChatFormatting.LIGHT_PURPLE + I18n.get("ftbbackups.lang.timer_progress",
                    currentBackupFile * 100 / totalBackupFiles, currentBackupFile, totalBackupFiles)
            );
        }
    }
}