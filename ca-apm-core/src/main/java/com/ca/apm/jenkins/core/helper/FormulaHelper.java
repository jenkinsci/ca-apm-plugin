package com.ca.apm.jenkins.core.helper;

import com.ca.apm.jenkins.core.entity.AgentComparisonResult;
import com.ca.apm.jenkins.core.entity.BuildPerformanceData;
import com.ca.apm.jenkins.core.entity.MetricPathComparisonResult;
import com.ca.apm.jenkins.core.entity.MetricPerformanceData;
import com.ca.apm.jenkins.core.entity.TimeSliceValue;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper utility to provide you the average, minimum, max values of the performance data per
 * metric_path
 *
 * <p>It also provides you formula to do percentage based or threshold value based comparison of
 * metrics between the two given builds
 *
 * @author Avinash Chandwani
 */
public class FormulaHelper {

  private FormulaHelper() {
    super();
  }

  /**
   * Get the Time Slices group by metric path, this is useful to pass the value to output-handler
   *
   * @param buildPerformanceData The buildPerformanceData object is retrieved using
   *     MetricDataHelper.getMetricData using the specified arguments
   * @return Map of Metric Path as key and value as list of TimeSlices of performance values
   *     obtained from APM
   */
  public static Map<String, List<TimeSliceValue>> getTimeSliceGroupByMetricPath(
      BuildPerformanceData buildPerformanceData) {
    Map<String, List<TimeSliceValue>> pathTimeSlices = null;
    Collection<MetricPerformanceData> performanceData = buildPerformanceData.getMetricData();
    if (performanceData.isEmpty()) {
      return null;
    }
    pathTimeSlices = new HashMap<String, List<TimeSliceValue>>();
    for (MetricPerformanceData mpd : performanceData) {
      String metricPath = mpd.getMetricPath();
      List<TimeSliceValue> timeSlices = mpd.getTimeSliceValues();
      pathTimeSlices.put(metricPath, timeSlices);
    }
    return pathTimeSlices;
  }

  /**
   * Get the average values of each metric path of the given build's performance data
   *
   * @param buildPerformanceData The entity you receive from MetricDataHelper on querying the
   *     metrics
   * @return Returns a Map with key = metricPath and value = the average of the metric value
   */
  public static Map<String, Double> getAverageValues(BuildPerformanceData buildPerformanceData) {
    if (buildPerformanceData == null) {
      JenkinsPlugInLogger.warning(
          "Build Performance Metric data is empty, hence not applying the average formula");
      return null;
    }
    Collection<MetricPerformanceData> transactions = buildPerformanceData.getMetricData();
    Map<String, Double> buildAverageValues = new HashMap<String, Double>();
    for (MetricPerformanceData btData : transactions) {
      String metricPath = btData.getMetricPath();
      List<TimeSliceValue> values = btData.getTimeSliceValues();
      int count = 0;
      double avgValue = 0.0;
      for (TimeSliceValue tValue : values) {
        double val = tValue.getValue();
        count++;
        avgValue += val;
      }
      if (count != 0) {
        avgValue = avgValue / count;
      }
      avgValue = JenkinsPluginUtility.getDoubleFormattedToTwoDecimalPlaces(avgValue);
      buildAverageValues.put(metricPath, avgValue);
    }
    return buildAverageValues;
  }

  /**
   * Get the maximum values of each metric path of the given build's performance data
   *
   * @param buildPerformanceData The entity you receive from MetricDataHelper on querying the
   *     metrics
   * @return Returns a Map with key = metricPath and value = the average of the metric value
   */
  public static Map<String, Double> getMaxValues(BuildPerformanceData buildPerformanceData) {
    if (buildPerformanceData == null) {
      JenkinsPlugInLogger.warning(
          "Build Performance Metric data is empty, hence not applying the max formula");
      return null;
    }
    Collection<MetricPerformanceData> transactions = buildPerformanceData.getMetricData();
    Map<String, Double> buildAverageValues = new HashMap<String, Double>();
    for (MetricPerformanceData btData : transactions) {
      String metricPath = btData.getMetricPath();
      List<TimeSliceValue> values = btData.getTimeSliceValues();
      double maxValue = 0.0;
      for (TimeSliceValue tValue : values) {
        double val = tValue.getValue();
        if (val > maxValue) {
          maxValue = val;
        }
      }
      buildAverageValues.put(metricPath, maxValue);
    }
    return buildAverageValues;
  }

  /**
   * Get the minimum values of each metric path of the given build's performance data
   *
   * @param buildPerformanceData The entity you receive from MetricDataHelper on querying the
   *     metrics
   * @return Returns a Map with key =metricPath and value = the average of the metric value
   */
  public static Map<String, Double> getMinValues(BuildPerformanceData buildPerformanceData) {
    if (buildPerformanceData == null) {
      JenkinsPlugInLogger.warning(
          "Build Performance Metric data is empty, hence not applying the min formula");
      return null;
    }
    Collection<MetricPerformanceData> transactions = buildPerformanceData.getMetricData();
    Map<String, Double> buildAverageValues = new HashMap<String, Double>();
    for (MetricPerformanceData btData : transactions) {
      String metricPath = btData.getMetricPath();
      List<TimeSliceValue> values = btData.getTimeSliceValues();
      double minValue = 0.0;
      for (TimeSliceValue tValue : values) {
        double val = tValue.getValue();
        if (val < minValue) {
          minValue = val;
        }
      }
      buildAverageValues.put(metricPath, minValue);
    }
    return buildAverageValues;
  }

  /**
   * Get the percentage change in positive or negative value depending upon the values for any
   * metric
   *
   * @param expectedValue The metric value corresponding to bench-mark build
   * @param actualValue The metric value corresponding to current build
   * @return Returns the percentage change positive or negative of the actual value w.r.t. expected
   *     value
   */
  public static double getPercentageChange(double expectedValue, double actualValue) {
    double percentageChange;
    if (expectedValue == 0.0) {
      percentageChange = actualValue * 100;
    } else {
      percentageChange = ((actualValue - expectedValue) * 100) / expectedValue;
    }
    return percentageChange;
  }

  /**
   * This helper method provides you comparison-result w.r.t. your one agent specifier configured in
   * your strategies properties. This method does the comparison of your current build's performance
   * with bench-mark build's performance of corresponding metric-path If the current build's metric
   * path's value is threshold% greater than bench-mark build's performance, the metric record is
   * considered to be slow performing Else it is considered as to be performing well
   *
   * @param thresholdPercentage The percentage value defined by the user in the strategies
   *     configuration
   * @param benchMarkValues The bench-mark performance data, which is average, minimum, max or
   *     count, this can be created from other helpers like getAverageValues, getMinValues,
   *     getMaxValues present in this same helper class
   * @param currentValues Current performance values of Current Build, per metric-path
   * @return Refer AgentComparisonResult
   */
  public static AgentComparisonResult thresholdPercentageBasedCrossBuildMetricPathComparison(
      double thresholdPercentage,
      Map<String, Double> benchMarkValues,
      Map<String, Double> currentValues) {
    AgentComparisonResult agentComparisonResult = new AgentComparisonResult();
    for (String metricPath : benchMarkValues.keySet()) {
      double expectedValue = benchMarkValues.get(metricPath);
      double actualValue = currentValues.get(metricPath);
      double percentageChange = FormulaHelper.getPercentageChange(expectedValue, actualValue);
      MetricPathComparisonResult metricPathComparisonResult = new MetricPathComparisonResult();
      actualValue = JenkinsPluginUtility.getDoubleFormattedToTwoDecimalPlaces(actualValue);
      expectedValue = JenkinsPluginUtility.getDoubleFormattedToTwoDecimalPlaces(expectedValue);
      percentageChange =
          JenkinsPluginUtility.getDoubleFormattedToTwoDecimalPlaces(percentageChange);
      metricPathComparisonResult.setActualValue(actualValue);
      metricPathComparisonResult.setExpectedValue(expectedValue);
      metricPathComparisonResult.setMetricPath(metricPath);

      metricPathComparisonResult.setPercentageChange(percentageChange);
      if (percentageChange > thresholdPercentage) {
        agentComparisonResult.addToSlowTransactions(metricPathComparisonResult);
      } else {
        agentComparisonResult.addToSuccessfulTransactions(metricPathComparisonResult);
      }
    }
    return agentComparisonResult;
  }

  /**
   * This helper method provides you comparison-result w.r.t. your one agent specifier configured in
   * your strategies properties. This method does the comparison of your current build's performance
   * with threshold value provided in the configuration. If the current build's metric path's value
   * is greater than threshold value, the metric record is considered to be slow performing Else it
   * is considered as to be performing well
   *
   * @param thresholdValue The threshold value defined by the user in the strategies configuration
   * @param currentAverageValues The current build's performance data, which is average, minimum,
   *     max or count, this can be created from other helpers like getAverageValues, getMinValues,
   *     getMaxValues present in this same helper class
   * @return Refer AgentComparisonResult
   */
  public static AgentComparisonResult thresholdValueBasedCrossBuildMetricPathComparison(
      double thresholdValue, Map<String, Double> currentAverageValues) {
    AgentComparisonResult agentComparisonResult = new AgentComparisonResult();
    for (String metricPath : currentAverageValues.keySet()) {
      double actualValue = currentAverageValues.get(metricPath);
      double percentageChange = FormulaHelper.getPercentageChange(thresholdValue, actualValue);
      MetricPathComparisonResult metricPathComparisonResult = new MetricPathComparisonResult();
      metricPathComparisonResult.setActualValue(actualValue);
      metricPathComparisonResult.setExpectedValue(thresholdValue);
      metricPathComparisonResult.setMetricPath(metricPath);
      metricPathComparisonResult.setPercentageChange(percentageChange);
      if (percentageChange > thresholdValue) {
        agentComparisonResult.addToSlowTransactions(metricPathComparisonResult);
      } else {
        agentComparisonResult.addToSuccessfulTransactions(metricPathComparisonResult);
      }
    }
    return agentComparisonResult;
  }

  public static void convertBytesIntoMegaBytes(BuildPerformanceData buildPerformanceData) {
    Collection<MetricPerformanceData> metricDataCollection = null;
    metricDataCollection = buildPerformanceData.getMetricData();
    if (metricDataCollection == null || metricDataCollection.isEmpty()) {
      return;
    }
    for (MetricPerformanceData metricPerformanceData : metricDataCollection) {
      List<TimeSliceValue> timeSlices = metricPerformanceData.getTimeSliceValues();
      for (TimeSliceValue tsV : timeSlices) {
        double maxMB = tsV.getMax() / (1024 * 1024);
        double minMB = tsV.getMin() / (1024 * 1024);
        double valMB = tsV.getValue() / (1024 * 1024);
        maxMB = JenkinsPluginUtility.getDoubleFormattedToTwoDecimalPlaces(maxMB);
        minMB = JenkinsPluginUtility.getDoubleFormattedToTwoDecimalPlaces(minMB);
        valMB = JenkinsPluginUtility.getDoubleFormattedToTwoDecimalPlaces(valMB);
        tsV.setMax(maxMB);
        tsV.setMin(minMB);
        tsV.setValue(valMB);
      }
    }
  }
}
