package dev.ftb.mods.ftbbackups.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;

/**
 * Implementations of this interface can be provided to
 * {@link dev.ftb.mods.ftbbackups.api.event.RegisterArchivalPluginEvent#register(IArchivalPlugin)} to register new
 * archival plugins. The event is fired on the server, when the server is starting up, and lazily on the client
 * when populating GUI data.
 */
public interface IArchivalPlugin {
    /**
     * {@return a unique ID for this plugin; it should be namespaced with your mod name}
     */
    ResourceLocation getId();

    /**
     * Get the file extension for backup files created by this plugin. By default, this is the path component
     * of the ID returned by {@link #getId()}, and is appended (along with a ".") to the filename.
     * <p>
     * An empty string return is valid here to indicate no file extension.
     *
     * @return the file extension
     */
    default String getFileExtension() {
        return getId().getPath();
    }

    /**
     * Called when actually creating the archive file
     *
     * @param context the archival context
     * @return the nominal size of the archived, in bytes
     * @throws Exception thrown if anything goes wrong
     */
    @ApiStatus.OverrideOnly
    long createArchive(ArchivalContext context) throws Exception;

    /**
     * Called to restore a backup archive, overwriting existing files in a game instance. This is only
     * called from the client world selection screen for SSP instances, while no world is actually running;
     * it's not relevant for dedicated server instances.
     *
     * @param context the restoration context
     * @throws Exception thrown if anything goes wrong
     */
    @ApiStatus.OverrideOnly
    void restoreArchive(RestorationContext context) throws Exception;

    /**
     * Convenience method to append a file extension
     * @param fileName the base file name
     * @return a filename with the archive extension appended
     */
    @ApiStatus.NonExtendable
    default String addFileExtension(String fileName) {
        return getFileExtension().isEmpty() ? fileName : fileName + "." + getFileExtension();
    }

    /**
     * Base interface for archival and restoration contexts.
     */
    @ApiStatus.NonExtendable
    interface Context {
        /**
         * {@return a Logger instance, which can be used for any logging that's needed}
         */
        Logger logger();

        /**
         * {@return the path to the file (or directory) where the archive should be written to or extracted from}
         */
        Path archivePath();

        /**
         * This method should be called by the plugin implementation for each file that is being backed-up or
         * restored. This is primarily used to provide feedback to players about backup/restoration progress.
         *
         * @param filename the filename currently being handled
         */
        void notifyProcessingFile(String filename);
    }

    /**
     * Passed to {@link IArchivalPlugin#createArchive(ArchivalContext)}. This provides information about the backup
     * to be made, along with some methods to provide feedback about the backup process.
     */
    @ApiStatus.NonExtendable
    interface ArchivalContext extends Context {
        /**
         * Get a manifest of the files to be archived; this is a map of the absolute file path to a string path within
         * the archive that is relative to the game instance directory,
         * e.g. "/path/to/your/instance/saves/New World/level.dat" -> "saves/New World/level.dat"
         * @return the archive manifest
         */
        Map<Path,String> manifest();

        /**
         * An integer compression level, in the range 0..9. The plugin is free to interpret this value as appropriate
         * (or ignore it completely), but a value of 0 indicates no compression is desired, and a value of 9
         * indicates the maximum possible compression is desired.
         *
         * @return the desired compression level
         */
        int compressionLevel();
    }

    /**
     * Passed to {@link IArchivalPlugin#restoreArchive(RestorationContext)}. This provides information about the backup
     * being restored, along with some methods to provide feedback about the restoration process.
     */
    @ApiStatus.NonExtendable
    interface RestorationContext extends Context {
        /**
         * {@return the folder into which files should be extracted, typically the game instance folder}
         */
        Path destinationFolder();
    }
}
