package dev.ftb.mods.ftbbackups;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbbackups.config.FTBBackupsClientConfig;
import dev.ftb.mods.ftbbackups.config.FTBBackupsServerConfig;
import dev.ftb.mods.ftblibrary.net.EditConfigChoicePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.time.Duration;
import java.time.Instant;

public class BackupCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> backupCommands = Commands.literal(FTBBackups.MOD_ID)
                .then(Commands.literal("time")
                        .executes(ctx -> time(ctx.getSource()))
                )
                .then(Commands.literal("start")
                        .requires(BackupCommands::isAdmin)
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                        .executes(ctx -> start(ctx.getSource(), ""))
                )
                .then(Commands.literal("size")
                        .requires(BackupCommands::isAdmin)
                        .executes(ctx -> size(ctx.getSource()))
                )
                .then(Commands.literal("status")
                        .requires(BackupCommands::isAdmin)
                        .executes(ctx -> status(ctx.getSource()))
                )
                .then(Commands.literal("reset")
                        .requires(BackupCommands::isAdmin)
                        .executes(ctx -> resetState(ctx.getSource()))
                )
                .then(Commands.literal("serverconfig")
                        .requires(BackupCommands::isAdmin)
                        .executes(ctx -> editServerConfig(ctx.getSource()))
                )
                .then(Commands.literal("clientconfig")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(ctx -> editClientConfig(ctx.getSource()))
                )
                .then(Commands.literal("config")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(ctx -> editConfig(ctx.getSource()))
                );

        dispatcher.register(backupCommands);

        if (FTBBackupsServerConfig.ADD_BACKUP_COMMAND_ALIAS.get()) {
            dispatcher.register(Commands.literal("backup")
                    .redirect(backupCommands.build()));

        }
    }

    private static boolean isAdmin(CommandSourceStack cs) {
        return cs.getServer().isSingleplayer() || cs.hasPermission(3);
    }

    private static int editServerConfig(CommandSourceStack source) throws CommandSyntaxException {
        NetworkManager.sendToPlayer(source.getPlayerOrException(), EditConfigChoicePacket.server(FTBBackupsServerConfig.KEY));
        return Command.SINGLE_SUCCESS;
    }

    private static int editClientConfig(CommandSourceStack source) throws CommandSyntaxException {
        NetworkManager.sendToPlayer(source.getPlayerOrException(), EditConfigChoicePacket.client(FTBBackupsClientConfig.KEY));
        return Command.SINGLE_SUCCESS;
    }

    private static int editConfig(CommandSourceStack source) throws CommandSyntaxException {
        NetworkManager.sendToPlayer(source.getPlayerOrException(), EditConfigChoicePacket.choose(
                FTBBackupsClientConfig.KEY, FTBBackupsServerConfig.KEY, Component.literal("FTB Backups 3"))
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int time(CommandSourceStack source) {
        Duration d = Duration.between(Instant.now(), Instant.ofEpochMilli(Backups.getServerInstance().nextBackupTime));
        String key = d.isNegative() ? "ftbbackups3.lang.timer.in_past" : "ftbbackups3.lang.timer";
        Duration a = d.abs();
        Component msg = Component.translatable(key, String.format("%02d:%02d:%02d", a.toHoursPart(), a.toMinutesPart(), a.toSecondsPart()));
        source.sendSuccess(() -> msg, true);
        if (d.isNegative() && FTBBackupsServerConfig.ONLY_IF_PLAYERS_ONLINE.get()) {
            source.sendSuccess(() -> Component.translatable("ftbbackups3.lang.timer.no_players"), true);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int start(CommandSourceStack source, String customName) {
        if (Backups.getServerInstance().run(source.getServer(), false, source.getDisplayName(), customName)) {
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.translatable("ftbbackups3.lang.manual_launch", source.getDisplayName()));
            }
        } else {
            source.sendSuccess(() -> Component.translatable("ftbbackups3.lang.already_running"), true);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int size(CommandSourceStack source) {
        long totalSize = Backups.getServerInstance().totalBackupSize();

        source.sendSuccess(() -> Component.translatable("ftbbackups3.lang.size.current",
                BackupUtils.formatSizeString(source.getServer().getWorldPath(LevelResource.ROOT))), true);
        source.sendSuccess(() -> Component.translatable("ftbbackups3.lang.size.total",
                BackupUtils.formatSizeString(totalSize)), true);
        source.sendSuccess(() -> Component.translatable("ftbbackups3.lang.size.available",
                BackupUtils.formatSizeString(Math.min(FTBBackupsServerConfig.MAX_TOTAL_SIZE.get(), Backups.getServerInstance().backupsFolder.toFile().getFreeSpace()))), true);

        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandSourceStack source) {
        source.getServer().getAllLevels().forEach(level -> {
            if (level != null) {
                Component enabled = Component.translatable("ftbbackups3.lang.autosave." + (level.noSave() ? "disabled" : "enabled")).withStyle(ChatFormatting.AQUA);
                Component dim = Component.literal(level.dimension().location().toString()).withStyle(ChatFormatting.YELLOW);
                source.sendSuccess(() -> Component.translatable("ftbbackups3.lang.autosave_status", dim, enabled), true);
            }
        });

        return Command.SINGLE_SUCCESS;
    }

    /**
     * @deprecated This seems like a hack around bigger issues
     */
    @Deprecated(forRemoval = true, since = "21.1.0")
    private static int resetState(CommandSourceStack source) {
        source.getServer().getAllLevels().forEach(level -> {
            if (level != null) {
                source.sendSuccess(() -> Component.literal("Reseting state + saving for " + level.dimension().location()), true);
                level.noSave = false;
                level.save(null, true, true);
            }
        });

        source.sendSuccess(() -> Component.literal("Reseting state + saving for players"), true);
        source.getServer().getPlayerList().saveAll();

        return 1;
    }
}
