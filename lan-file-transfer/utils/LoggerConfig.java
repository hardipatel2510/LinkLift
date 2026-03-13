package utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.Date;

/**
 * Utility class to configure java.util.logging.Logger with a professional format.
 */
public class LoggerConfig {
    private static final String format = "[%1$tF %1$tT] [%2$-7s] [%3$s] %4$s %n";

    public static Logger getLogger(Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        
        // Prevent adding multiple handlers if the logger is requested multiple times
        if (logger.getHandlers().length == 0) {
            logger.setUseParentHandlers(false); // Don't use the root logger's default console handler
            
            ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format(format,
                            new Date(record.getMillis()),
                            record.getLevel().getLocalizedName(),
                            Thread.currentThread().getName(),
                            record.getMessage()
                    );
                }
            });
            
            logger.addHandler(handler);
            logger.setLevel(Level.INFO); // Set default level to INFO
        }
        return logger;
    }
}
