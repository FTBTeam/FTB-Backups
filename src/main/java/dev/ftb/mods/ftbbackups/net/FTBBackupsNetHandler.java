package dev.ftb.mods.ftbbackups.net;

import dev.ftb.mods.ftbbackups.FTBBackups;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class FTBBackupsNetHandler {
    private static final String NETWORK_VERSION = "1.0";

    public static void init(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(FTBBackups.MOD_ID)
                .versioned(NETWORK_VERSION);

        registrar.playToClient(BackupProgressPacket.TYPE, BackupProgressPacket.STREAM_CODEC, BackupProgressPacket::handler);
    }
}
