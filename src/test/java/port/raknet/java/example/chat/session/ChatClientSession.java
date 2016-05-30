package port.raknet.java.example.chat.session;

import java.util.ArrayList;

import port.raknet.java.example.chat.protocol.ChatPacket;
import port.raknet.java.example.chat.protocol.KickPacket;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.session.RakNetSession;

public class ChatClientSession {

	public static final int LOGIN = 0;
	public static final int CHAT = 1;

	private final String username;
	private final RakNetSession session;
	private final ArrayList<String> chatHistory;

	public ChatClientSession(String username, RakNetSession session) {
		this.username = username;
		this.session = session;
		this.chatHistory = new ArrayList<String>();
	}

	public String getUsername() {
		return this.username;
	}

	public String[] getChatHistory() {
		return chatHistory.toArray(new String[chatHistory.size()]);
	}

	public void addChatMessage(String message) {
		chatHistory.add(message);
	}

	public void sendMessage(String message) {
		ChatPacket chat = new ChatPacket();
		chat.message = message;
		chat.encode();
		session.sendPacket(Reliability.RELIABLE, chat);
	}

	public void disconnect(String reason) {
		KickPacket kick = new KickPacket();
		kick.reason = reason;
		kick.encode();
		session.sendPacket(Reliability.RELIABLE, kick);
	}

}
