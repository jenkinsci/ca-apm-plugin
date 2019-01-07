package com.ca.apm.jenkins.api;

import com.ca.apm.jenkins.api.exception.BuildValidationException;

/**
 * This interface is the contract for implementing a load generator output file read.
 * getBuildTSFromOutputFile method gets called up when reading the provided load generator output
 * file
 */
public interface LoadGenOPFileReader {

  /**
   * This method is getting invoked from JmeterMetadataRetriever class to read the provided jmeter
   * output file and return the array of values for start time , end time of jmeter run.
   */
  public long[] getBuildTSFromOutputFile(String loadGenOutputFile) throws BuildValidationException;
}
