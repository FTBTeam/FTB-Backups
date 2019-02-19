package com.feed_the_beast.mods.ftbbackups;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = FTBBackups.MOD_ID, value = Side.CLIENT)
public class FTBBackupsClientEventHandler
{
	public static int currentBackupFile = 0;
	public static int totalBackupFiles = 0;

	@SubscribeEvent
	public static void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
	{
		currentBackupFile = 0;
		totalBackupFiles = 0;
	}

	@SubscribeEvent
	public static void onDebugInfoEvent(RenderGameOverlayEvent.Text event)
	{
		if (Minecraft.getMinecraft().gameSettings.showDebugInfo)
		{
			return;
		}

		if (totalBackupFiles > 0 && totalBackupFiles > currentBackupFile)
		{
			event.getLeft().add(TextFormatting.LIGHT_PURPLE + I18n.format("ftbbackups.lang.timer_progress", currentBackupFile * 100 / totalBackupFiles, currentBackupFile, totalBackupFiles));
		}
	}
}