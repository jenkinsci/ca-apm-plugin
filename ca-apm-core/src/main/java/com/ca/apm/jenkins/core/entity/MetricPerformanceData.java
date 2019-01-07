package com.ca.apm.jenkins.core.entity;

import java.util.LinkedList;
import java.util.List;

/**
 * This entity is the one record of metric performance data which is received from APM. This entity
 * represents list of time-slice values for the given metric-path.TimeSliceValue represents one
 * entry of max, min, value and count metric values for given metric-path
 *
 * @author Avinash Chandwani
 */
public class MetricPerformanceData {

  private String metricPath;
  private List<TimeSliceValue> timeSliceValues;

  public MetricPerformanceData() {
    super();
    this.timeSliceValues = new LinkedList<TimeSliceValue>();
  }

  public MetricPerformanceData(String metricPath, List<TimeSliceValue> timeSliceValues) {
    super();
    this.metricPath = metricPath;
    this.timeSliceValues = timeSliceValues;
  }

  /**
   * Returns the metric path of this Data-Record
   *
   * @return the metric path of the current performance data
   */
  public String getMetricPath() {
    return metricPath;
  }

  public void setMetricPath(String metricPath) {
    this.metricPath = metricPath;
  }

  public List<TimeSliceValue> getTimeSliceValues() {
    return timeSliceValues;
  }

  public void setTimeSliceValues(List<TimeSliceValue> timeSliceValues) {
    this.timeSliceValues = timeSliceValues;
  }

  public void addToTimeSliceValue(TimeSliceValue sliceValue) {
    timeSliceValues.add(sliceValue);
  }

  @Override
  public String toString() {
    return "MetricPerformanceData [metricPath="
        + metricPath
        + ", timeSliceValues="
        + timeSliceValues
        + "]";
  }
}
