package com.ca.apm.jenkins.core.entity;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StaticThresholdResult {

  private boolean passed = false;
  private int thresholdInMillis = 0;
  private Map<String, List<TimeSliceValue>> metricSeries =
      new TreeMap<>();
  private int buildNumber;

  public boolean isPassed() {
    return passed;
  }

  public void setPassed(boolean passed) {
    this.passed = passed;
  }

  public int getThresholdInMillis() {
    return thresholdInMillis;
  }

  public void setThresholdInMillis(int thresholdInMillis) {
    this.thresholdInMillis = thresholdInMillis;
  }

  public Map<String, List<TimeSliceValue>> getMetricSeries() {
    return metricSeries;
  }

  public void setMetricSeries(Map<String, List<TimeSliceValue>> metricSeries) {
    this.metricSeries = metricSeries;
  }

  public int getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(int buildNumber) {
    this.buildNumber = buildNumber;
  }

  public boolean isEmpty() {
    return (metricSeries == null || metricSeries.isEmpty());
  }
}
