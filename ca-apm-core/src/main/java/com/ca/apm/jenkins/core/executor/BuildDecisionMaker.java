package com.ca.apm.jenkins.core.executor;

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

	public BuildDecisionMaker(ComparisonResult comparisonResult) {
		this.comparisonResult = comparisonResult;
	}

	boolean isFailed() {
		boolean isFailed = false;
		JenkinsPlugInLogger.info("Inside isFailed Method-->" + comparisonResult.getStrategyResults().size());
		if (comparisonResult == null || comparisonResult.getStrategyResults() == null
				|| comparisonResult.getStrategyResults().isEmpty()) {
			JenkinsPlugInLogger.severe(
					"Comparison Result is not generated, hence cannot find out whether to fail or pass the current build");
			return isFailed;
		}

		for (StrategyResult<?> strategyResult : comparisonResult.getStrategyResults()) {

			if (strategyResult.getResult().getClass().getName()
					.equals("com.ca.apm.jenkins.core.entity.DefaultStrategyResult")) {
				DefaultStrategyResult defaultStrategyResult = (DefaultStrategyResult) strategyResult.getResult();
				for (String agentSpecifier : defaultStrategyResult.getResult().keySet()) {
					AgentComparisonResult agentComparisonResult = defaultStrategyResult.getResult().get(agentSpecifier);
					List<MetricPathComparisonResult> slowEntries = agentComparisonResult.getSlowEntries();
					if (slowEntries == null || slowEntries.isEmpty()) {
						continue;
					}
					JenkinsPlugInLogger.info(
							"Decision maker found failure in strategy result " + strategyResult.getStrategyName());
					JenkinsPlugInLogger.printLogOnConsole(2,"  *******"+ strategyResult.getStrategyName()+ "'s performance crossed the threshold mark *******" );
					isFailed = true;
					break;
				}

			} else {
				JenkinsPlugInLogger.warning(strategyResult.getStrategyName()
						+ " is not a default strategy, hence its output cannot be judged to pass/fail the build");
			}

		}
		JenkinsPlugInLogger.printLogOnConsole(2,Constants.NewLine);
		return isFailed;
	}
}
