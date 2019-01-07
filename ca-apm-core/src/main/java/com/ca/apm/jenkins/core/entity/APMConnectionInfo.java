package com.ca.apm.jenkins.core.entity;

/**
 * This entity holds the APM Connection information. This is used by MetricDataHelper entity to
 * provide the performance-metric data based upon the configurations set
 *
 * @author Avinash Chandwani
 */
public class APMConnectionInfo {

  private String emURL;
  private String emUserName;
  private String emPassword;
  private String authToken;
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

  public String getEmUserName() {
    return emUserName;
  }

  public void setEmUserName(String emUserName) {
    this.emUserName = emUserName;
  }

  public String getEmPassword() {
    return emPassword;
  }

  public void setEmPassword(String emPassword) {
    this.emPassword = emPassword;
  }

  public String getAuthToken() {
    return authToken;
  }

  public void setAuthToken(String authToken) {
    this.authToken = authToken;
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
        + ", emUserName="
        + emUserName
        + ", emPassword="
        + emPassword
        + ", authToken="
        + authToken
        + ", emTimeZone="
        + emTimeZone
        + "]";
  }
}
