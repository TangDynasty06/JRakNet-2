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
package net.marfgamer.raknet.latency;

import javax.swing.JFrame;
import javax.swing.JTextPane;

import net.marfgamer.raknet.session.ClientSession;

/**
 * The frame used by <code>RakNetLatencyTest</code> to display the latency of
 * each client by their address
 *
 * @author Trent Summerlin
 */
public class RakNetLatencyFrame extends JFrame {

	private static final long serialVersionUID = -4981385799018471709L;

	private final JTextPane infoPane;
	private final JTextPane serverPane;

	public RakNetLatencyFrame() {
		// Set window options
		this.setSize(450, 300);
		this.setResizable(false);
		this.setTitle("Client latencies");
		this.getContentPane().setLayout(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Tells what the user what this window does
		this.infoPane = new JTextPane();
		infoPane.setText("These are the latencies of every connected client");
		infoPane.setBounds(10, 10, 425, 20);
		getContentPane().add(infoPane);

		// Displays all the server
		this.serverPane = new JTextPane();
		serverPane.setEditable(false);
		serverPane.setBounds(10, 40, 425, 220);
		getContentPane().add(serverPane);
	}

	/**
	 * Sets the displayed servers on the frame
	 * 
	 * @param servers
	 */
	public void setClients(ClientSession[] clients) {
		StringBuilder serverChars = new StringBuilder();
		for (int i = 0; i < clients.length; i++) {
			ClientSession session = clients[i];
			serverChars.append(session.getSocketAddress() + " - " + session.getLatency() + "\n");
		}
		serverPane.setText(serverChars.toString());
	}

}
