package com.concordia.dsms.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

class ClientLogger {
    private final Logger logger;

    ClientLogger(String clientId) {
        try {
        Files.createDirectories(Path.of("logs/clients"));
            logger = Logger.getLogger("Client-" + clientId);
            logger.setUseParentHandlers(false); //Disable default console logging
            FileHandler handler = new FileHandler("logs/clients/" + clientId + ".log", true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize logger for client " + clientId, e);
        }
    }

    void info(String message) {
        logger.log(Level.INFO, message);
    }

    void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

}
