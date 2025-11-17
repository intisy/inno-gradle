package io.github.intisy.gradle.inno;

import org.gradle.api.Project;
import org.gradle.api.logging.LogLevel;

/**
 * A logger for the GitHub plugin.
 */
@SuppressWarnings("unused")
public class Logger {
    private final InnoSetupTask extension;
    private final org.gradle.api.logging.Logger logger;
    private Project project;

    /**
     * Creates a new logger.
     * @param extension The GitHub extension.
     */
    public Logger(InnoSetupTask extension) {
        this.extension = extension;
        this.logger = org.gradle.api.logging.Logging.getLogger(Logger.class);
    }

    /**
     * Creates a new logger.
     * @param extension The GitHub extension.
     * @param project The project.
     */
    public Logger(InnoSetupTask extension, Project project) {
        if (extension == null || project == null) {
            throw new NullPointerException("extension and project cannot be null");
        }
        this.extension = extension;
        this.logger = project.getLogger();
        this.project = project;
    }

    /**
     * Logs a standard lifecycle message, visible in the default Gradle output.
     * @param message The message to log.
     */
    public void log(String message) {
        logger.lifecycle(message);
    }

    /**
     * Logs an error message.
     * @param message The message to log.
     */
    public void error(String message) {
        logger.error(message);
    }

    /**
     * Logs an error message along with an exception's stack trace.
     * @param message The message to log.
     * @param throwable The exception to log.
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Logs a debug message.
     * <p>
     * This message will be shown at the LIFECYCLE level (visible by default) only if
     * the user sets `github.debug = true` in their build script, providing an easy
     * way to enable verbose logging for this plugin specifically.
     * @param message The message to log.
     */
    public void debug(String message) {
        LogLevel logLevel;
        if (extension.isDebug() || project != null && ((logLevel = project.getGradle().getStartParameter().getLogLevel()).equals(LogLevel.INFO) || logLevel.equals(LogLevel.DEBUG))) {
            logger.lifecycle(message);
        }
    }

    /**
     * Logs a warning message.
     * @param message The message to log.
     */
    public void warn(String message) {
        logger.warn(message);
    }
}