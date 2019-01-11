package com.ca.apm.jenkins.core.executor;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.APMConnectionInfo;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.entity.ComparisonResult;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * The executor for running the configured comparison-strategies. This takes care of loading the
 * basic and custom-created comparison strategies from the extensions path and executes the
 * comparison-strategy
 *
 * @author Avinash Chandwani
 */
public class ComparisonExecutor {

  private ComparisonMetadata comparisonMetadata;
  private ComparisonResult comparisonResult;

  public ComparisonExecutor(ComparisonMetadata metadataInfo) {
    this.comparisonMetadata = metadataInfo;
  }

  /** Start the execution of Comparison-Strategies */
  void execute() throws BuildExecutionException {
    JenkinsPlugInLogger.printLogOnConsole(
        1, "Starting to execute comparison strategies" + Constants.NewLine);
    JenkinsPlugInLogger.info("Starting to execute comparison strategies");
    StringBuilder builder = new StringBuilder();
    Map<String, StrategyConfiguration> comparisonStrategies =
        comparisonMetadata.getStrategiesInfo().getComparisonStrategiesInfo();
    if (comparisonStrategies == null || comparisonStrategies.isEmpty()) {
      JenkinsPlugInLogger.severe(
          "No comparison-strategy configuration was found, hence stopping the plugin-run");
      JenkinsPlugInLogger.printLogOnConsole(
          1, "No comparison-strategy configuration was found, hence stopping the plugin-run");
      return;
    }
    for (String comparisonStrategy : comparisonStrategies.keySet()) {
      StrategyConfiguration strategyConfiguration = comparisonStrategies.get(comparisonStrategy);
      String comparatorClass =
          strategyConfiguration.getPropertyValue(
              comparisonStrategy + "." + Constants.comparatorClasssName);
      APMConnectionInfo apmConnectionInfo = null;
      Class<?> pluginClass = null;
      try {
        pluginClass = comparisonMetadata.getIoUtility().findClass(comparatorClass);
        Object comparisonStrategyObj = pluginClass.newInstance();
        Method setPropertiesMethod =
            pluginClass.getDeclaredMethod(
                Constants.comparatorConfigMethod, StrategyConfiguration.class);
        setPropertiesMethod.invoke(comparisonStrategyObj, strategyConfiguration);
        Method compareMethod =
            pluginClass.getDeclaredMethod(
                Constants.comparatorExecuteMethod, BuildInfo.class, BuildInfo.class);

        JenkinsPlugInLogger.fine(
            "Before calling comparison-strategy, currentBuildInfo="
                + comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo()
                + " and benchmarkBuildInfo="
                + comparisonMetadata.getLoadRunnerMetadataInfo().getBenchMarkBuildInfo());

        StrategyResult<?> strategyResult =
            (StrategyResult<?>)
                compareMethod.invoke(
                    comparisonStrategyObj,
                    comparisonMetadata.getLoadRunnerMetadataInfo().getBenchMarkBuildInfo(),
                    comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo());
        if (strategyResult == null) {
          JenkinsPlugInLogger.severe("No result obtained from " + comparisonStrategy);
        } else {
          strategyResult.setStrategyName(comparisonStrategy);
          comparisonMetadata.addToStrategyResults(strategyResult);
          JenkinsPlugInLogger.fine(comparisonStrategy + " strategy completed successfully");
        }
        if (comparisonMetadata
            .getStrategiesInfo()
            .isComparisonStrategyNonMapped(comparisonStrategy)) {
          JenkinsPlugInLogger.warning(
              comparisonStrategy
                  + " is not mapped to any output-handler, hence this output will not be used any where");
          builder
              .append(
                  "     "
                      + comparisonStrategy
                      + " is not mapped to any output-handler, hence this output will not be used any where")
              .append(Constants.NewLine);
        }
      } catch (ClassNotFoundException e) {
        JenkinsPlugInLogger.severe(
            comparatorClass
                + " could not be found which is configured  for "
                + comparisonStrategy
                + " strategy, hence ignoring this comparison",
            e);
        builder
            .append("     ")
            .append("Error :" + comparisonStrategy + " execution failed" + e.getMessage())
            .append(Constants.NewLine);
        continue;
      } catch (IllegalAccessException e) {
        JenkinsPlugInLogger.severe(
            "Error in executing comparison strategy ->" + comparisonStrategy + e.getMessage(), e);
        builder
            .append("     ")
            .append("Error :" + comparisonStrategy + " execution failed" + e.getMessage())
            .append(Constants.NewLine);
        continue;
      } catch (InvocationTargetException e) {
    	 if((e.getTargetException().getMessage()!=null)&& (e.getTargetException().getMessage().toString().contains("Connection refused")))
        {
          apmConnectionInfo = comparisonMetadata.getApmConnectionInfo();
          int apmHostNameIndex = apmConnectionInfo.getEmURL().indexOf("//") + 2;
          if (e.getTargetException()
              .getMessage()
              .contains(
                  apmConnectionInfo
                      .getEmURL()
                      .substring(
                          apmHostNameIndex, apmConnectionInfo.getEmURL().lastIndexOf(':')))) {
            throw new BuildExecutionException(e.getTargetException().getMessage());
          }
        } else if ((e.getTargetException().getMessage()!=null) && (e.getTargetException().getMessage().toString().contains("Unauthorized"))) {
          throw new BuildExecutionException(e.getTargetException().getMessage());
        } else {
          JenkinsPlugInLogger.severe(
              "Error in executing comparison strategy ->"
                  + comparisonStrategy
                  + " with ->"
                  + e.getTargetException(),
              e);
          builder
              .append("     ")
              .append("Error :" + comparisonStrategy + " execution failed" + e.getMessage())
              .append(Constants.NewLine);
          continue;
        }
      } catch (InstantiationException e) {
        JenkinsPlugInLogger.severe(
            "Error in executing comparison strategy ->"
                + comparisonStrategy
                + " with ->"
                + e.getMessage(),
            e);
        builder
            .append("     ")
            .append("Error :" + comparisonStrategy + " execution failed" + e.getMessage())
            .append(Constants.NewLine);
        continue;
      } catch (NoSuchMethodException e) {
        JenkinsPlugInLogger.severe(
            "Error in executing comparison strategy ->"
                + comparisonStrategy
                + " with ->"
                + e.getMessage(),
            e);
        builder
            .append("     ")
            .append("Error :" + comparisonStrategy + " execution failed" + e.getMessage())
            .append(Constants.NewLine);
        continue;
      } catch (SecurityException e) {
        JenkinsPlugInLogger.severe(
            "Error in executing comparison strategy ->"
                + comparisonStrategy
                + " with ->"
                + e.getMessage(),
            e);
        builder
            .append("     ")
            .append("Error :" + comparisonStrategy + " execution failed")
            .append(Constants.NewLine);
        continue;
      }
      builder
          .append("     ")
          .append(comparisonStrategy + " executed successfully")
          .append(Constants.NewLine);
    }
    JenkinsPlugInLogger.info(
        "    Total Number of strategies executed were "
            + comparisonStrategies.size()
            + Constants.NewLine
            + builder.toString());
    JenkinsPlugInLogger.printLogOnConsole(
        3,
        "Total Number of strategies executed were "
            + comparisonStrategies.size()
            + Constants.NewLine
            + builder.toString());
    JenkinsPlugInLogger.printLogOnConsole(
        2, "Comparison Strategy Step completed successfully" + Constants.NewLine);
    JenkinsPlugInLogger.info("Comparison Strategy Step completed successfully" + Constants.NewLine);
  }

  public ComparisonResult getComparisonResult() {
    return comparisonResult;
  }
}
