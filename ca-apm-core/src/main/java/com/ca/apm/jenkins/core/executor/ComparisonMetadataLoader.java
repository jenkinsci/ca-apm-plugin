package com.ca.apm.jenkins.core.executor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
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
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import com.ca.apm.jenkins.core.util.IOUtility;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;

/**
 * Loader Class to read all configurations provided by the user. Reads : APM
 * Connection Configuration, LoadRunner Configuration, Strategies Configuration
 *
 * @author Avinash Chandwani
 */
public class ComparisonMetadataLoader {

	private ComparisonMetadata comparisonMetadata;

	private String performanceComparatorProperties;

	private static String defaultMetricClamp = "10";

	private static final String INPUTPROPERTIESFILEDOESNOTEXIST = "Input Properties file(s) defined in parameters does not exist, please check";

	public ComparisonMetadataLoader(BuildInfo currentBuildInfo, BuildInfo benchmarkBuildInfo, JenkinsInfo jenkinsInfo,
			String performanceComparatorProperties) {
		super();
		comparisonMetadata = new ComparisonMetadata(jenkinsInfo);
		LoadRunnerMetadata loadRunnerMetadata = comparisonMetadata.getLoadRunnerMetadataInfo();
		loadRunnerMetadata.setCurrentBuildInfo(currentBuildInfo);
		loadRunnerMetadata.setBenchMarkBuildInfo(benchmarkBuildInfo);
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
	public void loadProperties() {
		doRead(performanceComparatorProperties);
	}

	public void validateConfigurations() throws BuildValidationException {
		JenkinsPlugInLogger.info("Configuration validation started");
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration validation started");
		validateAllConfigurations();
		JenkinsPlugInLogger.info("Configuration validation completed");
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration validation completed" + Constants.NEWLINE);
		prepareOutputProperties();
	}

	private void doRead(String performanceComparatorProperties) {
		JenkinsPlugInLogger.printLogOnConsole(0, "Comparator Plugin Execution Started" + Constants.NEWLINE);
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration loading started" + Constants.NEWLINE);
		JenkinsPlugInLogger.printLogOnConsole(2, "File Names are ");
		PropertiesConfiguration properties = loadConfiguration(performanceComparatorProperties);
		boolean isIoSuccess = readIOUtilityConfiguration(properties);
		boolean isAPMSuccess = readAPMConnectionConfiguration(properties);
		boolean isGenericSuccess = readGenericConfiguration(properties);
		boolean isStrategiesFileSuccess = readStrategiesConfiguration(properties);
		boolean isloadRunnerSuccess = true;
		comparisonMetadata.getLoadRunnerMetadataInfo()
				.setHistogramBuildInfoList(comparisonMetadata.getJenkinsInfo().getHistogramBuildInfoList());
		
		checkIfFileExists(isAPMSuccess, isloadRunnerSuccess, isStrategiesFileSuccess, isIoSuccess, isGenericSuccess);
		JenkinsPlugInLogger.printLogOnConsole(1, " Configuration loading completed" + Constants.NEWLINE);
		JenkinsPlugInLogger.info("Loading of Properties file completed");
	}

	private PropertiesConfiguration loadConfiguration(String performanceComparatorProperties) {
		PropertiesConfiguration properties = new PropertiesConfiguration();
		InputStream input;
		try {
			input = new FileInputStream(performanceComparatorProperties);
			properties.load(input);
		} catch (FileNotFoundException e) {
			JenkinsPlugInLogger.severe("The configuration file is not found ", e);
		} catch (ConfigurationException e) {
			JenkinsPlugInLogger.severe("The configuration file has encountered some errors ", e);
		}
		return properties;
	}

	private void readStrategiesAdditionalInformation(PropertiesConfiguration properties) {
		String benchMarkBuildNumber = properties.getString(Constants.BENCHMARKBUILDNUMBER, "-1");
		comparisonMetadata.getStrategiesInfo().addAdditionalProperties(Constants.BENCHMARKBUILDNUMBER,
				benchMarkBuildNumber);
		if (benchMarkBuildNumber.isEmpty()) {
			benchMarkBuildNumber = "0";
		}
		int benchMarkBuildNo = Integer.parseInt(benchMarkBuildNumber);
		comparisonMetadata.getLoadRunnerMetadataInfo().setBenchMarkBuildNumber(benchMarkBuildNo);
		if (!properties.containsKey(Constants.BUILDPASSORFAIL)
				|| properties.getProperty(Constants.BUILDPASSORFAIL).toString().isEmpty()
				|| properties.getProperty(Constants.BUILDPASSORFAIL).toString() == null) {
			comparisonMetadata.setFailTheBuild(true);
		} else {
			comparisonMetadata.setFailTheBuild(properties.getBoolean(Constants.BUILDPASSORFAIL));
		}
		if (!properties.containsKey(Constants.ISPUBLISHBUILDRESULTTOEM)
				|| properties.getProperty(Constants.ISPUBLISHBUILDRESULTTOEM).toString().isEmpty()
				|| properties.getProperty(Constants.ISPUBLISHBUILDRESULTTOEM).toString() == null) {
			comparisonMetadata.setPublishBuildResulttoEM(false);
		} else {
			comparisonMetadata.setPublishBuildResulttoEM(properties.getBoolean(Constants.ISPUBLISHBUILDRESULTTOEM));
		}
	}

	private void readComparisonStrategiesInformation(PropertiesConfiguration properties) {
		String[] comparisonStrategies = properties.getStringArray(Constants.COMPARISONSTRATEGIESLIST);
		StrategyConfiguration strategyConfiguration;
		for (String comparisonStrategy : comparisonStrategies) {
			Iterator<String> strategyKeys = properties.getKeys(comparisonStrategy);
			strategyConfiguration = new StrategyConfiguration();
			strategyConfiguration.addProperty("name", comparisonStrategy);
			while (strategyKeys.hasNext()) {
				String key = strategyKeys.next();
				if (key.endsWith("." + Constants.OUTPUTHANDLERS)) {
					String[] outputHandlers = properties.getStringArray(key);
					addToOutputHandlerToComparisonStrategies(outputHandlers, comparisonStrategy);

				} else if (key.endsWith(Constants.AGENTSPECIFIER)) {
					// It is mandatory field
					strategyConfiguration.setAgentSpecifiers(Arrays.asList(properties.getStringArray(key)));
				} else {
					strategyConfiguration.addProperty(key, properties.getString(key));
				}
			}
			comparisonMetadata.getStrategiesInfo().addComparisonStrategyInfo(comparisonStrategy, strategyConfiguration);
		}
	}

	private void addToOutputHandlerToComparisonStrategies(String[] outputHandlers, String comparisonStrategy) {
		for (String outputHandler : outputHandlers) {
			if (outputHandler.isEmpty()) {
				comparisonMetadata.getStrategiesInfo().addToNonMappedComparisonStrategies(comparisonStrategy);
				continue;
			}
			comparisonMetadata.getStrategiesInfo().addToOutputHandlerToComparisonStrategies(outputHandler,
					comparisonStrategy);
		}
	}

	private void readOutputHandlerStrategiesInformation(PropertiesConfiguration properties) {

		String[] outputStrategies = properties.getStringArray(Constants.OUTPUTHANDLERSLIST);
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
			if (key.equals("email.password")) {
				value = EmailHelper.passwordEncrytion(properties, key, value, performanceComparatorProperties);
			}
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

	private void checkIfFileExists(boolean isApm, boolean isLoadRunner, boolean isStrategies, boolean isIoProperties,
			boolean isGenericSuccess) {
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
			throw new BuildComparatorException(INPUTPROPERTIESFILEDOESNOTEXIST);
		}
		if (!isGenericSuccess) {
			comparisonMetadata.setMetadataInCorrect(true);
			throw new BuildComparatorException(INPUTPROPERTIESFILEDOESNOTEXIST);
		}
	}

	@SuppressWarnings("unused")
	private void checkIfPropertiesFilesExist(String apmProperties, String loadRunnerMetadataProperties,
			String strategiesProperties, String ioProperties) {
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
			throw new BuildComparatorException(INPUTPROPERTIESFILEDOESNOTEXIST);
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

		for (Map.Entry<String, StrategyConfiguration> strategyNameEntry : strategyConfigurations.entrySet()) {
			StrategyConfiguration config = strategyNameEntry.getValue();

			if (config.getPropertyValue(strategyNameEntry.getKey() + "." + Constants.COMPARATORCLASSSNAME) == null
					|| config.getPropertyValue(strategyNameEntry.getKey() + "." + Constants.COMPARATORCLASSSNAME)
							.isEmpty()) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages.append(
						"Error : Comparison Strategy handler for " + strategyNameEntry.getKey() + " is not defined")
						.append(Constants.NEWLINE);
			}
			if (config.getAgentSpecifiers() == null || config.getAgentSpecifiers().isEmpty()) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages.append("Error : No Agent Specifier(s) defined for " + strategyNameEntry.getKey())
						.append(Constants.NEWLINE);
			}
			if (config.getPropertyValue(strategyNameEntry.getKey() + "." + Constants.METRICSPECIFIER) == null || config
					.getPropertyValue(strategyNameEntry.getKey() + "." + Constants.METRICSPECIFIER).isEmpty()) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages.append("Error : No metric specifier defined for " + strategyNameEntry.getKey())
						.append(Constants.NEWLINE);
			}

			if (strategiesInfo.isComparisonStrategyNonMapped(strategyNameEntry.getKey())) {
				warningMessages.append(Constants.NEWLINE).append(JenkinsPlugInLogger.getLevelString(3));
				warningMessages.append("Warning : No output handler(s) mapped to " + strategyNameEntry.getKey());
			}
		}
		if (warningMessages.length() > 0) {
			warningMessages.append(Constants.NEWLINE);
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
		for (Map.Entry<String, OutputHandlerConfiguration> outputHandlerEntry : outputHandlers.entrySet()) {
			String outputHandlerClass = null;
			if (outputHandlerEntry.getKey().equals(Constants.EMAILOUTPUTHANDLERNAME)) {
				outputHandlerClass = Constants.OUTPUTHANDLERCLASSPATH + "." + Constants.EMAILOUTPUTHANDLERCLASSSNAME
						+ "." + Constants.OUTPUTHANDLERSUFFIX;
			} else if (outputHandlerEntry.getKey().equals(Constants.JSONFILEOUTPUTHANDLERNAME)) {
				outputHandlerClass = Constants.OUTPUTHANDLERCLASSPATH + "." + Constants.JSONFILEOUTPUTHANDLERCLASSSNAME
						+ "." + Constants.OUTPUTHANDLERSUFFIX;
			} else if (outputHandlerEntry.getKey().equals(Constants.CHARTOUTPUTHANDLERNAME)) {
				outputHandlerClass = Constants.OUTPUTHANDLERCLASSPATH + "." + Constants.CHARTOUTPUTHANDLERCLASSSNAME
						+ "." + Constants.OUTPUTHANDLERSUFFIX;
			} else if (outputHandlerEntry.getKey().equals(Constants.HISTOGRAMOUTPUTHANDLERNAME)) {
				outputHandlerClass = Constants.OUTPUTHANDLERCLASSPATH + "." + Constants.HISTOGRAMOUTPUTHANDLERNAME + "."
						+ Constants.OUTPUTHANDLERSUFFIX;
			} else {
				OutputHandlerConfiguration outputHandlerConfig = outputHandlers.get(outputHandlerEntry.getKey());
				if (outputHandlerConfig.getPropertyValue(outputHandlerEntry.getKey() + ".outputhandler") != null)
					outputHandlerClass = Constants.OUTPUTHANDLERCLASSPATH + "."
							+ outputHandlerConfig.getPropertyValue(outputHandlerEntry.getKey() + ".outputhandler") + "."
							+ Constants.OUTPUTHANDLERSUFFIX;
			}

			if (outputHandlerClass == null) {
				errorMessages.append(
						"No handler class is provided for customized output handler " + outputHandlerEntry.getKey());
			}
		}
	}

	private void validateGenericProp(StringBuilder errorMessages) {

		if (comparisonMetadata.getCommonPropertyValue(Constants.EMWEBVIEWPORT) == null
				|| comparisonMetadata.getCommonPropertyValue(Constants.EMWEBVIEWPORT).isEmpty())
			errorMessages.append(Constants.EMWEBVIEWPORT + " property value is not found");
		if (comparisonMetadata.getCommonPropertyValue(Constants.APPLICATIONNAME) == null
				|| comparisonMetadata.getCommonPropertyValue(Constants.APPLICATIONNAME).isEmpty())
			errorMessages.append("application.name property value is not found");
		if (comparisonMetadata.getCommonPropertyValue(Constants.EMAUTHTOKEN) == null
				|| comparisonMetadata.getCommonPropertyValue(Constants.EMAUTHTOKEN).isEmpty())
			errorMessages.append("em.EMAUTHTOKEN property value is not found");
		if (comparisonMetadata.getCommonPropertyValue(Constants.EMURL) == null
				|| comparisonMetadata.getCommonPropertyValue(Constants.EMURL).isEmpty())
			errorMessages.append("em.url property value is not found");
	}

	private void printErrorMessages(StringBuilder errorMessages) throws BuildValidationException {
		if (errorMessages.length() > 0) {
			JenkinsPlugInLogger.severe(Constants.NEWLINE + errorMessages.toString());
			JenkinsPlugInLogger.printLogOnConsole(0, errorMessages.toString());
			throw new BuildValidationException(errorMessages.toString());
		}
	}

	private void validateLoadRunnerTimes(StringBuilder errorMessages) {
		LoadRunnerMetadata loadRunnerMetadata = comparisonMetadata.getLoadRunnerMetadataInfo();
		long currentEndTime = loadRunnerMetadata.getCurrentBuildInfo().getEndTime();
		long currentStartTime = loadRunnerMetadata.getCurrentBuildInfo().getStartTime();
		long benchMarkEndTime = loadRunnerMetadata.getBenchMarkBuildInfo().getEndTime();
		long benchMarkStartTime = loadRunnerMetadata.getBenchMarkBuildInfo().getStartTime();
		if (currentEndTime < currentStartTime) {
			errorMessages.append("Error : Current Build's load runner end time is less than start time")
					.append(Constants.NEWLINE);
		}
		if (benchMarkEndTime < benchMarkStartTime) {
			errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
			errorMessages.append("Error : Benchmark Build's load runner end time is less than start time")
					.append(Constants.NEWLINE);
		}
		if (benchMarkStartTime > currentStartTime) {
			errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
			errorMessages
					.append("Error : Current Build's load runner start time is less than benchmark build's start time")
					.append(Constants.NEWLINE);
		}
		if (benchMarkEndTime > currentEndTime) {
			errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
			errorMessages.append("Error : Current Build's load runner end time is less than benchmark build's end time")
					.append(Constants.NEWLINE);
		}
	}

	private void validateBenchMarkBuildNumber(StringBuilder errorMessages) {
		int currentBuildNumber = comparisonMetadata.getJenkinsInfo().getCurrentBuildNumber();
		int jenkinsLastSuccessfulBuildNumber = comparisonMetadata.getJenkinsInfo().getLastSuccessfulBuildNumber();
		String propertiesBenchMarkBuildValue = comparisonMetadata.getStrategiesInfo()
				.getPropertyValue(Constants.BENCHMARKBUILDNUMBER);
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
					.append(Constants.NEWLINE);
		}
		if (benchMarkBuildNumber <= 0) {
			comparisonMetadata.setMetadataInCorrect(true);
			errorMessages
					.append("There is no previous successful build or invalid benchmark build number, please enter valid Benchmark build number")
					.append(Constants.NEWLINE);
		}
	}

	private boolean readAPMConnectionConfiguration(PropertiesConfiguration properties) {
		boolean isSuccess = true;
		try {
			APMConnectionInfo apmConnectionInfo = comparisonMetadata.getApmConnectionInfo();
			apmConnectionInfo.setEmURL(properties.getString(Constants.EMURL));

			if (properties.getString(Constants.EMURL).isEmpty() || properties.getString(Constants.EMURL) == null) {
				comparisonMetadata.setMetadataInCorrect(true);
				JenkinsPlugInLogger.severe(" em.url property value is not found");
				JenkinsPlugInLogger.printLogOnConsole(2, " em.url property value is not found");
				isSuccess = false;
			} else {
				apmConnectionInfo.setEmURL(properties.getString(Constants.EMURL));
				comparisonMetadata.addToCommonProperties(Constants.EMURL, properties.getString(Constants.EMURL));
			}

			if (properties.getString(Constants.EMAUTHTOKEN).isEmpty()
					|| properties.getString(Constants.EMAUTHTOKEN) == null) {
				comparisonMetadata.setMetadataInCorrect(true);
				JenkinsPlugInLogger.severe(" em.authtoken property value is not found");
				JenkinsPlugInLogger.printLogOnConsole(2, " em.authtoken property value is not found");
				isSuccess = false;
			} else {
				apmConnectionInfo.setEmAuthToken(properties.getString(Constants.EMAUTHTOKEN));
				comparisonMetadata.addToCommonProperties(Constants.EMAUTHTOKEN,
						properties.getString(Constants.EMAUTHTOKEN));
			}
			if (properties.getString(Constants.EMTIMEZONE).isEmpty()
					|| properties.getString(Constants.EMTIMEZONE) == null) {

				JenkinsPlugInLogger.severe(" em.timezone property value is not found");
				JenkinsPlugInLogger.printLogOnConsole(2, " em.timezone property value is not found");
				isSuccess = false;
			} else {
				apmConnectionInfo.setEmTimeZone(properties.getString(Constants.EMTIMEZONE));
				comparisonMetadata.addToCommonProperties(Constants.EMTIMEZONE,
						properties.getString(Constants.EMTIMEZONE));
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

	private void readDOIProperties(PropertiesConfiguration properties) {
		if (!properties.containsKey(Constants.APPLICATIONHOST)
				|| properties.getProperty(Constants.APPLICATIONHOST).toString().isEmpty()
				|| properties.getProperty(Constants.APPLICATIONHOST).toString() == null) {
			comparisonMetadata.addToCommonProperties(Constants.APPLICATIONHOST, "");
		} else {
			comparisonMetadata.addToCommonProperties(Constants.APPLICATIONHOST,
					properties.getProperty(Constants.APPLICATIONHOST).toString());
		}

		if (!properties.containsKey(Constants.DOITIMEZONE)
				|| properties.getProperty(Constants.DOITIMEZONE).toString().isEmpty()
				|| properties.getProperty(Constants.DOITIMEZONE).toString() == null) {
			comparisonMetadata.addToCommonProperties(Constants.DOITIMEZONE, "");
		} else {
			comparisonMetadata.addToCommonProperties(Constants.DOITIMEZONE,
					properties.getProperty(Constants.DOITIMEZONE).toString());
		}

		if (!properties.containsKey(Constants.DOITENANTID)
				|| properties.getProperty(Constants.DOITENANTID).toString().isEmpty()
				|| properties.getProperty(Constants.DOITENANTID).toString() == null) {
			comparisonMetadata.addToCommonProperties(Constants.DOITENANTID, "");
		} else {
			comparisonMetadata.addToCommonProperties(Constants.DOITENANTID,
					properties.getProperty(Constants.DOITENANTID).toString());
		}

		if (!properties.containsKey(Constants.JARVISENDPOINT)
				|| properties.getProperty(Constants.JARVISENDPOINT).toString().isEmpty()
				|| properties.getProperty(Constants.JARVISENDPOINT).toString() == null) {
			comparisonMetadata.addToCommonProperties(Constants.JARVISENDPOINT, "");
		} else {
			comparisonMetadata.addToCommonProperties(Constants.JARVISENDPOINT,
					properties.getProperty(Constants.JARVISENDPOINT).toString());
		}
	}

	private boolean readGenericConfiguration(PropertiesConfiguration properties) {
		boolean isSuccess = true;
		try {
			comparisonMetadata.addToCommonProperties(Constants.EMWEBVIEWPORT,
					properties.getString(Constants.EMWEBVIEWPORT));
			comparisonMetadata.addToCommonProperties(Constants.APPLICATIONNAME,
					properties.getString(Constants.APPLICATIONNAME));
			if (!properties.containsKey(Constants.METRICCLAMP) || properties.getString(Constants.METRICCLAMP).isEmpty()
					|| properties.getString(Constants.METRICCLAMP) == null) {
				comparisonMetadata.addToCommonProperties(Constants.METRICCLAMP, defaultMetricClamp);

			} else {
				comparisonMetadata.addToCommonProperties(Constants.METRICCLAMP,
						properties.getString(Constants.METRICCLAMP));
			}
			MetricDataHelper.setMetricClamp(comparisonMetadata.getCommonPropertyValue(Constants.METRICCLAMP));
			if (!properties.containsKey(Constants.ISBUILDCHANGEEVENTTODOI)
					|| properties.getProperty(Constants.ISBUILDCHANGEEVENTTODOI).toString().isEmpty()
					|| properties.getProperty(Constants.ISBUILDCHANGEEVENTTODOI).toString() == null) {
				comparisonMetadata.setBuildChangeEventtoDOI(false);
			} else {
				comparisonMetadata.setBuildChangeEventtoDOI(properties.getBoolean(Constants.ISBUILDCHANGEEVENTTODOI));
				readDOIProperties(properties);
			}

		} catch (NoSuchElementException ex) {
			JenkinsPlugInLogger.severe("A required property not found ", ex);
			isSuccess = false;
		}
		return isSuccess;
	}

	private void setEmailProperty(EmailInfo emailInfo, String key, String value) {

		if (key.equals(Constants.EMAILSMTPHOST)) {
			emailInfo.setSmtpHost(value);
		} else if (key.equals(Constants.EMAILSMTPAUTH)) {
			emailInfo.setMailSmtpAuth(Boolean.parseBoolean(value));
		} else if (key.equals(Constants.EMAILMODE)) {
			emailInfo.setMailMode(value);
		} else if (key.equals(Constants.GMAILSMTPPORT)) {
			emailInfo.setGmailSmtpPort(value);
		} else if (key.equals(Constants.GMAILSOCKETPORT)) {
			emailInfo.setGmailSocketPort(value);
		} else if (key.equals(Constants.EMAILSENDERID)) {
			emailInfo.setSenderEmailId(value);
		} else {
			setEmailRecipients(emailInfo, key, value);
		}
	}

	private void setEmailRecipients(EmailInfo emailInfo, String key, String value) {
		if (key.equals(Constants.EMAILTORECIPIENTS)) {
			String[] recipients = value.split(",");
			if (recipients.length == 0 || recipients[0].isEmpty()) {
				JenkinsPlugInLogger.warning("No recepient(s) email provided in the configuration");
			} else {
				emailInfo.setToRecipients(Arrays.asList(recipients));
			}
		} else if (key.equals(Constants.EMAILCCRECIPIENTS)) {
			String[] recipients = value.split(",");
			if (recipients.length > 0 && !recipients[0].isEmpty()) {
				emailInfo.setCcRecipients(Arrays.asList(recipients));
			}
		} else if (key.equals(Constants.EMAILBCCRECIPIENTS)) {
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

			if (!properties.containsKey(Constants.LOGGINGLEVEL)
					|| properties.getProperty(Constants.LOGGINGLEVEL).toString().isEmpty()
					|| properties.getProperty(Constants.LOGGINGLEVEL).toString() == null) {
				FileHelper.initializeLog(Constants.DEFAULTLOGGINGLEVEL, loggingFolder, currentBuildNumber);
			} else {
				String loggingLevel = properties.getString(Constants.LOGGINGLEVEL);
				FileHelper.initializeLog(loggingLevel, loggingFolder, currentBuildNumber);
			}

			String extensionsDirectory = properties.getString(Constants.EXTENSIONSDIRECTORY);
			String outputDirectory = null;
			outputDirectory = jInfo.getBuildWorkSpaceFolder() + File.separator + jInfo.getJobName() + File.separator
					+ jInfo.getCurrentBuildNumber();
			comparisonMetadata.getLoadRunnerMetadataInfo().setJenkinsInfo(jInfo);
			IOUtility ioUtility = comparisonMetadata.getIoUtility();
			ioUtility.addToIOProperties(Constants.EXTENSIONSDIRECTORY, extensionsDirectory);
			ioUtility.addToIOProperties(Constants.OUTPUTDIRECTORY, outputDirectory);
			ioUtility.loadExtensionsLibraries();
			JenkinsPlugInLogger.printLogOnConsole(2,
					"Reading configuration properties done" + Constants.NEWLINE + Constants.NEWLINE);
			JenkinsPlugInLogger.printLogOnConsole(2, "Jenkins Information is ");
			JenkinsPlugInLogger.printLogOnConsole(3, "WorkSpace Folder :" + jInfo.getBuildWorkSpaceFolder());
			JenkinsPlugInLogger.printLogOnConsole(3, "Job Name :" + jInfo.getJobName());
			JenkinsPlugInLogger.printLogOnConsole(3,
					"Build Number :" + jInfo.getCurrentBuildNumber() + Constants.NEWLINE);
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
		outputConfiguration.addToCommonProperties(Constants.OUTPUTDIRECTORY,
				ioUtility.getIOPropertyValue(Constants.OUTPUTDIRECTORY));
		outputConfiguration.addToCommonProperties(Constants.WORKSPACEDIRECTORY,
				comparisonMetadata.getJenkinsInfo().getBuildWorkSpaceFolder());
		outputConfiguration.addToCommonProperties(Constants.JENKINSJOBNAME,
				comparisonMetadata.getJenkinsInfo().getJobName());
		outputConfiguration.addToCommonProperties(Constants.EXTENSIONSDIRECTORY,
				ioUtility.getIOPropertyValue(Constants.EXTENSIONSDIRECTORY));
		long endTime = comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getEndTime();
		long startTime = comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getStartTime();
		String durationInString = JenkinsPluginUtility.getDurationInString(startTime, endTime);
		outputConfiguration.addToCommonProperties("runner.start", "" + startTime);
		outputConfiguration.addToCommonProperties("runner.end", "" + endTime);
		outputConfiguration.addToCommonProperties("runner.duration", durationInString);
		outputConfiguration.addToCommonProperties(Constants.JENKINSJOBNAME,
				"" + comparisonMetadata.getJenkinsInfo().getJobName());
		outputConfiguration.addToCommonProperties(Constants.JENKINSCURRENTBUILD,
				"" + comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getNumber());
		outputConfiguration.addToCommonProperties(Constants.JENKINSBENCHMARKBUILD,
				"" + comparisonMetadata.getLoadRunnerMetadataInfo().getBenchMarkBuildInfo().getNumber());
		outputConfiguration.addToSCMRepoAttribs(Constants.JENKINSCURRENTBUILDSCMREPOPARAMS,
				comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getSCMRepoParams());
		outputConfiguration.addToSCMRepoAttribs(Constants.JENKINSBENCHMARKBUILDSCMREPOPARAMS,
				comparisonMetadata.getLoadRunnerMetadataInfo().getBenchMarkBuildInfo().getSCMRepoParams());

		outputConfiguration.addToCommonProperties(Constants.EMURL,
				"" + comparisonMetadata.getApmConnectionInfo().getEmURL());
		outputConfiguration.addToCommonProperties(Constants.EMAUTHTOKEN,
				"" + comparisonMetadata.getApmConnectionInfo().getEmAuthToken());
		outputConfiguration.addToCommonProperties(Constants.APPLICATIONNAME,
				"" + comparisonMetadata.getCommonPropertyValue(Constants.APPLICATIONNAME));
		outputConfiguration.addToCommonProperties(Constants.EMWEBVIEWPORT,
				"" + comparisonMetadata.getCommonPropertyValue(Constants.EMWEBVIEWPORT));
		outputConfiguration
				.setHistogramBuildInfoList(comparisonMetadata.getLoadRunnerMetadataInfo().getHistogramBuildInfoList());
		outputConfiguration.addToCommonProperties(Constants.APPLICATIONHOST,
				"" + comparisonMetadata.getCommonPropertyValue(Constants.APPLICATIONHOST));
	}

	public ComparisonMetadata getComparisonMetadata() {
		return comparisonMetadata;
	}

	public void setComparisonMetadata(ComparisonMetadata comparisonMetadata) {
		this.comparisonMetadata = comparisonMetadata;
	}
}
