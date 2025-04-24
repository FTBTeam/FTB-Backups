package dev.ftb.mods.ftbbackups.archival;

import dev.ftb.mods.ftbbackups.FTBBackups;
import dev.ftb.mods.ftbbackups.FTBBackupsServerConfig;
import dev.ftb.mods.ftbbackups.api.IArchivalPlugin;
import net.minecraft.resources.ResourceLocation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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

        byte[] buffer = new byte[FTBBackupsServerConfig.BUFFER_SIZE.get()];

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

    @Override
    public void restoreArchive(RestorationContext context) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(context.archivePath().toFile()));

        byte[] buffer = new byte[FTBBackupsServerConfig.BUFFER_SIZE.get()];

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            context.notifyProcessingFile(zipEntry.getName());
            if (zipEntry.isDirectory()) {
                context.logger().warn("ignoring directory entry {}", zipEntry.getName());
                continue;
            }

            Path destFile = safePath(context.destinationFolder(), zipEntry);
            context.logger().info("extract to {}", destFile);
            try (FileOutputStream fos = new FileOutputStream(destFile.toFile())) {
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }

            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    private static Path safePath(Path destDir, ZipEntry zipEntry) throws IOException {
        Path subPath = Path.of(zipEntry.getName());
        if (subPath.isAbsolute()) {
            throw new IOException("absolute paths not allowed!");
        }
        return destDir.resolve(subPath);
    }
}
