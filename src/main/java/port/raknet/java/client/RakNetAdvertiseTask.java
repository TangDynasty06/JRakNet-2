package port.raknet.java.client;

import java.util.HashMap;

import port.raknet.java.protocol.raknet.UnconnectedPing;

/**
 * Used by the client to <strike>activate the batsignal</strike> <i>broadcast an
 * unconnected ping to the network</i>
 *
 * @author Trent Summerlin
 */
public class RakNetAdvertiseTask implements Runnable {

	public static final long TICK = 1000L;
	private static final long ADVERTISER_TIMEOUT = (TICK * 3);

	private final RakNetClient client;
	private final HashMap<Long, Long> timeouts;
	private long pingId = 0L;

	public RakNetAdvertiseTask(RakNetClient client) {
		this.client = client;
		this.timeouts = new HashMap<Long, Long>();
	}

	@Override
	public void run() {
		// Send ping
		UnconnectedPing ping = new UnconnectedPing();
		ping.pingId = pingId++;
		ping.encode();
		client.broadcastRaw(ping);

		// Make sure servers are still advertising
		for (long serverId : client.getAdvertisers()) {
			if (timeouts.containsKey(serverId)) {
				long time = timeouts.get(serverId);
				if (time <= 0) {
					client.removeAdvertiser(serverId);
				} else {
					timeouts.put(serverId, time -= TICK);
				}
			} else {
				timeouts.put(serverId, ADVERTISER_TIMEOUT);
			}
		}
	}

}
