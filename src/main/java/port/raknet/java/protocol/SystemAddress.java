package port.raknet.java.protocol;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SystemAddress {
	private final String ipAddress;
	private final int port;
	private final int version;

	public SystemAddress(String ipAddress, int port, int version) {
		this.ipAddress = ipAddress;
		this.port = port;
		this.version = version;
	}

	public SystemAddress(String ipAddress, int port) {
		this(ipAddress, port, 4);
	}

	public static SystemAddress fromSocketAddress(SocketAddress address) {
		if (address instanceof InetSocketAddress) {
			return new SystemAddress(((InetSocketAddress) address).getHostString(),
					((InetSocketAddress) address).getPort(), 4);
		}
		return null;
	}

	public InetSocketAddress toSocketAddress() {
		return new InetSocketAddress(ipAddress, port);
	}

	public int getPort() {
		return port;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public int getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return ipAddress + ":" + port;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SystemAddress) {
			return ((SystemAddress) obj).getIpAddress().equals(ipAddress) && ((SystemAddress) obj).getPort() == port;
		}
		return obj.equals(this);
	}
}
