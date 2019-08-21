package com.ca.apm.jenkins.core.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Custom formatter for our own Logging mechanism. format method has the logging
 * format to be used to print logging the events
 *
 * @author Avinash Chandwani
 */
public class JenkinsPluginCustomFormatter extends Formatter {

	private static final String FORMAT = "{0,date} {0,time}";
	Date dat = new Date();
	private MessageFormat formatter;

	/**
	 * Overriden format method, which does the formatting of the logging message
	 */
	public synchronized String format(LogRecord record) {

		StringBuilder sb = new StringBuilder();
		dat.setTime(record.getMillis());
		StringBuffer text = new StringBuffer();
		if (formatter == null) {
			formatter = new MessageFormat(FORMAT);
		}
		sb.append(new Date(record.getMillis()));
		sb.append(text);
		sb.append(" ");
		sb.append(record.getLevel().getName());
		sb.append(" ");
		if (record.getSourceClassName() == null) {
			sb.append(record.getLoggerName());
		}
		if (record.getSourceMethodName() != null) {
			sb.append(" ");
		}

		String message = formatMessage(record);
		sb.append(message);
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
				JenkinsPlugInLogger.severe("Exception in executing format(LogRecord record) : " + ex.getMessage());
			}
		}
		sb.append("\n");
		return sb.toString();
	}
}
