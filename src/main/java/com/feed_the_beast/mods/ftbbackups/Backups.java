package com.feed_the_beast.mods.ftbbackups;

import com.feed_the_beast.mods.ftbbackups.net.BackupProgressPacket;
import com.feed_the_beast.mods.ftbbackups.net.FTBBackupsNetHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.PacketDistributor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public enum Backups
{
	INSTANCE;

	public final List<Backup> backups = new ArrayList<>();
	public File backupsFolder;
	public long nextBackup = -1L;
	public BackupStatus doingBackup = BackupStatus.NONE;
	public boolean printFiles = false;

	public int currentFile = 0;
	public int totalFiles = 0;
	private String currentFileName = "";
	public boolean hadPlayersOnline = false;

	public void init(MinecraftServer server)
	{
		File dataDir = server.getDataDirectory();
		backupsFolder = FTBBackupsConfig.folder.trim().isEmpty() ? FMLPaths.GAMEDIR.get().resolve("backups").toFile() : new File(FTBBackupsConfig.folder);

		try
		{
			backupsFolder = backupsFolder.getCanonicalFile();
		}
		catch (Exception ex)
		{
		}

		doingBackup = BackupStatus.NONE;
		backups.clear();

		File file = new File(dataDir, "local/ftbutilities/backups.json");

		if (!file.exists())
		{
			File oldFile = new File(backupsFolder, "backups.json");

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
						json.addProperty("size", BackupUtils.getSize(new File(backupsFolder, json.get("file").getAsString())));
					}

					backups.add(new Backup(json));
				}
			}
			catch (Throwable ex)
			{
				ex.printStackTrace();
			}
		}

		FTBBackups.LOGGER.info("Backups folder - " + backupsFolder.getAbsolutePath());
		nextBackup = System.currentTimeMillis() + FTBBackupsConfig.backupTimer;
	}

	public void tick(MinecraftServer server, long now)
	{
		if (nextBackup > 0L && nextBackup <= now)
		{
			if (!FTBBackupsConfig.onlyIfPlayersOnline || hadPlayersOnline || !server.getPlayerList().getPlayers().isEmpty())
			{
				hadPlayersOnline = false;
				run(server, true, new StringTextComponent("Server"), "");
			}
		}

		if (doingBackup.isDone())
		{
			doingBackup = BackupStatus.NONE;

			for (ServerWorld world : server.getWorlds())
			{
				if (world != null)
				{
					world.disableLevelSaving = false;
				}
			}

			if (!FTBBackupsConfig.silent)
			{
				FTBBackupsNetHandler.MAIN.send(PacketDistributor.ALL.noArg(), new BackupProgressPacket(0, 0));
			}
		}
		else if (doingBackup.isRunning() && printFiles)
		{
			if (currentFile == 0 || currentFile == totalFiles - 1)
			{
				FTBBackups.LOGGER.info("[" + currentFile + " | " + (int) ((currentFile / (double) totalFiles) * 100D) + "%]: " + currentFileName);
			}

			if (!FTBBackupsConfig.silent)
			{
				FTBBackupsNetHandler.MAIN.send(PacketDistributor.ALL.noArg(), new BackupProgressPacket(currentFile, totalFiles));
			}
		}
	}

	public void notifyAll(MinecraftServer server, ITextComponent component, boolean error)
	{
		component = component.deepCopy();
		component.getStyle().setColor(Color.func_240744_a_(error ? TextFormatting.DARK_RED : TextFormatting.LIGHT_PURPLE));
		FTBBackups.LOGGER.info(component.getString());
		server.getPlayerList().func_232641_a_(component, ChatType.GAME_INFO, Util.DUMMY_UUID);
	}

	public boolean run(MinecraftServer server, boolean auto, ITextComponent name, String customName)
	{
		if (doingBackup.isRunningOrDone())
		{
			return false;
		}

		if (auto && !FTBBackupsConfig.auto)
		{
			return false;
		}

		notifyAll(server, new TranslationTextComponent("ftbbackups.lang.start", name), false);
		nextBackup = System.currentTimeMillis() + FTBBackupsConfig.backupTimer;

		for (ServerWorld world : server.getWorlds())
		{
			if (world != null)
			{
				world.disableLevelSaving = true;
			}
		}

		doingBackup = BackupStatus.RUNNING;
		server.getPlayerList().saveAllPlayerData();

		new Thread(() -> {
			try
			{
				try
				{
					createBackup(server, customName);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					notifyAll(server, new TranslationTextComponent("ftbbackups.lang.saving_failed"), true);
				}
			}
			catch (Exception ex)
			{
				notifyAll(server, new TranslationTextComponent("ftbbackups.lang.saving_failed"), true);
				ex.printStackTrace();
			}

			doingBackup = BackupStatus.DONE;
		}).start();

		return true;
	}

	private void createBackup(MinecraftServer server, String customName)
	{
		File src = server.func_240776_a_(FolderName.field_237253_i_).toFile();

		try
		{
			src = src.getCanonicalFile();
		}
		catch (Exception ex)
		{
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

			for (String s : FTBBackupsConfig.extraFiles)
			{
				consumer.accept(new File(s));
			}

			MinecraftForge.EVENT_BUS.post(new BackupEvent.Pre(consumer));

			for (File file : BackupUtils.listTree(src))
			{
				if (file.getName().equals("session.lock") || !file.canRead())
				{
					continue;
				}

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

				int backupsToKeep = FTBBackupsConfig.backupsToKeep - 1;

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
					long freeSpace = Math.min(FTBBackupsConfig.maxTotalSize, backupsFolder.getFreeSpace());

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

			if (FTBBackupsConfig.compressionLevel > 0)
			{
				out.append(".zip");
				dstFile = BackupUtils.newFile(new File(backupsFolder, out.toString()));

				long start = System.currentTimeMillis();

				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile));
				zos.setLevel(FTBBackupsConfig.compressionLevel);

				byte[] buffer = new byte[FTBBackupsConfig.bufferSize];

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
						FTBBackups.LOGGER.error("Failed to read file " + entry.getValue() + ": " + ex);
					}

					currentFile++;
				}

				zos.close();
				fileSize = BackupUtils.getSize(dstFile);
				FTBBackups.LOGGER.info("Done compressing in " + BackupUtils.getTimeString(System.currentTimeMillis() - start) + " seconds (" + BackupUtils.getSizeString(fileSize) + ")!");
			}
			else
			{
				dstFile = new File(backupsFolder, out.toString());
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
						FTBBackups.LOGGER.error("Failed to copy file " + entry.getValue() + ": " + ex);
					}

					currentFile++;
				}
			}

			FTBBackups.LOGGER.info("Created " + dstFile.getAbsolutePath() + " from " + src.getAbsolutePath());
			success = true;
		}
		catch (Exception ex)
		{
			if (!FTBBackupsConfig.silent)
			{
				String errorName = ex.getClass().getName();
				notifyAll(server, new TranslationTextComponent("ftbbackups.lang.fail", errorName), true);
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

		if (error == null && !FTBBackupsConfig.silent)
		{
			String timeString = BackupUtils.getTimeString(System.currentTimeMillis() - time.getTimeInMillis());
			ITextComponent component;

			if (FTBBackupsConfig.displayFileSize)
			{
				long totalSize = 0L;

				for (Backup backup1 : backups)
				{
					totalSize += backup1.size;
				}

				String sizeB = BackupUtils.getSizeString(fileSize);
				String sizeT = BackupUtils.getSizeString(totalSize);
				String sizeString = sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT);
				component = new TranslationTextComponent("ftbbackups.lang.end_2", timeString, sizeString);
			}
			else
			{
				component = new TranslationTextComponent("ftbbackups.lang.end_1", timeString);
			}

			component.getStyle().setColor(Color.func_240744_a_(TextFormatting.LIGHT_PURPLE));
			server.getPlayerList().func_232641_a_(component, ChatType.CHAT, Util.DUMMY_UUID);
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