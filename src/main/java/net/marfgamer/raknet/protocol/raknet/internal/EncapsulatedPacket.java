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
package net.marfgamer.raknet.protocol.raknet.internal;

import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.protocol.Message;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.utils.ArrayUtils;

public class EncapsulatedPacket implements Bytable {

	/**
	 * Returns the header length of an EncapsulatedPacket with the specified
	 * reliability and whether or not it is split. These two are key factors in
	 * packet splitting as even a slight change in header size can massively
	 * change the amount of bytes that can be sent as a single packet.
	 * 
	 * @param reliability
	 * @param split
	 * @return int
	 */
	public static int getHeaderLength(Reliability reliability, boolean split) {
		int headerSize = 0;
		headerSize += 1; // Reliability
		headerSize += 2; // Payload length

		if (reliability.isReliable()) {
			headerSize += 3; // Message index
		}

		if (reliability.isOrdered() || reliability.isSequenced()) {
			headerSize += 3; // Order index
			headerSize += 1; // Order channel
		}

		if (split) {
			headerSize += 4; // Split count
			headerSize += 2; // Count ID
			headerSize += 4; // Cound index
		}

		return headerSize;
	}

	public static EncapsulatedPacket[] split(EncapsulatedPacket packet, int mtuSize, int splitId) {
		byte[][] splitData = ArrayUtils.splitArray(packet.payload,
				mtuSize - CustomPacket.HEADER_LENGTH - getHeaderLength(packet.reliability, true));
		ArrayList<EncapsulatedPacket> packets = new ArrayList<EncapsulatedPacket>();
		for (int i = 0; i < splitData.length; i++) {
			// Copy packet data
			EncapsulatedPacket encapsulated = new EncapsulatedPacket();
			encapsulated.reliability = packet.reliability;
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

	/**
	 * Returns the maximum size of bytes that can be sent as one packet
	 * 
	 * @return int
	 */
	public static int getMaxPacketSize(Reliability reliability, int maxTransferUnit) {
		return (maxTransferUnit - CustomPacket.HEADER_LENGTH
				- EncapsulatedPacket.getHeaderLength(reliability, true)) * RakNet.MAX_SPLIT_COUNT;
	}

	// Binary flag data
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

	public Message convertPayload() {
		return new Message(payload);
	}

}
