package com.ca.apm.jenkins.api.exception;

public class BuildValidationException extends Exception {

  /**
   * This exception is to capture Validation Errors occured due to incorrect or mismatching data If
   * this exception is raised, the build has to be failed
   */
  private static final long serialVersionUID = 1L;

  public BuildValidationException(String message) {
    super(message);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
