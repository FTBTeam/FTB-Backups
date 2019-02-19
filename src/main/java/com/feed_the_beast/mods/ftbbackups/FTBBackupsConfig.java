package com.feed_the_beast.mods.ftbbackups;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBBackups.MOD_ID)
@Config(modid = FTBBackups.MOD_ID, category = "", name = "ftbutilities/backups")
public class FTBBackupsConfig
{
	public static final General general = new General();

	public static class General
	{
		@Config.LangKey("addServer.resourcePack.enabled")
		@Config.Comment("Enables backups.")
		public boolean enabled = true;

		@Config.Comment("If set to true, no messages will be displayed in chat/status bar.")
		public boolean silent = false;

		@Config.RangeInt(min = 0, max = 32000)
		@Config.Comment({
				"The number of backup files to keep.",
				"More backups = more space used",
				"0 - Infinite"
		})
		public int backups_to_keep = 12;

		@Config.RangeDouble(min = 0.05D, max = 600D)
		@Config.Comment({
				"Timer in hours.",
				"1.0 - backups every hour",
				"6.0 - backups every 6 hours",
				"0.5 - backups every 30 minutes"
		})
		public double backup_timer = 2D;

		@Config.RangeInt(min = 0, max = 9)
		@Config.Comment({
				"0 - Disabled (output = folders)",
				"1 - Best speed",
				"9 - Smallest file size"
		})
		public int compression_level = 1;

		@Config.Comment("Absolute path to backups folder.")
		public String folder = "";

		@Config.Comment("Prints (current size | total size) when backup is done.")
		public boolean display_file_size = true;

		@Config.Comment("Add extra files that will be placed in backup _extra_/ folder.")
		public String[] extra_files = { };

		@Config.Comment("Maximum total size that is allowed in backups folder. Older backups will be deleted to free space for newer ones.")
		public String max_total_size = "50 GB";

		@Config.Comment("Disables level saving while performing backup.")
		public boolean disable_level_saving = true;

		@Config.Comment("Only create backups when players have been online.")
		public boolean only_if_players_online = true;

		@Config.Comment("Create a backup when server is stopped.")
		public boolean force_on_shutdown = false;

		@Config.Comment("Buffer size for writing files Don't change unless you know what you are doing.")
		@Config.RangeInt(min = 256, max = 65536)
		public int buffer_size = 4096;

		public long time()
		{
			return (long) (backup_timer * 3600000L);
		}

		public long getMaxTotalSize()
		{
			String s = BackupUtils.removeAllWhitespace(max_total_size).toUpperCase();

			if (s.endsWith("GB"))
			{
				return Long.parseLong(s.substring(0, s.length() - 2)) * BackupUtils.GB;
			}
			else if (s.endsWith("MB"))
			{
				return Long.parseLong(s.substring(0, s.length() - 2)) * BackupUtils.MB;
			}
			else if (s.endsWith("KB"))
			{
				return Long.parseLong(s.substring(0, s.length() - 2)) * BackupUtils.KB;
			}

			return Long.parseLong(s);
		}
	}

	public static boolean sync()
	{
		ConfigManager.sync(FTBBackups.MOD_ID, Config.Type.INSTANCE);
		return true;
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (event.getModID().equals(FTBBackups.MOD_ID))
		{
			sync();
		}
	}
}