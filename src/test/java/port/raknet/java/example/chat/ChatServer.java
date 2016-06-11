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
package port.raknet.java.example.chat;

import java.util.HashMap;

import port.raknet.java.RakNetOptions;
import port.raknet.java.event.Hook;
import port.raknet.java.example.chat.handler.ChatClientDisconnectHandler;
import port.raknet.java.example.chat.handler.ChatClientPacketHandler;
import port.raknet.java.example.chat.session.ChatClientSession;
import port.raknet.java.exception.RakNetException;
import port.raknet.java.server.RakNetServer;
import port.raknet.java.session.RakNetSession;

/**
 * Used to handle chat messages from <code>ChatClient</code> and send them to
 * the other connected clients
 *
 * @author Trent Summerlin
 */
public class ChatServer {

	private final int port;
	private final HashMap<RakNetSession, ChatClientSession> sessions;

	public ChatServer(int port) {
		this.port = port;
		this.sessions = new HashMap<RakNetSession, ChatClientSession>();
	}

	/**
	 * Checks if the server has a <code>ChatClientSession</code> assigned to the
	 * specified <code>RakNetSession</code>
	 * 
	 * @param session
	 * @return boolean
	 */
	public boolean hasSession(RakNetSession session) {
		return sessions.containsKey(session);
	}

	/**
	 * Checks if any of the <code>ChatClientSession's</code> have the specified
	 * username
	 * 
	 * @param username
	 * @return boolean
	 */
	public boolean hasUsername(String username) {
		for (ChatClientSession session : sessions.values()) {
			if (session.getUsername().equalsIgnoreCase(username)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds a <code>ChatClientSession</code> assigned to the specified
	 * RakNetSession
	 * 
	 * @param session
	 * @param client
	 */
	public void addSession(RakNetSession session, ChatClientSession client) {
		sessions.put(session, client);
	}

	/**
	 * Removes the <code>ChatClientSession</code> assigned to the specified
	 * <code>RakNetSession</code>
	 * 
	 * @param session
	 */
	public void removeSession(RakNetSession session) {
		sessions.remove(session);
	}

	/**
	 * Returns the <code>ChatClientSession</code> assigned to the specified
	 * <code>RakNetSession</code>
	 * 
	 * @param session
	 * @return ChatClientSession
	 */
	public ChatClientSession getSession(RakNetSession session) {
		return sessions.get(session);
	}

	/**
	 * Broadcasts a message to every client and prints it out to the console
	 * 
	 * @param message
	 */
	public void broadcastMessage(String message) {
		for (ChatClientSession session : sessions.values()) {
			session.sendMessage(message);
		}
		System.out.println(message);
	}

	/**
	 * Starts the server
	 * 
	 * @throws RakNetException
	 */
	public void startServer() throws RakNetException {
		// Set options
		RakNetOptions options = new RakNetOptions();
		options.serverPort = this.port;

		// Create and start server
		RakNetServer server = new RakNetServer(options);
		server.addHook(Hook.PACKET_RECEIVED, new ChatClientPacketHandler(this));
		server.addHook(Hook.SESSION_DISCONNECTED, new ChatClientDisconnectHandler(this));
		server.start();
		System.out.println("Started server on port " + options.serverPort + "!");
	}

	public static void main(String[] args) throws RakNetException {
		ChatServer server = new ChatServer(30851);
		server.startServer();
	}

}
