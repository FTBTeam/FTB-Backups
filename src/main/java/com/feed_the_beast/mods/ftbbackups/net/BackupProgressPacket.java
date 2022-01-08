package com.feed_the_beast.mods.ftbbackups.net;

import com.feed_the_beast.mods.ftbbackups.FTBBackupsClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class BackupProgressPacket
{
	private int current, total;

	public BackupProgressPacket(int c, int t)
	{
		current = c;
		total = t;
	}

	public BackupProgressPacket(FriendlyByteBuf buf)
	{
		total = buf.readVarInt();
		current = buf.readVarInt();
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeVarInt(total);
		buf.writeVarInt(current);
	}

	public void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBBackupsClient.setFiles(current, total));
		context.get().setPacketHandled(true);
	}
}