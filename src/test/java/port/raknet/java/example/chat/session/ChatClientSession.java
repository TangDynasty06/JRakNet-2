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
package port.raknet.java.example.chat.session;

import java.util.ArrayList;

import port.raknet.java.example.chat.protocol.ChatPacket;
import port.raknet.java.example.chat.protocol.KickPacket;
import port.raknet.java.protocol.Reliability;
import port.raknet.java.session.RakNetSession;

/**
 * Used to handle chat clients
 *
 * @author Trent Summerlin
 */
public class ChatClientSession {

	private final String username;
	private final RakNetSession session;
	private final ArrayList<String> chatHistory;

	public ChatClientSession(String username, RakNetSession session) {
		this.username = username;
		this.session = session;
		this.chatHistory = new ArrayList<String>();
	}

	/**
	 * Returns the client's username
	 * 
	 * @return String
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Returns every message the client has sent to the server
	 * 
	 * @return String[]
	 */
	public String[] getChatHistory() {
		return chatHistory.toArray(new String[chatHistory.size()]);
	}

	/**
	 * Adds a chat message to the chat history
	 * 
	 * @param message
	 */
	public void addChatMessage(String message) {
		chatHistory.add(message);
	}

	/**
	 * Sends a chat message to the client
	 * 
	 * @param message
	 */
	public void sendMessage(String message) {
		ChatPacket chat = new ChatPacket();
		chat.message = message;
		chat.encode();
		session.sendPacket(Reliability.RELIABLE, chat);
	}

	/**
	 * Disconnects the client from the server with the specified reason
	 * 
	 * @param reason
	 */
	public void disconnect(String reason) {
		KickPacket kick = new KickPacket();
		kick.reason = reason;
		kick.encode();
		session.sendPacket(Reliability.RELIABLE, kick);
	}

}
