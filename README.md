# JRakNet
JRakNet is a networking library for Java which implements the UDP based protocol [RakNet](https://github.com/OculusVR/RakNet).
This library was meant to be used for Minecraft: Pocket Edition servers and clients, but can still be used to create game servers and clients for other video games with ease.

| Protocol Info      | Version |
| -------------------|:-------:|
| Current Protocol   | 8       |
| Supported Protocol | 7       |

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
Some DataPacket ID's are reserved by RakNet. Because of this, it is recommended that all game packets not relating to RakNet begin with their own special ID, Minecraft: Pocket Edition does this. It is also recommended that game servers and game clients do not use raw packets at all.

![JRakNet Banner](http://i.imgur.com/t897jIS.png)
