package port.raknet.java.protocol.raknet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import port.raknet.java.protocol.Bytable;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;

public class EncapsulatedPacket implements Bytable {
	
	public static final short DEFAULT_SIZE = 11;
	public static final byte FLAG_RELIABILITY = (byte) 0xF4;
	public static final byte FLAG_SPLIT = (byte) 0x10;

	// Encapsulation data
	public Reliability reliability;
	public boolean split;

	// Reliability data
	public int messageIndex;

	// Order data
	public int orderIndex;
	public int orderChannel;

	// Split data
	public int splitCount;
	public int splitId;
	public int splitIndex;

	// Packet payload
	public byte[] payload;

	/**
	 * Only used by the handler if the packet is ordered
	 */
	public int seqNumber = -1;

	public void encode(ByteBuf buffer) {
		buffer.writeByte((byte) ((reliability.asByte() << 5) | (split ? FLAG_SPLIT : 0)));
		buffer.writeShort((payload.length * 8) & 0xFFFF);

		if (reliability.isReliable()) {
			this.writeLTriad(buffer, messageIndex);
		}

		if (reliability.isOrdered() || reliability.isSequenced()) {
			this.writeLTriad(buffer, orderIndex);
			buffer.writeByte(orderChannel);
		}

		if (split) {
			buffer.writeInt(splitCount);
			buffer.writeShort(splitId & 0xFFFF);
			buffer.writeInt(splitIndex);
		}

		buffer.writeBytes(payload);
	}

	public void decode(ByteBuf buffer) {
		short flags = (short) (buffer.readByte() & 0xFF);
		this.reliability = Reliability.lookup((byte) ((flags & FLAG_RELIABILITY) >> 5));
		this.split = (flags & FLAG_SPLIT) > 0;
		int length = (buffer.readUnsignedShort() / 8);

		if (reliability.isReliable()) {
			this.messageIndex = this.readLTriad(buffer);
		}

		if (reliability.isOrdered() || reliability.isSequenced()) {
			this.orderIndex = this.readLTriad(buffer);
			this.orderChannel = buffer.readByte();
		}

		if (split) {
			this.splitCount = buffer.readInt();
			this.splitId = buffer.readShort();
			this.splitIndex = buffer.readInt();
		}

		this.payload = new byte[length];
		buffer.readBytes(payload);
	}

	public Packet convertPayload() {
		return new Packet(Unpooled.copiedBuffer(payload));
	}

}
