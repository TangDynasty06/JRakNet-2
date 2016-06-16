/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Trent Summerlin

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 */
package net.marfgamer.raknet;

import java.lang.reflect.Field;

/**
 * Contains all the Network ID's for RakNet and the current networking protocol
 *
 * @author Trent Summerlin
 */
public interface RakNet {

	// Protocol version
	public static final int NETWORK_PROTOCOL = 8;

	// Transfer size
	public static final int MINIMUM_TRANSFER_UNIT = 530;
	public static final int MAX_PACKETS_PER_SECOND = 500;
	public static final int MAX_RELIABLE_PACKETS_IN_QUEUE = 128;

	// Status
	public static final short ID_UNCONNECTED_PING = 0x01;
	public static final short ID_UNCONNECTED_LEGACY_PING = 0x02;
	public static final short ID_UNCONNECTED_PONG = 0x1C;
	public static final short ID_UNCONNECTED_LEGACY_PONG = 0x1D;

	// Connection
	public static final short ID_UNCONNECTED_CONNECTION_REQUEST_1 = 0x05;
	public static final short ID_UNCONNECTED_CONNECTION_REPLY_1 = 0x06;
	public static final short ID_UNCONNECTED_CONNECTION_REQUEST_2 = 0x07;
	public static final short ID_UNCONNECTED_CONNECTION_REPLY_2 = 0x08;
	public static final short ID_UNCONNECTED_SERVER_FULL = 0x14;
	public static final short ID_CONNECTED_CLIENT_CONNECT_REQUEST = 0x09;
	public static final short ID_CONNECTED_SERVER_HANDSHAKE = 0x10;
	public static final short ID_CONNECTED_CLIENT_HANDSHAKE = 0x13;
	public static final short ID_CONNECTED_CLOSE_CONNECTION = 0x15;

	// Keep-alive and latency testing
	public static final short ID_CONNECTED_PING = 0x00;
	public static final short ID_CONNECTED_PONG = 0x03;

	// Custom Packets
	public static final short ID_CUSTOM_0 = 0x80;
	public static final short ID_CUSTOM_1 = 0x81;
	public static final short ID_CUSTOM_2 = 0x82;
	public static final short ID_CUSTOM_3 = 0x83;
	public static final short ID_CUSTOM_4 = 0x84;
	public static final short ID_CUSTOM_5 = 0x85;
	public static final short ID_CUSTOM_6 = 0x86;
	public static final short ID_CUSTOM_7 = 0x87;
	public static final short ID_CUSTOM_8 = 0x88;
	public static final short ID_CUSTOM_9 = 0x89;
	public static final short ID_CUSTOM_A = 0x8A;
	public static final short ID_CUSTOM_B = 0x8B;
	public static final short ID_CUSTOM_C = 0x8C;
	public static final short ID_CUSTOM_D = 0x8D;
	public static final short ID_CUSTOM_E = 0x8E;
	public static final short ID_CUSTOM_F = 0x8F;

	// Reliability
	public static final short ID_ACK = 0xC0;
	public static final short ID_NACK = 0xA0;

	// Channels
	public static final short MAX_CHANNELS = 32;

	// Time
	public static final long ONE_MINUTES_MILLIS = 60 * 1000L;
	public static final long FIVE_MINUTES_MILLIS = 300 * 1000L;

	/**
	 * Used to get a packet name by it's ID
	 * 
	 * @param id
	 * @return String
	 */
	public static String getName(int id) {
		try {
			Class<?> rakClass = RakNet.class;
			for (Field field : rakClass.getFields()) {
				String name = field.getName();
				if (field.getType().equals(short.class) && name.startsWith("ID")) {
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
