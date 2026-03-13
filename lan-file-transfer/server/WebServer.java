package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import utils.LoggerConfig;

/**
 * Provides a lightweight HTTP web interface for the LAN File Transfer System.
 * Serves an HTML page listing files, and exposes endpoints for downloading them from a browser.
 */
public class WebServer {
    private static final Logger logger = LoggerConfig.getLogger(WebServer.class);
    
    private final int port;
    private final FileManager fileManager;
    private final ExecutorService executorService;
    private HttpServer server;

    public WebServer(int port, FileManager fileManager, ExecutorService executorService) {
        this.port = port;
        this.fileManager = fileManager;
        this.executorService = executorService;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Context for the HTML dashboard
            server.createContext("/", new DashboardHandler(fileManager));
            
            // Context for downloading files
            server.createContext("/download", new DownloadHandler(fileManager));
            
            // Context for uploading files via browser
            server.createContext("/upload", new UploadHandler(fileManager));
            
            // Tie the HTTP server handler to our existing thread pool 
            server.setExecutor(executorService); 
            server.start();
            
            logger.info("Web Interface started on http://localhost:" + port);
        } catch (IOException e) {
            logger.severe("Failed to start Web Interface: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Web Interface stopped.");
        }
    }

    /**
     * Handles requests to the root page. Returns beautifully styled HTML listing files.
     */
    static class DashboardHandler implements HttpHandler {
        private final FileManager fileManager;

        public DashboardHandler(FileManager fileManager) {
            this.fileManager = fileManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>LAN File Server</title>\n")
                .append("    <style>\n")
                .append("        :root { --primary: #3b82f6; --bg: #f3f4f6; --text: #1f2937; --card: #ffffff; }\n")
                .append("        body { font-family: 'Inter', -apple-system, sans-serif; background: var(--bg); color: var(--text); padding: 2rem; margin: 0; display: flex; justify-content: center; }\n")
                .append("        .container { background: var(--card); border-radius: 12px; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); width: 100%; max-width: 800px; padding: 2rem; }\n")
                .append("        h1 { margin-top: 0; color: var(--primary); text-align: center; border-bottom: 2px solid #e5e7eb; padding-bottom: 1rem; }\n")
                .append("        ul { list-style: none; padding: 0; margin: 0; }\n")
                .append("        li { display: flex; justify-content: space-between; align-items: center; padding: 1rem; border-bottom: 1px solid #e5e7eb; transition: background 0.2s; }\n")
                .append("        li:hover { background: #f9fafb; }\n")
                .append("        li:last-child { border-bottom: none; }\n")
                .append("        .filename { font-weight: 500; font-size: 1.1rem; }\n")
                .append("        .btn { background: var(--primary); color: white; text-decoration: none; padding: 0.5rem 1rem; border-radius: 6px; font-weight: bold; transition: background 0.2s; }\n")
                .append("        .btn:hover { background: #2563eb; }\n")
                .append("        .empty { text-align: center; padding: 2rem; color: #6b7280; font-style: italic; }\n")
                .append("        .upload-section { margin-top: 1.5rem; padding-top: 1.5rem; border-top: 2px solid #e5e7eb; display: flex; gap: 10px; }\n")
                .append("        .upload-section input { flex: 1; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 6px; }\n")
                .append("        .upload-section input[type=file]::file-selector-button { background: #e5e7eb; border: none; padding: 0.4rem 0.8rem; border-radius: 4px; cursor: pointer; transition: background 0.2s; }\n")
                .append("        .upload-section input[type=file]::file-selector-button:hover { background: #d1d5db; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <div class=\"container\">\n")
                .append("        <h1>Files on LAN Server</h1>\n");

            List<String> files = fileManager.listFiles();
            
            if (files.isEmpty()) {
                html.append("        <div class=\"empty\">No files are currently shared on this server.</div>\n");
            } else {
                html.append("        <ul>\n");
                for (String fileData : files) {
                    // fileData format is: "filename (size bytes)"
                    // Lets extract just the filename
                    int lastParen = fileData.lastIndexOf(" (");
                    String rawFileName = lastParen > 0 ? fileData.substring(0, lastParen) : fileData;
                    
                    // HTML Encode just in case
                    String encodedName = rawFileName.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
                    
                    // URL Encode for the href query parameter
                    String urlName = java.net.URLEncoder.encode(rawFileName, "UTF-8").replace("+", "%20");

                    html.append("            <li>\n")
                        .append("                <span class=\"filename\">").append(encodedName).append("</span>\n")
                        .append("                <a class=\"btn\" href=\"/download?file=").append(urlName).append("\">Download</a>\n")
                        .append("            </li>\n");
                }
                html.append("        </ul>\n");
            }

            html.append("        <div class=\"upload-section\">\n")
                .append("            <input type=\"file\" id=\"fileInput\">\n")
                .append("            <button id=\"uploadBtn\" class=\"btn\" onclick=\"uploadFile()\">Upload File</button>\n")
                .append("        </div>\n")
                .append("    </div>\n")
                .append("    <script>\n")
                .append("    function uploadFile() {\n")
                .append("        const fileInput = document.getElementById('fileInput');\n")
                .append("        if (fileInput.files.length === 0) { alert('Please select a file first.'); return; }\n")
                .append("        const file = fileInput.files[0];\n")
                .append("        const btn = document.getElementById('uploadBtn');\n")
                .append("        btn.innerText = 'Uploading...';\n")
                .append("        btn.disabled = true;\n")
                .append("        fetch('/upload?name=' + encodeURIComponent(file.name), {\n")
                .append("            method: 'POST',\n")
                .append("            body: file\n")
                .append("        }).then(res => {\n")
                .append("            if (res.ok) window.location.reload();\n")
                .append("            else alert('Upload failed. Error ' + res.status);\n")
                .append("        }).catch(err => alert('Upload error: ' + err))\n")
                .append("        .finally(() => { btn.innerText = 'Upload File'; btn.disabled = false; });\n")
                .append("    }\n")
                .append("    </script>\n")
                .append("</body>\n")
                .append("</html>");

            byte[] response = html.toString().getBytes("UTF-8");
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    /**
     * Handles browser file downloads seamlessly
     */
    static class DownloadHandler implements HttpHandler {
        private final FileManager fileManager;

        public DownloadHandler(FileManager fileManager) {
            this.fileManager = fileManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("file=")) {
                exchange.sendResponseHeaders(400, -1); // Bad Request
                return;
            }

            // Extract filename and safely decode it
            String encodedFileName = query.substring(5); // skip "file="
            String fileName = java.net.URLDecoder.decode(encodedFileName, "UTF-8");

            long fileSize = fileManager.getFileSize(fileName);
            if (fileSize == -1) {
                // File not found
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            // Set headers to trigger a browser download action
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName.replace("\"", "\\\"") + "\"");
            exchange.sendResponseHeaders(200, fileSize);

            logger.info("Web Browser requested download: " + fileName);

            // Stream the file in 8KB chunks to avoid massive memory usage
            try (InputStream is = fileManager.getFileInputStream(fileName);
                 OutputStream os = exchange.getResponseBody()) {
                
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            } catch (IOException e) {
                logger.warning("Download connection interrupted: " + e.getMessage());
            }
        }
    }

    /**
     * Handles file uploads directly from the web browser via AJAX raw stream
     */
    static class UploadHandler implements HttpHandler {
        private final FileManager fileManager;

        public UploadHandler(FileManager fileManager) {
            this.fileManager = fileManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("name=")) {
                exchange.sendResponseHeaders(400, -1); // Bad Request
                return;
            }

            // Extract filename from URL param
            String fileName = java.net.URLDecoder.decode(query.substring(5), "UTF-8");
            logger.info("Web Browser uploading file: " + fileName);

            try (InputStream is = exchange.getRequestBody();
                 OutputStream os = fileManager.getFileOutputStream(fileName)) {
                
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            } catch (IOException e) {
                logger.warning("Upload connection interrupted: " + e.getMessage());
                exchange.sendResponseHeaders(500, -1);
                return;
            }

            // Successfully received. Respond with no body
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            logger.info("Web Browser upload complete: " + fileName);
        }
    }
}
