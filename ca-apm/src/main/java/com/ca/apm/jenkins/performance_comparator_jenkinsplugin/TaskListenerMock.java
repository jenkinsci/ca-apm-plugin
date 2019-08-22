package com.ca.apm.jenkins.performance_comparator_jenkinsplugin;

import hudson.console.ConsoleNote;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

public class TaskListenerMock implements TaskListener {

	@Override
	public PrintStream getLogger() {
		return new PrintStream(System.out);
	}

	@Override
	public void annotate(ConsoleNote consoleNote) throws IOException {
		// Do Nothing
	}

	@Override
	public void hyperlink(String s, String s1) throws IOException {
		// Do Nothing
	}

	@Override
	public PrintWriter error(String s) {
		return null;
	}

	@Override
	public PrintWriter error(String s, Object... objects) {
		return null;
	}

	@Override
	public PrintWriter fatalError(String s) {
		return null;
	}

	@Override
	public PrintWriter fatalError(String s, Object... objects) {
		return null;
	}
}
