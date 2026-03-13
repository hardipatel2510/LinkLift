package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import utils.LoggerConfig;

/**
 * Main entry point for the LAN File Server.
 * Sets up the threadpool, starts the UDP broadcast service, and listens for TCP client connections.
 */
public class FileServer {
    private static final Logger logger = LoggerConfig.getLogger(FileServer.class);
    
    // Choose a standard port for the server
    private static final int TCP_PORT = 9000;
    // Standard port for the web interface
    private static final int WEB_PORT = 8080;
    // Maximum concurrent client connections
    private static final int THREAD_POOL_SIZE = 10;
    
    private final FileManager fileManager;
    private final ExecutorService executorService;
    private UDPBroadcastService udpBroadcastService;
    private WebServer webServer;

    public FileServer() {
        // Shared directory is created in the current working directory under "shared_files"
        this.fileManager = new FileManager("shared_files");
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        // Start the UDP Broadcast service in a separate thread
        udpBroadcastService = new UDPBroadcastService(TCP_PORT);
        executorService.submit(udpBroadcastService);
        
        // Start the Web Server
        webServer = new WebServer(WEB_PORT, fileManager, executorService);
        webServer.start();
        
        logger.info("LAN File Transfer Server initialized.");
        logger.info("Awaiting TCP connections on Port " + TCP_PORT + "...");

        try {
            String localIp = utils.NetworkUtils.getLocalIPAddress().getHostAddress();
            System.out.println("\n=====================================================");
            System.out.println("  LAN File Server is Online!");
            System.out.println("  To download files on mobile or desktop, open:");
            System.out.println("  http://" + localIp + ":" + WEB_PORT);
        } catch (java.net.SocketException e) {
            logger.warning("Could not resolve local IP address for display.");
        }

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            Thread consoleThread = new Thread(() -> {
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                System.out.println("  Type 'exit' to shut down the server.");
                System.out.println("=====================================================\n");
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                        logger.info("Exit command received. Shutting down server...");
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            // ignore
                        }
                        break;
                    }
                }
            });
            consoleThread.setDaemon(true);
            consoleThread.start();

            while (true) {
                // Wait for a client connection
                Socket clientSocket = serverSocket.accept();
                
                // Submit connection handling to the thread pool
                ClientHandler handler = new ClientHandler(clientSocket, fileManager);
                executorService.submit(handler);
            }
        } catch (IOException e) {
            logger.severe("Server exception: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        if (udpBroadcastService != null) {
            udpBroadcastService.stopService();
        }
        if (webServer != null) {
            webServer.stop();
        }
        executorService.shutdown();
        logger.info("Server shut down cleanly.");
    }

    public static void main(String[] args) {
        FileServer server = new FileServer();
        server.start();
    }
}
