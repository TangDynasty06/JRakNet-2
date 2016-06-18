/*
 *       _   _____            _      _   _          _   
 *      | | |  __ \          | |    | \ | |        | |  
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_ 
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_ 
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *                                                  
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Trent Summerlin

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 */
package net.marfgamer.raknet.example.chat;

import java.util.Scanner;

import net.marfgamer.raknet.RakNetOptions;
import net.marfgamer.raknet.client.RakNetClient;
import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.example.chat.handler.ChatServerDisconnectHandler;
import net.marfgamer.raknet.example.chat.handler.ChatServerPacketHandler;
import net.marfgamer.raknet.example.chat.protocol.LoginPacket;
import net.marfgamer.raknet.example.chat.protocol.MessagePacket;
import net.marfgamer.raknet.example.chat.protocol.QuitPacket;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.protocol.Reliability;
import net.marfgamer.raknet.session.ServerSession;

/**
 * Used to connect to a <code>ChatServer</code> as well as sending chat messages
 * to it along with receive messages from it
 *
 * @author Trent Summerlin
 */
public class ChatClient {

	private final String username;
	private final RakNetClient client;

	public ChatClient(String username) throws RakNetException {
		this.username = username;
		this.client = new RakNetClient(new RakNetOptions());
	}

	/**
	 * Sends a chat message to the server
	 * 
	 * @param message
	 */
	public void sendChatMessage(String message) {
		ServerSession session = client.getSession();
		if (session != null) {
			MessagePacket chat = new MessagePacket();
			chat.message = message;
			chat.encode();

			session.sendPacket(Reliability.RELIABLE, chat);
		}
	}

	/**
	 * Connects the client to the server
	 * 
	 * @param address
	 * @param port
	 * @throws RakNetException
	 * @throws InterruptedException
	 */
	public void connect(String address, int port) throws RakNetException, InterruptedException {
		client.connect(address, port);
		client.addHook(Hook.PACKET_RECEIVED, new ChatServerPacketHandler());
		client.addHook(Hook.SESSION_DISCONNECTED, new ChatServerDisconnectHandler());

		LoginPacket login = new LoginPacket();
		login.username = this.username;
		login.encode();
		client.getSession().sendPacket(Reliability.RELIABLE, login);
	}

	/**
	 * Disconnects from the server
	 */
	public void quit() {
		ServerSession session = client.getSession();
		if (session != null) {
			session.sendPacket(Reliability.RELIABLE, new QuitPacket());
		}
		System.exit(0);
	}

	public static void main(String[] args) throws RakNetException, InterruptedException {
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
