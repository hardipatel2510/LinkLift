package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;
import utils.LoggerConfig;
import utils.NetworkUtils;

/**
 * Periodically broadcasts a UDP packet on the LAN so that clients can auto-discover the file server.
 */
public class UDPBroadcastService implements Runnable {
    private static final Logger logger = LoggerConfig.getLogger(UDPBroadcastService.class);
    // Standard port for the client to listen on
    public static final int DISCOVERY_PORT = 8888;
    private static final long BROADCAST_INTERVAL_MS = 2000;

    private final int tcpPort;
    private volatile boolean running = true;

    public UDPBroadcastService(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public void stopService() {
        this.running = false;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            InetAddress broadcastAddress = NetworkUtils.getBroadcastAddress();
            
            if (broadcastAddress == null) {
                logger.warning("Could not find a valid subnet broadcast address. Falling back to global broadcast.");
                broadcastAddress = InetAddress.getByName("255.255.255.255");
            }
            
            String message = "LAN_FILE_SERVER:" + tcpPort;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);

            logger.info("Starting UDP Discovery broadcast targeting " + broadcastAddress.getHostAddress() + ":" + DISCOVERY_PORT);

            while (running) {
                try {
                    socket.send(packet);
                    // Using fine log level to avoid spamming the console
                    logger.fine("Broadcasted server presence: " + message);
                    Thread.sleep(BROADCAST_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Broadcast thread interrupted.");
                    break;
                } catch (IOException e) {
                    logger.warning("Failed to send broadcast packet: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe("Could not initialize UDP Broadcast service: " + e.getMessage());
        }
        logger.info("UDP Broadcast Service stopped.");
    }
}
