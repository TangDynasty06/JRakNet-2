package port.raknet.java.example.chat.handler;

import port.raknet.java.event.Hook;
import port.raknet.java.event.HookRunnable;
import port.raknet.java.example.chat.protocol.ChatPacket;
import port.raknet.java.example.chat.protocol.Info;
import port.raknet.java.example.chat.protocol.KickPacket;
import port.raknet.java.protocol.Packet;
import port.raknet.java.protocol.raknet.internal.EncapsulatedPacket;

public class ChatServerHandler implements HookRunnable, Info {

	@Override
	public void run(Hook hook, Object... parameters) {
		if (hook == Hook.PACKET_RECEIVED) {
			EncapsulatedPacket encapsulated = (EncapsulatedPacket) parameters[1];
			Packet packet = encapsulated.convertPayload();
			short pid = packet.getId();

			if (pid == ID_CHAT) {
				ChatPacket chat = new ChatPacket(packet);
				chat.decode();
				System.out.println(chat.message);
			} else if (pid == ID_KICK) {
				KickPacket kick = new KickPacket(packet);
				kick.decode();
				System.out.println("Kicked from server! (" + kick.reason + ")");
				System.exit(0);
			}
		} else if (hook == Hook.SESSION_DISCONNECTED) {
			System.out.println("Lost connection to server!");
			System.exit(0);
		}
	}

}
