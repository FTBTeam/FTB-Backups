package dev.ftb.mods.ftbbackups;


import dev.ftb.mods.ftbbackups.net.BackupProgressPacket;
import dev.ftb.mods.ftbbackups.net.FTBBackupsNetHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FTBBackups.MOD_ID)
public class FTBBackups
{
	public static final String MOD_ID = "ftbbackups";
	public static final Logger LOGGER = LogManager.getLogger("FTB Utilities Backups");

	public FTBBackups(IEventBus eventBus)
	{
		eventBus.addListener(this::clientSetup);
//		FTBBackupsNetHandler.init();
		FTBBackupsConfig.register(eventBus);
		//ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
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
			//TODO: Send Packets
			//FTBBackupsNetHandler.MAIN.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new BackupProgressPacket(Backups.INSTANCE.currentFile, Backups.INSTANCE.totalFiles));
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
	public static void worldTick(ServerTickEvent event) {
		/*if (event.phase != ServerTickEvent.PostTickEvent.Phase.START && !event.side.isClient()) {
			Backups.INSTANCE.tick(event.getServer(), System.currentTimeMillis());
		}*/
		//I Guess this is just called server side now??
		Backups.INSTANCE.tick(event.getServer(), System.currentTimeMillis());
	}
}