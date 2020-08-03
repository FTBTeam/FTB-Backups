package com.feed_the_beast.mods.ftbbackups;

import com.feed_the_beast.mods.ftbbackups.net.BackupProgressPacket;
import com.feed_the_beast.mods.ftbbackups.net.FTBBackupsNetHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Pair;
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
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}

	private void clientSetup(FMLClientSetupEvent event)
	{
		FTBBackupsClient.init();
	}

	@SubscribeEvent
	public static void serverAboutToStart(FMLServerStartedEvent event)
	{
		Backups.INSTANCE.init(event.getServer());
	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event)
	{
		BackupCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public static void serverStopping(FMLServerStoppingEvent event)
	{
		if (FTBBackupsConfig.forceOnShutdown)
		{
			Backups.INSTANCE.run(event.getServer(), true, new StringTextComponent("Server"), "");
		}
	}

	@SubscribeEvent
	public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (event.getPlayer() instanceof ServerPlayerEntity)
		{
			FTBBackupsNetHandler.MAIN.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()), new BackupProgressPacket(Backups.INSTANCE.currentFile, Backups.INSTANCE.totalFiles));
		}
	}

	@SubscribeEvent
	public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		if (event.getPlayer() instanceof ServerPlayerEntity)
		{
			Backups.INSTANCE.hadPlayersOnline = true;
		}
	}

	@SubscribeEvent
	public static void serverTick(TickEvent.ServerTickEvent event)
	{
		if (event.phase != TickEvent.Phase.START)
		{
			MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);

			if (server != null)
			{
				Backups.INSTANCE.tick(server, System.currentTimeMillis());
			}
		}
	}
}