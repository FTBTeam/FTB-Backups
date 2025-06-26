package dev.ftb.mods.ftbbackups.config;

import dev.ftb.mods.ftbbackups.FTBBackups;
import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.EnumValue;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import dev.ftb.mods.ftblibrary.util.PanelPositioning;

public interface FTBBackupsClientConfig {
    String KEY = FTBBackups.MOD_ID + "-client";
    SNBTConfig CONFIG = SNBTConfig.create(KEY)
            .comment("Client-specific configuration for FTB Backups 3",
                    "Modpack defaults should be defined in <instance>/config/" + KEY + ".snbt",
                    "  (may be overwritten on modpack update)",
                    "Players may locally override this by copying into <instance>/local/" + KEY + ".snbt",
                    "  (will NOT be overwritten on modpack update)"
            );

    SNBTConfig OVERLAY = CONFIG.addGroup("overlay");
    BooleanValue SHOW_OVERLAY = OVERLAY.addBoolean("show_overlay", true);
    EnumValue<PanelPositioning> OVERLAY_POS = OVERLAY.addEnum("overlay_pos", PanelPositioning.NAME_MAP, PanelPositioning.TOP_LEFT);
    IntValue OVERLAY_INSET_X = OVERLAY.addInt("overlay_inset_x", 4);
    IntValue OVERLAY_INSET_Y = OVERLAY.addInt("overlay_inset_y", 4);
}
