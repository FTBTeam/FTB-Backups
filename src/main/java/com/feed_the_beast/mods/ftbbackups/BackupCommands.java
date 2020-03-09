package com.feed_the_beast.mods.ftbbackups;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * @author LatvianModder
 */
public class BackupCommands
{
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		LiteralCommandNode<CommandSource> cmd = dispatcher.register(Commands.literal("ftbackups")
				.then(Commands.literal("time")
						.executes(ctx -> time(ctx.getSource()))
				)
				.then(Commands.literal("start")
						.requires(cs -> cs.getServer().isSinglePlayer() || cs.hasPermissionLevel(3))
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
						)
						.executes(ctx -> start(ctx.getSource(), ""))
				)
				.then(Commands.literal("size")
						.requires(cs -> cs.getServer().isSinglePlayer() || cs.hasPermissionLevel(3))
						.executes(ctx -> size(ctx.getSource()))
				)
		);

		dispatcher.register(Commands.literal("backup").redirect(cmd));
	}

	private static int time(CommandSource source)
	{
		source.sendFeedback(new TranslationTextComponent("ftbbackups.lang.timer", BackupUtils.getTimeString(Backups.INSTANCE.nextBackup - System.currentTimeMillis())), true);
		return 1;
	}

	private static int start(CommandSource source, String customName)
	{
		if (Backups.INSTANCE.run(source.getServer(), false, source.getDisplayName(), customName))
		{
			for (ServerPlayerEntity player : source.getServer().getPlayerList().getPlayers())
			{
				player.sendMessage(new TranslationTextComponent("ftbbackups.lang.manual_launch", source.getDisplayName()));
			}
		}
		else
		{
			source.sendFeedback(new TranslationTextComponent("ftbbackups.lang.already_running"), true);
		}

		return 1;
	}

	private static int size(CommandSource source)
	{
		long totalSize = 0L;

		for (Backup backup : Backups.INSTANCE.backups)
		{
			totalSize += backup.size;
		}

		source.sendFeedback(new TranslationTextComponent("ftbbackups.lang.size.current", BackupUtils.getSizeString(source.getWorld().getSaveHandler().getWorldDirectory())), true);
		source.sendFeedback(new TranslationTextComponent("ftbbackups.lang.size.total", BackupUtils.getSizeString(totalSize)), true);
		source.sendFeedback(new TranslationTextComponent("ftbbackups.lang.size.available", BackupUtils.getSizeString(Math.min(FTBBackupsConfig.maxTotalSize, Backups.INSTANCE.backupsFolder.getFreeSpace()))), true);

		return 1;
	}
}