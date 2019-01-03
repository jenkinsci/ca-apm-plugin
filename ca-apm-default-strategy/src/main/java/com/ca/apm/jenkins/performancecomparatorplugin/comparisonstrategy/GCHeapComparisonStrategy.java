package com.ca.apm.jenkins.performancecomparatorplugin.comparisonstrategy;

import java.util.List;
import java.util.Map;

import com.ca.apm.jenkins.api.ComparisonStrategy;
import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.AgentComparisonResult;
import com.ca.apm.jenkins.core.entity.BuildPerformanceData;
import com.ca.apm.jenkins.core.entity.DefaultStrategyResult;
import com.ca.apm.jenkins.core.entity.TimeSliceValue;
import com.ca.apm.jenkins.core.helper.FormulaHelper;
import com.ca.apm.jenkins.core.helper.MetricDataHelper;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;

/**
 * A Default implementation of the comparison-strategy. The main goal of this
 * strategy is to compare the GC Heap use metrics
 * 
 * The goal of this comparison-strategy is to compare the GC Heap : Bytes in Use
 * of given agent specifier
 * 
 * It takes the average of the GC Heap metric and compares the current-build's
 * metric path value with the bench-mark build's same corresponding value It
 * uses the threshold percentage value from the configuration. If the
 * performance of a metric path is found to be greater than bench-mark build's
 * corresponding metric path value by a given threshold, the metric-path is said
 * be performing slow else performing under threshold limits
 * 
 * @author Avinash Chandwani
 *
 */
public class GCHeapComparisonStrategy implements ComparisonStrategy<DefaultStrategyResult> {

	private StrategyConfiguration strategyConfiguration;
	private String comparisonStrategyName;

	public void setConfiguration(StrategyConfiguration strategyConfiguration) {
		this.strategyConfiguration = strategyConfiguration;
		this.comparisonStrategyName = strategyConfiguration.getPropertyValue("name");
	}

	public StrategyResult<DefaultStrategyResult> doCompare(BuildInfo benchMarkBuild, BuildInfo currentBuild)
			throws BuildComparatorException, BuildExecutionException {

		JenkinsPlugInLogger.fine("GC Heap Strategy comparison has been started");
		String threshold = strategyConfiguration.getPropertyValue(comparisonStrategyName + "." + Constants.threshold);
		double thresholdValue = Double.parseDouble(threshold);
		String metricSpecifier = strategyConfiguration.getPropertyValue(comparisonStrategyName  + "."+ Constants.metricSpecifier);
		List<String> agentSpecifiers = strategyConfiguration.getAgentSpecifiers();

		StrategyResult<DefaultStrategyResult> comparisonOutput = new StrategyResult<DefaultStrategyResult>();
		DefaultStrategyResult strategyResult = new DefaultStrategyResult();
		comparisonOutput.setResult(strategyResult);

		for (String agentSpecifier : agentSpecifiers) {
			AgentComparisonResult agentComparisonResult = null;
			try {
				BuildPerformanceData benchMarkPerformanceData = MetricDataHelper.getMetricData(agentSpecifier,
						metricSpecifier, benchMarkBuild);
				BuildPerformanceData currentBuildPerformanceData = MetricDataHelper.getMetricData(agentSpecifier,
						metricSpecifier, currentBuild);
				FormulaHelper.convertBytesIntoMegaBytes(benchMarkPerformanceData);
				FormulaHelper.convertBytesIntoMegaBytes(currentBuildPerformanceData);
				Map<String, Double> benchMarkAverageValues = FormulaHelper.getAverageValues(benchMarkPerformanceData);
				Map<String, Double> currentAverageValues = FormulaHelper.getAverageValues(currentBuildPerformanceData);
				agentComparisonResult = FormulaHelper.thresholdPercentageBasedCrossBuildMetricPathComparison(
						thresholdValue, benchMarkAverageValues, currentAverageValues);
				Map<String, List<TimeSliceValue>> benchMarkSliceValues = FormulaHelper
						.getTimeSliceGroupByMetricPath(benchMarkPerformanceData);
				Map<String, List<TimeSliceValue>> currentSliceValues = FormulaHelper
						.getTimeSliceGroupByMetricPath(currentBuildPerformanceData);
				agentComparisonResult.attachEveryPointResult(benchMarkSliceValues, currentSliceValues);
			} catch (BuildComparatorException e) {
				JenkinsPlugInLogger.severe("An error has occured while collecting performance metrics for "
						+ comparisonStrategyName + "from APM-> for agentSpecifier=" + agentSpecifier
						+ ",metricSpecifier =" + metricSpecifier + e.getMessage(), e);
				continue;
			}
			strategyResult.addOneResult(agentSpecifier, agentComparisonResult);
		}
		JenkinsPlugInLogger.fine("GC Heap Strategy comparison has been completed successfully");
		return comparisonOutput;
	}
}