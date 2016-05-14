package port.raknet.java.event;

import java.util.HashMap;

public enum Hook {

	/**
	 * Received whenever the server receives a status request <br>
	 * <br>
	 * 
	 * Parameter 0: The server identifier <code>Type: java.lang.String</code>
	 * <br>
	 * Parameter 1: The address of who is requesting the status
	 * <code>Type: java.net.InetAddress</code>
	 */
	SERVER_PING,

	/**
	 * Received whenever the server receives a legacy status request <br>
	 * <br>
	 * 
	 * Parameter 0: The server broadcast data
	 * <code>Type: java.lang.String</code><br>
	 * Parameter 1: The address of who is requesting the status
	 * <code>Type: java.net.InetAddress</code>
	 */
	LEGACY_PING,

	/**
	 * Received whenever a client is officially connected to a server <br>
	 * <br>
	 * Parameter 0: The client's address
	 * <code>Type: port.raknet.java.net.ClietnSession</code><br>
	 * Parameter 1: The time the client connected <code>Type: long</code>
	 */
	CLIENT_CONNECTED,

	/**
	 * Received whenever a client disconnects from the server <br>
	 * <br>
	 * Parameter 0: The client's address
	 * <code>Type: port.raknet.java.net.ClientSession</code><br>
	 * Parameter 1: The reason the client was disconnected
	 * <code>Type: java.lang.String</code><br>
	 * Parameter 2: The time the client disconnected <code>Type: long</code>
	 */
	CLIENT_DISCONNECTED,

	/**
	 * Received whenever a packet is received<br>
	 * <br>
	 * 
	 * Parameter 0: The packet ID <code>Type: short</code><br>
	 * Parameter 1: The packet
	 * <code>Type: port.raknet.java.protocol.Packet</code><br>
	 * Parameter 2: The ClientSession that sent the packet
	 * <code>Type: port.raknet.java.net.ClientSession</code>
	 */
	PACKET_RECEIVED;

	public static HashMap<Hook, HookRunnable> getHooks() {
		HashMap<Hook, HookRunnable> hooks = new HashMap<Hook, HookRunnable>();
		for (Hook hook : Hook.values()) {
			hooks.put(hook, new HookRunnable() {
				@Override
				public void run(Object... parameters) {
				}
			});
		}
		return hooks;
	}

}
