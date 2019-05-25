package com.ca.apm.jenkins.core.entity;

import java.util.List;

/**
 * This entity is placeholder for one metric-path's performance comparison record This object can be
 * created in the Comparison-Strategy and be used for output-reporting. You can see in the
 * default-implementation of comparison-strategies and output-handlers
 *
 * @author Avinash Chandwani
 */
public class MetricPathComparisonResult {

  private String metricPath;
  private double expectedValue;
  private double actualValue;
  private double percentageChange;
  private double thresholdPercentage;
  private List<TimeSliceValue> benchMarkBuildTimeSliceValues;
  private List<TimeSliceValue> currentBuildTimeSliceValues;

  public MetricPathComparisonResult() {
    super();
  }

  public MetricPathComparisonResult(String metricPath, double expectedValue, double actualValue) {
    super();
    this.metricPath = metricPath;
    this.expectedValue = expectedValue;
    this.actualValue = actualValue;
    percentageChange = 0.00;
    if (actualValue == expectedValue) {
      percentageChange = 0.00;
    } else if (expectedValue == 0.0) {
      percentageChange = actualValue * 100;
    } else {
      percentageChange = (actualValue - expectedValue) * 100 / expectedValue;
    }
  }

  public MetricPathComparisonResult(
      String metricPath, double expectedValue, double actualValue, double percentageChange) {
    super();
    this.metricPath = metricPath;
    this.expectedValue = expectedValue;
    this.actualValue = actualValue;
    this.percentageChange = percentageChange;
  }

  public String getMetricPath() {
    return metricPath;
  }

  public void setMetricPath(String metricPath) {
    this.metricPath = metricPath;
  }

  public double getExpectedValue() {
    return expectedValue;
  }

  public void setExpectedValue(double expectedValue) {
    this.expectedValue = expectedValue;
  }

  public double getActualValue() {
    return actualValue;
  }

  public void setActualValue(double actualValue) {
    this.actualValue = actualValue;
  }

  public double getPercentageChange() {
    return percentageChange;
  }

  public void setPercentageChange(double percentageChange) {
    this.percentageChange = percentageChange;
  }

  public List<TimeSliceValue> getBenchMarkBuildTimeSliceValues() {
    return benchMarkBuildTimeSliceValues;
  }

  public void setBenchMarkBuildTimeSliceValues(List<TimeSliceValue> benchMarkBuildTimeSliceValues) {
    this.benchMarkBuildTimeSliceValues = benchMarkBuildTimeSliceValues;
  }

  public List<TimeSliceValue> getCurrentBuildTimeSliceValues() {
    return currentBuildTimeSliceValues;
  }

  public void setCurrentBuildTimeSliceValues(List<TimeSliceValue> currentBuildTimeSliceValues) {
    this.currentBuildTimeSliceValues = currentBuildTimeSliceValues;
  }

  public double getThresholdPercentage() {
    return thresholdPercentage;
  }

  public void setThresholdPercentage(double thresholdPercentage) {
    this.thresholdPercentage = thresholdPercentage;
  }

  @Override
  public String toString() {
    return "ComparisonOutput [metricPath="
        + metricPath
        + ", expectedValue="
        + expectedValue
        + ", actualValue="
        + actualValue
        + ", percentageChange="
        + percentageChange
        + "]";
  }
}
