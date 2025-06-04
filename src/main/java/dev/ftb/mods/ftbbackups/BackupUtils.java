package dev.ftb.mods.ftbbackups;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class BackupUtils {
    public static final long KB = 1024L;
    public static final long MB = KB * 1024L;
    public static final long GB = MB * 1024L;
    public static final long TB = GB * 1024L;

    public static final double KB_D = 1024D;
    public static final double MB_D = KB_D * 1024D;
    public static final double GB_D = MB_D * 1024D;
    public static final double TB_D = GB_D * 1024D;

    public static long getSize(Path path) {
        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    Backups.LOGGER.error("getSize: skipped: {} ({})", file, exc);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (exc != null) {
                        Backups.LOGGER.error("getSize: had trouble traversing dir: {} ({})", dir, exc);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }

    public static String formatSizeString(double b) {
        if (b >= TB_D) {
            return String.format("%.1fTB", b / TB_D);
        } else if (b >= GB_D) {
            return String.format("%.1fGB", b / GB_D);
        } else if (b >= MB_D) {
            return String.format("%.1fMB", b / MB_D);
        } else if (b >= KB_D) {
            return String.format("%.1fKB", b / KB_D);
        }

        return ((long) b) + "B";
    }

    public static String formatSizeString(Path file) {
        return formatSizeString(getSize(file));
    }

    public static void writeJson(Writer writer, @Nullable JsonElement element, boolean prettyPrinting) {
        if (element == null || element.isJsonNull()) {
            try {
                writer.write("null");
            } catch (IOException ex) {
                throw new JsonIOException(ex);
            }

            return;
        }

        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setLenient(true);
        jsonWriter.setHtmlSafe(false);
        jsonWriter.setSerializeNulls(true);

        if (prettyPrinting) {
            jsonWriter.setIndent("\t");
        }

        try {
            Streams.write(element, jsonWriter);
        } catch (IOException ex) {
            throw new JsonIOException(ex);
        }
    }

    public static void writeJson(Path jsonFile, @Nullable JsonElement element, boolean prettyPrinting) {
        try {
            Files.createDirectories(jsonFile.getParent());
            try (OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(jsonFile.toFile()), StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(output)) {
                writeJson(writer, element, prettyPrinting);
            }
        } catch (IOException e) {
            Backups.LOGGER.error("could not write JSON file {}: {} / {}", jsonFile, e.getClass(), e.getMessage());
        }
    }

    public static JsonElement readJson(Path jsonFile) {
        if (!Files.exists(jsonFile)) {
            return JsonNull.INSTANCE;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile.toFile()), StandardCharsets.UTF_8))) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement element = Streams.parse(jsonReader);

            if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonSyntaxException("Did not consume the entire document.");
            }

            return element;
        } catch (Exception ex) {
            return JsonNull.INSTANCE;
        }
    }
}
