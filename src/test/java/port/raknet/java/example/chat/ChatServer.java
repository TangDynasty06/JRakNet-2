package port.raknet.java.example.chat;

import java.util.HashMap;

import port.raknet.java.RakNetOptions;
import port.raknet.java.event.Hook;
import port.raknet.java.example.chat.handler.ChatClientHandler;
import port.raknet.java.example.chat.session.ChatClientSession;
import port.raknet.java.exception.RakNetException;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.session.RakNetSession;

public class ChatServer {

	private final int port;
	private final HashMap<RakNetSession, ChatClientSession> sessions;

	public ChatServer(int port) {
		this.port = port;
		this.sessions = new HashMap<RakNetSession, ChatClientSession>();
	}

	public boolean hasSession(RakNetSession session) {
		return sessions.containsKey(session);
	}

	public boolean hasUsername(String username) {
		for (ChatClientSession session : sessions.values()) {
			if (session.getUsername().equalsIgnoreCase(username)) {
				return true;
			}
		}
		return false;
	}

	public void addSession(RakNetSession session, ChatClientSession client) {
		sessions.put(session, client);
	}

	public void removeSession(RakNetSession session) {
		sessions.remove(session);
	}

	public ChatClientSession getSession(RakNetSession session) {
		return sessions.get(session);
	}

	public void broadcastMessage(String message) {
		for (ChatClientSession session : sessions.values()) {
			session.sendMessage(message);
		}
		System.out.println(message);
	}

	public void startServer() throws RakNetException {
		// Set options
		RakNetOptions options = new RakNetOptions();
		options.serverPort = this.port;

		// Create and start server
		RakNetServer server = new RakNetServer(options);
		ChatClientHandler handler = new ChatClientHandler(this);
		server.addHook(Hook.PACKET_RECEIVED, handler);
		server.addHook(Hook.SESSION_DISCONNECTED, handler);
		server.startServer();
		System.out.println("Started server on port " + options.serverPort + "!");
	}

	public static void main(String[] args) throws RakNetException {
		ChatServer server = new ChatServer(30851);
		server.startServer();
	}

}
