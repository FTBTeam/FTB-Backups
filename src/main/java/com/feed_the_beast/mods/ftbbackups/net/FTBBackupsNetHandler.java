package com.feed_the_beast.mods.ftbbackups.net;

import com.feed_the_beast.mods.ftbbackups.FTBBackups;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Predicate;

public class FTBBackupsNetHandler
{
	public static SimpleChannel main;
	private static final String MAIN_VERSION = "1";

	public static void init()
	{
		Predicate<String> validator = v -> MAIN_VERSION.equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v);

		main = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(FTBBackups.MOD_ID, "main"))
				.clientAcceptedVersions(validator)
				.serverAcceptedVersions(validator)
				.networkProtocolVersion(() -> MAIN_VERSION)
				.simpleChannel();

		main.registerMessage(1, BackupProgressMessage.class, BackupProgressMessage::write, BackupProgressMessage::new, BackupProgressMessage::handle);
	}
}