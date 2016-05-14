package port.raknet.java.protocol;

import java.util.ArrayList;

import port.raknet.java.protocol.raknet.EncapsulatedPacket;

public class SplitManager {

	private final ArrayList<SplitPacket> splits;

	public SplitManager() {
		this.splits = new ArrayList<SplitPacket>();
	}

	public SplitPacket updateSplit(EncapsulatedPacket packet) {
		SplitPacket split = splits.get(packet.splitId);
		if (split == null) {
			split = new SplitPacket(packet.splitId, packet.splitCount);
			splits.add(packet.splitId, split);
		}

		split.update(packet);
		return split;
	}

	public Packet[] getCompleted() {
		ArrayList<Packet> completed = new ArrayList<Packet>();
		for (SplitPacket split : splits) {
			Packet stitched = split.checkPacket();
			if (stitched != null) {
				completed.add(stitched);
			}
		}
		return completed.toArray(new Packet[completed.size()]);
	}

}
