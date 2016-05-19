package port.raknet.java.client;

public class RakNetClientThread extends Thread {

	private final RakNetClient client;

	public RakNetClientThread(RakNetClient client) {
		this.client = client;
	}

	@Override
	public void run() {
		client.startClient();
	}
	
}
