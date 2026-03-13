package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;
import utils.LoggerConfig;

/**
 * Handles individual client TCP connections. Processes LIST, UPLOAD, and DOWNLOAD commands.
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerConfig.getLogger(ClientHandler.class);
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for file transfer

    private final Socket clientSocket;
    private final FileManager fileManager;

    public ClientHandler(Socket clientSocket, FileManager fileManager) {
        this.clientSocket = clientSocket;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        logger.info("Client connected: " + clientAddress);

        try (
            DataInputStream dis = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()))
        ) {
            String request = dis.readUTF();
            logger.info("Received request from " + clientAddress + ": " + request);

            if (request.equals("LIST")) {
                handleList(dos);
            } else if (request.equals("UPLOAD")) {
                handleUpload(dis, dos);
            } else if (request.equals("DOWNLOAD")) {
                handleDownload(dis, dos);
            } else {
                logger.warning("Unknown request: " + request);
                dos.writeUTF("ERROR: Unknown command.");
                dos.flush();
            }

        } catch (IOException e) {
            logger.warning("Error communicating with client " + clientAddress + ": " + e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
                logger.info("Client disconnected: " + clientAddress);
            } catch (IOException e) {
                logger.severe("Could not close client socket: " + e.getMessage());
            }
        }
    }

    private void handleList(DataOutputStream dos) throws IOException {
        List<String> files = fileManager.listFiles();
        dos.writeInt(files.size()); // Send the count first
        for (String fileStr : files) {
            dos.writeUTF(fileStr); // Send each file name
        }
        dos.flush();
        logger.info("Sent file list to client (" + files.size() + " files).");
    }

    private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
        String filename = dis.readUTF();
        long fileSize = dis.readLong();

        dos.writeUTF("OK");
        dos.flush();

        // Write the chunks from network to file
        try (OutputStream fos = fileManager.getFileOutputStream(filename)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long bytesReadTotal = 0;
            while (bytesReadTotal < fileSize) {
                int read = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - bytesReadTotal));
                if (read == -1) break; // Premature EOF
                fos.write(buffer, 0, read);
                bytesReadTotal += read;
            }
            logger.info("Successfully received file: " + filename + " (" + bytesReadTotal + " bytes)");
        } catch (IOException e) {
            logger.severe("Failed to save uploaded file: " + e.getMessage());
            throw e; // rethrow logic can be adjusted based on needs
        }
    }

    private void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {
        String filename = dis.readUTF();
        long fileSize = fileManager.getFileSize(filename);

        if (fileSize == -1) {
            dos.writeUTF("ERROR: File not found.");
            dos.flush();
            return;
        }

        dos.writeUTF("OK");
        dos.writeLong(fileSize);
        dos.flush();

        // Send file chunks over network
        try (InputStream fis = fileManager.getFileInputStream(filename)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            long bytesSent = 0;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
                bytesSent += read;
            }
            dos.flush();
            logger.info("Successfully sent file: " + filename + " (" + bytesSent + " bytes)");
        } catch (IOException e) {
            logger.severe("Failed to send file: " + e.getMessage());
            throw e;
        }
    }
}
