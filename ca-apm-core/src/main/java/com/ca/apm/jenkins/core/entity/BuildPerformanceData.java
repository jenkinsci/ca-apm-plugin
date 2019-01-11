package com.ca.apm.jenkins.core.entity;

import java.util.Collection;
import java.util.LinkedList;

/**
 * This entity is the placeholder for the metric-performance data collected from EM based upon the
 * comparison-strategy configuration defined in the properties file This is a list of
 * MetricPerformanceData entity, which is the metric-performance data of one metric path
 *
 * @author Avinash Chandwani
 */
public class BuildPerformanceData {

  private Collection<MetricPerformanceData> metricData;

  public BuildPerformanceData() {
    super();
    metricData = new LinkedList<MetricPerformanceData>();
  }

  public Collection<MetricPerformanceData> getMetricData() {
    return metricData;
  }

  public void setMetricData(Collection<MetricPerformanceData> transactions) {
    this.metricData = transactions;
  }

  public void addToMetricPerformanceData(MetricPerformanceData metricPerformanceData) {
    this.metricData.add(metricPerformanceData);
  }

  @Override
  public String toString() {
    return "BuildPerformanceData [metricData=" + metricData + "]";
  }
}
