package port.raknet.java.example.chat.handler;

import port.raknet.java.event.HookRunnable;
import port.raknet.java.example.chat.ChatServer;
import port.raknet.java.example.chat.session.ChatClientSession;
import port.raknet.java.session.RakNetSession;

/**
 * Handles all client disconnections
 *
 * @author Trent Summerlin
 */
public class ChatClientDisconnectHandler implements HookRunnable {

	private final ChatServer server;

	public ChatClientDisconnectHandler(ChatServer server) {
		this.server = server;
	}
	
	@Override
	public void run(Object... parameters) {
		RakNetSession session = (RakNetSession) parameters[0];
		String reason = parameters[1].toString();
		if (server.hasSession(session)) {
			ChatClientSession client = server.getSession(session);
			server.broadcastMessage(client.getUsername() + " has been kicked from the server due to: " + reason);
			server.removeSession(session);
		}
	}

}
