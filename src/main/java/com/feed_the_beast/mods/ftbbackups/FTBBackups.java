package com.feed_the_beast.mods.ftbbackups;

import com.feed_the_beast.mods.ftbbackups.net.BackupProgressMessage;
import com.feed_the_beast.mods.ftbbackups.net.FTBBackupsNetHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FTBBackups.MOD_ID)
public class FTBBackups
{
	public static final String MOD_ID = "ftbbackups";
	public static final Logger LOGGER = LogManager.getLogger("FTB Utilities Backups");

	public FTBBackups()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadCommon);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadClient);
		FTBBackupsConfig.register();
	}

	private void loadCommon(FMLCommonSetupEvent event)
	{
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
		MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
		MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
		MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
		FTBBackupsNetHandler.init();
	}

	private void loadClient(FMLClientSetupEvent event)
	{
		FTBBackupsClient.init(event);
	}

	private void onServerStarting(FMLServerStartingEvent event)
	{
		BackupCommands.register(event.getCommandDispatcher());
	}

	private void onServerStarted(FMLServerStartedEvent event)
	{
		Backups.INSTANCE.init(event.getServer());
	}

	private void onServerStopping(FMLServerStoppingEvent event)
	{
		if (FTBBackupsConfig.forceOnShutdown)
		{
			Backups.INSTANCE.run(event.getServer(), true, new StringTextComponent("Server"), "");
		}
	}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (event.getPlayer() instanceof ServerPlayerEntity)
		{
			FTBBackupsNetHandler.main.sendTo(new BackupProgressMessage(Backups.INSTANCE.currentFile, Backups.INSTANCE.totalFiles), ((ServerPlayerEntity) event.getPlayer()).connection.getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
		}
	}

	private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		Backups.INSTANCE.hadPlayersOnline = true;
	}

	private void onServerTick(TickEvent.ServerTickEvent event)
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