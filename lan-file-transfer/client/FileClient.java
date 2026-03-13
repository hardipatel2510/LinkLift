package client;

import client.ServerDiscoveryService.ServerInfo;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import utils.LoggerConfig;

/**
 * Main entry point for the LAN File Client.
 * Automatically discovers the server and provides an interactive CLI for testing.
 */
public class FileClient {
    private static final Logger logger = LoggerConfig.getLogger(FileClient.class);
    private static final String DOWNLOAD_DIR = "downloads";

    public static void main(String[] args) {
        logger.info("Starting LAN File Client...");

        ServerDiscoveryService discoveryService = new ServerDiscoveryService();
        ServerInfo serverInfo = discoveryService.discoverServer();

        if (serverInfo == null) {
            System.err.println("Could not discover server automatically. Exiting.");
            return;
        }

        System.out.println("\n====================================");
        System.out.println("   Connected to LAN File Server");
        System.out.println("   IP: " + serverInfo.ipAddress);
        System.out.println("   Port: " + serverInfo.tcpPort);
        System.out.println("====================================");
        System.out.println("Available commands:");
        System.out.println("  ls                  - List available files on the server");
        System.out.println("  upload <filepath>   - Upload a local file to the server");
        System.out.println("  download <filename> - Download a file from the server");
        System.out.println("  exit                - Exit the client");
        System.out.println("------------------------------------\n");

        DownloadManager downloadManager = new DownloadManager();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("client> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting client. Goodbye!");
                break;
            }

            String[] parts = input.split(" ", 2);
            String command = parts[0].toLowerCase();

            switch (command) {
                case "ls":
                    List<String> files = downloadManager.listFiles(serverInfo);
                    if (files.isEmpty()) {
                        System.out.println("No files found on server.");
                    } else {
                        System.out.println("Files on server:");
                        for (String f : files) {
                            System.out.println("  - " + f);
                        }
                    }
                    break;

                case "upload":
                    if (parts.length < 2) {
                        System.out.println("Usage: upload <filepath>");
                    } else {
                        String filePath = parts[1].trim();
                        if (filePath.startsWith("'") && filePath.endsWith("'")) {
                            filePath = filePath.substring(1, filePath.length() - 1);
                        } else if (filePath.startsWith("\"") && filePath.endsWith("\"")) {
                            filePath = filePath.substring(1, filePath.length() - 1);
                        }
                        
                        // Terminal drag and drop often appends an extra space at the very end
                        filePath = filePath.trim();
                        // And escapes spaces with backslashes
                        filePath = filePath.replace("\\ ", " ");
                        
                        downloadManager.uploadFile(serverInfo, filePath);
                    }
                    break;

                case "download":
                    if (parts.length < 2) {
                        System.out.println("Usage: download <filename>");
                    } else {
                        String fileName = parts[1].trim();
                        if (fileName.startsWith("'") && fileName.endsWith("'")) {
                            fileName = fileName.substring(1, fileName.length() - 1);
                        } else if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                            fileName = fileName.substring(1, fileName.length() - 1);
                        }
                        fileName = fileName.trim();
                        fileName = fileName.replace("\\ ", " ");
                        downloadManager.downloadFile(serverInfo, fileName, DOWNLOAD_DIR);
                    }
                    break;

                default:
                    System.out.println("Unknown command: " + command);
            }
        }
        
        scanner.close();
    }
}
