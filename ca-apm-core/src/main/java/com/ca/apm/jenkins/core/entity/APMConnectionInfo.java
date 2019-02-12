package com.ca.apm.jenkins.core.entity;

/**
 * This entity holds the APM Connection information. This is used by MetricDataHelper entity to
 * provide the performance-metric data based upon the configurations set
 *
 * @author Avinash Chandwani
 */
public class APMConnectionInfo {

  private String emURL;
  private String emAuthToken;
  private String emTimeZone;

  public APMConnectionInfo() {
    super();
  }

  public String getEmURL() {
    return emURL;
  }

  public void setEmURL(String emURL) {
    this.emURL = emURL;
  }

  public String getEmAuthToken() {
    return emAuthToken;
  }

  public void setEmAuthToken(String emAuthToken) {
    this.emAuthToken = emAuthToken;
  }

  public String getEmTimeZone() {
    return emTimeZone;
  }

  public void setEmTimeZone(String emTimeZone) {
    this.emTimeZone = emTimeZone;
  }

  @Override
  public String toString() {
    return "APMConnectionInfo [emURL="
        + emURL
        + ", authToken="
        + emAuthToken
        + ", emTimeZone="
        + emTimeZone
        + "]";
  }
}
