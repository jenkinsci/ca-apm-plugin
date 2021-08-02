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
 * The executor for running the configured comparison-strategies. This takes
 * care of loading the basic and custom-created comparison strategies from the
 * extensions path and executes the comparison-strategy
 *
 * @author Avinash Chandwani
 */
public class ComparisonExecutor {

	private ComparisonMetadata comparisonMetadata;
	private ComparisonResult comparisonResult;
	private static final String SPACE = "     ";
	private static final String ERROR = "Error :";
	private static final String EXECUTIONFAILED = " execution failed";
	private static final String ERRORINEXECUTINGCOMPARISONSTRATEGY = "Error in executing comparison strategy ->";
	private static final String WITH = " with ->";

	public ComparisonExecutor(ComparisonMetadata metadataInfo) {
		this.comparisonMetadata = metadataInfo;
	}

	/** Start the execution of Comparison-Strategies */
	private void executeComparison(Map.Entry<String, StrategyConfiguration> comparisonStrategyEntry)
			throws BuildExecutionException, NoSuchMethodException, InstantiationException {
		APMConnectionInfo apmConnectionInfo = null;
		StringBuilder builder = new StringBuilder();
		StrategyConfiguration strategyConfiguration = comparisonStrategyEntry.getValue();
		JenkinsPlugInLogger.info("Executing comparisonStrategy " + comparisonStrategyEntry.getKey());

		String comparatorClass = strategyConfiguration
				.getPropertyValue(comparisonStrategyEntry.getKey() + "." + Constants.COMPARATORCLASSSNAME);
		try {

			Class<?> pluginClass = null;
			comparatorClass = Constants.COMPARATORCLASSPATH + "." + comparatorClass + "ComparisonStrategy";
			pluginClass = comparisonMetadata.getIoUtility().findClass(comparatorClass);
			Object comparisonStrategyObj = pluginClass.newInstance();
			Method setPropertiesMethod = pluginClass.getDeclaredMethod(Constants.COMPARATORCONFIGMETHOD,
					StrategyConfiguration.class);
			setPropertiesMethod.invoke(comparisonStrategyObj, strategyConfiguration);
			Method compareMethod = pluginClass.getDeclaredMethod(Constants.COMPARATOREXECUTEMETHOD, BuildInfo.class,
					BuildInfo.class);
			BuildInfo benchmarkBuildInfo = new BuildInfo();
			for (Map.Entry<String, BuildInfo> entry : comparisonMetadata.getLoadRunnerMetadataInfo()
					.getAppToBenchMarkBuildInfo().entrySet()) {
				if (comparisonStrategyEntry.getKey().substring(0, comparisonStrategyEntry.getKey().indexOf('.'))
						.equals(entry.getKey())) {
					benchmarkBuildInfo = entry.getValue();
					break;
				}
			}
			JenkinsPlugInLogger.fine("Before calling comparison-strategy, currentBuildInfo="
					+ comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo() + " and benchmarkBuildInfo="
					+ benchmarkBuildInfo);

			StrategyResult<?> strategyResult = (StrategyResult<?>) compareMethod.invoke(comparisonStrategyObj,
					benchmarkBuildInfo, comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo());
			if (strategyResult == null) {
				JenkinsPlugInLogger.severe("No result obtained from " + comparisonStrategyEntry.getKey());
			} else {
				strategyResult.setStrategyName(comparisonStrategyEntry.getKey());
				comparisonMetadata.addToStrategyResults(strategyResult);
				JenkinsPlugInLogger.fine(comparisonStrategyEntry.getKey() + " strategy completed successfully");
			}
			if (comparisonMetadata.getStrategiesInfo()
					.isComparisonStrategyNonMapped(comparisonStrategyEntry.getKey())) {
				JenkinsPlugInLogger.warning(comparisonStrategyEntry.getKey()
						+ " is not mapped to any output-handler, hence this output will not be used any where");
				builder.append(SPACE + comparisonStrategyEntry.getKey()
						+ " is not mapped to any output-handler, hence this output will not be used any where")
						.append(Constants.NEWLINE);
			}
		} catch (ClassNotFoundException e) {
			JenkinsPlugInLogger.severe(comparatorClass + " could not be found which is configured  for "
					+ comparisonStrategyEntry.getKey() + " strategy, hence ignoring this comparison", e);
			builder.append(SPACE).append(ERROR + comparisonStrategyEntry.getKey() + EXECUTIONFAILED + e.getMessage())
					.append(Constants.NEWLINE);

		} catch (IllegalAccessException e) {
			JenkinsPlugInLogger
					.severe(ERRORINEXECUTINGCOMPARISONSTRATEGY + comparisonStrategyEntry.getKey() + e.getMessage(), e);
			builder.append(SPACE).append(ERROR + comparisonStrategyEntry.getKey() + EXECUTIONFAILED + e.getMessage())
					.append(Constants.NEWLINE);

		} catch (InvocationTargetException e) {

			if ((e.getTargetException().getMessage() != null)
					&& (e.getTargetException().getMessage().contains("Connection refused"))) {
				apmConnectionInfo = comparisonMetadata.getApmConnectionInfo();
				int apmHostNameIndex = apmConnectionInfo.getEmURL().indexOf("//") + 2;
				if (e.getTargetException().getMessage().contains(apmConnectionInfo.getEmURL()
						.substring(apmHostNameIndex, apmConnectionInfo.getEmURL().lastIndexOf(':')))) {
					throw new BuildExecutionException(e.getTargetException().getMessage());
				}
			} else if ((e.getTargetException().getMessage() != null)
					&& (e.getTargetException().getMessage().contains("Unauthorized"))) {
				throw new BuildExecutionException(e.getTargetException().getMessage());
			} else {
				JenkinsPlugInLogger.severe(ERRORINEXECUTINGCOMPARISONSTRATEGY + comparisonStrategyEntry.getKey() + WITH
						+ e.getTargetException(), e);
				builder.append(SPACE)
						.append(ERROR + comparisonStrategyEntry.getKey() + EXECUTIONFAILED + e.getMessage())
						.append(Constants.NEWLINE);

			}
		}

	}

	/** Start the execution of Comparison-Strategies */
	void execute() throws BuildExecutionException {
		JenkinsPlugInLogger.printLogOnConsole(1, "Starting to execute comparison strategies" + Constants.NEWLINE);
		JenkinsPlugInLogger.info("Starting to execute comparison strategies");
		StringBuilder builder = new StringBuilder();
		Map<String, StrategyConfiguration> comparisonStrategies = comparisonMetadata.getStrategiesInfo()
				.getComparisonStrategiesInfo();
		if (comparisonStrategies == null || comparisonStrategies.isEmpty()) {
			JenkinsPlugInLogger.severe("No comparison-strategy configuration was found, hence stopping the plugin-run");
			JenkinsPlugInLogger.printLogOnConsole(1,
					"No comparison-strategy configuration was found, hence stopping the plugin-run");
			return;
		}

		for (Map.Entry<String, StrategyConfiguration> comparisonStrategyEntry : comparisonStrategies.entrySet()) {
			try {
				executeComparison(comparisonStrategyEntry);
			} catch (InstantiationException | NoSuchMethodException | SecurityException e) {
				JenkinsPlugInLogger.severe(
						ERRORINEXECUTINGCOMPARISONSTRATEGY + comparisonStrategyEntry.getKey() + WITH + e.getMessage(),
						e);
				builder.append(SPACE)
						.append(ERROR + comparisonStrategyEntry.getKey() + EXECUTIONFAILED + e.getMessage())
						.append(Constants.NEWLINE);
				continue;
			}
			builder.append(SPACE).append(comparisonStrategyEntry.getKey() + " executed successfully")
					.append(Constants.NEWLINE);
		}
		JenkinsPlugInLogger.info("    Total Number of strategies executed were " + comparisonStrategies.size()
				+ Constants.NEWLINE + builder.toString());
		JenkinsPlugInLogger.printLogOnConsole(3, "Total Number of strategies executed were "
				+ comparisonStrategies.size() + Constants.NEWLINE + builder.toString());
		JenkinsPlugInLogger.printLogOnConsole(2, "Comparison Strategy Step completed successfully" + Constants.NEWLINE);
		JenkinsPlugInLogger.info("Comparison Strategy Step completed successfully" + Constants.NEWLINE);
	}

	public ComparisonResult getComparisonResult() {
		return comparisonResult;
	}
}
