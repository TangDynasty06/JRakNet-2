package port.raknet.java.event;

/**
 * Executed when a certain event occurs within the server that might be
 * desirable by the programmer to change it's outcome
 *
 * @author Trent Summerlin
 */
public enum Hook {

	/**
	 * Received whenever the server receives a status request <br>
	 * <br>
	 * 
	 * Parameter 0: The address of who is requesting the status (InetAddress)
	 * <br>
	 * Parameter 1: The broadcast message (String)
	 */
	SERVER_PING,

	/**
	 * Received whenever the server receives a legacy status request <br>
	 * <br>
	 * 
	 * Parameter 0: The address of who is requesting the status (InetAddress)
	 * <br>
	 * Parameter 1: The broadcast message (String)
	 */
	LEGACY_PING,

	/**
	 * Received whenever a session is officially connected <br>
	 * <br>
	 * Parameter 0: The RakNetSession (RakNetSession)<br>
	 * Parameter 1: The time the session connected (long)
	 */
	SESSION_CONNECTED,

	/**
	 * Received whenever a session disconnects <br>
	 * <br>
	 * 
	 * Parameter 0: The RakNetSession (RakNetSession)<br>
	 * Parameter 1: The reason the session was disconnected (String)<br>
	 * Parameter 2: The time the session disconnected (long)
	 */
	SESSION_DISCONNECTED,

	/**
	 * Received whenever a packet is received<br>
	 * <br>
	 * 
	 * Parameter 0: The RakNetSession (RakNetSession)<br>
	 * Parameter 1: The EncapsulatedPacket (EncapsulatedPacket)
	 */
	PACKET_RECEIVED,

	/**
	 * Received whenever an exception occurs<br>
	 * <br>
	 * 
	 * Parameter 0: The exception
	 */
	EXCEPTION_CAUGHT;

}
