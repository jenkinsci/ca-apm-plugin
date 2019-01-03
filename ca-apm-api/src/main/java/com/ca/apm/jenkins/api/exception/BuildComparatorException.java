package com.ca.apm.jenkins.api.exception;

/**
 * A custom exception for this application. Whenever you want to raise an
 * exception in your implemented entities, you can raise this exception. The
 * plugin task will be stopped
 * 
 * @author Avinash Chandwani
 *
 */
public class BuildComparatorException extends RuntimeException {

	private static final long serialVersionUID = -7441516668410176500L;

	public BuildComparatorException(String message) {
		super(message);
	}

	@Override
	public String toString() {
		return super.toString();
	}
}