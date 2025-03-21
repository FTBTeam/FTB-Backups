package dev.ftb.mods.ftbbackups.archival;

import dev.ftb.mods.ftbbackups.FTBBackups;
import dev.ftb.mods.ftbbackups.api.IArchivalPlugin;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public enum FileCopyArchiver implements IArchivalPlugin {
    INSTANCE;

    public static final ResourceLocation ID = FTBBackups.id("filecopy");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getFileExtension() {
        return "";
    }

    @Override
    public long createArchive(ArchivalContext context) {
        long archiveSize = 0L;
        for (Map.Entry<Path, String> entry : context.manifest().entrySet()) {
            try {
                Path file = entry.getKey();
                context.notifyProcessingFile(entry.getValue());
                Path newFile = context.archivePath().resolve(Path.of(entry.getValue()));
                Files.createDirectories(newFile.getParent());
                Files.copy(file, newFile);
                archiveSize += Files.size(newFile);
            } catch (Exception ex) {
                context.logger().error("Failed to copy file {}: {}", entry.getValue(), ex);
            }
        }
        return archiveSize;
    }
}
