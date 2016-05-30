package port.raknet.java.example.chat;

import java.util.Scanner;

import port.raknet.java.RakNetOptions;
import port.raknet.java.client.RakNetClient;
import port.raknet.java.event.Hook;
import port.raknet.java.example.chat.handler.ChatServerHandler;
import port.raknet.java.example.chat.protocol.ChatPacket;
import port.raknet.java.example.chat.protocol.LoginPacket;
import port.raknet.java.example.chat.protocol.QuitPacket;
import port.raknet.java.exception.RakNetException;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.session.ServerSession;

public class ChatClient {

	private final String username;
	private final RakNetClient client;

	public ChatClient(String username) {
		this.username = username;
		this.client = new RakNetClient(new RakNetOptions());
	}

	public void sendChatMessage(String message) {
		ServerSession session = client.getSession();
		if (session != null) {
			ChatPacket chat = new ChatPacket();
			chat.message = message;
			chat.encode();

			session.sendPacket(Reliability.RELIABLE, chat);
		}
	}

	public void connect(String address, int port) throws RakNetException {
		ChatServerHandler handler = new ChatServerHandler();
		client.connect(address, port);
		client.addHook(Hook.PACKET_RECEIVED, handler);
		client.addHook(Hook.SESSION_DISCONNECTED, handler);

		LoginPacket login = new LoginPacket();
		login.username = this.username;
		login.encode();
		client.getSession().sendPacket(Reliability.RELIABLE, login);
	}

	public void quit() {
		ServerSession session = client.getSession();
		if (session != null) {
			session.sendPacket(Reliability.RELIABLE, new QuitPacket());
		}
		System.exit(0);
	}

	public static void main(String[] args) throws RakNetException {
		@SuppressWarnings("resource")
		Scanner input = new Scanner(System.in);
		System.out.print("Enter your username: ");
		while (!input.hasNextLine())
			;
		String username = input.nextLine();

		System.out.print("Enter server address: ");
		while (!input.hasNextLine())
			;
		String address = input.nextLine();

		System.out.print("Enter server port: ");
		while (!input.hasNextLine())
			;
		int port = Integer.parseInt(input.nextLine());

		ChatClient client = new ChatClient(username);
		client.connect(address, port);
		while (true) {
			while (!input.hasNextLine())
				;
			String message = input.nextLine();
			if (message.startsWith("/")) {
				message = message.substring(1);
				if (message.equalsIgnoreCase("quit")) {
					client.quit();
				} else {
					System.out.println("Unknown command!");
				}
			} else {
				client.sendChatMessage(message);
			}
		}
	}

}
