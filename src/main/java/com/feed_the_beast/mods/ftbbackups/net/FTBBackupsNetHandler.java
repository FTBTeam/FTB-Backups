package com.feed_the_beast.mods.ftbbackups.net;

import com.feed_the_beast.mods.ftbbackups.FTBBackups;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class FTBBackupsNetHandler
{
	public static SimpleChannel main;
	private static final String MAIN_VERSION = "1";

	public static void init()
	{
		main = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(FTBBackups.MOD_ID, "main"))
				.clientAcceptedVersions(MAIN_VERSION::equals)
				.serverAcceptedVersions(MAIN_VERSION::equals)
				.networkProtocolVersion(() -> MAIN_VERSION)
				.simpleChannel();

		main.registerMessage(1, BackupProgressMessage.class, BackupProgressMessage::write, BackupProgressMessage::new, BackupProgressMessage::handle);
	}
}