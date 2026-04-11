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

// handles the physical files on disk, thread-safe so stuff doesn't explode
public class FileManager {
    private static final Logger logger = LoggerConfig.getLogger(FileManager.class);
    private final Path sharedDirectory;

    public FileManager(String directoryPath) {
        this.sharedDirectory = Paths.get(directoryPath).toAbsolutePath();
        initDirectory();
    }

    // make sure our folder is actually there
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

    // get a quick list of what we got
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

    // how big is this file?
    public synchronized long getFileSize(String filename) {
        File file = sharedDirectory.resolve(filename).toFile();
        if (file.exists() && file.isFile()) {
            return file.length();
        }
        return -1;
    }

    // open the tap to read a file
    public synchronized InputStream getFileInputStream(String filename) throws IOException {
        File file = sharedDirectory.resolve(filename).toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found: " + filename);
        }
        return new FileInputStream(file);
    }

    // open the hose to write to disk
    public synchronized OutputStream getFileOutputStream(String filename) throws IOException {
        File file = sharedDirectory.resolve(filename).toFile();
        return new FileOutputStream(file);
    }
}
