package com.ca.apm.jenkins.api.exception;

/**
 * A custom exception for this application. 
 *
 */
public class DecodeExceededDataLength extends Exception{
	 private static final long serialVersionUID = -7441516668410176501L;

	  public DecodeExceededDataLength(String message) {
	    super(message);
	  }

	  @Override
	  public String toString() {
	    return super.toString();
	  }

}
