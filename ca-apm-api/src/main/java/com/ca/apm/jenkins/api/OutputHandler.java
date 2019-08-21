package com.ca.apm.jenkins.api;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import java.util.List;

/**
 * This interface is a contract for you to implement your own output handling with the help of
 * comparison result we are providing and some utilities like EmailHelper. setOutputConfiguration is
 * called during startup of this object, which provides you the output handler configuration and
 * some generic parameters and their values
 *
 * @author Avinash Chandwani
 */
public interface OutputHandler<T> {

  /**
   * This method is automatically called by Output Executor during the plugin run You can setup
   * local variables from buildRunInfo object provided to you. This will be useful for you in the
   * output publishing
   *
   * @param outputConfiguration The configuration values of the general output-handling properties
   *     like output directory etc.
   */
  public void setOutputConfiguration(OutputConfiguration outputConfiguration);

  /**
   * This method is called for publishing your output the way you want to produce We have provided
   * many APIs like TemplateHelper and EmailHelper. You can generate output in any format using the
   * comparison result entity as the data points and can export output in any manner
   *
   * @param strategyResults This is a list of selected comparison-strategy results, which you had
   *     configured in the properties
   * @throws BuildComparatorException Throws BuildComparatorException with proper message if any
   *     error/exception has occured during the execution
   */

  /**
   * This method is called for publishing your output the way you want to produce We have provided
   * many APIs like TemplateHelper and EmailHelper. You can generate output in any format using the
   * comparison result entity as the data points and can export output in any manner
   *
   * @param strategyResults This is a list of selected comparison-strategy results, which you had
   *     configured in the properties
   * @throws BuildComparatorException Throws BuildComparatorException with proper message if any
   *     error/exception has occured during the execution
   * @throws BuildExecutionException Throws BuildExecutionException with proper message if any
   *     error/exception has occured during the execution
   */
  public void publishOutput(List<T> strategyResults)
      throws BuildExecutionException;
}
