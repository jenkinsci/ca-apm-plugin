package com.ca.apm.jenkins.core.executor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.APMConnectionInfo;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.entity.EmailInfo;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.entity.LoadRunnerMetadata;
import com.ca.apm.jenkins.core.entity.OutputHandlerConfiguration;
import com.ca.apm.jenkins.core.entity.StrategiesInfo;
import com.ca.apm.jenkins.core.helper.EmailHelper;
import com.ca.apm.jenkins.core.helper.FileHelper;
import com.ca.apm.jenkins.core.helper.MetricDataHelper;
import com.ca.apm.jenkins.core.helper.VertexAttributesUpdateHelper;
import com.ca.apm.jenkins.core.load.LoadRunnerMetadataRetriever;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import com.ca.apm.jenkins.core.util.IOUtility;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;

/**
 * Loader Class to read all configurations provided by the user. Reads : APM
 * Connection Configuration, LoadRunner Configuration, Strategies Configuration
 * 
 * @author Avinash Chandwani
 *
 */
public class ComparisonMetadataLoader {

	private ComparisonMetadata comparisonMetadata;

	private String performanceComparatorProperties;

	public ComparisonMetadataLoader(JenkinsInfo jenkinsInfo, String performanceComparatorProperties) {
		super();
		comparisonMetadata = new ComparisonMetadata(jenkinsInfo);
		this.performanceComparatorProperties = performanceComparatorProperties;
	}

	/**
	 * The method to start the loading of the input configuration provided
	 * 
	 * @throws BuildComparatorException
	 *             In case of any error occuring during loading due to any
	 *             incorrect property or missing property this exception will be
	 *             thrown
	 */
	public void loadProperties() throws BuildValidationException, BuildExecutionException {
		doRead(performanceComparatorProperties);
	}

	public void validateConfigurations() throws BuildValidationException {
		JenkinsPlugInLogger.info("Configuration validation started");
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration validation started");
		validateAllConfigurations();
		JenkinsPlugInLogger.info("Configuration validation completed");
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration validation completed" + Constants.NewLine);
		prepareOutputProperties();
	}

	private void doRead(String performanceComparatorProperties) throws BuildComparatorException, BuildExecutionException {
		JenkinsPlugInLogger.printLogOnConsole(0, "Comparator Plugin Execution Started" + Constants.NewLine);
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration loading started" + Constants.NewLine);
		JenkinsPlugInLogger.printLogOnConsole(2, "File Names are ");
		PropertiesConfiguration properties = loadConfiguration(performanceComparatorProperties);
		boolean isIoSuccess = readIOUtilityConfiguration(properties);
		boolean isAPMSuccess = readAPMConnectionConfiguration(properties);
		boolean isGenericSuccess = readGenericConfiguration(properties);
		boolean isStrategiesFileSuccess = readStrategiesConfiguration(properties);
		boolean isloadRunnerSuccess = readLoadRunnerConfigurations(properties);
		checkIfFileExists(isAPMSuccess, isloadRunnerSuccess, isStrategiesFileSuccess, isIoSuccess, isGenericSuccess);
		JenkinsPlugInLogger.printLogOnConsole(1, " Configuration loading completed" + Constants.NewLine);
		JenkinsPlugInLogger.info("Loading of Properties file completed");
	}

	private PropertiesConfiguration loadConfiguration(String performanceComparatorProperties) {
		PropertiesConfiguration properties = new PropertiesConfiguration();
		InputStream input;
		try {
			input = new FileInputStream(performanceComparatorProperties);
			properties.load(input);
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				JenkinsPlugInLogger.severe("The configuration file is not found ", e);
			}
		} catch (ConfigurationException e) {
			JenkinsPlugInLogger.severe("The configuration file has encountered some errors ", e);
		}
		return properties;
	}

	private void readStrategiesAdditionalInformation(PropertiesConfiguration properties) {
		String benchMarkBuildNumber = properties.getString(Constants.benchMarkBuildNumber, "-1");
		comparisonMetadata.getStrategiesInfo().addAdditionalProperties(Constants.benchMarkBuildNumber,
				benchMarkBuildNumber);
		if (benchMarkBuildNumber.isEmpty()) {
			benchMarkBuildNumber = "0";
		}
		int benchMarkBuildNo = Integer.parseInt(benchMarkBuildNumber);
		comparisonMetadata.getLoadRunnerMetadataInfo().setBenchMarkBuildNumber(benchMarkBuildNo);
		boolean failTheBuild = properties.getBoolean(Constants.buildPassOrFail);
		comparisonMetadata.setFailTheBuild(failTheBuild);
		if(!properties.containsKey(Constants.isPublishBuildResulttoEM)||properties.getProperty(Constants.isPublishBuildResulttoEM).toString().isEmpty() ){
		    comparisonMetadata.setPublishBuildResulttoEM(false);
		}else {
		    comparisonMetadata.setPublishBuildResulttoEM(properties.getBoolean(Constants.isPublishBuildResulttoEM));
		}
	}

	private void readComparisonStrategiesInformation(PropertiesConfiguration properties) {
		String[] comparisonStrategies = properties.getStringArray(Constants.comparisonStrategiesList);
		StrategyConfiguration strategyConfiguration;
		for (String comparisonStrategy : comparisonStrategies) {
			Iterator<String> strategyKeys = properties.getKeys(comparisonStrategy);
			strategyConfiguration = new StrategyConfiguration();
			strategyConfiguration.addProperty("name", comparisonStrategy);
			while (strategyKeys.hasNext()) {
				String key = strategyKeys.next();
				if (key.endsWith("." + Constants.outputHandlers)) {
					String[] outputHandlers = properties.getStringArray(key);
					for (String outputHandler : outputHandlers) {
						if (outputHandler.isEmpty()) {
							comparisonMetadata.getStrategiesInfo()
									.addToNonMappedComparisonStrategies(comparisonStrategy);
							continue;
						}
						comparisonMetadata.getStrategiesInfo().addToOutputHandlerToComparisonStrategies(outputHandler,
								comparisonStrategy);
					}
				} else if (key.endsWith(Constants.agentSpecifier)) {
					// It is mandatory field
					strategyConfiguration.setAgentSpecifiers(Arrays.asList(properties.getStringArray(key)));
				} else {
					strategyConfiguration.addProperty(key, properties.getString(key));
				}
			}
			comparisonMetadata.getStrategiesInfo().addComparisonStrategyInfo(comparisonStrategy, strategyConfiguration);
		}
	}

	private void readOutputHandlerStrategiesInformation(PropertiesConfiguration properties) {

		String[] outputStrategies = properties.getStringArray(Constants.outputHandlersList);
		OutputHandlerConfiguration outputHandlerInfo;
		if (outputStrategies.length == 1 && outputStrategies[0].length() == 0) {
			JenkinsPlugInLogger.severe("No Output Handler Defined in the configuration");
			return;
		}
		for (String outputHandler : outputStrategies) {
			Iterator<String> strategyKeys = properties.getKeys(outputHandler);
			outputHandlerInfo = new OutputHandlerConfiguration();
			while (strategyKeys.hasNext()) {
				String key = strategyKeys.next();
				outputHandlerInfo.addProperty(key, properties.getString(key));
			}
			outputHandlerInfo.addProperty("name", outputHandler);
			comparisonMetadata.getStrategiesInfo().addOutputHandlersInfo(outputHandler, outputHandlerInfo);
		}
	}

	private void readEmailInformation(PropertiesConfiguration properties) {
		Iterator<String> keys = properties.getKeys("email");
		EmailInfo emailInfo = new EmailInfo();
		while (keys.hasNext()) {
			String key = keys.next();
			String value = properties.getString(key);
			setEmailProperty(emailInfo, key, value);
		}
		EmailHelper.setEmailInfo(emailInfo);
	}

	private boolean readStrategiesConfiguration(PropertiesConfiguration properties) {
		boolean isSuccess = true;
		try {
			readStrategiesAdditionalInformation(properties);
			readComparisonStrategiesInformation(properties);
			readOutputHandlerStrategiesInformation(properties);
			readEmailInformation(properties);
		} catch (NoSuchElementException ex) {
			isSuccess = false;
			JenkinsPlugInLogger.severe("Required property not found ", ex);
			JenkinsPlugInLogger.printLogOnConsole(2, "Missing strategies property, please check logs for more details");
		}
		JenkinsPlugInLogger.printLogOnConsole(2, "Strategies Configuration loading done");
		return isSuccess;
	}

	private void checkIfFileExists(boolean isApm, boolean isLoadRunner, boolean isStrategies, boolean isIoProperties, boolean isGenericSuccess)
			throws BuildComparatorException {
		boolean isErrorFree = true;
		if (!isApm) {
			isErrorFree = false;
			JenkinsPlugInLogger.printLogOnConsole(2, "Error in reading APM Authentication configuration");
		}
		if (!isLoadRunner) {
			isErrorFree = false;
			JenkinsPlugInLogger.printLogOnConsole(2, "Error in reading load runner configuration");
		}
		if (!isStrategies) {
			isErrorFree = false;
			JenkinsPlugInLogger.printLogOnConsole(2, "Error in reading strategies configuration");
		}
		if (!isIoProperties) {
			isErrorFree = false;
			JenkinsPlugInLogger.printLogOnConsole(2, "Error in reading other configuration");
		}
		if (!isErrorFree) {
			comparisonMetadata.setMetadataInCorrect(true);
			throw new BuildComparatorException(
					"Input Properties file(s) defined in parameters does not exist, please check");
		}
		if (!isGenericSuccess) {
			comparisonMetadata.setMetadataInCorrect(true);
			throw new BuildComparatorException(
					"Input Properties file(s) defined in parameters does not exist, please check");
		}
		
	}

	@SuppressWarnings("unused")
	private void checkIfPropertiesFilesExist(String apmProperties, String loadRunnerMetadataProperties,
			String strategiesProperties, String ioProperties) throws BuildComparatorException {
		boolean fileExist = true;
		boolean apmFile = FileHelper.fileExists(apmProperties);
		boolean loadRunnerFile = FileHelper.fileExists(loadRunnerMetadataProperties);
		boolean strategyFile = FileHelper.fileExists(strategiesProperties);
		boolean ioPropertiesFile = FileHelper.fileExists(ioProperties);
		if (!apmFile) {
			fileExist = false;
			JenkinsPlugInLogger.printLogOnConsole(2,
					"Error : APM authentication configuration file does not exist, file path is : " + apmProperties);
		}
		if (!loadRunnerFile) {
			fileExist = false;
			JenkinsPlugInLogger.printLogOnConsole(2,
					"Error : Load Runner Configuration file does not exist, file path is : "
							+ loadRunnerMetadataProperties);
		}
		if (!strategyFile) {
			fileExist = false;
			JenkinsPlugInLogger.printLogOnConsole(2,
					"Error : Strategies Configuration file does not exist, file path is : " + strategiesProperties);
		}
		if (!ioPropertiesFile) {
			fileExist = false;
			JenkinsPlugInLogger.printLogOnConsole(2,
					"Error : System Configuration file does not exist, file path is : " + ioProperties);
		}
		if (!fileExist) {
			comparisonMetadata.setMetadataInCorrect(true);
			throw new BuildComparatorException(
					"Input Properties file(s) defined in parameters does not exist, please check");
		}
	}

	private void validateComparisonStrategies(StringBuilder errorMessages) {
		StrategiesInfo strategiesInfo = comparisonMetadata.getStrategiesInfo();
		Map<String, StrategyConfiguration> strategyConfigurations = strategiesInfo.getComparisonStrategiesInfo();
		StringBuilder warningMessages = new StringBuilder();
		if (strategyConfigurations.isEmpty()) {
			errorMessages.append("No Comparison-Strategy(ies) defined in the configuration, hence exiting");
			return;
		}
		for (String strategyName : strategyConfigurations.keySet()) {
			StrategyConfiguration config = strategyConfigurations.get(strategyName);
			if (config.getPropertyValue(strategyName + "." + Constants.comparatorClasssName) == null
					|| config.getPropertyValue(strategyName + "." + Constants.comparatorClasssName).isEmpty()) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages.append("Error : Comparison Strategy handler for " + strategyName + " is not defined")
						.append(Constants.NewLine);
			}
			if (config.getAgentSpecifiers() == null || config.getAgentSpecifiers().isEmpty()) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages.append("Error : No Agent Specifier(s) defined for " + strategyName)
						.append(Constants.NewLine);
			}
			if (config.getPropertyValue(strategyName + "." + Constants.metricSpecifier) == null
					|| config.getPropertyValue(strategyName + "." + Constants.metricSpecifier).isEmpty()) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages.append("Error : No metric specifier defined for " + strategyName)
						.append(Constants.NewLine);
			}
			if (strategiesInfo.isComparisonStrategyNonMapped(strategyName)) {
				warningMessages.append(Constants.NewLine).append(JenkinsPlugInLogger.getLevelString(3));
				warningMessages.append("Warning : No output handler(s) mapped to " + strategyName);
			}
		}
		if (warningMessages.length() > 0) {
			warningMessages.append(Constants.NewLine);
			JenkinsPlugInLogger.printLogOnConsole(0, warningMessages.toString());
			JenkinsPlugInLogger.warning(warningMessages.toString());
		}
	}

	private void validateOutputHandlers(StringBuilder errorMessages) {
		StrategiesInfo strategiesInfo = comparisonMetadata.getStrategiesInfo();
		Map<String, OutputHandlerConfiguration> outputHandlers = strategiesInfo.getOutputHandlersInfo();
		if (outputHandlers == null || outputHandlers.isEmpty()) {
			errorMessages.append("No output-handler(s) defined in the configuration, hence exiting");
			return;
		}
		for (String outputHandler : outputHandlers.keySet()) {
			OutputHandlerConfiguration outputHandlerConfig = outputHandlers.get(outputHandler);
			String outputHandlerClass = outputHandlerConfig.getPropertyValue(outputHandler + ".outputhandler");
			if (outputHandlerClass == null) {
				errorMessages.append("No handler class provided for output handler " + outputHandler);
			}
		}
	}

	private void validateGenericProp(StringBuilder errorMessages) {
		
		/*if(comparisonMetadata.getCommonPropertyValue(Constants.appMapURL) == null || comparisonMetadata.getCommonPropertyValue(Constants.applicationName) == null
		                                                   || comparisonMetadata.getCommonPropertyValue(Constants.metricClamp) == null){
			errorMessages.append(" One or more of the properties "+Constants.appMapURL+","+Constants.applicationName+","+Constants.metricClamp+" are not found ");
			
		}*/
		
		if (comparisonMetadata.getCommonPropertyValue(Constants.appMapURL) == null
				|| comparisonMetadata.getCommonPropertyValue(Constants.applicationName) == null) {
			errorMessages.append(" One or more of the properties " + Constants.appMapURL + ","
					+ Constants.applicationName + " are not found ");

		}
	}
	
	private void printErrorMessages(StringBuilder errorMessages) throws BuildValidationException {
		if (errorMessages.length() > 0) {
			JenkinsPlugInLogger.severe(Constants.NewLine + errorMessages.toString());
			JenkinsPlugInLogger.printLogOnConsole(0, errorMessages.toString());
			throw new BuildValidationException(errorMessages.toString());
		}
	}

	private void validateLoadRunnerTimes(StringBuilder errorMessages) throws BuildComparatorException {
		LoadRunnerMetadata loadRunnerMetadata = comparisonMetadata.getLoadRunnerMetadataInfo();
		long currentEndTime = loadRunnerMetadata.getCurrentBuildInfo().getEndTime();
		long currentStartTime = loadRunnerMetadata.getCurrentBuildInfo().getStartTime();
		long benchMarkEndTime = loadRunnerMetadata.getBenchMarkBuildInfo().getEndTime();
		long benchMarkStartTime = loadRunnerMetadata.getBenchMarkBuildInfo().getStartTime();
		if (currentEndTime < currentStartTime) {
			errorMessages.append("Error : Current Build's load runner end time is less than start time")
					.append(Constants.NewLine);
		}
		if (benchMarkEndTime < benchMarkStartTime) {
			errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
			errorMessages.append("Error : Benchmark Build's load runner end time is less than start time")
					.append(Constants.NewLine);
		}
		if (benchMarkStartTime > currentStartTime) {
			errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
			errorMessages
					.append("Error : Current Build's load runner start time is less than benchmark build's start time")
					.append(Constants.NewLine);
		}
		if (benchMarkEndTime > currentEndTime) {
			errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
			errorMessages.append("Error : Current Build's load runner end time is less than benchmark build's end time")
					.append(Constants.NewLine);
		}

	}

	private void validateBenchMarkBuildNumber(StringBuilder errorMessages) {
		int currentBuildNumber = comparisonMetadata.getJenkinsInfo().getCurrentBuildNumber();
		int jenkinsLastSuccessfulBuildNumber = comparisonMetadata.getJenkinsInfo().getLastSuccessfulBuildNumber();
		String propertiesBenchMarkBuildValue = comparisonMetadata.getStrategiesInfo()
				.getPropertyValue(Constants.benchMarkBuildNumber);
		int propertiesBenchMarkBuildNumber = 0;
		int benchMarkBuildNumber = 0;
		if (!propertiesBenchMarkBuildValue.isEmpty()) {
			propertiesBenchMarkBuildNumber = Integer.parseInt(propertiesBenchMarkBuildValue);
		}

		if (propertiesBenchMarkBuildNumber == 0) {
			benchMarkBuildNumber = jenkinsLastSuccessfulBuildNumber;
			comparisonMetadata.getLoadRunnerMetadataInfo().setBenchMarkBuildNumber(benchMarkBuildNumber);
		} else {
			benchMarkBuildNumber = propertiesBenchMarkBuildNumber;
			comparisonMetadata.getLoadRunnerMetadataInfo().setBenchMarkBuildNumber(propertiesBenchMarkBuildNumber);
		}

		JenkinsPlugInLogger.printLogOnConsole(3, "BenchMark Build Number selected is "
				+ comparisonMetadata.getLoadRunnerMetadataInfo().getBenchMarkBuildNumber());
		if (currentBuildNumber < benchMarkBuildNumber) {
			comparisonMetadata.setMetadataInCorrect(true);
			errorMessages.append("Benchmark build number is greater than the current build number")
					.append(Constants.NewLine);
		}
		if (benchMarkBuildNumber <= 0) {
			comparisonMetadata.setMetadataInCorrect(true);
			errorMessages.append("Invalid Benchmark build number").append(Constants.NewLine);
		}
	}

	private void assignBenchMarkBuildNumber() {
		int currentBuildNumber = comparisonMetadata.getJenkinsInfo().getCurrentBuildNumber();
		int jenkinsLastSuccessfulBuildNumber = comparisonMetadata.getJenkinsInfo().getLastSuccessfulBuildNumber();
		String propertiesBenchMarkBuildValue = comparisonMetadata.getStrategiesInfo()
				.getPropertyValue(Constants.benchMarkBuildNumber);
		int propertiesBenchMarkBuildNumber = 0;
		int benchMarkBuildNumber = 0;
		if (!propertiesBenchMarkBuildValue.isEmpty()) {
			propertiesBenchMarkBuildNumber = Integer.parseInt(propertiesBenchMarkBuildValue);
		}

		if (propertiesBenchMarkBuildNumber == 0) {
			benchMarkBuildNumber = jenkinsLastSuccessfulBuildNumber;
			comparisonMetadata.getLoadRunnerMetadataInfo().setBenchMarkBuildNumber(benchMarkBuildNumber);
		} else {
			benchMarkBuildNumber = propertiesBenchMarkBuildNumber;
			comparisonMetadata.getLoadRunnerMetadataInfo().setBenchMarkBuildNumber(propertiesBenchMarkBuildNumber);
		}

		JenkinsPlugInLogger.printLogOnConsole(3, "BenchMark Build Number selected is "
				+ comparisonMetadata.getLoadRunnerMetadataInfo().getBenchMarkBuildNumber());
		if (currentBuildNumber < benchMarkBuildNumber) {
			comparisonMetadata.setMetadataInCorrect(true);
		}
		if (benchMarkBuildNumber <= 0) {
			comparisonMetadata.setMetadataInCorrect(true);
			
		}
	}
	
	private boolean readAPMConnectionConfiguration(PropertiesConfiguration properties) {
		boolean isSuccess = true;
		try {
			APMConnectionInfo apmConnectionInfo = comparisonMetadata.getApmConnectionInfo();
			apmConnectionInfo.setEmURL(properties.getString(Constants.emURL));
			apmConnectionInfo.setEmUserName(properties.getString(Constants.emUserName));
			apmConnectionInfo.setEmPassword(properties.getString(Constants.emPassword));
			if (apmConnectionInfo.getEmPassword().isEmpty())
				apmConnectionInfo.setAuthToken(apmConnectionInfo.getEmUserName());
			if(properties.getString(Constants.emTimeZone).isEmpty() || properties.getString(Constants.emTimeZone) == null){
				
				JenkinsPlugInLogger.severe(" em.timezone property value is not found");
				JenkinsPlugInLogger.printLogOnConsole(2," em.timezone property value is not found");
				isSuccess = false;
			}else{
				apmConnectionInfo.setEmTimeZone(properties.getString(Constants.emTimeZone));
			   comparisonMetadata.addToCommonProperties(Constants.emTimeZone, properties.getString(Constants.emTimeZone));
			}
			JenkinsPlugInLogger.printLogOnConsole(2, "APM Properties file loading done");
			MetricDataHelper.setAPMConnectionInfo(apmConnectionInfo);
			VertexAttributesUpdateHelper.setAPMConnectionInfo(apmConnectionInfo);
			} catch (NoSuchElementException ex) {
			JenkinsPlugInLogger.severe("A required property not found ", ex);
			isSuccess = false;
		}
		return isSuccess;
	}
	
	
	
	private boolean readGenericConfiguration(PropertiesConfiguration properties) {
		boolean isSuccess = true;
		try {
			comparisonMetadata.addToCommonProperties(Constants.appMapURL, properties.getString(Constants.appMapURL));
			comparisonMetadata.addToCommonProperties(Constants.applicationName,properties.getString(Constants.applicationName));
			/*comparisonMetadata.addToCommonProperties(Constants.metricClamp, properties.getString(Constants.metricClamp));
			MetricDataHelper.setMetricClamp(properties.getString(Constants.metricClamp));*/
			MetricDataHelper.setApplicationName(properties.getString(Constants.applicationName));
		} catch (NoSuchElementException ex) {
			JenkinsPlugInLogger.severe("A required property not found ", ex);
			isSuccess = false;
		}
		return isSuccess;
	}

	public LoadRunnerMetadataRetriever loadMetaDataRetriever(LoadRunnerMetadata loadRunnerMetadata,
			String loadRunnerClassName) throws BuildExecutionException{
		Class<?> pluginClass = null;
		LoadRunnerMetadataRetriever loadRunnerMetadataRetreiver = null;
		try {
			pluginClass = Class.forName(loadRunnerClassName);
			Constructor<?> c = pluginClass.getConstructor(LoadRunnerMetadata.class);
			Object loadRunnerMetadataRetriverObj = c.newInstance(loadRunnerMetadata);
			((LoadRunnerMetadataRetriever) loadRunnerMetadataRetriverObj).setLoadRunnerMetadata(loadRunnerMetadata);
			Method readExtraMetadataMethod = pluginClass.getDeclaredMethod(Constants.loadRunnerMetadataExtractMethod);
			loadRunnerMetadataRetreiver = (LoadRunnerMetadataRetriever) readExtraMetadataMethod
					.invoke(loadRunnerMetadataRetriverObj);
		} catch (ClassNotFoundException e) {
			JenkinsPlugInLogger.severe(loadRunnerMetadata + " class is not present in the classpath ", e);
		} catch (InstantiationException e) {
			JenkinsPlugInLogger.severe("Cannot instantiate " + loadRunnerClassName, e);
		} catch (IllegalAccessException e) {
			JenkinsPlugInLogger.severe("Error in reading load-runner metadata", e);
		} catch (NoSuchMethodException e) {
			JenkinsPlugInLogger.severe("The method fetchExtraMetadata doesnt exist in " + loadRunnerClassName, e);
		} catch (SecurityException e) {
			JenkinsPlugInLogger.severe("A security error occured while fetching load runner metadata", e);
		} catch (IllegalArgumentException e) {
			JenkinsPlugInLogger.severe("Illegal arguments to the load-runner retriever class ", e);
		} catch (InvocationTargetException e) {
			JenkinsPlugInLogger.severe("Invocation error occured while loading load-runner metadata", e);
			throw new BuildExecutionException(e.getTargetException().getMessage());
		}
		return loadRunnerMetadataRetreiver;
	}

	private boolean readLoadRunnerConfigurations(PropertiesConfiguration properties) throws BuildExecutionException{
		boolean isSuccess = true;
		try {
			assignBenchMarkBuildNumber();
			LoadRunnerMetadata loadRunnerMetadataInfo = comparisonMetadata.getLoadRunnerMetadataInfo();
			loadRunnerMetadataInfo.addToLoadRunnerProperties("currentBuildNumber",
					"" + comparisonMetadata.getJenkinsInfo().getCurrentBuildNumber());
			loadRunnerMetadataInfo.setCurrentBuildNumber(comparisonMetadata.getJenkinsInfo().getCurrentBuildNumber());
			Iterator<String> propIter = properties.getKeys();
			while (propIter.hasNext()) {
				String key = propIter.next();
				loadRunnerMetadataInfo.addToLoadRunnerProperties(key, properties.getString(key));
			}
			String loadRunnerName = properties.getString(Constants.loadGeneratorName);
			loadRunnerMetadataInfo.addToLoadRunnerProperties(Constants.loadGeneratorName, loadRunnerName);
			String metadataReaderClassName = properties
					.getString(loadRunnerName + "." + Constants.metadataReaderClassName);
			loadMetaDataRetriever(loadRunnerMetadataInfo, metadataReaderClassName);
			JenkinsPlugInLogger.printLogOnConsole(2, "Loadrunner-metadata file loading done");
		} catch (NoSuchElementException ex) {
			isSuccess = false;
			JenkinsPlugInLogger.severe("Required property not found", ex);
		}

		return isSuccess;
	}

	private void setEmailProperty(EmailInfo emailInfo, String key, String value) {

		if (key.equals(Constants.emailSMTPHost)) {
			emailInfo.setSmtpHost(value);
		} else if (key.equals(Constants.emailSMTPAuth)) {
			emailInfo.setMailSmtpAuth(Boolean.parseBoolean(value));
		} else if (key.equals(Constants.emailSenderId)) {
			emailInfo.setSenderEmailId(value);
		} else if (key.equals(Constants.emailPassword)) {
			emailInfo.setPassword(value);
		} else if (key.equals(Constants.emailToRecipients)) {
			String[] recipients = value.split(",");
			if (recipients.length == 0 || recipients[0].isEmpty()) {
				JenkinsPlugInLogger.warning("No recepient(s) email provided in the configuration");
			} else {
				emailInfo.setToRecipients(Arrays.asList(recipients));
			}
		} else if (key.equals(Constants.emailCCRecipients)) {
			String[] recipients = value.split(",");
			if (recipients.length > 0 && !recipients[0].isEmpty()) {
				emailInfo.setCcRecipients(Arrays.asList(recipients));
			}
		} else if (key.equals(Constants.emailBccRecipients)) {
			String[] recipients = value.split(",");
			if (recipients.length > 0 && !recipients[0].isEmpty()) {
				emailInfo.setBccRecipients(Arrays.asList(recipients));
			}
		}
	}

	private boolean readIOUtilityConfiguration(PropertiesConfiguration properties) {
		boolean isSuccess = true;
		try {
			JenkinsInfo jInfo = comparisonMetadata.getJenkinsInfo();
			String loggingFolder = jInfo.getBuildWorkSpaceFolder() + File.separator + jInfo.getJobName();
			int currentBuildNumber = jInfo.getCurrentBuildNumber();
			JenkinsPlugInLogger.printLogOnConsole(2, "Logging folder location is " + loggingFolder);
			String loggingLevel = properties.getString(Constants.loggingLevel);
			FileHelper.initializeLog(loggingLevel, loggingFolder, currentBuildNumber);
			String extensionsDirectory = properties.getString(Constants.extensionsDirectory);
			String outputDirectory = null;
			outputDirectory = jInfo.getBuildWorkSpaceFolder() + File.separator + jInfo.getJobName() + File.separator
					+ jInfo.getCurrentBuildNumber();
			comparisonMetadata.getLoadRunnerMetadataInfo().setJenkinsInfo(jInfo);
			IOUtility ioUtility = comparisonMetadata.getIoUtility();
			ioUtility.addToIOProperties(Constants.extensionsDirectory, extensionsDirectory);
			ioUtility.addToIOProperties(Constants.outputDirectory, outputDirectory);
			ioUtility.loadExtensionsLibraries();
			JenkinsPlugInLogger.printLogOnConsole(2,
					"Reading configuration properties done" + Constants.NewLine + Constants.NewLine);
			JenkinsPlugInLogger.printLogOnConsole(2, "Jenkins Information is ");
			JenkinsPlugInLogger.printLogOnConsole(3, "WorkSpace Folder :" + jInfo.getBuildWorkSpaceFolder());
			JenkinsPlugInLogger.printLogOnConsole(3, "Job Name :" + jInfo.getJobName());
			JenkinsPlugInLogger.printLogOnConsole(3,
					"Build Number :" + jInfo.getCurrentBuildNumber() + Constants.NewLine);
			JenkinsPlugInLogger.printLogOnConsole(3,
					"Logging folder location is " + loggingFolder + File.separator + jInfo.getCurrentBuildNumber());
		} catch (NoSuchElementException ex) {
			isSuccess = false;
		}
		return isSuccess;
	}

	private void validateAllConfigurations() throws BuildValidationException {
		StringBuilder errorMessages = new StringBuilder();
		validateLoadRunnerTimes(errorMessages);
		validateBenchMarkBuildNumber(errorMessages);
		validateComparisonStrategies(errorMessages);
		validateOutputHandlers(errorMessages);
		validateGenericProp(errorMessages);
		printErrorMessages(errorMessages);
		errorMessages.setLength(0);
	}

	private void prepareOutputProperties() {
		IOUtility ioUtility = comparisonMetadata.getIoUtility();
		OutputConfiguration outputConfiguration = comparisonMetadata.getOutputConfiguration();
		outputConfiguration.addToCommonProperties(Constants.outputDirectory,
				ioUtility.getIOPropertyValue(Constants.outputDirectory));
		outputConfiguration.addToCommonProperties(Constants.workSpaceDirectory,
				comparisonMetadata.getJenkinsInfo().getBuildWorkSpaceFolder());
		outputConfiguration.addToCommonProperties(Constants.jenkinsJobName,
				comparisonMetadata.getJenkinsInfo().getJobName());
		outputConfiguration.addToCommonProperties(Constants.extensionsDirectory,
				ioUtility.getIOPropertyValue(Constants.extensionsDirectory));
		long endTime = comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getEndTime();
		long startTime = comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getStartTime();
		String durationInString = JenkinsPluginUtility.getDurationInString(startTime, endTime);
		outputConfiguration.addToCommonProperties("runner.start", "" + startTime);
		outputConfiguration.addToCommonProperties("runner.end", "" + endTime);
		outputConfiguration.addToCommonProperties("runner.duration", durationInString);
		outputConfiguration.addToCommonProperties(Constants.jenkinsJobName,
				"" + comparisonMetadata.getJenkinsInfo().getJobName());
		outputConfiguration.addToCommonProperties(Constants.jenkinsCurrentBuild,
				"" + comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getNumber());
		outputConfiguration.addToCommonProperties(Constants.jenkinsBenchMarkBuild,
				"" + comparisonMetadata.getLoadRunnerMetadataInfo().getBenchMarkBuildInfo().getNumber());
		outputConfiguration.addToCommonProperties(Constants.loadGeneratorName,
				comparisonMetadata.getLoadRunnerMetadataInfo().getLoadRunnerPropertyValue(Constants.loadGeneratorName));
		outputConfiguration.addToCommonProperties(Constants.emURL,""+comparisonMetadata.getApmConnectionInfo().getEmURL());
		outputConfiguration.addToCommonProperties(Constants.emUserName,""+comparisonMetadata.getApmConnectionInfo().getEmUserName());
		outputConfiguration.addToCommonProperties(Constants.emPassword,""+comparisonMetadata.getApmConnectionInfo().getEmPassword());
		outputConfiguration.addToCommonProperties(Constants.applicationName,""+comparisonMetadata.getCommonPropertyValue(Constants.applicationName));
		outputConfiguration.addToCommonProperties(Constants.appMapURL,""+comparisonMetadata.getCommonPropertyValue(Constants.appMapURL));
		outputConfiguration.setHistogramBuildInfoList(comparisonMetadata.getLoadRunnerMetadataInfo().getHistogramBuildInfo());
				
	}

	public ComparisonMetadata getComparisonMetadata() {
		return comparisonMetadata;
	}

	public void setComparisonMetadata(ComparisonMetadata comparisonMetadata) {
		this.comparisonMetadata = comparisonMetadata;
	}
}