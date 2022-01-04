package ru.perveevm.polygon.packages;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Perveev Mike (perveev_m@mail.ru)
 */
public class ConsoleLogger {
    private final Logger logger;

    public ConsoleLogger(final String name) {
        logger = Logger.getLogger(name);
    }

    public void logInfo(final String message) {
        logger.log(Level.INFO, message);
    }

    public void logBeginStage(final String stageName) {
        logInfo(String.format("=== %s ===", stageName));
    }
}
