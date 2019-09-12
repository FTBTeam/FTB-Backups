package com.feed_the_beast.mods.ftbbackups;

import com.feed_the_beast.mods.ftbbackups.net.FTBBackupsNetHandler;
import com.feed_the_beast.mods.ftbbackups.net.MessageBackupProgress;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public enum Backups
{
	INSTANCE;

	public final List<Backup> backups = new ArrayList<>();
	public long nextBackup = -1L;
	public int doingBackup = 0;
	public boolean printFiles = false;

	private int currentFile = 0;
	private int totalFiles = 0;
	private String currentFileName = "";
	public boolean hadPlayersOnline = false;

	public void init()
	{
		FTBBackupsConfig.general.clearCachedFolder();
		doingBackup = 0;
		backups.clear();

		File file = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory(), "local/ftbutilities/backups.json");

		if (!file.exists())
		{
			File oldFile = new File(FTBBackupsConfig.general.getFolder(), "backups.json");

			if (oldFile.exists())
			{
				oldFile.renameTo(file);
			}
		}

		JsonElement element = BackupUtils.readJson(file);

		if (element != null && element.isJsonArray())
		{
			try
			{
				for (JsonElement e : element.getAsJsonArray())
				{
					JsonObject json = e.getAsJsonObject();

					if (!json.has("size"))
					{
						json.addProperty("size", BackupUtils.getSize(new File(FTBBackupsConfig.general.getFolder(), json.get("file").getAsString())));
					}

					backups.add(new Backup(json));
				}
			}
			catch (Throwable ex)
			{
				ex.printStackTrace();
			}
		}

		FTBBackups.LOGGER.info("Backups folder - " + FTBBackupsConfig.general.getFolder().getAbsolutePath());
		nextBackup = System.currentTimeMillis() + FTBBackupsConfig.general.time();
	}

	public void tick(MinecraftServer server, long now)
	{
		if (nextBackup > 0L && nextBackup <= now)
		{
			if (!FTBBackupsConfig.general.only_if_players_online || hadPlayersOnline || !server.getPlayerList().getPlayers().isEmpty())
			{
				hadPlayersOnline = false;
				run(server, server, "");
			}
		}

		if (doingBackup > 1)
		{
			doingBackup = 0;

			if (FTBBackupsConfig.general.disable_level_saving)
			{
				for (WorldServer world : server.worlds)
				{
					if (world != null)
					{
						world.disableLevelSaving = false;
					}
				}
			}

			if (!FTBBackupsConfig.general.silent)
			{
				FTBBackupsNetHandler.NET.sendToAll(new MessageBackupProgress(0, 0));
			}
		}
		else if (doingBackup > 0 && printFiles)
		{
			if (currentFile == 0 || currentFile == totalFiles - 1)
			{
				FTBBackups.LOGGER.info("[" + currentFile + " | " + (int) ((currentFile / (double) totalFiles) * 100D) + "%]: " + currentFileName);
			}

			if (!FTBBackupsConfig.general.silent)
			{
				FTBBackupsNetHandler.NET.sendToAll(new MessageBackupProgress(currentFile, totalFiles));
			}
		}
	}

	public void notifyAll(MinecraftServer server, Function<ICommandSender, ITextComponent> function, boolean error)
	{
		for (EntityPlayerMP player : server.getPlayerList().getPlayers())
		{
			ITextComponent component = function.apply(player);
			component.getStyle().setColor(error ? TextFormatting.DARK_RED : TextFormatting.LIGHT_PURPLE);
			player.sendMessage(component);
		}

		ITextComponent component = function.apply(null);
		component.getStyle().setColor(error ? TextFormatting.DARK_RED : TextFormatting.LIGHT_PURPLE);
		server.sendMessage(component);
	}

	public boolean run(MinecraftServer server, ICommandSender sender, String customName)
	{
		if (doingBackup != 0)
		{
			return false;
		}

		boolean auto = !(sender instanceof EntityPlayerMP);

		if (auto && !FTBBackupsConfig.general.enabled)
		{
			return false;
		}

		notifyAll(server, player -> FTBBackups.lang(player, "ftbbackups.lang.start", sender.getName()), false);
		nextBackup = System.currentTimeMillis() + FTBBackupsConfig.general.time();

		if (FTBBackupsConfig.general.disable_level_saving)
		{
			for (WorldServer world : server.worlds)
			{
				if (world != null)
				{
					world.disableLevelSaving = true;
				}
			}
		}

		doingBackup = 1;

		ThreadedFileIOBase.getThreadedIOInstance().queueIO(() ->
		{
			try
			{
				doBackup(server, customName);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			doingBackup = 2;
			return false;
		});

		return true;
	}

	private void doBackup(MinecraftServer server, String customName)
	{
		File src = server.getWorld(0).getSaveHandler().getWorldDirectory();

		try
		{
			if (server.getPlayerList() != null)
			{
				server.getPlayerList().saveAllPlayerData();
			}

			for (WorldServer world : server.worlds)
			{
				if (world != null)
				{
					world.saveAllChunks(true, null);
				}
			}
		}
		catch (Exception ex)
		{
			notifyAll(server, player -> FTBBackups.lang(player, "ftbbackups.lang.saving_failed"), true);
			ex.printStackTrace();
			return;
		}

		Calendar time = Calendar.getInstance();
		File dstFile;
		boolean success = false;
		StringBuilder out = new StringBuilder();

		if (customName.isEmpty())
		{
			appendNum(out, time.get(Calendar.YEAR), '-');
			appendNum(out, time.get(Calendar.MONTH) + 1, '-');
			appendNum(out, time.get(Calendar.DAY_OF_MONTH), '-');
			appendNum(out, time.get(Calendar.HOUR_OF_DAY), '-');
			appendNum(out, time.get(Calendar.MINUTE), '-');
			appendNum(out, time.get(Calendar.SECOND), '\0');
		}
		else
		{
			out.append(customName);
		}

		Exception error = null;
		long fileSize = 0L;

		try
		{
			LinkedHashMap<File, String> fileMap = new LinkedHashMap<>();
			String mcdir = server.getDataDirectory().getCanonicalFile().getAbsolutePath();

			Consumer<File> consumer = file0 -> {
				for (File file : BackupUtils.listTree(file0))
				{
					String s1 = file.getAbsolutePath().replace(mcdir, "");

					if (s1.startsWith(File.separator))
					{
						s1 = s1.substring(File.separator.length());
					}

					fileMap.put(file, "_extra_" + File.separator + s1);
				}
			};

			for (String s : FTBBackupsConfig.general.extra_files)
			{
				consumer.accept(new File(s));
			}

			MinecraftForge.EVENT_BUS.post(new BackupEvent.Pre(consumer));

			for (File file : BackupUtils.listTree(src))
			{
				String filePath = file.getAbsolutePath();
				fileMap.put(file, src.getName() + File.separator + filePath.substring(src.getAbsolutePath().length() + 1));
			}

			for (Map.Entry<File, String> entry : fileMap.entrySet())
			{
				fileSize += BackupUtils.getSize(entry.getKey());
			}

			long totalSize = 0L;

			if (!backups.isEmpty())
			{
				backups.sort(null);

				int backupsToKeep = FTBBackupsConfig.general.backups_to_keep - 1;

				if (backupsToKeep > 0 && backups.size() > backupsToKeep)
				{
					while (backups.size() > backupsToKeep)
					{
						Backup backup = backups.remove(0);
						FTBBackups.LOGGER.info("Deleting old backup: " + backup.fileId);
						BackupUtils.delete(backup.getFile());
					}
				}

				for (Backup backup : backups)
				{
					totalSize += backup.size;
				}

				if (fileSize > 0L)
				{
					long freeSpace = Math.min(FTBBackupsConfig.general.getMaxTotalSize(), FTBBackupsConfig.general.getFolder().getFreeSpace());

					while (totalSize + fileSize > freeSpace && !backups.isEmpty())
					{
						Backup backup = backups.remove(0);
						totalSize -= backup.size;
						FTBBackups.LOGGER.info("Deleting backup to free space: " + backup.fileId);
						BackupUtils.delete(backup.getFile());
					}
				}
			}

			totalFiles = fileMap.size();
			FTBBackups.LOGGER.info("Backing up " + totalFiles + " files...");
			printFiles = true;

			if (FTBBackupsConfig.general.compression_level > 0)
			{
				out.append(".zip");
				dstFile = BackupUtils.newFile(new File(FTBBackupsConfig.general.getFolder(), out.toString()));

				long start = System.currentTimeMillis();

				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile));
				zos.setLevel(FTBBackupsConfig.general.compression_level);

				byte[] buffer = new byte[FTBBackupsConfig.general.buffer_size];

				FTBBackups.LOGGER.info("Compressing " + totalFiles + " files...");

				currentFile = 0;
				for (Map.Entry<File, String> entry : fileMap.entrySet())
				{
					try
					{
						ZipEntry ze = new ZipEntry(entry.getValue());
						currentFileName = entry.getValue();

						zos.putNextEntry(ze);
						FileInputStream fis = new FileInputStream(entry.getKey());

						int len;
						while ((len = fis.read(buffer)) > 0)
						{
							zos.write(buffer, 0, len);
						}
						zos.closeEntry();
						fis.close();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}

					currentFile++;
				}

				zos.close();
				fileSize = BackupUtils.getSize(dstFile);
				FTBBackups.LOGGER.info("Done compressing in " + BackupUtils.getTimeString(System.currentTimeMillis() - start) + " seconds (" + BackupUtils.getSizeString(fileSize) + ")!");
			}
			else
			{
				dstFile = new File(FTBBackupsConfig.general.getFolder(), out.toString());
				dstFile.mkdirs();

				currentFile = 0;
				for (Map.Entry<File, String> entry : fileMap.entrySet())
				{
					try
					{
						File file = entry.getKey();
						currentFileName = entry.getValue();
						File dst1 = new File(dstFile, entry.getValue());
						BackupUtils.copyFile(file, dst1);
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}

					currentFile++;
				}
			}

			FTBBackups.LOGGER.info("Created " + dstFile.getAbsolutePath() + " from " + src.getAbsolutePath());
			success = true;
		}
		catch (Exception ex)
		{
			if (!FTBBackupsConfig.general.silent)
			{
				String errorName = ex.getClass().getName();
				notifyAll(server, player -> FTBBackups.lang(player, "ftbbackups.lang.fail", errorName), true);
			}

			ex.printStackTrace();
			error = ex;
		}

		printFiles = false;

		Backup backup = new Backup(time.getTimeInMillis(), out.toString().replace('\\', '/'), getLastIndex() + 1, success, fileSize);
		backups.add(backup);
		MinecraftForge.EVENT_BUS.post(new BackupEvent.Post(backup, error));

		JsonArray array = new JsonArray();

		for (Backup backup1 : backups)
		{
			array.add(backup1.toJsonObject());
		}

		BackupUtils.toJson(new File(server.getDataDirectory(), "local/ftbutilities/backups.json"), array, true);

		if (error == null && !FTBBackupsConfig.general.silent)
		{
			String timeString = BackupUtils.getTimeString(System.currentTimeMillis() - time.getTimeInMillis());

			if (FTBBackupsConfig.general.display_file_size)
			{
				long totalSize = 0L;

				for (Backup backup1 : backups)
				{
					totalSize += backup1.size;
				}

				String sizeB = BackupUtils.getSizeString(fileSize);
				String sizeT = BackupUtils.getSizeString(totalSize);
				String sizeString = sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT);
				notifyAll(server, player -> FTBBackups.lang(player, "ftbbackups.lang.end_2", timeString, sizeString), false);
			}
			else
			{
				notifyAll(server, player -> FTBBackups.lang(player, "ftbbackups.lang.end_1", timeString), false);
			}
		}
	}

	private void appendNum(StringBuilder sb, int num, char c)
	{
		if (num < 10)
		{
			sb.append('0');
		}

		sb.append(num);

		if (c != '\0')
		{
			sb.append(c);
		}
	}

	private int getLastIndex()
	{
		int i = 0;

		for (Backup b : backups)
		{
			i = Math.max(i, b.index);
		}

		return i;
	}
}