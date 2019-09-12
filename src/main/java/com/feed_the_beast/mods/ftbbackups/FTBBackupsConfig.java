package com.feed_the_beast.mods.ftbbackups;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBBackups.MOD_ID)
@Config(modid = FTBBackups.MOD_ID, category = "")
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

		@Config.Comment({
				"Maximum total size that is allowed in backups folder. Older backups will be deleted to free space for newer ones.",
				"You can use TB, GB, MB and KB for filesizes.",
				"You can use % to set maximum total size based on your available disk space. It is still limited by max total backup count, so it's not gonna fill up large drives.",
				"Valid inputs: 50 GB, 10 MB, 33%"
		})
		public String max_total_size = "75%";

		@Config.Comment("Disables level saving while performing backup.")
		public boolean disable_level_saving = true;

		@Config.Comment("Only create backups when players have been online.")
		public boolean only_if_players_online = true;

		@Config.Comment("Create a backup when server is stopped.")
		public boolean force_on_shutdown = false;

		@Config.Comment("Buffer size for writing files Don't change unless you know what you are doing.")
		@Config.RangeInt(min = 256, max = 65536)
		public int buffer_size = 4096;

		private long cachedMaxTotalSize = -1L;
		private File cachedFolder;

		public long time()
		{
			return (long) (backup_timer * 3600000L);
		}

		public long getMaxTotalSize()
		{
			if (cachedMaxTotalSize == -1L)
			{
				cachedMaxTotalSize = getMaxTotalSize0();
			}

			return cachedMaxTotalSize;
		}

		private long getMaxTotalSize0()
		{
			String s = BackupUtils.removeAllWhitespace(max_total_size).toUpperCase();

			if (s.endsWith("%"))
			{
				return (long) (Double.parseDouble(s.substring(0, s.length() - 1).trim()) * 0.01D * getFolder().getTotalSpace());
			}
			else if (s.endsWith("TB"))
			{
				return Long.parseLong(s.substring(0, s.length() - 2).trim()) * BackupUtils.TB;
			}
			else if (s.endsWith("GB"))
			{
				return Long.parseLong(s.substring(0, s.length() - 2).trim()) * BackupUtils.GB;
			}
			else if (s.endsWith("MB"))
			{
				return Long.parseLong(s.substring(0, s.length() - 2).trim()) * BackupUtils.MB;
			}
			else if (s.endsWith("KB"))
			{
				return Long.parseLong(s.substring(0, s.length() - 2).trim()) * BackupUtils.KB;
			}

			return Long.parseLong(s);
		}

		public File getFolder()
		{
			if (cachedFolder == null)
			{
				cachedFolder = FTBBackupsConfig.general.folder.trim().isEmpty() ? new File(FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory(), "backups") : new File(FTBBackupsConfig.general.folder.trim());
			}

			return cachedFolder;
		}

		public void clearCachedFolder()
		{
			cachedFolder = null;
		}
	}

	public static boolean sync()
	{
		ConfigManager.sync(FTBBackups.MOD_ID, Config.Type.INSTANCE);
		general.cachedMaxTotalSize = -1L;
		general.cachedFolder = null;
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