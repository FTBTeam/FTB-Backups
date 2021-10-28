package com.feed_the_beast.mods.ftbbackups;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class FTBBackupsConfig
{
	public static boolean auto;
	public static boolean silent;
	public static int backupsToKeep;
	public static long backupTimer;
	public static int compressionLevel;
	public static String folder;
	public static boolean displayFileSize;
	public static List<String> extraFiles;
	public static long maxTotalSize;
	public static boolean onlyIfPlayersOnline;
	public static boolean forceOnShutdown;
	public static int bufferSize;

	private static Pair<CommonConfig, ForgeConfigSpec> common;

	public static void register()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(FTBBackupsConfig::reload);

		common = new ForgeConfigSpec.Builder().configure(CommonConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.COMMON, common.getRight());
	}

	public static void reload(ModConfigEvent event)
	{
		ModConfig config = event.getConfig();

		if (config.getSpec() == common.getRight())
		{
			reloadCommon();
		}
	}

	public static void reloadCommon()
	{
		CommonConfig cfg = common.getLeft();
		auto = cfg.auto.get();
		silent = cfg.silent.get();
		backupsToKeep = cfg.backupsToKeep.get();
		backupTimer = cfg.backupTimer.get() * 60000L;
		compressionLevel = cfg.compressionLevel.get();
		folder = cfg.folder.get();
		displayFileSize = cfg.displayFileSize.get();
		extraFiles = new ArrayList<>(cfg.extraFiles.get());

		String mts = cfg.maxTotalSize.get();

		if (mts.endsWith("TB"))
		{
			maxTotalSize = Long.parseLong(mts.substring(0, mts.length() - 2).trim()) * BackupUtils.TB;
		}
		else if (mts.endsWith("GB"))
		{
			maxTotalSize = Long.parseLong(mts.substring(0, mts.length() - 2).trim()) * BackupUtils.GB;
		}
		else if (mts.endsWith("MB"))
		{
			maxTotalSize = Long.parseLong(mts.substring(0, mts.length() - 2).trim()) * BackupUtils.MB;
		}
		else if (mts.endsWith("KB"))
		{
			maxTotalSize = Long.parseLong(mts.substring(0, mts.length() - 2).trim()) * BackupUtils.KB;
		}
		else
		{
			maxTotalSize = Long.parseLong(mts.trim());
		}

		onlyIfPlayersOnline = cfg.onlyIfPlayersOnline.get();
		forceOnShutdown = cfg.forceOnShutdown.get();
		bufferSize = cfg.bufferSize.get();
	}

	private static class CommonConfig
	{
		private final ForgeConfigSpec.BooleanValue auto;
		private final ForgeConfigSpec.BooleanValue silent;
		private final ForgeConfigSpec.IntValue backupsToKeep;
		private final ForgeConfigSpec.IntValue backupTimer;
		private final ForgeConfigSpec.IntValue compressionLevel;
		private final ForgeConfigSpec.ConfigValue<String> folder;
		private final ForgeConfigSpec.BooleanValue displayFileSize;
		private final ForgeConfigSpec.ConfigValue<List<? extends String>> extraFiles;
		private final ForgeConfigSpec.ConfigValue<String> maxTotalSize;
		private final ForgeConfigSpec.BooleanValue onlyIfPlayersOnline;
		private final ForgeConfigSpec.BooleanValue forceOnShutdown;
		private final ForgeConfigSpec.IntValue bufferSize;

		private CommonConfig(ForgeConfigSpec.Builder builder)
		{
			auto = builder
					.comment("Enables backups to run automatically.")
					.translation("ftbbackups.general.auto")
					.define("auto", true);

			silent = builder
					.comment("If set to true, no messages will be displayed in chat/status bar.")
					.translation("ftbbackups.general.silent")
					.define("silent", false);

			backupsToKeep = builder
					.comment(
							"The number of backup files to keep.",
							"More backups = more space used",
							"0 - Infinite"
					)
					.translation("ftbbackups.general.backups_to_keep")
					.defineInRange("backups_to_keep", 12, 0, 32000);

			backupTimer = builder
					.comment(
							"Timer in minutes.",
							"5 - backups every 5 minutes",
							"60 - backups every hour",
							"360 - backups every 6 hours",
							"1440 - backups once every day"
					)
					.translation("ftbbackups.general.backup_timer")
					.defineInRange("backup_timer", 120, 1, 43800);

			compressionLevel = builder
					.comment(
							"0 - Disabled (output = folders)",
							"1 - Best speed",
							"9 - Smallest file size"
					)
					.translation("ftbbackups.general.compression_level")
					.defineInRange("compression_level", 1, 0, 9);

			folder = builder
					.comment("Absolute path to backups folder.")
					.translation("ftbbackups.general.folder")
					.define("folder", "");

			displayFileSize = builder
					.comment("Prints (current size | total size) when backup is done.")
					.translation("ftbbackups.general.display_file_size")
					.define("display_file_size", true);

			extraFiles = builder
					.comment("Add extra files that will be placed in backup _extra_/ folder.")
					.translation("ftbbackups.general.extra_files")
					.defineList("extra_files", new ArrayList<>(), o -> true);

			maxTotalSize = builder
					.comment("Maximum total size that is allowed in backups folder. Older backups will be deleted to free space for newer ones.")
					.translation("ftbbackups.general.max_total_size")
					.define("max_total_size", "50 GB");

			onlyIfPlayersOnline = builder
					.comment("Only create backups when players have been online.")
					.translation("ftbbackups.general.only_if_players_online")
					.define("only_if_players_online", true);

			forceOnShutdown = builder
					.comment("Create a backup when server is stopped.")
					.translation("ftbbackups.general.force_on_shutdown")
					.define("force_on_shutdown", false);

			builder.comment("Advanced features that shouldn't be changed unless you know what you are doing.").push("advanced");

			bufferSize = builder
					.comment("Buffer size for writing files.")
					.translation("ftbbackups.general.buffer_size")
					.defineInRange("buffer_size", 4096, 256, 65536);

			builder.pop();
		}
	}
}