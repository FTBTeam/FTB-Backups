package com.feed_the_beast.mods.ftbbackups;

import com.feed_the_beast.mods.ftbbackups.net.BackupProgressPacket;
import com.feed_the_beast.mods.ftbbackups.net.FTBBackupsNetHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FTBBackups.MOD_ID)
public class FTBBackups
{
	public static final String MOD_ID = "ftbbackups";
	public static final Logger LOGGER = LogManager.getLogger("FTB Utilities Backups");

	public FTBBackups()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
		FTBBackupsConfig.register();
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}

	private void setup(FMLCommonSetupEvent event)
	{
		MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
		MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
		MinecraftForge.EVENT_BUS.addListener(this::playerLoggedIn);
		MinecraftForge.EVENT_BUS.addListener(this::playerLoggedOut);
		MinecraftForge.EVENT_BUS.addListener(this::serverTick);
		FTBBackupsNetHandler.init();
	}

	private void clientSetup(FMLClientSetupEvent event)
	{
		FTBBackupsClient.init();
	}

	private void serverAboutToStart(FMLServerStartedEvent event)
	{
		Backups.INSTANCE.init(event.getServer());
	}

	private void serverStarting(FMLServerStartingEvent event)
	{
		BackupCommands.register(event.getCommandDispatcher());
	}

	private void serverStopping(FMLServerStoppingEvent event)
	{
		if (FTBBackupsConfig.forceOnShutdown)
		{
			Backups.INSTANCE.run(event.getServer(), true, new StringTextComponent("Server"), "");
		}
	}

	private void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (event.getPlayer() instanceof ServerPlayerEntity)
		{
			FTBBackupsNetHandler.MAIN.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()), new BackupProgressPacket(Backups.INSTANCE.currentFile, Backups.INSTANCE.totalFiles));
		}
	}

	private void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		if (event.getPlayer() instanceof ServerPlayerEntity)
		{
			Backups.INSTANCE.hadPlayersOnline = true;
		}
	}

	private void serverTick(TickEvent.ServerTickEvent event)
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