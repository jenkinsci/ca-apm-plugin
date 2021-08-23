package com.ca.apm.jenkins.core.entity;

import java.io.Serializable;
import java.util.List;

import com.ca.apm.jenkins.api.entity.BuildInfo;

/**
 * This entity holds the Jenkins Specific Information for the use by Plug-in run
 *
 * @author Avinash Chandwani
 */
public class JenkinsInfo implements Serializable {

  private int currentBuildNumber;
  private int lastSuccessfulBuildNumber;
  private String buildWorkSpaceFolder;
  private String jobName;
  private List<BuildInfo> histogramBuildInfoList;

  public JenkinsInfo(
      int currentBuildNumber,
      int lastSuccessfulBuildNumber,
      List<BuildInfo> histogramBuildInfoList,
      String buildWorkSpaceFolder,
      String jobName) {
    super();
    this.currentBuildNumber = currentBuildNumber;
    this.lastSuccessfulBuildNumber = lastSuccessfulBuildNumber;
    this.buildWorkSpaceFolder = buildWorkSpaceFolder;
    this.jobName = jobName;
    this.histogramBuildInfoList = histogramBuildInfoList;
  }

  public int getCurrentBuildNumber() {
    return currentBuildNumber;
  }

  public void setCurrentBuildNumber(int buildNumber) {
    this.currentBuildNumber = buildNumber;
  }

  public int getLastSuccessfulBuildNumber() {
    return lastSuccessfulBuildNumber;
  }

  public void setLastSuccessfulBuildNumber(int lastSuccessfulBuildNumber) {
    this.lastSuccessfulBuildNumber = lastSuccessfulBuildNumber;
  }

  public String getBuildWorkSpaceFolder() {
    return buildWorkSpaceFolder;
  }

  public void setBuildWorkSpaceFolder(String buildWorkSpaceFolder) {
    this.buildWorkSpaceFolder = buildWorkSpaceFolder;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public List<BuildInfo> getHistogramBuildInfoList() {
    return histogramBuildInfoList;
  }

public void setHistogramBuildInfoList(List<BuildInfo> histogramBuildInfoList) {
	this.histogramBuildInfoList = histogramBuildInfoList;
}
  
  
}
