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
package net.marfgamer.raknet.event;

/**
 * Executed when a certain event occurs within the server that might be
 * desirable by the programmer to change it's outcome
 *
 * @author Trent Summerlin
 */
public enum Hook {

	/**
	 * Received whenever the server receives a status request <br>
	 * <br>
	 * 
	 * Parameter 0: The address of who is requesting the status (InetAddress)
	 * <br>
	 * Parameter 1: The broadcast message (String)
	 */
	SERVER_PING,

	/**
	 * Received whenever the server receives a legacy status request <br>
	 * <br>
	 * 
	 * Parameter 0: The address of who is requesting the status (InetAddress)
	 * <br>
	 * Parameter 1: The broadcast message (String)
	 */
	LEGACY_PING,

	/**
	 * Received whenever a server is discovered by the client<br>
	 * <br>
	 * 
	 * Parameter 0: The discovered server (DiscoveredRakNetServer)<br>
	 * Parameter 1: The address of the discovered server (InetSocketAddress)<br>
	 * Parameter 2: The time the server was discovered (long)
	 */
	SERVER_DISCOVERED,

	/**
	 * Received whenever a server that was discovered can no longer be found on
	 * the local network<br>
	 * <br>
	 * 
	 * Parameter 0: The undiscovered server (DiscoveredRakNetServer)<br>
	 * Parameter 1: The address of the undiscovered server (InetSocketAddress)
	 * <br>
	 * Parameter 2: The time the server lost connection (long)
	 */
	SERVER_UNDISCOVERED,

	/**
	 * Received whenever a session is officially connected <br>
	 * <br>
	 * Parameter 0: The RakNetSession (RakNetSession)<br>
	 * Parameter 1: The time the session connected (long)
	 */
	SESSION_CONNECTED,

	/**
	 * Received whenever a session disconnects <br>
	 * <br>
	 * 
	 * Parameter 0: The RakNetSession (RakNetSession)<br>
	 * Parameter 1: The reason the session was disconnected (String)<br>
	 * Parameter 2: The time the session disconnected (long)
	 */
	SESSION_DISCONNECTED,

	/**
	 * Received whenever an address has been blocked by the server handler<br>
	 * <br>
	 * 
	 * Parameter 0: The blocked client (BlockedClient)<br>
	 * Parameter 1: The time the client was blocked (long)
	 */
	ADDRESS_BLOCKED,

	/**
	 * Received whenever an address has been unblocked by the server handler<br>
	 * <br>
	 * 
	 * Parameter 0: The now unblocked client (BlockedClient)<br>
	 * Parameter 1: The time the client was unblocked (long)
	 */
	ADDRESS_UNBLOCKED,

	/**
	 * Received whenever a packet is received<br>
	 * <br>
	 * 
	 * Parameter 0: The RakNetSession (RakNetSession)<br>
	 * Parameter 1: The EncapsulatedPacket (EncapsulatedPacket)
	 */
	PACKET_RECEIVED,

	/**
	 * Received whenever a Netty handler exception occurs<br>
	 * <br>
	 * 
	 * Parameter 0: The Exception (Exception)<br>
	 * Parameter 1: The ChannelHandlerContext (ChannelHandlerContext)<br>
	 * Parameter 2: The time the error occurred (long)
	 */
	HANDLER_EXCEPTION_OCCURED;

}
