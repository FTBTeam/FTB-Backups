package dev.ftb.mods.ftbbackups;


import dev.ftb.mods.ftblibrary.snbt.config.*;
import net.neoforged.neoforge.common.util.Lazy;

import java.util.ArrayList;

public interface FTBBackupsConfig {
    String KEY = FTBBackups.MOD_ID + "-server";
    SNBTConfig CONFIG = SNBTConfig.create(KEY)
            .comment("Server-specific configuration for FTB Backups 3",
                    "Modpack defaults should be defined in <instance>/config/" + KEY + ".snbt",
                    "  (may be overwritten on modpack update)",
                    "Server admins may locally override this by copying into <instance>/world/serverconfig/" + KEY + ".snbt",
                    "  (will NOT be overwritten on modpack update)"
            );

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
                    "Timer in minutes.",
                    "5 - backups every 5 minutes",
                    "60 - backups every hour",
                    "360 - backups every 6 hours",
                    "1440 - backups once every day"
            );

    IntValue COMPRESSION_LEVEL = CONFIG.addInt("compression_level", 1, 0, 9)
            .comment(
                    "0 - Disabled (output = folders)",
                    "1 - Best speed",
                    "9 - Smallest file size"
            );

    StringValue FOLDER = CONFIG.addString("folder", "")
            .comment("Absolute path to backups folder.");

    BooleanValue DISPLAY_FILE_SIZE = CONFIG.addBoolean("display_file_size", true)
            .comment("Prints (current size | total size) when backup is done.");

    StringListValue EXTRA_FILES = CONFIG.addStringList("extra_files", new ArrayList<>())
            .comment("Add extra files that will be placed in backup _extra_/ folder.");

    StringValue MAX_TOTAL_SIZE_RAW = CONFIG.addString("max_total_size", "50 GB")
            .comment("Maximum total size that is allowed in backups folder. Older backups will be deleted to free space for newer ones.");

    BooleanValue ONLY_IF_PLAYERS_ONLINE = CONFIG.addBoolean("only_if_players_online", true)
            .comment("Only create backups when players have been online.");

    BooleanValue FORCE_ON_SHUTDOWN = CONFIG.addBoolean("force_on_shutdown", false)
            .comment("Create a backup when server is stopped.");

    SNBTConfig ADVANCED = CONFIG.addGroup("advanced")
            .comment("Advanced features that shouldn't be changed unless you know what you are doing.");

    IntValue BUFFER_SIZE = ADVANCED.addInt("buffer_size", 4096, 256, 65536)
            .comment("Buffer size for writing files.");


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

    static void onConfigChanged(boolean isServer) {
        MAX_TOTAL_SIZE.invalidate();
    }
}
