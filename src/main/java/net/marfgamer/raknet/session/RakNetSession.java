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
package net.marfgamer.raknet.session;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import net.marfgamer.raknet.RakNet;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.exception.packet.PacketQueueOverloadException;
import net.marfgamer.raknet.exception.packet.SplitPacketQueueException;
import net.marfgamer.raknet.exception.packet.UnexpectedPacketException;
import net.marfgamer.raknet.protocol.MessageIdentifiers;
import net.marfgamer.raknet.protocol.Packet;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.protocol.raknet.internal.Acknowledge;
import net.marfgamer.raknet.protocol.raknet.internal.CustomPacket;
import net.marfgamer.raknet.protocol.raknet.internal.EncapsulatedPacket;
import net.marfgamer.raknet.protocol.raknet.internal.SplitPacket;

/**
 * Represents a session in RakNet, used by the internal handlers to easily track
 * data and send packets which normally require much more data to send
 *
 * @author Trent Summerlin
 */
public abstract class RakNetSession implements RakNet, MessageIdentifiers, Reliability.INTERFACE {

	// Channel data
	private final Channel channel;
	private final InetSocketAddress address;

	// Session data
	private long sessionId;
	private short maximumTransferUnit;
	private long latency;

	// Packet sequencing data
	private int sendSeqNumber;
	private int receiveSeqNumber;
	private long lastSendTime;
	private long lastReceiveTime;
	private int receivedPacketsThisSecond;

	// Queue data
	private int splitId;
	private int sendMessageIndex;
	private int[] sendIndex;
	private int[] receiveIndex;
	private final ArrayList<Integer> receivedCustoms;
	private final ConcurrentHashMap<Integer, CustomPacket> reliableQueue;
	private final ConcurrentHashMap<Integer, CustomPacket> recoveryQueue;
	private final ConcurrentHashMap<Integer, HashMap<Integer, EncapsulatedPacket>> splitQueue;

	public RakNetSession(Channel channel, InetSocketAddress address) {
		this.channel = channel;
		this.address = address;
		this.sendIndex = new int[32];
		this.receiveIndex = new int[32];
		this.receivedCustoms = new ArrayList<Integer>();
		this.reliableQueue = new ConcurrentHashMap<Integer, CustomPacket>();
		this.recoveryQueue = new ConcurrentHashMap<Integer, CustomPacket>();
		this.splitQueue = new ConcurrentHashMap<Integer, HashMap<Integer, EncapsulatedPacket>>();
	}

	/**
	 * Returns the session's remote address
	 * 
	 * @return InetAddress
	 */
	public InetAddress getAddress() {
		return address.getAddress();
	}

	/**
	 * Returns the session's remote port
	 * 
	 * @return int
	 */
	public int getPort() {
		return address.getPort();
	}

	/**
	 * Returns the session's remote address as a <code>InetSocketAddress</code>
	 * 
	 * @return InetSocketAddress
	 */
	public InetSocketAddress getSocketAddress() {
		return this.address;
	}

	/**
	 * Returns the sessions's ID
	 * 
	 * @return long
	 */
	public long getSessionId() {
		return this.sessionId;
	}

	/**
	 * Sets the session's ID
	 * 
	 * @param sessionId
	 */
	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Returns the session's MTU size
	 * 
	 * @return short
	 */
	public short getMaximumTransferUnit() {
		return this.maximumTransferUnit;
	}

	/**
	 * Sets the session's MTU size
	 * 
	 * @param maximumTransferUnit
	 */
	public void setMaximumTransferUnit(short maximumTransferUnit) {
		this.maximumTransferUnit = maximumTransferUnit;
	}

	/**
	 * Returns the session's latency
	 * 
	 * @return long
	 */
	public long getLatency() {
		return this.latency;
	}

	/**
	 * Sets the session's latency
	 * 
	 * @param latency
	 */
	public void setLatency(long latency) {
		this.latency = latency;
	}

	/**
	 * Returns the amount of packets that have been received this second
	 * 
	 * @return int
	 */
	public int getReceivedPacketsThisSecond() {
		return this.receivedPacketsThisSecond;
	}

	/**
	 * Updates the <code>receivedPacketsThisSecond</code> for the session
	 */
	public void pushReceivedPacketsThisSecond() {
		this.receivedPacketsThisSecond++;
	}

	/**
	 * Resets the <code>receivedPacketThisSecond</code> for the session
	 */
	public void resetReceivedPacketsThisSecond() {
		this.receivedPacketsThisSecond = 0;
	}

	/**
	 * Returns the last time a packet was sent to the session
	 * 
	 * @return long
	 */
	public long getLastSendTime() {
		return this.lastSendTime;
	}

	/**
	 * Returns the last time a packet was received from the session
	 * 
	 * @return long
	 */
	public long getLastReceiveTime() {
		return this.lastReceiveTime;
	}

	/**
	 * Updates the <code>lastReceiveTime</code> for the session
	 */
	public void pushLastReceiveTime(long amount) {
		this.lastReceiveTime += amount;
	}

	/**
	 * Resets the <code>lastReceiveTime</code> for the session
	 */
	public void resetLastReceiveTime() {
		this.lastReceiveTime = 0L;
	}

	/**
	 * Sends an EncapsulatedPacket, will automatically split packets if
	 * necessary.
	 * 
	 * @param channel
	 * @param packet
	 */
	public final void sendEncapsulated(EncapsulatedPacket packet) {
		// If packet is too big, split it up
		ArrayList<EncapsulatedPacket> toSend = new ArrayList<EncapsulatedPacket>();
		if (CustomPacket.HEADER_LENGTH + EncapsulatedPacket.getHeaderLength(packet.reliability, false)
				+ packet.payload.length > this.maximumTransferUnit) {
			EncapsulatedPacket[] split = SplitPacket.createSplit(packet, maximumTransferUnit, splitId++);
			for (EncapsulatedPacket encapsulated : split) {
				toSend.add(encapsulated);
			}
		} else {
			toSend.add(packet);
		}

		// Send each EncapsulatedPacket
		for (EncapsulatedPacket encapsulated : toSend) {
			// Create CustomPacket and set data
			CustomPacket custom = new CustomPacket();

			if (packet.reliability.isReliable()) {
				encapsulated.messageIndex = sendMessageIndex++;
			} else {
				encapsulated.messageIndex = 0;
			}

			if (packet.reliability.isOrdered() || packet.reliability.isSequenced()) {
				encapsulated.orderIndex = this.sendIndex[encapsulated.orderChannel]++;
			} else {
				encapsulated.orderChannel = 0;
				encapsulated.orderIndex = 0;
			}

			custom.seqNumber = this.sendSeqNumber++;
			custom.packets.add(encapsulated);
			custom.encode();

			System.out.println(custom.array().length + " <- CUSTOM PACKET LENGTH");

			// Send CustomPacket and update Acknowledge queues
			this.sendRaw(custom);
			if (encapsulated.reliability.isReliable()) {
				reliableQueue.put(custom.seqNumber, custom);
			}
			recoveryQueue.put(custom.seqNumber, custom);
		}
	}

	/**
	 * Sends an EncapsulatedPacket using the specified packet and reliability
	 * 
	 * @param packet
	 * @param reliability
	 */
	public final void sendPacket(Reliability reliability, Packet packet) {
		EncapsulatedPacket encapsulated = new EncapsulatedPacket();
		encapsulated.reliability = reliability;
		encapsulated.payload = packet.array();
		this.sendEncapsulated(encapsulated);
	}

	/**
	 * Sends raw data to the session
	 * 
	 * @param packet
	 */
	public final void sendRaw(Packet packet) {
		channel.writeAndFlush(new DatagramPacket(packet.buffer(), address));
		this.lastSendTime = System.currentTimeMillis();
	}

	/**
	 * Returns all reliable packets that have not yet been Acknowledged
	 * 
	 * @return CustomPacket[]
	 */
	public CustomPacket[] getReliableQueue() {
		return this.reliableQueue.values().toArray(new CustomPacket[reliableQueue.size()]);
	}

	/**
	 * Returns all packets that have not yet been Acknowledged
	 * 
	 * @return CustomPacket[]
	 */
	public final CustomPacket[] getRecoveryQueue() {
		return this.recoveryQueue.values().toArray(new CustomPacket[recoveryQueue.size()]);
	}

	/**
	 * Removes all packets in the ACK packet from the recovery queue, as they
	 * have already been acknowledged
	 * 
	 * @param ack
	 * @throws UnexpectedPacketException
	 */
	public final void handleAck(Acknowledge ack) throws UnexpectedPacketException {
		if (ack.getId() == ID_ACK) {
			for (int packet : ack.packets) {
				reliableQueue.remove(packet);
				recoveryQueue.remove(packet);
			}
		} else {
			throw new UnexpectedPacketException(ID_ACK, ack.getId());
		}
	}

	/**
	 * Resends all packets with the ID's contained in the NACK packet
	 * 
	 * @param nack
	 * @throws UnexpectedPacketException
	 */
	public final void handleNack(Acknowledge nack) throws UnexpectedPacketException {
		if (nack.getId() == ID_NACK) {
			int[] packets = nack.packets;
			for (int i = 0; i < packets.length; i++) {
				CustomPacket recovered = recoveryQueue.get(packets[i]);
				if (recovered != null) {
					this.sendRaw(recovered);
				}
			}
		} else {
			throw new UnexpectedPacketException(ID_NACK, nack.getId());
		}
	}

	public final void handleCustom0(CustomPacket custom) throws RakNetException {
		// Acknowledge packet even if it has been received before
		Acknowledge ack = new Acknowledge(ID_ACK);
		ack.packets = new int[] { custom.seqNumber };
		ack.encode();
		this.sendRaw(ack);

		// Make sure this packet wasn't already received
		if (!receivedCustoms.contains(custom.seqNumber)) {
			// Make sure none of the packets were lost
			if (custom.seqNumber - receiveSeqNumber > 1) {
				Acknowledge nack = new Acknowledge(ID_NACK);
				int[] missing = new int[custom.seqNumber - receiveSeqNumber - 1];
				for (int i = 0; i < missing.length; i++) {
					missing[i] = receiveSeqNumber + i + 1;
				}
				nack.packets = missing;
				nack.encode();
				this.sendRaw(nack);
			}
			this.receiveSeqNumber = custom.seqNumber;

			// Handle encapsulated packets
			for (EncapsulatedPacket encapsulated : custom.packets) {
				this.handleEncapsulated0(encapsulated);
			}
			receivedCustoms.add(custom.seqNumber);
		}
	}

	private final void handleEncapsulated0(EncapsulatedPacket encapsulated) throws RakNetException {
		// Handle packet order based on it's reliability
		Reliability reliability = encapsulated.reliability;

		if (reliability.isOrdered()) {
			// TODO: Ordered packets
		} else if (reliability.isSequenced()) {
			if (encapsulated.orderIndex < receiveIndex[encapsulated.orderChannel]) {
				return; // Packet is old, no error needed
			}
			receiveIndex[encapsulated.orderChannel] = encapsulated.orderIndex + 1;
		}

		// Handle split data of packet
		if (encapsulated.split == true) {
			if (!splitQueue.containsKey(encapsulated.splitId)) {
				if (splitQueue.size() > MAX_SPLITS_PER_QUEUE) {
					throw new PacketQueueOverloadException(this, "split queue", MAX_SPLITS_PER_QUEUE);
				}
				if (encapsulated.splitCount > MAX_SPLIT_COUNT) {
					throw new SplitPacketQueueException(encapsulated);
				}

				HashMap<Integer, EncapsulatedPacket> split = new HashMap<Integer, EncapsulatedPacket>();
				split.put(encapsulated.splitIndex, encapsulated);
				splitQueue.put(encapsulated.splitId, split);
			} else {
				HashMap<Integer, EncapsulatedPacket> split = splitQueue.get(encapsulated.splitId);
				split.put(encapsulated.splitIndex, encapsulated);
				splitQueue.put(encapsulated.splitId, split);
			}

			if (splitQueue.get(encapsulated.splitId).size() == encapsulated.splitCount) {
				ByteBuf b = Unpooled.buffer();
				int size = 0;
				HashMap<Integer, EncapsulatedPacket> packets = splitQueue.get(encapsulated.splitId);
				for (int i = 0; i < encapsulated.splitCount; i++) {
					b.writeBytes(packets.get(i).payload);
					size += packets.get(i).payload.length;
				}
				byte[] data = Arrays.copyOfRange(b.array(), 0, size);
				splitQueue.remove(encapsulated.splitId);

				EncapsulatedPacket ep = new EncapsulatedPacket();
				ep.payload = data;
				ep.orderChannel = encapsulated.orderChannel;
				ep.reliability = encapsulated.reliability;
				this.handleEncapsulated0(ep);
			}
			return;
		}

		// Handle packet
		this.handleEncapsulated(encapsulated);
	}

	public abstract void handleEncapsulated(EncapsulatedPacket encapsulated);

}
