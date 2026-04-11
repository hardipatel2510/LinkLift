package client;

import client.ServerDiscoveryService.ServerInfo;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import utils.LoggerConfig;

// handles the actual tcp file transfers
public class DownloadManager {
    private static final Logger logger = LoggerConfig.getLogger(DownloadManager.class);
    private static final int BUFFER_SIZE = 8192; // 8KB chunks

    // grab the file list from the server
    public List<String> listFiles(ServerInfo serverInfo) {
        List<String> files = new ArrayList<>();
        try (Socket socket = new Socket(serverInfo.ipAddress, serverInfo.tcpPort);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
             
            dos.writeUTF("LIST");
            dos.flush();
            
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                files.add(dis.readUTF());
            }
        } catch (IOException e) {
            logger.severe("Failed to list files: " + e.getMessage());
        }
        return files;
    }

    // push a local file up to the server in chunks
    public void uploadFile(ServerInfo serverInfo, String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            logger.warning("Upload failed: File does not exist - " + filePath);
            return;
        }

        try (Socket socket = new Socket(serverInfo.ipAddress, serverInfo.tcpPort);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             InputStream fis = new FileInputStream(file)) {
             
            // send command and metadata separated
            logger.info("Requesting upload: UPLOAD " + file.getName() + " (" + file.length() + " bytes)");
            dos.writeUTF("UPLOAD");
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());
            dos.flush();

            // wait for the ok
            String response = dis.readUTF();
            if (!"OK".equals(response)) {
                logger.warning("Server rejected upload: " + response);
                return;
            }

            // start pushing bytes
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            long bytesSent = 0;
            long totalSize = file.length();

            logger.info("Starting upload...");
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
                bytesSent += read;
                printProgress(bytesSent, totalSize);
            }
            dos.flush();
            System.out.println(); // Cleanup progress line
            logger.info("Upload complete: " + file.getName());

        } catch (IOException e) {
            logger.severe("Upload failed: " + e.getMessage());
        }
    }

    // pull a file down and save it locally
    public void downloadFile(ServerInfo serverInfo, String filename, String saveDir) {
        try (Socket socket = new Socket(serverInfo.ipAddress, serverInfo.tcpPort);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            // ask for the file
            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(filename);
            dos.flush();

            // Wait for response status
            String response = dis.readUTF();
            if (!"OK".equals(response)) {
                logger.warning("Server rejected download: " + response);
                return;
            }

            long fileSize = dis.readLong();
            File savePath = new File(saveDir, filename);

            // Create parent directories if they don't exist
            savePath.getParentFile().mkdirs();

            // Receive Data
            try (OutputStream fos = new FileOutputStream(savePath)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long bytesReceived = 0;

                logger.info("Starting download to " + savePath.getAbsolutePath() + "...");
                while (bytesReceived < fileSize) {
                    int read = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - bytesReceived));
                    if (read == -1) break; // Unexpected end of stream
                    fos.write(buffer, 0, read);
                    bytesReceived += read;
                    printProgress(bytesReceived, fileSize);
                }
                System.out.println(); // Cleanup progress line
                logger.info("Download complete.");
            }

        } catch (IOException e) {
            logger.severe("Download failed: " + e.getMessage());
        }
    }

    // dump a simple progress bar to the console
    private void printProgress(long current, long total) {
        if (total == 0) return;
        int percent = (int) ((current * 100) / total);
        // Uses carriage return to overwrite the line
        System.out.print("\r\tProgress: [" + percent + "%] " + current + "/" + total + " bytes");
    }
}
