package utils;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Utility class for network operations such as retrieving local IP and broadcast addresses.
 */
public class NetworkUtils {

    /**
     * Finds the broadcast address of the primary active non-loopback network interface.
     * Useful for sending UDP broadcast packets to the local network.
     * 
     * @return InetAddress representing the broadcast address, or null if not found
     * @throws SocketException on network error
     */
    public static InetAddress getBroadcastAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast != null) {
                    return broadcast;
                }
            }
        }
        return null;
    }

    /**
     * Gets the local non-loopback IP address (IPv4 preferred).
     * 
     * @return InetAddress of the local machine on the active interface
     * @throws SocketException on network error
     */
    public static InetAddress getLocalIPAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof java.net.Inet4Address) {
                    return addr;
                }
            }
        }
        return InetAddress.getLoopbackAddress(); // Fallback to loopback if no external IP found
    }
}
