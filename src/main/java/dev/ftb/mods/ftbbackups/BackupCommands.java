package dev.ftb.mods.ftbbackups;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
				.then(Commands.literal("status")
						.requires(cs -> cs.getServer().isSingleplayer() || cs.hasPermission(3))
						.executes(ctx -> status(ctx.getSource()))
				)
				.then(Commands.literal("reset")
						.requires(cs -> cs.getServer().isSingleplayer() || cs.hasPermission(3))
						.executes(ctx -> resetState(ctx.getSource()))
				)
		);
	}

	private static int time(CommandSourceStack source)
	{
		source.sendSuccess(() -> Component.translatable("ftbbackups.lang.timer", BackupUtils.getTimeString(Backups.INSTANCE.nextBackup - System.currentTimeMillis())),true);
		return 1;
	}

	private static int start(CommandSourceStack source, String customName)
	{
		if (Backups.INSTANCE.run(source.getServer(), false, source.getDisplayName(), customName))
		{
			for (ServerPlayer player : source.getServer().getPlayerList().getPlayers())
			{
				player.sendSystemMessage(Component.translatable("ftbbackups.lang.manual_launch", source.getDisplayName()));
			}
		}
		else
		{
			source.sendSuccess(() -> Component.translatable("ftbbackups.lang.already_running"), true);
		}

		return 1;
	}

	private static int size(CommandSourceStack source)
	{
		long totalSize = Backups.INSTANCE.backups.stream().mapToLong(backup -> backup.size).sum();

        source.sendSuccess(() -> Component.translatable("ftbbackups.lang.size.current", BackupUtils.getSizeString(source.getServer().getWorldPath(LevelResource.ROOT).toFile())), true);
		source.sendSuccess(() -> Component.translatable("ftbbackups.lang.size.total", BackupUtils.getSizeString(totalSize)), true);
		source.sendSuccess(() -> Component.translatable("ftbbackups.lang.size.available", BackupUtils.getSizeString(Math.min(FTBBackupsConfig.maxTotalSize, Backups.INSTANCE.backupsFolder.getFreeSpace()))), true);

		return 1;
	}

	private static int status(CommandSourceStack source)
	{
		source.getServer().getAllLevels().forEach(level -> {
			if (level != null) {
				//[Dev: Current status for ServerLevel[world]: NO SAVE !!] x3
				//level.dimension().toString() - ResourceKey[minecraft:dimension / minecraft:overworld]
				//level.toSting() - ServerLevel[New World]
				source.sendSuccess(() -> Component.literal("Current status for " + level.toString() + ": " + (!level.noSave ? "NO SAVE !!" : "Autosave")), true);
			}
		});

		return 1;
	}

	private static int resetState(CommandSourceStack source)
	{
		source.getServer().getAllLevels().forEach(level -> {
			if (level != null) {
				source.sendSuccess(() -> Component.literal("Reseting state + saving for " + level.toString()), true);
				level.noSave = false;
				level.save(null, true, true);
			}
		});

		source.sendSuccess(() -> Component.literal("Reseting state + saving for players"), true);
		source.getServer().getPlayerList().saveAll();

		return 1;
	}
}