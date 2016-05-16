package port.raknet.java.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.RakNetServer;
import port.raknet.java.event.Hook;
import port.raknet.java.exception.RakNetException;
import port.raknet.java.exception.UnexpectedPacketException;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.SplitPacket;
import port.raknet.java.protocol.SystemAddress;
import port.raknet.java.protocol.raknet.Acknowledge;
import port.raknet.java.protocol.raknet.ClientConnectRequest;
import port.raknet.java.protocol.raknet.ClientHandshake;
import port.raknet.java.protocol.raknet.CustomPacket;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;
import port.raknet.java.protocol.raknet.Ping;
import port.raknet.java.protocol.raknet.Pong;
import port.raknet.java.protocol.raknet.ServerHandshake;

/**
 * Represents a RakNet session, used to sending data to clients and tracking
 * it's state
 *
 * @author Trent Summerlin
 */
public class ClientSession implements RakNet {

	// Channel data
	private final RakNetServer server;
	private final RakNetHandler handler;
	private final ChannelHandlerContext context;
	private final InetSocketAddress address;

	// Client data
	private SessionState state;
	private long clientId;
	private short mtuSize;

	// Packet sequencing data
	private int sendSeqNumber;
	private int receiveSeqNumber;
	private long lastReceiveTime;

	// Queue data
	private int splitId;
	private int sendIndex;
	private int receiveIndex;
	private HashMap<Integer, Map<Integer, EncapsulatedPacket>> splitQueue;

	// Acknowledge data
	private final HashMap<Integer, CustomPacket> reliableQueue;
	private final HashMap<Integer, CustomPacket> recoveryQueue;

	public ClientSession(RakNetServer server, RakNetHandler handler, ChannelHandlerContext context,
			InetSocketAddress address) {
		this.server = server;
		this.handler = handler;
		this.state = SessionState.DISCONNECTED;
		this.context = context;
		this.address = address;
		this.reliableQueue = new HashMap<Integer, CustomPacket>();
		this.recoveryQueue = new HashMap<Integer, CustomPacket>();
		this.splitQueue = new HashMap<Integer, Map<Integer, EncapsulatedPacket>>();
	}

	/**
	 * Returns the clients remote address
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
	 * Returns the client's remote address as a <code>SystemAddress</code>
	 * 
	 * @return SystemAddress
	 */
	public SystemAddress getSystemAddress() {
		return SystemAddress.fromSocketAddress(address);
	}

	/**
	 * Returns the client's connect RakNet state
	 * 
	 * @return SessionState
	 */
	public SessionState getState() {
		return this.state;
	}

	/**
	 * Set the clients specified RakNet state
	 * 
	 * @param state
	 */
	public void setState(SessionState state) {
		this.state = state;
	}

	/**
	 * Returns the client's ID
	 * 
	 * @return long
	 */
	public long getClientId() {
		return this.clientId;
	}

	/**
	 * Sets the client's ID
	 * 
	 * @param clientId
	 */
	public void setClientId(long clientId) {
		this.clientId = clientId;
	}

	/**
	 * Returns the client's MTU size
	 * 
	 * @return short
	 */
	public short getMTUSize() {
		return this.mtuSize;
	}

	/**
	 * Sets the client MTU size
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
	 * Handles a CustomPacket and it's EncapsulatedPacket
	 * 
	 * @param custom
	 */
	public void handleCustom(CustomPacket custom) {
		// Make sure none of the packets were lost
		if (custom.seqNumber - receiveSeqNumber > 1) {
			Acknowledge nack = new Acknowledge(NACK);
			int[] missing = new int[custom.seqNumber - receiveSeqNumber - 1];
			for (int i = 0; i < missing.length; i++) {
				missing[i] = receiveSeqNumber + i + 1;
			}
			nack.packets = missing;
			nack.encode();
			this.sendRaw(nack);
		}

		// Acknowledge packet
		Acknowledge ack = new Acknowledge(ACK);
		ack.packets = new int[] { custom.seqNumber };
		ack.encode();
		this.sendRaw(ack);

		// Handle EncapsulatedPackets
		for (EncapsulatedPacket packet : custom.packets) {
			try {
				this.handleEncapsulated(packet);
			} catch (RakNetException rne) {
				handler.removeSession(this, rne.getClass().getSimpleName() + ": " + rne.getLocalizedMessage());
				return;
			}
		}

	}

	/**
	 * Handles an EncapsulatedPacket
	 * 
	 * @param EncapsulatedPacket
	 */
	private void handleEncapsulated(EncapsulatedPacket encapsulated) throws RakNetException {
		// Handle packet order based on it's reliability
		Reliability reliability = encapsulated.reliability;

		// TODO: RELIABLE_ORDERED
		if (reliability.isSequenced()) {
			if (encapsulated.orderIndex < receiveIndex) {
				return; // Packet is old, no error needed
			}
		}
		this.receiveIndex = encapsulated.orderIndex;

		// Handle split data of packet
		if (encapsulated.split == true) {
			if (!splitQueue.containsKey(encapsulated.splitId)) {
				if (splitQueue.size() >= 128) {
					handler.removeSession(this, "Too many split packets!");
					return;
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
				ep.reliability = Reliability.RELIABLE;
				this.handleEncapsulated(ep);
			}
			return;
		}

		// Handle packet
		Packet packet = encapsulated.convertPayload();
		short pid = packet.getId();
		if (pid == ID_CONNECTED_CANCEL_CONNECTION) {
			handler.removeSession(this, "Client cancelled connection");
		} else if (pid == ID_CONNECTED_PING) {
			Ping cp = new Ping(packet);
			cp.decode();

			Pong sp = new Pong();
			sp.pingId = cp.pingId;
			sp.encode();
			this.sendPacket(sp);
		} else if (pid == ID_CONNECTED_PONG) {
			// Do nothing, used for keep-alive only
		} else if (state == SessionState.CONNECTING_2) {
			if (pid == ID_CONNECTED_CLIENT_CONNECT_REQUEST) {
				ClientConnectRequest cchr = new ClientConnectRequest(packet);
				cchr.decode();

				ServerHandshake scha = new ServerHandshake();
				scha.clientAddress = this.getSystemAddress();
				scha.sendPing = cchr.sendPing;
				scha.sendPong = System.currentTimeMillis();
				scha.encode();

				this.sendPacket(scha);
				this.state = SessionState.HANDSHAKING;
			}
		} else if (state == SessionState.HANDSHAKING) {
			if (pid == ID_CONNECTED_CLIENT_HANDSHAKE) {
				ClientHandshake cch = new ClientHandshake(packet);
				cch.decode();

				this.state = SessionState.CONNECTED;
			}
		} else if (state == SessionState.CONNECTED) {
			server.executeHook(Hook.PACKET_RECEIVED, this, encapsulated);
		}
	}

	/**
	 * Sends an EncapsulatedPacket, will split automatically if the packet is
	 * larger than the MTU
	 * 
	 * @param channel
	 * @param packet
	 */
	public void sendEncapsulated(EncapsulatedPacket packet) {
		// If packet is too big, split it up
		ArrayList<EncapsulatedPacket> toSend = new ArrayList<EncapsulatedPacket>();
		if (CustomPacket.DEFAULT_SIZE + packet.payload.length > this.mtuSize) {
			EncapsulatedPacket[] split = SplitPacket.createSplit(packet, mtuSize, splitId++);
			for (EncapsulatedPacket encapsulated : split) {
				toSend.add(encapsulated);
			}
		} else {
			toSend.add(packet);
		}

		// Send each EncapsulatedPacket
		for (EncapsulatedPacket encapsulated : toSend) {
			// Update packet orderIndex
			encapsulated.orderIndex = this.sendIndex++;

			// Create CustomPacket and set data
			CustomPacket custom = new CustomPacket();
			custom.seqNumber = this.sendSeqNumber++;
			custom.packets.add(encapsulated);
			custom.encode();

			// Send CustomPacket and update Acknowledge queues
			this.sendRaw(custom);
			recoveryQueue.put(custom.seqNumber, custom);
			if (encapsulated.reliability.isReliable()) {
				reliableQueue.put(custom.seqNumber, custom);
			}
		}
	}

	/**
	 * Sends an EncapsulatedPacket using the specified packet and reliability
	 * 
	 * @param packet
	 * @param reliability
	 */
	public void sendPacket(Reliability reliability, Packet packet) {
		EncapsulatedPacket encapsulated = new EncapsulatedPacket();
		encapsulated.reliability = reliability;
		encapsulated.payload = packet.array();
		this.sendEncapsulated(encapsulated);
	}

	/**
	 * Sends a packet with the specified packet and reliability
	 * <code>RELIABILITY_ORDERED</code>
	 * 
	 * @param packet
	 */
	public void sendPacket(Packet packet) {
		this.sendPacket(Reliability.RELIABLE_ORDERED, packet);
	}

	/**
	 * Sends raw data to the client
	 * 
	 * @param packet
	 */
	public void sendRaw(Packet packet) {
		context.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

	/**
	 * Resends each in the queue that has not yet been acknowledged
	 */
	public void resendACK() {
		for (CustomPacket custom : reliableQueue.values()) {
			this.sendRaw(custom);
		}
	}

	/**
	 * Removes all ACK packet from the ACKQueue found in the ACK packet
	 * 
	 * @param ack
	 * @throws UnexpectedPacketException
	 */
	public void checkACK(Acknowledge ack) throws UnexpectedPacketException {
		if (ack.getId() == ACK) {
			int[] packets = ack.packets;
			for (int i = 0; i < packets.length; i++) {
				reliableQueue.remove(packets[i]);
			}
		} else {
			throw new UnexpectedPacketException(ACK, ack.getId());
		}
	}

	/**
	 * Resends all packets with the ID's contained in the NACK packet
	 * 
	 * @param nack
	 * @throws UnexpectedPacketException
	 */
	public void checkNACK(Acknowledge nack) throws UnexpectedPacketException {
		if (nack.getId() == NACK) {
			int[] packets = nack.packets;
			for (int i = 0; i < packets.length; i++) {
				CustomPacket recovered = recoveryQueue.get(packets[i]);
				this.sendRaw(recovered);
			}
		} else {
			throw new UnexpectedPacketException(NACK, nack.getId());
		}
	}

}
