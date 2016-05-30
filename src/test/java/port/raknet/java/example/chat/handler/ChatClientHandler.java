package port.raknet.java.example.chat.handler;

import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.example.chat.ChatServer;
import port.raknet.java.example.chat.protocol.ChatPacket;
import port.raknet.java.example.chat.protocol.Info;
import port.raknet.java.example.chat.protocol.KickPacket;
import port.raknet.java.example.chat.protocol.LoginPacket;
import port.raknet.java.example.chat.session.ChatClientSession;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;
import port.raknet.java.session.RakNetSession;

public class ChatClientHandler implements HookRunnable, Info {

	private final ChatServer server;

	public ChatClientHandler(ChatServer server) {
		this.server = server;
	}

	@Override
	public void run(Hook hook, Object... parameters) {
		if (hook == Hook.PACKET_RECEIVED) {
			RakNetSession session = (RakNetSession) parameters[0];
			EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];
			Packet packet = encapsulated.convertPayload();
			short pid = packet.getId();

			ChatClientSession client = server.getSession(session);
			if (pid == ID_LOGIN) {
				if (!server.hasSession(session)) {
					LoginPacket login = new LoginPacket(packet);
					login.decode();

					if (!server.hasUsername(login.username)) {
						server.addSession(session, client = new ChatClientSession(login.username, session));
						server.broadcastMessage(client.getUsername() + " has joined the chatroom!");
					} else {
						KickPacket kick = new KickPacket();
						kick.reason = "Client with username already exists!";
						kick.encode();
						session.sendPacket(Reliability.RELIABLE, kick);
					}
				}
			} else if (pid == ID_CHAT) {
				if (server.hasSession(session)) {
					ChatPacket chat = new ChatPacket(packet);
					chat.decode();

					client.addChatMessage(chat.message);
					server.broadcastMessage(client.getUsername() + ": " + chat.message);
				}
			} else if (pid == ID_QUIT) {
				if (server.hasSession(session)) {
					server.broadcastMessage(client.getUsername() + " has left the chatroom!");
					server.removeSession(session);
				}
			}
		} else if (hook == Hook.SESSION_DISCONNECTED) {
			RakNetSession session = (RakNetSession) parameters[0];
			String reason = parameters[1].toString();
			if (server.hasSession(session)) {
				ChatClientSession client = server.getSession(session);
				server.broadcastMessage(client.getUsername() + " has been kicked from the server due to: " + reason);
				server.removeSession(session);
			}
		}
	}

}
