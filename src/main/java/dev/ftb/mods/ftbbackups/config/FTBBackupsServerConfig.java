package dev.ftb.mods.ftbbackups.config;


import dev.ftb.mods.ftbbackups.BackupUtils;
import dev.ftb.mods.ftbbackups.Backups;
import dev.ftb.mods.ftbbackups.FTBBackups;
import dev.ftb.mods.ftbbackups.archival.ZipArchiver;
import dev.ftb.mods.ftblibrary.snbt.config.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.Lazy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public interface FTBBackupsServerConfig {
    String KEY = FTBBackups.MOD_ID + "-server";
    SNBTConfig CONFIG = SNBTConfig.create(KEY)
            .comment("Server-specific configuration for FTB Backups 3",
                    "Modpack defaults should be defined in <instance>/config/" + KEY + ".snbt",
                    "  (may be overwritten on modpack update)",
                    "Server admins may locally override this by copying into <instance>/world/serverconfig/" + KEY + ".snbt",
                    "  (will NOT be overwritten on modpack update)"
            );

    // relative to the game directory: <instance>/ftbbackups3
    String DEFAULT_BACKUP_FOLDER = FTBBackups.MOD_ID;

    BooleanValue AUTO = CONFIG.addBoolean("auto", true)
            .comment("Enables backups to run automatically");

    BooleanValue SILENT = CONFIG.addBoolean("silent", false)
            .comment("If set to true, no messages will be displayed in chat/status bar");

    IntValue BACKUPS_TO_KEEP = CONFIG.addInt("backups_to_keep", 12, 0, 32000)
            .comment("The number of backup files to keep.",
                    "More backups = more space used",
                    "0 - Infinite"
            );

    IntValue BACKUP_TIMER_MINUTES = CONFIG.addInt("backup_timer", 120, 1, 43800)
            .comment(
                    "Backup frequency in minutes.",
                    "5 - backups every 5 minutes",
                    "60 - backups every hour",
                    "360 - backups every 6 hours",
                    "1440 - backups once every day"
            );

    IntValue COMPRESSION_LEVEL = CONFIG.addInt("compression_level", 5, 0, 9)
            .comment("Compression level for archived files. Note that this is dependent on the particular plugin in use.",
                    "Higher values typically mean backups take longer to do but take less space on disk.",
                    "0 - No compression",
                    "1 - Minimal compression",
                    "9 - Full compression"
            );

    StringValue FOLDER = CONFIG.addString("folder", "")
            .comment("Absolute path to backups folder. Default of \"\" means to use \"ftbackups3\" within the game instance folder.");

    BooleanValue DISPLAY_FILE_SIZE = CONFIG.addBoolean("display_file_size", true)
            .comment("Broadcasts to all online players a \"(current size | total size)\" message when backup is done.");

    StringListValue EXTRA_FILES = CONFIG.addStringList("extra_files", new ArrayList<>())
            .comment("Add extra files/folders to be backed up, in addition to the world folder. These files *must* be within the game instance!");

    StringValue MAX_TOTAL_SIZE_RAW = CONFIG.addString("max_total_size", "50 GB")
            .comment("Maximum total size that is allowed in backups folder. Older backups will be deleted to free space for newer ones.");

    BooleanValue ONLY_IF_PLAYERS_ONLINE = CONFIG.addBoolean("only_if_players_online", true)
            .comment("Only create backups when at least one player is online.");

    BooleanValue FORCE_ON_SHUTDOWN = CONFIG.addBoolean("force_on_shutdown", false)
            .comment("Create a backup when server is stopped.");

    SNBTConfig ADVANCED = CONFIG.addGroup("advanced")
            .comment("Advanced features that shouldn't be changed unless you know what you are doing.");

    ArchivalPluginValue ARCHIVAL_PLUGIN = CONFIG.add(new ArchivalPluginValue(CONFIG, "archival_plugin", ZipArchiver.ID))
            .comment("Method to use to create a backup archive.",
                    "Builtin methods are \"ftbbackups:zip\" (create a ZIP file) and \"ftbbackups:filecopy\" (simple recursive copy of files with no compression)",
                    "More archival plugins may be added by other mods.");

    BooleanValue NOTIFY_ADMINS_ONLY = CONFIG.addBoolean("notify_admins_only", false)
            .comment("If true, only player with permission level >= 2 (or SSP integrated server owners) will be notified on-screen about backup progress");

    IntValue BUFFER_SIZE = ADVANCED.addInt("buffer_size", 4096, 256, 65536)
            .comment("Buffer size for reading/writing files.");

    BooleanValue ADD_BACKUP_COMMAND_ALIAS = CONFIG.addBoolean("add_backup_command_alias", true)
            .comment("If true, the /backup command will be aliased to /ftbbackups3 backup");

    Lazy<Long> MAX_TOTAL_SIZE = Lazy.of(() -> {
        String mts = MAX_TOTAL_SIZE_RAW.get();

        long maxTotalSize;
        long l = Long.parseLong(mts.substring(0, mts.length() - 2).trim());
        if (mts.endsWith("TB")) {
            maxTotalSize = l * BackupUtils.TB;
        } else if (mts.endsWith("GB")) {
            maxTotalSize = l * BackupUtils.GB;
        } else if (mts.endsWith("MB")) {
            maxTotalSize = l * BackupUtils.MB;
        } else if (mts.endsWith("KB")) {
            maxTotalSize = l * BackupUtils.KB;
        } else {
            maxTotalSize = Long.parseLong(mts.trim());
        }
        return maxTotalSize;
    });

    static long getBackupTimerMillis() {
        return BACKUP_TIMER_MINUTES.get() * 60000L;  // 60000 millisecs in a minute
    }

    static void onConfigChanged(boolean ignoredIsServer) {
        MAX_TOTAL_SIZE.invalidate();
    }

    static ResourceLocation archivalPlugin() {
        ResourceLocation rl = ARCHIVAL_PLUGIN.get();
        if (rl == null) {
            Backups.LOGGER.error("Invalid archive plugin id {}, defaulting to ftbbackups:zip!", ARCHIVAL_PLUGIN.get());
            return ZipArchiver.ID;
        }
        return rl;
    }

    static Path getBackupFolder() {
        String folder = FTBBackupsServerConfig.FOLDER.get();
        return folder.trim().isEmpty() ?
                FMLPaths.GAMEDIR.get().resolve(DEFAULT_BACKUP_FOLDER) :
                Paths.get(folder);
    }
}
