package com.feed_the_beast.mods.ftbbackups.command;

import com.feed_the_beast.mods.ftbbackups.BackupUtils;
import com.feed_the_beast.mods.ftbbackups.Backups;
import com.feed_the_beast.mods.ftbbackups.FTBBackups;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/**
 * @author LatvianModder
 */
public class CommandTime extends CommandBase
{
	@Override
	public String getName()
	{
		return "time";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "commands.backup.time.usage";
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 0;
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender)
	{
		return true;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args)
	{
		sender.sendMessage(FTBBackups.lang(sender, "ftbbackups.lang.timer", BackupUtils.getTimeString(Backups.INSTANCE.nextBackup - System.currentTimeMillis())));
	}
}