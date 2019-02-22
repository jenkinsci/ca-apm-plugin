package com.ca.apm.jenkins.core.executor;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.entity.ComparisonResult;
import com.ca.apm.jenkins.core.entity.OutputHandlerConfiguration;
import com.ca.apm.jenkins.core.entity.StrategiesInfo;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * The executor for running the configured output-handlers. This takes care of
 * loading the basic and custom-created output-handlers from the extensions path
 * and executes the output-handler step.
 *
 * @author Avinash Chandwani
 */
public class OutputHandlingExecutor {

	private ComparisonMetadata comparisonMetadata;

	public OutputHandlingExecutor(ComparisonMetadata comparisonMetadata) {
		this.comparisonMetadata = comparisonMetadata;
	}

	/**
	 * Start the execution of Output-Handler
	 *
	 * @param outputConfiguration
	 * @throws BuildExecutionException
	 */
	void execute(OutputConfiguration outputConfiguration, boolean isFailToBuild) {
		JenkinsPlugInLogger.printLogOnConsole(1, "Starting the output handling phase" + Constants.NewLine);
		JenkinsPlugInLogger.log(Level.INFO, "Output Handler Step started");
		StringBuilder builder = new StringBuilder();
		StrategiesInfo strategiesInfo = comparisonMetadata.getStrategiesInfo();
		Map<String, OutputHandlerConfiguration> outputStrategies = strategiesInfo.getOutputHandlersInfo();
		if (outputStrategies == null || outputStrategies.isEmpty()) {
			return;
		}
		ComparisonResult comparisonResult = comparisonMetadata.getComparisonResult();
		for (String outputHandler : outputStrategies.keySet()) {
			Class<?> pluginClass = null;
			String outputHandlerClass = getOutputHandlerClass(outputHandler, outputStrategies);
			try {
				pluginClass = comparisonMetadata.getIoUtility().findClass(outputHandlerClass);
				Object outputHandlerObj = pluginClass.newInstance();
				Method setConfigurationMethod = pluginClass.getDeclaredMethod(Constants.outputHandlerConfigMethod,
						OutputConfiguration.class);
				outputConfiguration.setHandlerSpecificProperties(outputStrategies.get(outputHandler).getProperties());
				outputConfiguration.addToCommonProperties("buildStatus", isFailToBuild == true ? "FAILURE" : "SUCCESS");
				outputConfiguration.addToCommonProperties("frequency", String
						.valueOf(comparisonMetadata.getComparisonResult().getStrategyResults().get(0).getFrequency()));
				setConfigurationMethod.invoke(outputHandlerObj, outputConfiguration);

				if (outputHandler.equals(Constants.histogramoutputhtml)) {

					Method setComparisonMetadataMethod = pluginClass
							.getDeclaredMethod(Constants.ComparisonMetadataConfigMethod, ComparisonMetadata.class);
					setComparisonMetadataMethod.invoke(outputHandlerObj, comparisonMetadata);

				}
				Set<String> comparisonStrategies = strategiesInfo.getMappedComparisonStrategies(outputHandler);
				List<StrategyResult<?>> selectedStrategyResult = comparisonResult
						.getSelectiveComparisonResults(outputHandler, comparisonStrategies);
				if (selectedStrategyResult == null) {
					JenkinsPlugInLogger.warning("No results obtained from the selective comparison strategy for "
							+ outputHandler + Constants.NewLine);
					JenkinsPlugInLogger.printLogOnConsole(3,
							"Warning : No results obtained from the selective comparison strategy for "
									+ outputHandler);
					continue;
				}
				Method publishMethod = pluginClass.getDeclaredMethod(Constants.outputHandlerExecuteMethod, List.class);
				publishMethod.invoke(outputHandlerObj, selectedStrategyResult);
			} catch (ClassNotFoundException e) {
				JenkinsPlugInLogger.severe("Qualified class " + outputHandlerClass
						+ " not found in the any extensions library. Hence ignoring the execution of this Output Handler"
						+ Constants.NewLine, e);
				JenkinsPlugInLogger.printLogOnConsole(3,
						"Qualified class " + outputHandlerClass + " not found in the any extensions library.");
				builder.append("     ").append(outputHandler + " output handler execution failed")
						.append(Constants.NewLine);
				continue;
			} catch (IllegalAccessException e) {
				JenkinsPlugInLogger.severe(
						"Error in executing Output strategy ->" + outputHandler + " with ->" + e.getMessage(), e);
				builder.append("     ").append(outputHandler + " output handler execution failed")
						.append(Constants.NewLine);
				continue;
			} catch (InvocationTargetException e) {
				JenkinsPlugInLogger.severe("Error in executing Output strategy ->" + outputHandler + " with ->"
						+ e.getMessage() + Constants.NewLine, e);
				builder.append("     ").append(outputHandler + " output handler execution failed")
						.append(Constants.NewLine);
				continue;
			} catch (InstantiationException e) {
				JenkinsPlugInLogger.severe("Error in executing Output strategy ->" + outputHandler + " with ->"
						+ e.getMessage() + Constants.NewLine, e);
				builder.append("     ").append(outputHandler + " output handler execution failed")
						.append(Constants.NewLine);
				continue;
			} catch (NoSuchMethodException e) {
				JenkinsPlugInLogger.severe("Error in executing Output strategy ->" + outputHandler + " with ->"
						+ e.getMessage() + Constants.NewLine, e);
				builder.append("     ").append(outputHandler + " output handler execution failed")
						.append(Constants.NewLine);
				continue;
			} catch (SecurityException e) {
				JenkinsPlugInLogger.severe("Error in executing Output strategy ->" + outputHandler + " with ->"
						+ e.getMessage() + Constants.NewLine, e);
				builder.append("     ").append(outputHandler + " output handler execution failed")
						.append(Constants.NewLine);
				continue;
			} catch (Exception e) {
				if (e instanceof BuildExecutionException) {
					JenkinsPlugInLogger.severe("Error in executing Output strategy ->" + outputHandler + " with ->"
							+ e.getMessage() + Constants.NewLine, e);
					builder.append("     ").append(outputHandler + " output handler execution failed")
							.append(Constants.NewLine);
				}
				continue;
			}
			builder.append("     ").append(outputHandler + " output handler executed successfully")
					.append(Constants.NewLine);
		}
		JenkinsPlugInLogger.printLogOnConsole(3, "Total number of output handlers executed are "
				+ outputStrategies.size() + Constants.NewLine + builder.toString());
		JenkinsPlugInLogger.info("    Total number of output handlers executed are " + outputStrategies.size()
				+ Constants.NewLine + builder.toString());
		builder.setLength(0);
		JenkinsPlugInLogger.printLogOnConsole(2, "Output Handling Phase is completed successfully");
		JenkinsPlugInLogger.info("Output Handler Step completed");
		comparisonMetadata.getIoUtility().closeClassLoader();
	}

	private String getOutputHandlerClass(String outputHandler,
			Map<String, OutputHandlerConfiguration> outputStrategies) {
		String outputHandlerClass = null;
		if (outputHandler.equals(Constants.emailOutputHandlerName)) {
			outputHandlerClass = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler.PlainTextEmailOutputHandler";
		} else if (outputHandler.equals(Constants.jsonFileOutputHandlerName)) {
			outputHandlerClass = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler.JSONFileStoreOutputHandler";
		} else if (outputHandler.equals(Constants.chartOutputHandlerName)) {
			outputHandlerClass = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler.ChartOutputHandler";
		} else if (outputHandler.equals(Constants.histogramOutputHandlerName)) {
			outputHandlerClass = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler.HistogramOutputHandler";
		} else {
			outputHandlerClass = outputStrategies.get(outputHandler).getPropertyValue(outputHandler + ".outputhandler");
		}
		return outputHandlerClass;
	}
}
