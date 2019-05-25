package com.ca.apm.jenkins.api.entity;

/**
 * This entity holds the Jenkins build information like number, start-time // * and end time of the
 * load-runner
 *
 * @author Avinash Chandwani
 */
public class BuildInfo {

  private int number;
  private long startTime;
  private long endTime;
  private String status;

  public BuildInfo() {
    super();
  }

  public BuildInfo(int number, long startTime, long endTime, String status) {
    super();
    this.number = number;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = status;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int buildNumber) {
    this.number = buildNumber;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long buildStartTime) {
    this.startTime = buildStartTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long buildEndTime) {
    this.endTime = buildEndTime;
  }
    
  public String getStatus() {
	return status;
}

public void setStatus(String status) {
	this.status = status;
}

public String toString() {
    return "BuildInfo [number="
        + number
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", status="
        + status
        + "]";
  }
}
