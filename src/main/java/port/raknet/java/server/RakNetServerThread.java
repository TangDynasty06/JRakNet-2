package port.raknet.java.server;

public class RakNetServerThread extends Thread {

	private final RakNetServer server;

	public RakNetServerThread(RakNetServer server) {
		this.server = server;
	}

	@Override
	public void run() {
		server.startServer();
	}
	
}
