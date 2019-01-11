package com.ca.apm.jenkins.core.entity;

import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This POJO is the holder of the complete metric comparisonr result. It holds a map ( Key =
 * Comparison Strategy Name, value = Comparison Strategy Result )
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
      strategyResults = new LinkedList<StrategyResult<?>>();
    }
    strategyResults.add(strategyResult);
  }

  public List<StrategyResult<?>> getSelectiveComparisonResults(
      String outputHandler, Set<String> comparisonStrategies) {
    if (comparisonStrategies == null || comparisonStrategies.isEmpty()) {
      JenkinsPlugInLogger.warning(
          "The output handler "
              + outputHandler
              + " is not mapped to any comparison-strategy, hence no results obtained");
      return null;
    } else {
      List<StrategyResult<?>> selectedStrategyResults = new LinkedList<StrategyResult<?>>();
      if (strategyResults == null) {
        JenkinsPlugInLogger.warning(
            "Comparison Strategy Phase did not produce any output, hence output handler won't receive any output to process");
        return null;
      }
      for (StrategyResult<?> strategyResult : strategyResults) {
        String strategyName = strategyResult.getStrategyName();
        if (comparisonStrategies.contains(strategyName)) {
          selectedStrategyResults.add(strategyResult);
        } else {
          JenkinsPlugInLogger.warning(strategyName + " is not mapped with " + outputHandler);
        }
      }
      return selectedStrategyResults;
    }
  }
}
