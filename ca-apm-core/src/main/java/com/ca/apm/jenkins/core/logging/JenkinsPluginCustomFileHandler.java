package com.ca.apm.jenkins.core.logging;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;

/**
 * Custom File-Handler for the logging mechanism
 * 
 * We have defined one file of size 1MB after which rollover of the file will
 * happen
 * 
 * @author Avinash Chandwani
 *
 */

public class JenkinsPluginCustomFileHandler extends FileHandler {

	public static final int FILE_SIZE = 1024 * 1024;

	public JenkinsPluginCustomFileHandler(String pattern) throws IOException, SecurityException {
		super(pattern, FILE_SIZE, 1, true);
	}

	@Override
	public synchronized void publish(LogRecord record) {
		if (!isLoggable(record)) {
			return;
		}
		super.publish(record);
	}
}