package com.ca.apm.jenkins.core.executor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.entity.ComparisonResult;
import com.ca.apm.jenkins.core.entity.OutputHandlerConfiguration;
import com.ca.apm.jenkins.core.entity.StrategiesInfo;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;

/**
 * The executor for running the configured output-handlers. This takes care of
 * loading the basic and custom-created output-handlers from the extensions path
 * and executes the output-handler step.
 *
 * @author Avinash Chandwani
 */
public class OutputHandlingExecutor {

	private ComparisonMetadata comparisonMetadata;

	private static final String SPACE = "     ";

	private static final String OUTPUTHANDLEREXECUTIONFAILED = " output handler execution failed";

	private static final String ERRORINEXECUTINGOUTPUTSTRATEGY = "Error in executing Output strategy ->";

	private static final String WITH = " with ->";
	private OutputConfiguration outputConfiguration;
	private boolean isFailtheBuild;

	public OutputHandlingExecutor(ComparisonMetadata comparisonMetadata) {
		this.comparisonMetadata = comparisonMetadata;
	}

	private void executeComparison(ComparisonResult comparisonResult, String outputHandlerClass,
			Map<String, OutputHandlerConfiguration> outputStrategies, StrategiesInfo strategiesInfo,
			Map.Entry<String, OutputHandlerConfiguration> outputHandlerEntry, StringBuilder builder)
			throws NoSuchMethodException, InstantiationException {
		try {
			Class<?> pluginClass = comparisonMetadata.getIoUtility().findClass(outputHandlerClass);
			Object outputHandlerObj = pluginClass.newInstance();
			Method setConfigurationMethod = pluginClass.getDeclaredMethod(Constants.OUTPUTHANDLERCONFIGMETHOD,
					OutputConfiguration.class);
			outputConfiguration
					.setHandlerSpecificProperties(outputStrategies.get(outputHandlerEntry.getKey()).getProperties());
			outputConfiguration.addToCommonProperties("buildStatus", isFailtheBuild ? "FAILURE" : "SUCCESS");
			outputConfiguration.addToCommonProperties("frequency", String
					.valueOf(comparisonMetadata.getComparisonResult().getStrategyResults().get(0).getFrequency()));
			setConfigurationMethod.invoke(outputHandlerObj, outputConfiguration);

			if (outputHandlerEntry.getKey().equals(Constants.HISTOGRAMOUTPUTHTML)) {

				Method setComparisonMetadataMethod = pluginClass
						.getDeclaredMethod(Constants.COMPARISONMETADATACONFIGMETHOD, ComparisonMetadata.class);
				setComparisonMetadataMethod.invoke(outputHandlerObj, comparisonMetadata);

			}
			Set<String> comparisonStrategies = strategiesInfo
					.getMappedComparisonStrategies(outputHandlerEntry.getKey());
			List<StrategyResult<?>> selectedStrategyResult = comparisonResult
					.getSelectiveComparisonResults(outputHandlerEntry.getKey(), comparisonStrategies);
			if (selectedStrategyResult.isEmpty()) {
				JenkinsPlugInLogger.warning("No results obtained from the selective comparison strategy for "
						+ outputHandlerEntry.getKey() + Constants.NEWLINE);
				JenkinsPlugInLogger.printLogOnConsole(3,
						"Warning : No results obtained from the selective comparison strategy for "
								+ outputHandlerEntry.getKey());

			}
			if (!selectedStrategyResult.isEmpty()) {
				Method publishMethod = pluginClass.getDeclaredMethod(Constants.OUTPUTHANDLEREXECUTEMETHOD, List.class);
				publishMethod.invoke(outputHandlerObj, selectedStrategyResult);
			}
		} catch (ClassNotFoundException e) {
			JenkinsPlugInLogger.severe("Qualified class " + outputHandlerClass
					+ " not found in the any extensions library. Hence ignoring the execution of this Output Handler"
					+ Constants.NEWLINE, e);
			JenkinsPlugInLogger.printLogOnConsole(3,
					"Qualified class " + outputHandlerClass + " not found in the any extensions library.");
			builder.append(SPACE).append(outputHandlerEntry.getKey() + OUTPUTHANDLEREXECUTIONFAILED)
					.append(Constants.NEWLINE);

		} catch (IllegalAccessException | InvocationTargetException e) {
			JenkinsPlugInLogger.severe(ERRORINEXECUTINGOUTPUTSTRATEGY + outputHandlerEntry.getKey() + WITH
					+ e.getMessage() + Constants.NEWLINE, e);
			builder.append(SPACE).append(outputHandlerEntry.getKey() + OUTPUTHANDLEREXECUTIONFAILED)
					.append(Constants.NEWLINE);

		}

	}

	/**
	 * Start the execution of Output-Handler
	 *
	 * @param outputConfiguration
	 * @throws BuildExecutionException
	 */
	void execute(OutputConfiguration outputConfiguration, boolean isFailtheBuild) {
		JenkinsPlugInLogger.printLogOnConsole(1, "Starting the output handling phase" + Constants.NEWLINE);
		JenkinsPlugInLogger.log(Level.INFO, "Output Handler Step started");
		this.isFailtheBuild = isFailtheBuild;
		this.outputConfiguration = outputConfiguration;
		StringBuilder builder = new StringBuilder();
		StrategiesInfo strategiesInfo = comparisonMetadata.getStrategiesInfo();
		Map<String, OutputHandlerConfiguration> outputStrategies = strategiesInfo.getOutputHandlersInfo();
		if (outputStrategies == null || outputStrategies.isEmpty()) {
			return;
		}
		ComparisonResult comparisonResult = comparisonMetadata.getComparisonResult();
		for (Map.Entry<String, OutputHandlerConfiguration> outputHandlerEntry : outputStrategies.entrySet()) {
			String outputHandlerClass = getOutputHandlerClass(outputHandlerEntry.getKey(), outputStrategies);
			try {
				executeComparison(comparisonResult, outputHandlerClass, outputStrategies, strategiesInfo,
						outputHandlerEntry, builder);
			} catch (InstantiationException | NoSuchMethodException | SecurityException e) {
				JenkinsPlugInLogger.severe(ERRORINEXECUTINGOUTPUTSTRATEGY + outputHandlerEntry.getKey() + WITH
						+ e.getMessage() + Constants.NEWLINE, e);
				builder.append(SPACE).append(outputHandlerEntry.getKey() + OUTPUTHANDLEREXECUTIONFAILED)
						.append(Constants.NEWLINE);
				continue;
			}
			builder.append(SPACE).append(outputHandlerEntry.getKey() + " output handler executed successfully")
					.append(Constants.NEWLINE);
		}
		JenkinsPlugInLogger.printLogOnConsole(3, "Total number of output handlers executed are "
				+ outputStrategies.size() + Constants.NEWLINE + builder.toString());
		JenkinsPlugInLogger.info("    Total number of output handlers executed are " + outputStrategies.size()
				+ Constants.NEWLINE + builder.toString());
		builder.setLength(0);
		JenkinsPlugInLogger.printLogOnConsole(2, "Output Handling Phase is completed successfully");
		JenkinsPlugInLogger.info("Output Handler Step completed");
		comparisonMetadata.getIoUtility().closeClassLoader();
	}

	private String getOutputHandlerClass(String outputHandler,
			Map<String, OutputHandlerConfiguration> outputStrategies) {
		String outputHandlerClass = null;
		if (outputHandler.equals(Constants.EMAILOUTPUTHANDLERNAME)) {
			outputHandlerClass = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler.PlainTextEmailOutputHandler";
		} else if (outputHandler.equals(Constants.JSONFILEOUTPUTHANDLERNAME)) {
			outputHandlerClass = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler.JSONFileStoreOutputHandler";
		} else if (outputHandler.equals(Constants.CHARTOUTPUTHANDLERNAME)) {
			outputHandlerClass = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler.ChartOutputHandler";
		} else if (outputHandler.equals(Constants.HISTOGRAMOUTPUTHANDLERNAME)) {
			outputHandlerClass = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler.HistogramOutputHandler";
		} else {
			outputHandlerClass = outputStrategies.get(outputHandler).getPropertyValue(outputHandler + ".outputhandler");
		}
		return outputHandlerClass;
	}
}
