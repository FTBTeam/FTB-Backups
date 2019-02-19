package com.feed_the_beast.mods.ftbbackups;

import com.feed_the_beast.mods.ftbbackups.command.CommandBackup;
import com.feed_the_beast.mods.ftbbackups.net.FTBBackupsNetHandler;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

@Mod(
		modid = FTBBackups.MOD_ID,
		name = FTBBackups.MOD_NAME,
		version = FTBBackups.VERSION,
		acceptableRemoteVersions = "*"
)
@Mod.EventBusSubscriber
public class FTBBackups
{
	public static final String MOD_ID = "ftbbackups";
	public static final String MOD_NAME = "FTB Utilities Backups";
	public static final String VERSION = "0.0.0.ftbbackups";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

	public static ITextComponent lang(@Nullable ICommandSender sender, String key, Object... args)
	{
		//return SidedUtils.lang(sender, MOD_ID, key, args);
		return new TextComponentTranslation(key, args); //FIXME
	}

	@Mod.EventHandler
	public void onPreInit(FMLPreInitializationEvent event)
	{
		FTBBackupsNetHandler.init();
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandBackup());
	}

	@Mod.EventHandler
	public void onServerStarted(FMLServerStartedEvent event)
	{
		Backups.INSTANCE.init();
	}

	@Mod.EventHandler
	public void onServerStopping(FMLServerStoppingEvent event)
	{
		if (FTBBackupsConfig.general.force_on_shutdown)
		{
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

			if (server != null)
			{
				Backups.INSTANCE.run(server, server, "");
			}
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event)
	{
		if (event.phase != TickEvent.Phase.START)
		{
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

			if (server != null)
			{
				Backups.INSTANCE.tick(server, System.currentTimeMillis());
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		Backups.INSTANCE.hadPlayersOnline = true;
	}
}