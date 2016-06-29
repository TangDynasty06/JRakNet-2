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
package net.marfgamer.raknet.protocol.raknet.internal;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.marfgamer.raknet.protocol.Message;

public class Acknowledge extends Message implements Bytable {

	public int[] packets;

	public Acknowledge(Message packet) {
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
		List<Integer> packets = new ArrayList<>();
		int cnt = 0;
		for (int i = 0; i < count && this.remaining() > 0 && cnt < 4096; i++) {
			if (!this.getBoolean()) {
				int start = this.getLTriad();
				int end = this.getLTriad();
				if ((end - start) > 512) {
					end = start + 512;
				}
				for (int c = start; c <= end; c++) {
					cnt = cnt + 1;
					packets.add(c);
				}
			} else {
				packets.add(this.getLTriad());
			}
		}

		// Manually set values
		this.packets = new int[packets.size()];
		for (int i = 0; i < packets.size(); i++) {
			this.packets[i] = packets.get(i).intValue();
		}
	}

}
