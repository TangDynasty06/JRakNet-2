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
package port.raknet.java.example.chat.handler;

import port.raknet.java.event.HookRunnable;
import port.raknet.java.example.chat.protocol.Info;
import port.raknet.java.example.chat.protocol.KickPacket;
import port.raknet.java.example.chat.protocol.MessagePacket;
import port.raknet.java.exception.UnexpectedPacketException;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;

/**
 * Used to handle packets from the chat server
 *
 * @author Trent Summerlin
 */
public class ChatServerPacketHandler implements HookRunnable, Info {

	@Override
	public void run(Object... parameters) {
		EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];
		Packet packet = encapsulated.convertPayload();

		if (packet.getId() == ID_IDENTIFIER) {
			try {
				short pid = packet.getUByte();

				if (pid == ID_CHAT) {
					MessagePacket chat = new MessagePacket(packet);
					chat.decode();
					System.out.println(chat.message);
				} else if (pid == ID_KICK) {
					KickPacket kick = new KickPacket(packet);
					kick.decode();
					System.out.println("Kicked from server! (" + kick.reason + ")");
					System.exit(0);
				}
			} catch (UnexpectedPacketException e) {
				System.out.println(
						"Received packet with wrong identifier (" + e.getRequiredString() + ")" + " dropping...");
			}
		}
	}

}
