package com.feed_the_beast.mods.ftbbackups.command;

import com.feed_the_beast.mods.ftbbackups.Backups;
import com.feed_the_beast.mods.ftbbackups.FTBBackups;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/**
 * @author LatvianModder
 */
public class CommandStart extends CommandBase
{
	@Override
	public String getName()
	{
		return "start";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "commands.backup.start.usage";
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
		if (Backups.INSTANCE.run(server, sender, args.length == 0 ? "" : args[0]))
		{
			for (EntityPlayerMP player : server.getPlayerList().getPlayers())
			{
				player.sendMessage(FTBBackups.lang(player, "ftbbackups.lang.manual_launch", sender.getName()));
			}
		}
		else
		{
			sender.sendMessage(FTBBackups.lang(sender, "ftbbackups.lang.already_running"));
		}
	}
}