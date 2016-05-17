package port.raknet.java.protocol;

import java.util.ArrayList;

import port.raknet.java.protocol.raknet.CustomPacket;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;
import port.raknet.java.utils.Utils;

public class SplitPacket {

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
	
}
