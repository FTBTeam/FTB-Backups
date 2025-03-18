package dev.ftb.mods.ftbbackups.net;

import dev.ftb.mods.ftbbackups.FTBBackupsClient;
import net.minecraft.network.FriendlyByteBuf;

public class BackupProgressPacket {
    private int current, total;

    public BackupProgressPacket(int c, int t) {
        current = c;
        total = t;
    }

    public BackupProgressPacket(FriendlyByteBuf buf) {
        total = buf.readVarInt();
        current = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(total);
        buf.writeVarInt(current);
    }

    //TODO This
	/*public void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBBackupsClient.setFiles(current, total));
		context.get().setPacketHandled(true);
	}*/
}
