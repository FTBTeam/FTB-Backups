package dev.ftb.mods.ftbbackups.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

public class BackupsClient {
    private static int currentBackupFile = 0;
    private static int totalBackupFiles = 0;

    public static void init(IEventBus eventBus) {
        //MinecraftForge.EVENT_BUS.addListener(FTBBackupsClient::onClientDisconnected);
        NeoForge.EVENT_BUS.addListener(BackupsClient::onDebugInfoEvent);

        eventBus.addListener(BackupsClient::registerGuiLayer);
    }

    public static void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(BackupOverlayLayer.ID, new BackupOverlayLayer());
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

    private static void onDebugInfoEvent(CustomizeGuiOverlayEvent.DebugText event) {
        OptionInstance<Boolean> booleanOptionInstance = Minecraft.getInstance().options.reducedDebugInfo();
        if (booleanOptionInstance.get()) {
            return;
        }

        if (totalBackupFiles > 0 && totalBackupFiles > currentBackupFile) {
            // I think this is right? @mikey (2025-03-18)
            event.getLeft().add(Component.translatable("ftbbackups.lang.timer_progress", currentBackupFile * 100 / totalBackupFiles, currentBackupFile, totalBackupFiles).withStyle(ChatFormatting.LIGHT_PURPLE).toString());
        }
    }
}
