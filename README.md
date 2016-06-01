# JRakNet
JRakNet is a networking library for Java which implements the UDP based protocol [RakNet](https://github.com/OculusVR/RakNet).
This library was meant to be used for Minecraft: Pocket Edition servers and clients, but can still be used to create game servers and clients for other video games with ease.

# How to create a server

```java
// Create options and set identifier
RakNetOptions options = new RakNetOptions();
options.serverIdentifier = "MCPE;A RakNet Server;70;0.14.3;0;10";

// Create server and add hooks
RakNetServer server = new RakNetServer(options);

// Client connected
server.addHook(Hook.SESSION_CONNECTED, new HookRunnable() {
  @Override
	public void run(Object... parameters) {
		RakNetSession session = (RakNetSession) parameters[0];
		System.out.println("Client from address " + session.getSocketAddress() + " has connected to the server");
	}
});

// Client disconnected
server.addHook(Hook.SESSION_DISCONNECTED, new HookRunnable() {
	@Override
	public void run(Object... parameters) {
		RakNetSession session = (RakNetSession) parameters[0];
		String reason = parameters[1].toString();
		System.out.println("Client from address " + session.getSocketAddress() + " has disconnected from the server for the reason \"" + reason + "\"");
	}
});

// Start server
server.startServer();
```
This can be tested using a Minecraft: Pocket Edition client, simply launch the game and click on "Play". Then, "A RakNet Server" should pop up, just like when someone else is playing on the same network and their name pops up.


# How to create a client

```java
// Server address and port
String address = "sg.lbsg.net";
int port = 19132;

// There are no special options needed for clients
RakNetClient client = new RakNetClient(new RakNetOptions());

// Server connected
client.addHook(Hook.SESSION_CONNECTED, new HookRunnable() {

	@Override
	public void run(Object... parameters) {
		RakNetSession session = (RakNetSession) parameters[0];
		System.out.println("Connected to server with address " + session.getSocketAddress());
	}

});

// Server disconnected
client.addHook(Hook.SESSION_DISCONNECTED, new HookRunnable() {

	@Override
	public void run(Object... parameters) {
		RakNetSession session = (RakNetSession) parameters[0];
		String reason = parameters[1].toString();
		System.out.println("Disconnected from server with address " + session.getSocketAddress() + " for the reason \"" + reason + "\"");
	}

});

// Attempt to connect to server
client.connect(new InetSocketAddress(address, port));
while(client.getState() != SessionState.CONNECTED); // Wait for client to connect before cancelling connection
client.cancelConnect(); // Will change to client.disconnect() in next release
```
This example attempts to connect to the main [LBSG](http://lbsg.net/) server. When it is connected, it closes the connection and shuts down.

# Notes
Some DataPacket ID's are reserved by RakNet, these ID's currently consist of 0x00, 0x03, 0x09, 0x10, 0x13, and 0x15. It is recommended that all ID's for game servers and game clients utilizing this library uses ID's for DataPackets greater than 0x1E. For raw packets, RakNet reserves 0x01, 0x02, 0x05, 0x06, 0x07, 0x08, 0x1A, 0x1C, 0x1D, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F, 0xA0, and 0xC0. It is recommended that game servers and game clients do not use raw packets at all, and instead use DataPackets only.

![JRakNet Banner](http://i.imgur.com/t897jIS.png)
