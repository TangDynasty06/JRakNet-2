package port.raknet.java.protocol;

import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import port.raknet.java.Utils;
import port.raknet.java.protocol.raknet.CustomPacket;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;

public class SplitPacket {

	private final int splitId;
	private final int splitCount;
	private final EncapsulatedPacket[] packets;

	public static EncapsulatedPacket[] createSplit(EncapsulatedPacket packet, int mtuSize, int splitId) {
		byte[][] splitData = Utils.splitArray(packet.payload, mtuSize - CustomPacket.DEFAULT_SIZE);
		ArrayList<EncapsulatedPacket> packets = new ArrayList<EncapsulatedPacket>();
		for (int i = 0; i < splitData.length; i++) {
			// Copy packet data
			EncapsulatedPacket encapsulated = new EncapsulatedPacket();
			encapsulated.messageIndex = packet.messageIndex;
			encapsulated.orderChannel = packet.orderChannel;
			encapsulated.orderIndex = packet.orderIndex;

			// Set split data
			encapsulated.split = true;
			encapsulated.splitIndex = i;
			encapsulated.splitId = splitId;
			encapsulated.splitCount = splitData.length;

			// Set payload data
			encapsulated.payload = splitData[i];
			packets.add(encapsulated);
		}
		return packets.toArray(new EncapsulatedPacket[packets.size()]);
	}

	public SplitPacket(int splitId, int splitCount) {
		this.splitId = splitId;
		this.splitCount = splitCount;
		this.packets = new EncapsulatedPacket[splitCount];
	}

	public Packet update(EncapsulatedPacket packet) {
		if (packet.split && packet.splitId == splitId && packet.splitCount == splitCount) {
			packets[packet.splitIndex] = packet;
			return this.checkPacket();
		} else {
			throw new IllegalArgumentException(
					"Invalid EncapsulatedPacket! Do the splitId, splitCount match, and is it even split?");
		}
	}

	public Packet checkPacket() {
		if (packets.length >= splitCount) {
			ByteBuf buffer = Unpooled.buffer();
			for (EncapsulatedPacket packet : packets) {
				buffer.writeBytes(packet.payload);
			}
			return new Packet(buffer);
		}
		return null;
	}

}
