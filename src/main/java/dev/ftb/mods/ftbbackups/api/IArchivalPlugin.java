package dev.ftb.mods.ftbbackups.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface IArchivalPlugin {
    ResourceLocation getId();

    default String getFileExtension() {
        return getId().getPath();
    }

    long createArchive(ArchivalContext context) throws IOException;

    static String addFileExtension(IArchivalPlugin plugin, String fileName) {
        return plugin.getFileExtension().isEmpty() ? fileName : fileName + "." + plugin.getFileExtension();
    }

    @ApiStatus.NonExtendable
    interface ArchivalContext {
        Map<Path,String> manifest();

        Logger logger();

        Path archivePath();

        void notifyProcessingFile(String filename);

        int compressionLevel();
    }
}
