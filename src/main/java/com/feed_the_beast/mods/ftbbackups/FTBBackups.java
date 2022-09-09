package com.feed_the_beast.mods.ftbbackups;

import com.feed_the_beast.mods.ftbbackups.net.BackupProgressPacket;
import com.feed_the_beast.mods.ftbbackups.net.FTBBackupsNetHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FTBBackups.MOD_ID)
@Mod.EventBusSubscriber(modid = FTBBackups.MOD_ID)
public class FTBBackups
{
	public static final String MOD_ID = "ftbbackups";
	public static final Logger LOGGER = LogManager.getLogger("FTB Utilities Backups");

	public FTBBackups()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
		FTBBackupsNetHandler.init();
		FTBBackupsConfig.register();
		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}

	private void clientSetup(FMLClientSetupEvent event)
	{
		FTBBackupsClient.init();
	}

	@SubscribeEvent
	public static void serverAboutToStart(ServerStartedEvent event)
	{
		Backups.INSTANCE.init(event.getServer());
	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event)
	{
		BackupCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public static void serverStopping(ServerStoppingEvent event)
	{
		if (FTBBackupsConfig.forceOnShutdown)
		{
			Backups.INSTANCE.run(event.getServer(), true, Component.literal("Server"), "");
		}
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer)
		{
			FTBBackupsNetHandler.MAIN.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new BackupProgressPacket(Backups.INSTANCE.currentFile, Backups.INSTANCE.totalFiles));
		}
	}

	@SubscribeEvent
	public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer)
		{
			Backups.INSTANCE.hadPlayersOnline = true;
		}
	}

	@SubscribeEvent
	public static void worldTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.START && !event.side.isClient()) {
			Backups.INSTANCE.tick(event.getServer(), System.currentTimeMillis());
		}
	}
}