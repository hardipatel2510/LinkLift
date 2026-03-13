package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import utils.LoggerConfig;

/**
 * Manages access to the shared directory on the server.
 * Implements thread-safe access to read, write, and list files.
 */
public class FileManager {
    private static final Logger logger = LoggerConfig.getLogger(FileManager.class);
    private final Path sharedDirectory;

    public FileManager(String directoryPath) {
        this.sharedDirectory = Paths.get(directoryPath).toAbsolutePath();
        initDirectory();
    }

    /**
     * Creates the shared directory if it doesn't already exist.
     */
    private void initDirectory() {
        try {
            if (!Files.exists(sharedDirectory)) {
                Files.createDirectories(sharedDirectory);
                logger.info("Created shared directory: " + sharedDirectory);
            } else {
                logger.info("Using existing shared directory: " + sharedDirectory);
            }
        } catch (IOException e) {
            logger.severe("Failed to initialize shared directory: " + e.getMessage());
        }
    }

    /**
     * Lists all files in the shared directory along with their size.
     * 
     * @return List of formatted strings containing filename and size.
     */
    public synchronized List<String> listFiles() {
        List<String> fileList = new ArrayList<>();
        File[] files = sharedDirectory.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getName() + " (" + file.length() + " bytes)");
                }
            }
        }
        return fileList;
    }

    /**
     * Gets the size of a specific file in bytes.
     * 
     * @param filename Name of the file
     * @return Size in bytes, or -1 if not found.
     */
    public synchronized long getFileSize(String filename) {
        File file = sharedDirectory.resolve(filename).toFile();
        if (file.exists() && file.isFile()) {
            return file.length();
        }
        return -1;
    }

    /**
     * Opens an InputStream for reading a file to send to the client.
     * 
     * @param filename Name of the file
     * @return InputStream to read the file
     * @throws IOException If file doesn't exist
     */
    public synchronized InputStream getFileInputStream(String filename) throws IOException {
        File file = sharedDirectory.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found: " + filename);
        }
        return new FileInputStream(file);
    }

    /**
     * Opens an OutputStream for writing a file received from the client.
     * 
     * @param filename Name of the file
     * @return OutputStream to write the file
     * @throws IOException If file cannot be created
     */
    public synchronized OutputStream getFileOutputStream(String filename) throws IOException {
        File file = sharedDirectory.resolve(filename).toFile();
        return new FileOutputStream(file);
    }
}
