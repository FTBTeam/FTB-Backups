package com.feed_the_beast.mods.ftbbackups.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraftforge.server.command.CommandTreeHelp;

/**
 * @author LatvianModder
 */
public class CommandBackup extends CommandTreeBase
{
	public CommandBackup()
	{
		addSubcommand(new CommandStart());
		addSubcommand(new CommandSize());
		addSubcommand(new CommandTime());
		addSubcommand(new CommandTreeHelp(this));
	}

	@Override
	public String getName()
	{
		return "backup";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "command.backup.usage";
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
}