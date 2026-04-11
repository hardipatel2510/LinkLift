package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import utils.LoggerConfig;

// boot up the server, threadpool, and listeners
public class FileServer {
    private static final Logger logger = LoggerConfig.getLogger(FileServer.class);
    
    // main tcp port
    private static final int TCP_PORT = 9000;
    // web ui port
    private static final int WEB_PORT = 8080;
    // max concurrent clients
    private static final int THREAD_POOL_SIZE = 10;
    
    private final FileManager fileManager;
    private final ExecutorService executorService;
    private UDPBroadcastService udpBroadcastService;
    private WebServer webServer;

    public FileServer() {
        // drop everything in shared_files locally
        this.fileManager = new FileManager("shared_files");
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        // spin up the udp broadcaster
        udpBroadcastService = new UDPBroadcastService(TCP_PORT);
        executorService.submit(udpBroadcastService);
        
        // start the web ui
        webServer = new WebServer(WEB_PORT, fileManager, executorService);
        webServer.start();
        
        logger.info("LAN File Transfer Server initialized.");
        logger.info("Awaiting TCP connections on Port " + TCP_PORT + "...");

        try {
            String localIp = utils.NetworkUtils.getLocalIPAddress().getHostAddress();
            String webUrl = "http://" + localIp + ":" + WEB_PORT;
            System.out.println("\n=====================================================");
            System.out.println("  LAN File Server is Online!");
            System.out.println("  To download files on mobile or desktop, open:\n");
            System.out.println("  => " + webUrl + " <=\n");
            
            try {
                // generate qr code for easy mobile access
                utils.qrcodegen.QrCode qr = utils.qrcodegen.QrCode.encodeText(webUrl, utils.qrcodegen.QrCode.Ecc.LOW);
                int border = 2; // quiet zone
                for (int y = -border; y < qr.size + border; y++) {
                    System.out.print("    "); // indent
                    for (int x = -border; x < qr.size + border; x++) {
                        boolean isDark = qr.getModule(x, y);
                        // using ANSI background colors: 40=black, 47=white
                        // this avoids the '?' encoding issue on Windows completely
                        System.out.print(isDark ? "\033[40m  \033[0m" : "\033[47m  \033[0m");
                    }
                    System.out.println();
                }
                System.out.println();
            } catch (Exception ex) {
                logger.warning("Could not generate QR Code: " + ex.getMessage());
            }
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
                // block until someone connects
                Socket clientSocket = serverSocket.accept();
                
                // hand off to the threadpool
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
