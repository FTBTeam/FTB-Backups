package com.feed_the_beast.mods.ftbbackups;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * @author LatvianModder
 */
public class BackupCommands
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("ftbbackups")
				.then(Commands.literal("time")
						.executes(ctx -> time(ctx.getSource()))
				)
				.then(Commands.literal("start")
						.requires(cs -> cs.getServer().isSingleplayer() || cs.hasPermission(3))
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
						)
						.executes(ctx -> start(ctx.getSource(), ""))
				)
				.then(Commands.literal("size")
						.requires(cs -> cs.getServer().isSingleplayer() || cs.hasPermission(3))
						.executes(ctx -> size(ctx.getSource()))
				)
		);
	}

	private static int time(CommandSourceStack source)
	{
		source.sendSuccess(new TranslatableComponent("ftbbackups.lang.timer", BackupUtils.getTimeString(Backups.INSTANCE.nextBackup - System.currentTimeMillis())), true);
		return 1;
	}

	private static int start(CommandSourceStack source, String customName)
	{
		if (Backups.INSTANCE.run(source.getServer(), false, source.getDisplayName(), customName))
		{
			for (ServerPlayer player : source.getServer().getPlayerList().getPlayers())
			{
				player.sendMessage(new TranslatableComponent("ftbbackups.lang.manual_launch", source.getDisplayName()), Util.NIL_UUID);
			}
		}
		else
		{
			source.sendSuccess(new TranslatableComponent("ftbbackups.lang.already_running"), true);
		}

		return 1;
	}

	private static int size(CommandSourceStack source)
	{
		long totalSize = 0L;

		for (Backup backup : Backups.INSTANCE.backups)
		{
			totalSize += backup.size;
		}

		source.sendSuccess(new TranslatableComponent("ftbbackups.lang.size.current", BackupUtils.getSizeString(source.getServer().getWorldPath(LevelResource.ROOT).toFile())), true);
		source.sendSuccess(new TranslatableComponent("ftbbackups.lang.size.total", BackupUtils.getSizeString(totalSize)), true);
		source.sendSuccess(new TranslatableComponent("ftbbackups.lang.size.available", BackupUtils.getSizeString(Math.min(FTBBackupsConfig.maxTotalSize, Backups.INSTANCE.backupsFolder.getFreeSpace()))), true);

		return 1;
	}
}