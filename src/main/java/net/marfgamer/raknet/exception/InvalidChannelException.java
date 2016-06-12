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
package net.marfgamer.raknet.exception;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;

/**
 * Occurs whenever the handler receives a message from the wrong channel,
 * supposedly one that has been closed and is no longer being used by the
 * client.
 *
 * @author Trent Summerlin
 */
public class InvalidChannelException extends RakNetException {

	private static final long serialVersionUID = 1338007126886687551L;

	private final Channel receivedChannel;
	private final Channel expectedChannel;

	public InvalidChannelException(Channel receivedChannel, Channel expectedChannel) {
		super("Received message from the wrong channel! Should be with port " + getLocalPort(expectedChannel)
				+ " but got " + getLocalPort(receivedChannel));
		this.receivedChannel = receivedChannel;
		this.expectedChannel = expectedChannel;
	}

	public Channel getReceivedChannel() {
		return this.receivedChannel;
	}

	public Channel getExpectedChanenl() {
		return this.expectedChannel;
	}

	/**
	 * Returns the local port of a channel
	 * 
	 * @param channel
	 * @return int
	 */
	private static int getLocalPort(Channel channel) {
		return ((InetSocketAddress) channel.localAddress()).getPort();
	}

	@Override
	public String getLocalizedMessage() {
		return "Received message on wrong channel";
	}

}
