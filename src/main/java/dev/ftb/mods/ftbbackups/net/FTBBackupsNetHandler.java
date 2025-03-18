package dev.ftb.mods.ftbbackups.net;

import dev.ftb.mods.ftbbackups.FTBBackups;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Predicate;

public class FTBBackupsNetHandler {

    //TODO NetHandler
	/*public static SimpleChannel MAIN;
	private static final String MAIN_VERSION = "1";

	public static void init()
	{
		Predicate<String> validator = v -> MAIN_VERSION.equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v);

		MAIN = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(FTBBackups.MOD_ID, "main"))
				.clientAcceptedVersions(validator)
				.serverAcceptedVersions(validator)
				.networkProtocolVersion(() -> MAIN_VERSION)
				.simpleChannel();

		MAIN.registerMessage(1, BackupProgressPacket.class, BackupProgressPacket::write, BackupProgressPacket::new, BackupProgressPacket::handle);
	}*/
}
