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
package net.marfgamer.raknet;

import java.net.InetSocketAddress;
import java.util.Scanner;

import net.marfgamer.raknet.event.Hook;
import net.marfgamer.raknet.exception.RakNetException;
import net.marfgamer.raknet.protocol.raknet.internal.EncapsulatedPacket;
import net.marfgamer.raknet.server.BlockedAddress;
import net.marfgamer.raknet.server.RakNetServer;
import net.marfgamer.raknet.session.RakNetSession;

/**
 * Used to test <code>RakNetServer</code>, meant for testing with Minecraft:
 * Pocket Edition clients <br>
 * <br>
 * Note: If you are unable to connect to the server through Minecraft: Windows
 * 10 edition and you are on the same device as this test is running on, that is
 * not a glitch with JRakNet but rather with Minecraft: Windows 10 edition
 *
 * @author Trent Summerlin
 */
public class RakNetServerTest {

	private static String identifier = "A RakNet Server";

	public static void main(String[] args) throws RakNetException {
		// Create server
		RakNetServer server = new RakNetServer(19132, 5, "MCPE;_IDENTIFIER_;81;0.15.0;_ONLINE_;_MAX_;_SERVERID_");

		// Client connected
		server.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			System.out.println("Client from address " + session.getSocketAddress() + " has connected to the server");
		});

		// Packet received
		server.addHook(Hook.PACKET_RECEIVED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];
			System.out.println("Received packet from client with address " + session.getSocketAddress()
					+ " with packet ID 0x" + Integer.toHexString(encapsulated.convertPayload().getId()).toUpperCase());
		});

		// Client disconnected
		server.addHook(Hook.SESSION_DISCONNECTED, (Object[] parameters) -> {
			RakNetSession session = (RakNetSession) parameters[0];
			String reason = parameters[1].toString();
			System.out.println("Client from address " + session.getSocketAddress()
					+ " has disconnected from the server for the reason \"" + reason + "\"");
		});

		// Server has been pinged
		server.addHook(Hook.SERVER_PING, (Object[] parameters) -> {
			parameters[1] = parameters[1].toString().replace("_IDENTIFIER_", identifier)
					.replace("_ONLINE_", Integer.toString(server.getConnections()))
					.replace("_MAX_", Integer.toString(server.getMaxConnections()))
					.replace("_SERVERID_", Long.toString(server.getServerId()));
		});

		// Address blocked
		server.addHook(Hook.CLIENT_ADDRESS_BLOCKED, (Object[] parameters) -> {
			BlockedAddress address = (BlockedAddress) parameters[0];
			System.out.println("Blocked address " + address.address + " for " + (address.time / 1000L) + " seconds");
		});

		// Client unblocked
		server.addHook(Hook.CLIENT_ADDRESS_UNBLOCKED, (Object[] parameters) -> {
			BlockedAddress address = (BlockedAddress) parameters[0];
			System.out.println("Unblocked address " + address.address);
		});

		// Exception caught
		server.addHook(Hook.HANDLER_EXCEPTION_OCCURED, (Object[] parameters) -> {
			Throwable throwable = (Throwable) parameters[0];
			InetSocketAddress naughtyAddress = (InetSocketAddress) parameters[1];
			System.out.println(
					"Handler exception " + throwable.getClass().getSimpleName() + " caused by " + naughtyAddress);
		});

		// Start server
		server.startThreaded();

		// Wait for input from console
		@SuppressWarnings("resource")
		Scanner s = new Scanner(System.in);
		System.out.println("Type something in and press enter to change the server name!");
		while (true) {
			if (s.hasNextLine()) {
				identifier = s.nextLine();
				if (identifier.length() == 0) {
					identifier = "A RakNet Server";
				}
				System.out.println("Set server name to: " + identifier);
			}
		}
	}

}
