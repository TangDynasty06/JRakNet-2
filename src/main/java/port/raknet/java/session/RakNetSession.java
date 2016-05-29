package port.raknet.java.session;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.exception.RakNetException;
import port.raknet.java.exception.SplitQueueOverloadException;
import port.raknet.java.exception.UnexpectedPacketException;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.SplitPacket;
import port.raknet.java.protocol.raknet.internal.Acknowledge;
import port.raknet.java.protocol.raknet.internal.CustomPacket;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;

/**
 * Represents a session in RakNet, used by the internal handlers to easily track
 * data and send packets which normally require much more data to send
 *
 * @author Trent Summerlin
 */
public abstract class RakNetSession implements RakNet {

	// Channel data
	private final Channel channel;
	private final InetSocketAddress address;

	// Session data
	private long sessionId;
	private short mtuSize;

	// Packet sequencing data
	private int sendSeqNumber;
	private int receiveSeqNumber;
	private long lastReceiveTime;

	// Queue data
	private int splitId;
	private int sendMessageIndex;
	private int[] sendOrderIndex;
	private int[] receiveIndex;
	private final HashMap<Integer, CustomPacket> recoveryQueue;
	private final HashMap<Integer, Map<Integer, EncapsulatedPacket>> splitQueue;

	public RakNetSession(Channel channel, InetSocketAddress address) {
		this.channel = channel;
		this.address = address;
		this.sendOrderIndex = new int[32];
		this.receiveIndex = new int[32];
		this.recoveryQueue = new HashMap<Integer, CustomPacket>();
		this.splitQueue = new HashMap<Integer, Map<Integer, EncapsulatedPacket>>();
	}

	/**
	 * Returns the client's remote address
	 * 
	 * @return InetAddress
	 */
	public InetAddress getAddress() {
		return address.getAddress();
	}

	/**
	 * Returns the client's remote port
	 * 
	 * @return int
	 */
	public int getPort() {
		return address.getPort();
	}

	/**
	 * Returns the client's remote address as a <code>InetSocketAddress</code>
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
	 * @param clientId
	 */
	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Returns the session's MTU size
	 * 
	 * @return short
	 */
	public short getMTUSize() {
		return this.mtuSize;
	}

	/**
	 * Sets the session's MTU size
	 * 
	 * @param mtuSize
	 */
	public void setMTUSize(short mtuSize) {
		this.mtuSize = mtuSize;
	}

	/**
	 * Returns the last time the a packet was received from the client
	 * 
	 * @return long
	 */
	public long getLastReceiveTime() {
		return this.lastReceiveTime;
	}

	/**
	 * Updates the <code>lastReceiveTime</code> for the client
	 */
	public void pushLastReceiveTime(long amount) {
		this.lastReceiveTime += amount;
	}

	/**
	 * Resets the <code>lastReceiveTime</code> for the client
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
		if (CustomPacket.DEFAULT_SIZE + EncapsulatedPacket.DEFAULT_SIZE + packet.payload.length > this.mtuSize) {
			EncapsulatedPacket[] split = SplitPacket.createSplit(packet, mtuSize, splitId++);
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
				encapsulated.orderIndex = this.sendOrderIndex[encapsulated.orderChannel]++;
			} else {
				encapsulated.orderChannel = 0;
				encapsulated.orderIndex = 0;
			}

			custom.seqNumber = this.sendSeqNumber++;
			custom.packets.add(encapsulated);
			custom.encode();

			// Send CustomPacket and update Acknowledge queues
			this.sendRaw(custom);
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
	 * Sends raw data to the client
	 * 
	 * @param packet
	 */
	public final void sendRaw(Packet packet) {
		try {
			channel.writeAndFlush(new DatagramPacket(packet.buffer(), address)).sync();
		} catch (InterruptedException e) {
			// A bad packet send is not the end of the world
		}
	}

	/**
	 * Removes all packets in the ACK packet from the recovery queue, as they
	 * have been acknowledged by the client already.
	 * 
	 * @throws UnexpectedPacketException
	 * 
	 */
	public final void handleAck(Acknowledge ack) throws UnexpectedPacketException {
		if (ack.getId() == ID_ACK) {
			for (int packet : ack.packets) {
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

		// Acknowledge packet
		Acknowledge ack = new Acknowledge(ID_ACK);
		ack.packets = new int[] { custom.seqNumber };
		ack.encode();
		this.sendRaw(ack);

		// Handle EncapsulatedPackets
		for (EncapsulatedPacket encapsulated : custom.packets) {
			this.handleEncapsulated0(encapsulated);
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
				if (splitQueue.size() >= 128) {
					throw new SplitQueueOverloadException(this);
				}

				Map<Integer, EncapsulatedPacket> split = new HashMap<>();
				split.put(encapsulated.splitIndex, encapsulated);
				splitQueue.put(encapsulated.splitId, split);
			} else {
				Map<Integer, EncapsulatedPacket> split = splitQueue.get(encapsulated.splitId);
				split.put(encapsulated.splitIndex, encapsulated);
				splitQueue.put(encapsulated.splitId, split);
			}

			if (splitQueue.get(encapsulated.splitId).size() == encapsulated.splitCount) {
				ByteBuf b = Unpooled.buffer();
				int size = 0;
				Map<Integer, EncapsulatedPacket> packets = splitQueue.get(encapsulated.splitId);
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
