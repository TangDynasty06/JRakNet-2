package port.raknet.java.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import port.raknet.java.RakNet;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.SplitManager;
import port.raknet.java.protocol.SplitPacket;
import port.raknet.java.protocol.SystemAddress;
import port.raknet.java.protocol.raknet.Acknowledge;
import port.raknet.java.protocol.raknet.CustomPacket;
import port.raknet.java.protocol.raknet.EncapsulatedPacket;

/**
 * Represents a RakNet session, used to sending data to clients and tracking
 * it's state
 *
 * @author Trent Summerlin
 */
public class ClientSession implements RakNet {

	// Channel data
	private final ChannelHandlerContext context;
	private final InetSocketAddress address;

	// Client data
	private SessionState state = SessionState.DISCONNECTED;
	private long clientId = 0L;
	private short mtuSize = 0;

	// Packet data
	private int splitId = 0;
	private int lastSeqNumber = 0;
	private int sendSeqNumber = 0;
	private long lastReceiveTime = 0L;
	private final SplitManager splitManager = new SplitManager();
	private final ArrayList<CustomPacket> recovery = new ArrayList<CustomPacket>();
	private final ArrayList<CustomPacket> ackQueue = new ArrayList<CustomPacket>();

	public ClientSession(ChannelHandlerContext context, InetSocketAddress address) {
		this.context = context;
		this.address = address;
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
	 */
	public long getLastReceiveTime() {
		return this.lastReceiveTime;
	}

	/**
	 * Updates the <code>lastReceiveTime</code> for the client
	 */
	public void updateLastReceiveTime(long amount) {
		this.lastReceiveTime += amount;
	}

	/**
	 * Resets the <code>lastReceiveTime</code> for the client
	 */
	public void resetLastReceiveTime() {
		this.lastReceiveTime = 0L;
	}

	/**
	 * Handles CustomPacket and returns all Packets retrieved from it
	 * 
	 * @param custom
	 */
	public Packet[] handleCustom(CustomPacket custom) {
		if (custom.getId() >= CUSTOM_0 && custom.getId() <= CUSTOM_F) {
			ArrayList<Packet> packets = new ArrayList<Packet>();
			for (EncapsulatedPacket encapsulated : custom.packets) {
				Reliability reliability = encapsulated.reliability;

				// Check packet order based on reliability
				if (reliability.isOrdered()) {
					if (lastSeqNumber + 1 != custom.seqNumber) {
						System.out.println("Ordered packet came out of order!");
						return new Packet[0]; // Packet came out of order
					} else {
						lastSeqNumber++;
					}
				} else if (reliability.isSequenced()) {
					if (lastSeqNumber > custom.seqNumber) {
						System.out.println("Sequenced packet as old!");
						return new Packet[0]; // Packet was old
					} else {
						lastSeqNumber = custom.seqNumber;
					}
				}

				if (reliability.isReliable()) {
					// Only send NACK if packet is reliable
					if (lastSeqNumber - custom.seqNumber > 1) {
						Acknowledge nack = new Acknowledge(NACK);
						int[] missing = new int[custom.seqNumber - lastSeqNumber - 1];
						for (int i = 0; i < missing.length; i++) {
							missing[i] = lastSeqNumber + i + 1;
						}
						nack.packets = missing;
						nack.encode();

						this.sendRaw(nack);
					}
				}

				// Acknowledge the packet was received
				Acknowledge ack = new Acknowledge(ACK);
				ack.packets = new int[] { custom.seqNumber };
				ack.encode();

				this.sendRaw(ack);

				if (encapsulated.split == true) {
					SplitPacket split = splitManager.updateSplit(encapsulated);
					Packet stitched = split.checkPacket();
					if (stitched != null) {
						packets.add(stitched);
					}
				} else {
					packets.add(new Packet(Unpooled.copiedBuffer(encapsulated.payload)));
				}
			}

			return packets.toArray(new Packet[packets.size()]);
		} else {
			System.err.println("Packet must be a CustomPacket! Not " + getName(custom.getId()));
			return null;
		}
	}

	/**
	 * Resends all packets that have not yet been acknowledged
	 */
	public void resendACK() {
		for (CustomPacket custom : ackQueue) {
			this.sendRaw(custom);
		}
	}

	/**
	 * Removes all ACK packet from the ACKQueue found in the ACK packet
	 * 
	 * @param ack
	 */
	public void checkACK(Acknowledge ack) {
		if (ack.getId() == ACK) {
			int[] packets = ack.packets;
			for (int i = 0; i < packets.length; i++) {
				ackQueue.remove(packets[i]);
			}
		} else {
			System.err.println("Must be ACK packet! Not " + getName(ack.getId()));
		}
	}

	/**
	 * Resends all packets with the ID's contained in the NACK packet
	 * 
	 * @param nack
	 */
	public void checkNACK(Acknowledge nack) {
		if (nack.getId() == NACK) {
			int[] packets = nack.packets;
			for (int i = 0; i < packets.length; i++) {
				CustomPacket recovered = recovery.get(packets[i]);
				this.sendRaw(recovered);
			}
		} else {
			System.err.println("Must be NACK packet! Not " + getName(nack.getId()));
		}
	}

	/**
	 * Sends an EncapsulatedPacket to the client, will split automatically if
	 * the packet is larger than the MTU
	 * 
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
			CustomPacket custom = new CustomPacket();
			custom.seqNumber = this.sendSeqNumber++;
			custom.packets.add(encapsulated);
			custom.encode();

			this.sendRaw(custom);
			recovery.add(custom.seqNumber, custom);
			if (encapsulated.reliability.isReliable()) {
				ackQueue.add(custom.seqNumber, custom);
			}
		}
	}

	/**
	 * Sends a packet with the specified reliability
	 * 
	 * @param packet
	 * @param reliability
	 */
	public void sendReliable(Packet packet, Reliability reliability) {
		EncapsulatedPacket encapsulated = new EncapsulatedPacket();
		encapsulated.reliability = reliability;
		encapsulated.payload = packet.array();
		this.sendEncapsulated(encapsulated);
	}

	/**
	 * Sends a packet with the reliability <code>RELIABILITY_ORDERED</code>
	 * 
	 * @param packet
	 */
	public void sendPacket(Packet packet) {
		this.sendReliable(packet, Reliability.RELIABLE_ORDERED);
	}

	/**
	 * Sends raw data to the client
	 * 
	 * @param packet
	 */
	public void sendRaw(Packet packet) {
		context.writeAndFlush(new DatagramPacket(packet.buffer(), address));
	}

}
