package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import server.UDPBroadcastService;
import utils.LoggerConfig;

/**
 * Listens for UDP broadcasts from the server to automatically discover the server's IP and Port.
 */
public class ServerDiscoveryService {
    private static final Logger logger = LoggerConfig.getLogger(ServerDiscoveryService.class);

    /**
     * Data object storing discovered server information.
     */
    public static class ServerInfo {
        public final String ipAddress;
        public final int tcpPort;

        public ServerInfo(String ipAddress, int tcpPort) {
            this.ipAddress = ipAddress;
            this.tcpPort = tcpPort;
        }
    }

    /**
     * Listens on the predefined UDP port for the server broadcast.
     * Blocks for a maximum of 10 seconds.
     * 
     * @return ServerInfo object containing details, or null if timeout/error.
     */
    public ServerInfo discoverServer() {
        logger.info("Listening for LAN File Server broadcasts on UDP port " + UDPBroadcastService.DISCOVERY_PORT + "...");
        
        try (DatagramSocket socket = new DatagramSocket(null)) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(UDPBroadcastService.DISCOVERY_PORT));
            socket.setSoTimeout(10000); // 10 seconds timeout
            
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            while (true) {
                try {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    
                    if (message.startsWith("LAN_FILE_SERVER:")) {
                        String[] parts = message.split(":");
                        if (parts.length == 2) {
                            int tcpPort = Integer.parseInt(parts[1]);
                            String serverIp = packet.getAddress().getHostAddress();
                            logger.info("Discovered LAN server at " + serverIp + ":" + tcpPort);
                            return new ServerInfo(serverIp, tcpPort);
                        }
                    }
                } catch (java.net.SocketTimeoutException e) {
                    logger.warning("Timeout while waiting for server broadcast. Make sure the server is running on the LAN.");
                    break;
                } catch (IOException e) {
                    logger.warning("Error while waiting for broadcast: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            logger.severe("Could not start UDP discovery listener: " + e.getMessage());
        }
        
        logger.info("Failed to discover server automatically.");
        return null;
    }
}
