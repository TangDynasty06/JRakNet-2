package port.raknet.java.client.task;

import port.raknet.java.client.RakNetClient;
import port.raknet.java.protocol.raknet.StatusRequest;

public class AdvertiseTask implements Runnable {

	private final RakNetClient client;
	private long pingId = 0L;

	public AdvertiseTask(RakNetClient client) {
		this.client = client;
	}

	@Override
	public void run() {
		StatusRequest request = new StatusRequest();
		request.pingId = pingId++;
		request.encode();

		client.broadcastRaw(request);
	}

}
