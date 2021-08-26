package com.ca.apm.jenkins.core.executor;

import java.util.Iterator;
import java.util.List;

import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.core.entity.AgentComparisonResult;
import com.ca.apm.jenkins.core.entity.ComparisonResult;
import com.ca.apm.jenkins.core.entity.DefaultStrategyResult;
import com.ca.apm.jenkins.core.entity.MetricPathComparisonResult;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;

public class BuildDecisionMaker {

	private ComparisonResult comparisonResult;
	private boolean isFailed = false;

	public BuildDecisionMaker(ComparisonResult comparisonResult) {
		this.comparisonResult = comparisonResult;
	}

	boolean isFailed() {
		JenkinsPlugInLogger.info("Inside isFailed Method ");
		if (comparisonResult == null || comparisonResult.getStrategyResults() == null) {
			JenkinsPlugInLogger.severe(
					"Comparison Result is not generated, hence cannot find out whether to fail or pass the current build");
			isFailed = true;
			return isFailed;
		}else if (comparisonResult.getStrategyResults().isEmpty()) {
				JenkinsPlugInLogger.severe(
					"Comparison Result is not generated, hence making the build fail, check the logs for more details ");
				return isFailed;
		}

		for (StrategyResult<?> strategyResult : comparisonResult.getStrategyResults()) {

			if (strategyResult.getResult().getClass().getName()
					.equals("com.ca.apm.jenkins.core.entity.DefaultStrategyResult")) {
				DefaultStrategyResult defaultStrategyResult = (DefaultStrategyResult) strategyResult.getResult();
				getBuildStatus(strategyResult, defaultStrategyResult);
			} else {
				JenkinsPlugInLogger.warning(strategyResult.getStrategyName()
						+ " is not a default strategy, hence its output cannot be judged to pass/fail the build");
			}
		}
		JenkinsPlugInLogger.printLogOnConsole(2, Constants.NEWLINE);
		return isFailed;
	}

	private void getBuildStatus(StrategyResult<?> strategyResult, DefaultStrategyResult defaultStrategyResult) {

		for (String agentSpecifier : defaultStrategyResult.getResult().keySet()) {
			AgentComparisonResult agentComparisonResult = defaultStrategyResult.getResult().get(agentSpecifier);
			List<MetricPathComparisonResult> slowEntries = agentComparisonResult.getSlowEntries();

			if (slowEntries != null && !slowEntries.isEmpty()) {
				List<MetricPathComparisonResult> slowEntrieslist = agentComparisonResult.getSlowEntries();
				Iterator it = slowEntrieslist.iterator();

				JenkinsPlugInLogger
						.info("Decision maker found failure in strategy result " + strategyResult.getStrategyName());
				JenkinsPlugInLogger.printLogOnConsole(2, "  *******" + strategyResult.getStrategyName()
						+ "'s performance crossed the threshold mark *******");
				while (it.hasNext()) {
					MetricPathComparisonResult metricPathComparisonResult = (MetricPathComparisonResult) it.next();
					JenkinsPlugInLogger.info("  " + strategyResult.getStrategyName() + ": metricPath = "
							+ metricPathComparisonResult.getMetricPath() + ", expectedValue = "
							+ metricPathComparisonResult.getExpectedValue() + ", actualValue = "
							+ metricPathComparisonResult.getActualValue() + ", percentageChange = "
							+ metricPathComparisonResult.getPercentageChange() + ", thresholdValue = "
							+ metricPathComparisonResult.getThresholdPercentage());

					JenkinsPlugInLogger.printLogOnConsole(2,
							"  " + strategyResult.getStrategyName() + ": metricPath = "
									+ metricPathComparisonResult.getMetricPath() + ", expectedValue = "
									+ metricPathComparisonResult.getExpectedValue() + ", actualValue = "
									+ metricPathComparisonResult.getActualValue() + ", percentageChange = "
									+ metricPathComparisonResult.getPercentageChange() + ", thresholdValue = "
									+ metricPathComparisonResult.getThresholdPercentage());
				}
				isFailed = true;
				break;
			}
		}

	}

}
