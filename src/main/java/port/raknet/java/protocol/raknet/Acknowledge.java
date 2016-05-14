package port.raknet.java.protocol.raknet;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import port.raknet.java.protocol.Bytable;
import port.raknet.java.protocol.Packet;

public class Acknowledge extends Packet implements Bytable {

	public int[] packets;

	public Acknowledge(Packet packet) {
		super(packet);
	}

	public Acknowledge(short id) {
		super(id);
	}

	@Override
	public void encode() {
		ByteBuf buf = Unpooled.buffer();
		int count = packets.length;
		int records = 0;

		if (count > 0) {
			int pointer = 0;
			int start = packets[0];
			int last = packets[0];

			while (pointer + 1 < count) {
				int current = packets[pointer++];
				int diff = current - last;
				if (diff == 1) {
					last = current;
				} else if (diff > 1) {
					if (start == last) {
						buf.writeBoolean(true);
						this.writeLTriad(buf, start);
						start = last = current;
					} else {
						buf.writeBoolean(false);
						this.writeLTriad(buf, start);
						this.writeLTriad(buf, last);
						start = last = current;
					}
					records = records + 1;
				}
			}

			if (start == last) {
				buf.writeBoolean(true);
				this.writeLTriad(buf, start);
			} else {
				buf.writeBoolean(false);
				this.writeLTriad(buf, start);
				this.writeLTriad(buf, last);
			}
			records = records + 1;
		}

		this.putUShort(records);
		this.put(buf.array());
	}

	@Override
	public void decode() {
		int count = this.getUShort();
		List<Integer> ack = new ArrayList<>();
		int cnt = 0;
		for (int i = 0; i < count && this.remaining() > 0 && cnt < 4096; i++) {
			if (!this.getBoolean()) {
				int start = this.readLTriad(buffer);
				int end = this.readLTriad(buffer);
				if ((end - start) > 512) {
					end = start + 512;
				}
				for (int c = start; c <= end; c++) {
					cnt = cnt + 1;
					ack.add(c);
				}
			} else {
				ack.add(this.readLTriad(buffer));
			}
		}

		this.packets = new int[ack.size()];
		for (int i = 0; i < packets.length; i++) {
			packets[i] = ack.get(i).intValue();
		}
	}

}
