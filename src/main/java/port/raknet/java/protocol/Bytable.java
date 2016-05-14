package port.raknet.java.protocol;

import io.netty.buffer.ByteBuf;

public interface Bytable {

	public default int readLTriad(ByteBuf buffer) {
		return (0xFF & buffer.readByte()) | (0xFF00 & (buffer.readByte() << 8))
				| (0xFF0000 & (buffer.readByte() << 16));
	}

	public default void writeLTriad(ByteBuf buffer, int t) {
		buffer.writeByte(t << 0);
		buffer.writeByte(t << 8);
		buffer.writeByte(t << 16);
	}

}
