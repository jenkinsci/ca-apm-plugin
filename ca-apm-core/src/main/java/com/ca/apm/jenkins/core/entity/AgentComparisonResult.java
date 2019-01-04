package com.ca.apm.jenkins.core.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This entity holds the cross-build comparison-result of one agent specifier
 * configured for given Comparison-strategy. This contains the list of
 * successEntries and slowEntries
 * 
 * successEntries represent the comparison-result of those metric-paths whose
 * performance was considered to be under the benchmark or threshold-limits.
 * 
 * slowEntries represent the comparison-result of those metric-paths whose
 * performance was considered to be above the tolerance or threshold limits
 * configured
 * 
 * With the help of two separate lists, we can easily find out the trouble
 * causing components
 * 
 * By default, we have a few re-usable functions in FormulaHelper, you can use
 * them and get this entity generated for you. Also if you want to decide your
 * own way to choose whether to tag a comparison-result to be normal or
 * alarming, you can do it your own way as well
 * 
 * @author Avinash Chandwani
 *
 */
public class AgentComparisonResult {

	private List<MetricPathComparisonResult> successEntries;
	private List<MetricPathComparisonResult> slowEntries;

	public AgentComparisonResult() {
		successEntries = new LinkedList<MetricPathComparisonResult>();
		slowEntries = new LinkedList<MetricPathComparisonResult>();
	}

	public List<MetricPathComparisonResult> getSuccessEntries() {
		return successEntries;
	}

	public void addToSuccessfulTransactions(MetricPathComparisonResult comparisonOutput) {
		successEntries.add(comparisonOutput);
	}

	public void addToSlowTransactions(MetricPathComparisonResult comparisonOutput) {
		slowEntries.add(comparisonOutput);
	}

	public void setSlowEntries(List<MetricPathComparisonResult> slowEntries) {
		this.slowEntries = slowEntries;
	}

	public List<MetricPathComparisonResult> getSlowEntries() {
		return slowEntries;
	}

	public void setSuccessEntries(List<MetricPathComparisonResult> successEntries) {
		this.successEntries = successEntries;
	}

	@Override
	public String toString() {
		return "ComparisonResult [successEntries=" + successEntries + ", slowEntries=" + slowEntries + "]";
	}

	public void attachEveryPointResult(double threshold, Map<String, List<TimeSliceValue>> currentTimeSliceValues) {
		if (!successEntries.isEmpty()) {
			for (MetricPathComparisonResult comparisonResult : successEntries) {
				List<TimeSliceValue> actualValues = currentTimeSliceValues.get(comparisonResult.getMetricPath());
				List<TimeSliceValue> expectedValues = new LinkedList<TimeSliceValue>();
				for (int i = 0; i < actualValues.size(); i++) {
					TimeSliceValue tsV = new TimeSliceValue(threshold, threshold, threshold, 1, actualValues.get(i).getfrequency());
					expectedValues.add(tsV);
				}
				comparisonResult.setBenchMarkBuildTimeSliceValues(expectedValues);
				comparisonResult.setCurrentBuildTimeSliceValues(actualValues);
			}
		}
		if (!slowEntries.isEmpty()) {
			for (MetricPathComparisonResult comparisonResult : slowEntries) {
				List<TimeSliceValue> actualValues = currentTimeSliceValues.get(comparisonResult.getMetricPath());
				List<TimeSliceValue> expectedValues = new LinkedList<TimeSliceValue>();
				for (int i = 0; i < actualValues.size(); i++) {
					TimeSliceValue tsV = new TimeSliceValue(threshold, threshold, threshold, 1, actualValues.get(i).getfrequency());
					expectedValues.add(tsV);
				}
				comparisonResult.setBenchMarkBuildTimeSliceValues(expectedValues);
				comparisonResult.setCurrentBuildTimeSliceValues(actualValues);
			}
		}
	}

	public void attachEveryPointResult(Map<String, List<TimeSliceValue>> benchMarkTimeSliceValues,
			Map<String, List<TimeSliceValue>> currentTimeSliceValues) {
		if (!successEntries.isEmpty()) {
			for (MetricPathComparisonResult comparisonResult : successEntries) {
				comparisonResult.setBenchMarkBuildTimeSliceValues(
						benchMarkTimeSliceValues.get(comparisonResult.getMetricPath()));
				comparisonResult
						.setCurrentBuildTimeSliceValues(currentTimeSliceValues.get(comparisonResult.getMetricPath()));
			}
		}
		if (!slowEntries.isEmpty()) {
			for (MetricPathComparisonResult comparisonResult : slowEntries) {
				comparisonResult.setBenchMarkBuildTimeSliceValues(
						benchMarkTimeSliceValues.get(comparisonResult.getMetricPath()));
				comparisonResult
						.setCurrentBuildTimeSliceValues(currentTimeSliceValues.get(comparisonResult.getMetricPath()));
			}
		}
	}
}
