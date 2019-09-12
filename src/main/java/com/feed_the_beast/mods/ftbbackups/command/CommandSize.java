package com.feed_the_beast.mods.ftbbackups.command;

import com.feed_the_beast.mods.ftbbackups.Backup;
import com.feed_the_beast.mods.ftbbackups.BackupUtils;
import com.feed_the_beast.mods.ftbbackups.Backups;
import com.feed_the_beast.mods.ftbbackups.FTBBackups;
import com.feed_the_beast.mods.ftbbackups.FTBBackupsConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/**
 * @author LatvianModder
 */
public class CommandSize extends CommandBase
{
	@Override
	public String getName()
	{
		return "size";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "commands.backup.size.usage";
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 2;
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return server.isSinglePlayer() || super.checkPermission(server, sender);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args)
	{
		long totalSize = 0L;

		for (Backup backup : Backups.INSTANCE.backups)
		{
			totalSize += backup.size;
		}

		sender.sendMessage(FTBBackups.lang(sender, "ftbbackups.lang.size.1", BackupUtils.getSizeString(server.getWorld(0).getSaveHandler().getWorldDirectory())));
		sender.sendMessage(FTBBackups.lang(sender, "ftbbackups.lang.size.2", BackupUtils.getSizeString(totalSize)));
		sender.sendMessage(FTBBackups.lang(sender, "ftbbackups.lang.size.3", BackupUtils.getSizeString(Math.min(FTBBackupsConfig.general.getMaxTotalSize(), FTBBackupsConfig.general.getFolder().getFreeSpace()))));
	}
}