package port.raknet.java.event;

import java.util.HashMap;

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
	 * Received whenever a client is officially connected to a server <br>
	 * <br>
	 * Parameter 0: The ClientSession (ClientSession)<br>
	 * Parameter 1: The time the client connected (long)
	 */
	CLIENT_CONNECTED,

	/**
	 * Received whenever a client disconnects from the server <br>
	 * <br>
	 * 
	 * Parameter 0: The ClientSession (ClientSession)<br>
	 * Parameter 1: The reason the client was disconnected (String)<br>
	 * Parameter 2: The time the client disconnected (long)
	 */
	CLIENT_DISCONNECTED,

	/**
	 * Received whenever a packet is received<br>
	 * <br>
	 * 
	 * Parameter 0: The ClientSession (ClientSession)<br>
	 * Parameter 1: The EncapsulatedPacket (EncapsulatedPacket)
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
