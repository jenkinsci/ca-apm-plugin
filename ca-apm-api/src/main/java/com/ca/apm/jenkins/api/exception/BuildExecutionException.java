package com.ca.apm.jenkins.api.exception;

/**
 * This exception is thrown on when an execution exception occurs
 *
 * @author Avinash Chandwani
 */
public class BuildExecutionException extends Exception {
  private static final long serialVersionUID = -7441516668410176500L;

  public BuildExecutionException(String message) {
    super(message);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
