package com.ca.apm.jenkins.core.logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.TaskListener;

/**
 * A util class to instantiate custom file logging for this plug-in
 *
 * <p>
 * Levels of logging are SEVERE &gt; WARNING &gt; INFO &gt; CONFIG &gt; FINE
 * &gt; FINER &gt; FINEST
 *
 * @author Avinash Chandwani
 */
public class JenkinsPlugInLogger {

	private static Logger logger = Logger.getLogger("JP_LOGGER");

	private static JenkinsPluginCustomFileHandler fileHandler;

	private static TaskListener taskListener;

	/*
	 *
	 */
	private JenkinsPlugInLogger() {
		super();
	}

	public static void setTaskListener(TaskListener taskListener) {
		JenkinsPlugInLogger.taskListener = taskListener;
	}

	public static Logger getLogger() {
		return logger;
	}

	private static Level getLoggingLevel(String level) {
		if ("SEVERE".equals(level)) {
			return Level.SEVERE;
		} else if ("WARNING".equals(level)) {
			return Level.WARNING;
		} else if ("INFO".equals(level)) {
			return Level.INFO;
		} else if ("CONFIG".equals(level)) {
			return Level.CONFIG;
		} else if ("FINE".equals(level)) {
			return Level.FINE;
		} else if ("FINER".equals(level)) {
			return Level.FINER;
		} else if ("FINEST".equals(level)) {
			return Level.FINEST;
		} else {
			return Level.SEVERE;
		}
	}

	/**
	 * Check if the current log level is greater than the current configured
	 * logging level. If it is greater this will return true and log will get
	 * printed else it won't be printed
	 *
	 * @param level
	 *            Logging Level
	 * @return Returns true if current logging level is greater that your
	 *         selected logging level
	 */
	public static boolean isLoggable(Level level) {
		boolean isLoggable = false;
		if (level.intValue() == Level.SEVERE.intValue()) {
			isLoggable = true;
		}
		return isLoggable;
	}

	private static void setLogLevel(Level level) {

		Handler[] handlers = Logger.getLogger("").getHandlers();
		for (int index = 0; index < handlers.length; index++) {
			handlers[index].setLevel(level);
		}
		logger.setLevel(level);
	}

	public static void configureLog(String level, String logFileName) {
		setLogLevel(getLoggingLevel(level));
		try {
			fileHandler = new JenkinsPluginCustomFileHandler(logFileName);
			fileHandler.setFormatter(new JenkinsPluginCustomFormatter());
			logger.addHandler(fileHandler);
			logger.setUseParentHandlers(false);
		} catch (Exception e) {
			JenkinsPlugInLogger.severe("Exception in closing httpClient : " + e.getMessage());
		}
	}

	public static void closeLogger() {
		if (fileHandler != null)
			fileHandler.close();
	}

	/**
	 * Print log at your desired logging level
	 *
	 * @param level
	 *            Level value
	 * @param message
	 *            String message to be printed
	 */
	public static void log(Level level, String message) {
		logger.log(level, message);
	}

	/**
	 * Print log at your desired logging level
	 *
	 * @param level
	 *            Level value
	 * @param message
	 *            String message to be printed
	 * @param throwable
	 *            Stacktrace to be printed
	 */
	public static void log(Level level, String message, Throwable throwable) {
		logger.log(level, message, throwable);
	}

	/*
	 * Logging Level ALL> SEVERE > WARNING > INFO > CONFIG > FINE > FINER >
	 * FINEST > OFF
	 */

	/**
	 * Print log message at finest level
	 *
	 * @param message
	 *            String message to be printed
	 */
	public static void finest(String message) {
		logger.finest(message);
	}

	/**
	 * Print log message at finer level
	 *
	 * @param message
	 *            String message to be printed
	 */
	public static void finer(String message) {
		logger.finer(message);
	}

	/**
	 * Print log message at info fine
	 *
	 * @param message
	 *            String message to be printed
	 */
	public static void fine(String message) {
		logger.fine(message);
	}

	/**
	 * Print log message at info level
	 *
	 * @param message
	 *            String message to be printed
	 */
	public static void info(String message) {
		logger.log(Level.INFO, message);
	}

	/**
	 * Print the message at warning level
	 *
	 * @param message
	 *            String message to be printed
	 */
	public static void warning(String message) {
		logger.warning(message);
	}

	/**
	 * Prints log message along with complete exception stacktrace
	 *
	 * @param message
	 *            String message to be printed
	 * @param thrown
	 *            throwable whose stacktrace is required to be printed on the
	 *            stream
	 */
	public static void severe(String message, Throwable thrown) {
		logger.log(Level.SEVERE, message, thrown);
	}

	/**
	 * Prints log message in severe level
	 *
	 * @param message
	 *            String message to be printed
	 */
	public static void severe(String message) {
		logger.log(Level.SEVERE, message);
	}

	public static String getLevelString(int level) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < level; i++) {
			builder.append(" ");
		}
		return builder.toString();
	}

	public static void printLogOnConsole(int level, String message) {
		StringBuilder consoleLogString = new StringBuilder();
		for (int i = 0; i < level; i++) {
			consoleLogString.append(" ");
		}
		consoleLogString.append(message);
		taskListener.getLogger().println(consoleLogString.toString());
	}

}
