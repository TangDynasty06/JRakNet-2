package port.raknet.java.protocol;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import port.raknet.java.RakNet;

public class Packet implements RakNet {

	public final static byte[] MAGIC = new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE,
			(byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0x12,
			(byte) 0x34, (byte) 0x56, (byte) 0x78 };

	protected final ByteBuf buffer;
	protected short id;

	public Packet(int id) {
		this.buffer = Unpooled.buffer();
		this.id = (short) id;
		this.putUByte(id);
	}

	public Packet(Packet packet) {
		this.buffer = packet.buffer;
		this.id = packet.id;
	}

	public Packet(ByteBuf buffer) {
		this.buffer = buffer;
		this.id = this.getUByte();
	}

	public final short getId() {
		return this.id;
	}

	public void encode() {
	}

	public void decode() {
	}

	public void get(byte[] dest) {
		for (int i = 0; i < dest.length; i++) {
			dest[i] = buffer.readByte();
		}
	}

	public byte[] get(int length) {
		byte[] data = new byte[length];
		for (int i = 0; i < data.length; i++) {
			data[i] = buffer.readByte();
		}
		return data;
	}

	public byte getByte() {
		return buffer.readByte();
	}

	public short getUByte() {
		return (short) (buffer.readByte() & 0xFF);
	}

	public boolean getBoolean() {
		return (this.getUByte() > 0x00);
	}

	public short getShort() {
		return buffer.readShort();
	}

	public int getUShort() {
		return (buffer.readShort() & 0xFFFF);
	}

	public int getLTriad() {
		return (0xFF & buffer.readByte()) | (0xFF00 & (buffer.readByte() << 8))
				| (0xFF0000 & (buffer.readByte() << 16));
	}

	public int getInt() {
		return buffer.readInt();
	}

	public long getLong() {
		return buffer.readLong();
	}

	public float getFloat() {
		return buffer.readFloat();
	}

	public double getDouble() {
		return buffer.readDouble();
	}

	public boolean checkMagic() {
		byte[] magicCheck = this.get(MAGIC.length);
		return Arrays.equals(magicCheck, MAGIC);
	}

	public String getString() {
		int len = this.getUShort();
		byte[] data = this.get(len);
		return new String(data);
	}

	public InetSocketAddress getAddress() {
		short version = this.getUByte();
		if (version == 4) {
			String address = ((~this.getByte()) & 0xFF) + "." + ((~this.getByte()) & 0xFF) + "."
					+ ((~this.getByte()) & 0xFF) + "." + ((~this.getByte()) & 0xFF);
			int port = this.getUShort();
			return new InetSocketAddress(address, port);
		} else if (version == 6) {
			throw new UnsupportedOperationException("Can't read IPv6 address: Not Implemented");
		} else {
			throw new UnsupportedOperationException("Can't read IPv" + version + " address: unknown");
		}
	}

	public Packet put(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			buffer.writeByte(data[i]);
		}
		return this;
	}

	public Packet pad(int length) {
		for (int i = 0; i < length; i++) {
			buffer.writeByte(0x00);
		}
		return this;
	}

	public Packet putByte(int b) {
		buffer.writeByte((byte) b);
		return this;
	}

	public Packet putUByte(int b) {
		buffer.writeByte(((byte) b) & 0xFF);
		return this;
	}

	public void putBoolean(boolean b) {
		this.putUByte(b ? 0x01 : 0x00);
	}

	public Packet putShort(int s) {
		buffer.writeShort(s);
		return this;
	}

	public Packet putUShort(int s) {
		buffer.writeShort(((short) s) & 0xFFFF);
		return this;
	}

	public Packet putLTriad(int t) {
		buffer.writeByte(t << 0);
		buffer.writeByte(t << 8);
		buffer.writeByte(t << 16);
		return this;
	}

	public Packet putInt(int i) {
		buffer.writeInt(i);
		return this;
	}

	public Packet putLong(long l) {
		buffer.writeLong(l);
		return this;
	}

	public Packet putFloat(double f) {
		buffer.writeFloat((float) f);
		return this;
	}

	public Packet putDouble(double d) {
		buffer.writeDouble(d);
		return this;
	}

	public Packet putMagic() {
		this.put(MAGIC);
		return this;
	}

	public Packet putString(String s) {
		byte[] data = s.getBytes();
		this.putUShort(data.length);
		this.put(data);
		return this;
	}

	public void putAddress(InetSocketAddress address) {
		this.putUByte(4);
		for (String part : address.getHostString().split(Pattern.quote("."))) {
			this.putByte((byte) ((byte) ~(Integer.parseInt(part)) & 0xFF));
		}
		this.putUShort(address.getPort());
	}

	public void putAddress(String address, int port) {
		this.putAddress(new InetSocketAddress(address, port));
	}

	public byte[] array() {
		return Arrays.copyOfRange(buffer.array(), 0, buffer.writerIndex());
	}

	public int size() {
		return array().length;
	}

	public ByteBuf buffer() {
		return this.buffer.duplicate().retain();
	}

	public int remaining() {
		return buffer.readableBytes();
	}

}
