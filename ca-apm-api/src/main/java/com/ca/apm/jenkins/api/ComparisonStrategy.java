package com.ca.apm.jenkins.api;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;

/**
 * This interface the contract for implementing a comparison Strategy. setConfiguration method gets
 * called up when initializing this strategy in which StrategyConfiguration entity is provided,
 * which contains all the properties configured for this Strategy. The main formula for comparison
 * has to be implemented in doCompare method. The properties present in StrategyConfiguration
 * provides you all the query inputs for getting metrics information from APM.
 *
 * @author Avinash Chandwani
 */
public interface ComparisonStrategy<T> {

  /**
   * This method is called automatically by plug-in run, whose main object is to provide you a
   * comparison strategy attribute entity, which have complete information about the particular
   * comparison strategy in question, You can create a local variable in your implemented class to
   * get hold of the properties required by you for executing the comparison strategy
   *
   * @param strategyConfiguration The comparison-strategy specific configuration provided in the
   *     properties file
   */
  public void setConfiguration(StrategyConfiguration strategyConfiguration);

  /**
   * This method compares the performance of current build with the benchmark build.
   *
   * @param benchMarkBuild This object contains build number, start time and end time of benchmark
   *     build number selected
   * @param currentBuild This object contains build number, start time and end time of the current
   *     build
   * @return ComparisonStrategyResult object is returned, which contain detailed transaction to
   *     transaction metric comparison values
   * @throws BuildComparatorException ComparisonStrategyResult object is returned, which contain
   *     detailed transaction to transaction metric comparison values
   */
  public StrategyResult<T> doCompare(BuildInfo benchMarkBuild, BuildInfo currentBuild)
      throws BuildExecutionException;
}
