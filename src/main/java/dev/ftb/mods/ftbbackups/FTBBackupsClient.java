package dev.ftb.mods.ftbbackups;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;

public class FTBBackupsClient {
    private static int currentBackupFile = 0;
    private static int totalBackupFiles = 0;

    public static void init() {
        //MinecraftForge.EVENT_BUS.addListener(FTBBackupsClient::onClientDisconnected);
        NeoForge.EVENT_BUS.addListener(FTBBackupsClient::onDebugInfoEvent);
    }

    public static void setFiles(int current, int total) {
        currentBackupFile = current;
        totalBackupFiles = total;
    }

	/*private static void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
	{
		currentBackupFile = 0;
		totalBackupFiles = 0;
	}*/

    private static void onDebugInfoEvent(RenderGuiLayerEvent.Post event) {
        OptionInstance<Boolean> booleanOptionInstance = Minecraft.getInstance().options.reducedDebugInfo();
        if (booleanOptionInstance.get()) {
            return;
        }

        if (totalBackupFiles > 0 && totalBackupFiles > currentBackupFile) {
            //event.getLeft().add(ChatFormatting.LIGHT_PURPLE + I18n.get("ftbbackups.lang.timer_progress", currentBackupFile * 100 / totalBackupFiles, currentBackupFile, totalBackupFiles));
        }
    }
}
