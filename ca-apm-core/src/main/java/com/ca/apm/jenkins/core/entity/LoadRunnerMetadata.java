package com.ca.apm.jenkins.core.entity;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An entity which contains the information about the metadata of the load-runner provided by the
 * user in the load-runner-properties.
 *
 * @author Avinash Chandwani
 */
public class LoadRunnerMetadata {

  private Map<String, String> loadRunnerProperties = null;
  private BuildInfo benchMarkBuildInfo;
  private BuildInfo currentBuildInfo;
  private List<BuildInfo> histogramBuildInfo;

  private JenkinsInfo jenkinsInfo;

  public LoadRunnerMetadata() {
    loadRunnerProperties = new HashMap<String, String>();
    benchMarkBuildInfo = new BuildInfo();
    currentBuildInfo = new BuildInfo();
    histogramBuildInfo = new ArrayList<BuildInfo>();
  }

  public void addToLoadRunnerProperties(String key, String value) {
    loadRunnerProperties.put(key, value);
  }

  public String getLoadRunnerPropertyValue(String key) {
    return loadRunnerProperties.get(key);
  }

  public BuildInfo getBenchMarkBuildInfo() {
    return benchMarkBuildInfo;
  }

  public void setBenchMarkBuildInfo(BuildInfo benchMarkBuildInfo) {
    this.benchMarkBuildInfo = benchMarkBuildInfo;
  }

  public BuildInfo getCurrentBuildInfo() {
    return currentBuildInfo;
  }

  public void setCurrentBuildInfo(BuildInfo currentBuildInfo) {
    this.currentBuildInfo = currentBuildInfo;
  }

  public int getCurrentBuildNumber() {
    return currentBuildInfo.getNumber();
  }

  public void setCurrentBuildNumber(int currentBuildNumber) {
    this.currentBuildInfo.setNumber(currentBuildNumber);
  }

  public int getBenchMarkBuildNumber() {
    return benchMarkBuildInfo.getNumber();
  }

  public void setBenchMarkBuildNumber(int benchMarkBuildNumber) {
    benchMarkBuildInfo.setNumber(benchMarkBuildNumber);
  }

  public void setBenchMarBuildTimes(long startTime, long endTime) {
    benchMarkBuildInfo.setStartTime(startTime);
    benchMarkBuildInfo.setEndTime(endTime);
  }

  public void setCurrentBuildTimes(long startTime, long endTime) {
    currentBuildInfo.setStartTime(startTime);
    currentBuildInfo.setEndTime(endTime);
  }

  public JenkinsInfo getJenkinsInfo() {
    return jenkinsInfo;
  }

  public void setJenkinsInfo(JenkinsInfo jenkinsInfo) {
    this.jenkinsInfo = jenkinsInfo;
  }

  public List<BuildInfo> getHistogramBuildInfo() {
    return histogramBuildInfo;
  }

  public void setHistogramBuildInfo(List<BuildInfo> histogramBuildInfo) {
    this.histogramBuildInfo = histogramBuildInfo;
  }
}
