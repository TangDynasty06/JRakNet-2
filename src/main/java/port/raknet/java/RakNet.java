package port.raknet.java;

import java.lang.reflect.Field;

public interface RakNet {

	// Protocol version
	public static final int NETWORK_PROTOCOL = 7;

	// Status Request
	public static final short ID_STATUS_REQUEST = 0x01;
	public static final short ID_LEGACY_STATUS_REQUEST = 0x02;

	// Connection
	public static final short ID_OPEN_CONNECTION_REQUEST_1 = 0x05;
	public static final short ID_OPEN_CONNECTION_REPLY_1 = 0x06;
	public static final short ID_OPEN_CONNECTION_REQUEST_2 = 0x07;
	public static final short ID_OPEN_CONNECTION_REPLY_2 = 0x08;
	public static final short ID_CLIENT_CONNECT_REQUEST = 0x09;
	public static final short ID_SERVER_HANDSHAKE = 0x10;
	public static final short ID_CLIENT_HANDSHAKE = 0x13;
	public static final short ID_INCOMPATIBLE_PROTOCOL_VERSION = 0x1A;

	// Status Response
	public static final short ID_STATUS_RESPONSE = 0x1C;
	public static final short ID_LEGACY_STATUS_RESPONSE = 0x1D;

	// Custom Packets
	public static final short CUSTOM_0 = 0x80;
	public static final short CUSTOM_1 = 0x81;
	public static final short CUSTOM_2 = 0x82;
	public static final short CUSTOM_3 = 0x83;
	public static final short CUSTOM_4 = 0x84;
	public static final short CUSTOM_5 = 0x85;
	public static final short CUSTOM_6 = 0x86;
	public static final short CUSTOM_7 = 0x87;
	public static final short CUSTOM_8 = 0x88;
	public static final short CUSTOM_9 = 0x89;
	public static final short CUSTOM_A = 0x8A;
	public static final short CUSTOM_B = 0x8B;
	public static final short CUSTOM_C = 0x8C;
	public static final short CUSTOM_D = 0x8D;
	public static final short CUSTOM_E = 0x8E;
	public static final short CUSTOM_F = 0x8F;

	// Reliability
	public static final short NACK = 0xA0;
	public static final short ACK = 0xC0;

	public default String getName(int id) {
		try {
			Class<?> rakClass = RakNet.class;
			for (Field field : rakClass.getFields()) {
				String name = field.getName();
				if (field.getType().equals(short.class)) {
					short fid = field.getShort(name);
					if (fid == id) {
						return name;
					}
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

}
