package port.raknet.java.protocol.raknet.internal;

import java.util.ArrayList;

import port.raknet.java.protocol.Packet;

public class CustomPacket extends Packet {
	
	public static final int DEFAULT_SIZE = 4;

	/**
	 * This is handled by the ClientSession class
	 */
	public int seqNumber;
	public ArrayList<EncapsulatedPacket> packets;

	public CustomPacket(Packet packet) {
		super(packet);
		this.packets = new ArrayList<EncapsulatedPacket>();
	}

	public CustomPacket() {
		super(ID_CUSTOM_4);
		this.packets = new ArrayList<EncapsulatedPacket>();
	}

	@Override
	public void encode() {
		int encoded = 0;
		this.putLTriad(seqNumber);
		for (EncapsulatedPacket packet : packets) {
			packet.messageIndex = encoded++;
			packet.encode(buffer);
		}
	}

	@Override
	public void decode() {
		this.seqNumber = this.getLTriad();
		while (this.remaining() >= 4) {
			EncapsulatedPacket encapsulated = new EncapsulatedPacket();
			encapsulated.decode(buffer);
			packets.add(encapsulated);
		}
	}

}
