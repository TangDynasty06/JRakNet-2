package port.raknet.java.event;

import java.util.HashMap;

public enum Hook {

	/**
	 * Received whenever the server receives a status request <br>
	 * <br>
	 * 
	 * Parameter 0: The server identifier <code>Type: java.lang.String</code><br>
	 * Parameter 1: The address of who is requesting the status <code>Type: java.net.InetAddress</code>
	 */
	SERVER_PING,
	
	/**
	 * Received whenever the server receives a legacy status request <br>
	 * <br>
	 * 
	 * Parameter 0: The server broadcast data <code>Type: java.lang.String</code><br>
	 * Parameter 1: The address of who is requesting the status <code>Type: java.net.InetAddress</code>
	 */
	LEGACY_PING,

	CLIENT_CONNECTED,

	CLIENT_DISCONNECTED,

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
