package com.ca.apm.jenkins.core.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;

/**
 * This POJO is the holder of the complete metric comparisonr result. It holds a
 * map ( Key = Comparison Strategy Name, value = Comparison Strategy Result )
 *
 * @author Avinash Chandwani
 */
public class ComparisonResult {

	private List<StrategyResult<?>> strategyResults;

	public List<StrategyResult<?>> getStrategyResults() {
		return strategyResults;
	}

	public void addToComparisonStrategyResult(StrategyResult<?> strategyResult) {
		if (strategyResults == null) {
			strategyResults = new LinkedList<>();
		}
		strategyResults.add(strategyResult);
	}

	public List<StrategyResult<?>> getSelectiveComparisonResults(String outputHandler,
			Set<String> comparisonStrategies) {
		List<StrategyResult<?>> selectedStrategyResults = new LinkedList<>();
		if (comparisonStrategies == null || comparisonStrategies.isEmpty()) {
			JenkinsPlugInLogger.warning("The output handler " + outputHandler
					+ " is not mapped to any comparison-strategy, hence no results obtained");

		} else {

			if (strategyResults == null) {
				JenkinsPlugInLogger.warning(
						"Comparison Strategy Phase did not produce any output, hence output handler won't receive any output to process");

			}
			for (StrategyResult<?> strategyResult : strategyResults) {
				String strategyName = strategyResult.getStrategyName();
				if (comparisonStrategies.contains(strategyName)) {
					selectedStrategyResults.add(strategyResult);
				} else {
					JenkinsPlugInLogger.warning(strategyName + " is not mapped with " + outputHandler);
				}
			}
		}
		return selectedStrategyResults;
	}
}
