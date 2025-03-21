package dev.ftb.mods.ftbbackups.archival;

import dev.ftb.mods.ftbbackups.FTBBackups;
import dev.ftb.mods.ftbbackups.FTBBackupsConfig;
import dev.ftb.mods.ftbbackups.api.IArchivalPlugin;
import net.minecraft.resources.ResourceLocation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public enum ZipArchiver implements IArchivalPlugin {
    INSTANCE;

    public static final ResourceLocation ID = FTBBackups.id("zip");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public long createArchive(ArchivalContext context) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(context.archivePath().toFile()));
        zos.setLevel(context.compressionLevel());

        byte[] buffer = new byte[FTBBackupsConfig.BUFFER_SIZE.get()];

        context.logger().info("Zipping {} files...", context.manifest().size());

        for (Map.Entry<Path, String> entry : context.manifest().entrySet()) {
            Path absPath = entry.getKey();
            String archiveEntry = entry.getValue();
            try {
                context.notifyProcessingFile(archiveEntry);
                ZipEntry ze = new ZipEntry(archiveEntry);

                zos.putNextEntry(ze);
                FileInputStream fis = new FileInputStream(absPath.toFile());

                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                fis.close();
            } catch (IOException ex) {
                throw new IOException(String.format("Failed to add file %s to ZIP archive: %s", archiveEntry, ex));
            }
        }

        zos.close();
        return Files.size(context.archivePath());
    }
}
