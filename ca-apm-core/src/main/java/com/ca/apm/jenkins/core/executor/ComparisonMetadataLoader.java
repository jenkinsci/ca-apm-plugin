package com.ca.apm.jenkins.core.executor;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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
import com.ca.apm.jenkins.core.entity.PropertiesInfo;
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

	private PropertiesInfo propertiesInfo;

	private static String defaultMetricClamp = "10";

	private static final String INPUTPROPERTIESFILEDOESNOTEXIST = "Input Properties file(s) defined in parameters does not exist, please check";

	public ComparisonMetadataLoader(BuildInfo currentBuildInfo, JenkinsInfo jenkinsInfo,
			PropertiesInfo propertiesInfo) {
		super();
		comparisonMetadata = new ComparisonMetadata(jenkinsInfo);
		LoadRunnerMetadata loadRunnerMetadata = comparisonMetadata.getLoadRunnerMetadataInfo();
		loadRunnerMetadata.setCurrentBuildInfo(currentBuildInfo);
		loadRunnerMetadata.setAppToBenchMarkBuildInfo(propertiesInfo.getAppToBenchmarkBuildInfo());
		this.propertiesInfo = propertiesInfo;
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
		doRead();
	}

	public void validateConfigurations() throws BuildValidationException {
		JenkinsPlugInLogger.info("Configuration validation started");
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration validation started");
		validateAllConfigurations();
		JenkinsPlugInLogger.info("Configuration validation completed");
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration validation completed" + Constants.NEWLINE);
		prepareOutputProperties();
	}

	private void doRead() {
		JenkinsPlugInLogger.printLogOnConsole(0, "Comparator Plugin Execution Started" + Constants.NEWLINE);
		JenkinsPlugInLogger.printLogOnConsole(1, "Configuration loading started" + Constants.NEWLINE);
		JenkinsPlugInLogger.printLogOnConsole(2, "File Names are ");
		boolean isIoSuccess = readIOUtilityConfiguration();
		boolean isAPMSuccess = readAPMConnectionConfiguration();
		// boolean isGenericSuccess = readGenericConfiguration();
		boolean isStrategiesFileSuccess = readStrategiesConfiguration();
		boolean isGenericSuccess = readGenericConfiguration();
		boolean isloadRunnerSuccess = true;
		comparisonMetadata.getLoadRunnerMetadataInfo()
				.setHistogramBuildInfoList(comparisonMetadata.getJenkinsInfo().getHistogramBuildInfoList());

		checkIfFileExists(isAPMSuccess, isloadRunnerSuccess, isStrategiesFileSuccess, isIoSuccess, isGenericSuccess);
		JenkinsPlugInLogger.printLogOnConsole(1, " Configuration loading completed" + Constants.NEWLINE);
		JenkinsPlugInLogger.info("Loading of Properties file completed");
	}

	private void readStrategiesAdditionalInformation() {

		if (propertiesInfo.getCommonPropertyValue(Constants.BUILDPASSORFAIL) == null
				|| propertiesInfo.getCommonPropertyValue(Constants.BUILDPASSORFAIL).isEmpty()) {
			comparisonMetadata.setFailTheBuild(true);
		} else {
			comparisonMetadata.setFailTheBuild(
					Boolean.parseBoolean(propertiesInfo.getCommonPropertyValue(Constants.BUILDPASSORFAIL)));
		}
		if (propertiesInfo.getAppsToPublishBuildResultToEM() != null) {
			comparisonMetadata.setAppsToPublishBuildResultToEM(propertiesInfo.getAppsToPublishBuildResultToEM());
		}
	}

	private void readComparisonStrategiesInformation() {

		Map<String, StrategyConfiguration> strategyConfigMap = propertiesInfo.getStrategyConfigProperty();
		for (Map.Entry<String, StrategyConfiguration> strategyConfigEntry : strategyConfigMap.entrySet()) {
			comparisonMetadata.getStrategiesInfo().addComparisonStrategyInfo(strategyConfigEntry.getKey(),
					strategyConfigEntry.getValue());

		}
		addToOutputHandlerToComparisonStrategies();

	}

	private void addToOutputHandlerToComparisonStrategies() {
		Map<String, Set<String>> outputHandlerToComparisonStrategiesMap = propertiesInfo
				.getOutputHandlerToComparisonStrategies();
		for (Map.Entry<String, Set<String>> outputHandlerToComparisonStrategiesentry : outputHandlerToComparisonStrategiesMap
				.entrySet()) {

			for (String comparisonStrategy : outputHandlerToComparisonStrategiesentry.getValue()) {
				comparisonMetadata.getStrategiesInfo().addToOutputHandlerToComparisonStrategies(
						outputHandlerToComparisonStrategiesentry.getKey(), comparisonStrategy);

			}

		}
	}

	private void readOutputHandlerStrategiesInformation() {

		Map<String, OutputHandlerConfiguration> OutputHandlerConfigurationMap = propertiesInfo.getOutputHandlerConfig();
		for (Map.Entry<String, OutputHandlerConfiguration> entry : OutputHandlerConfigurationMap.entrySet()) {
			comparisonMetadata.getStrategiesInfo().addOutputHandlersInfo(entry.getKey(), entry.getValue());
		}

	}

	private void readEmailInformation() {
		Map<String, String> emailInfoMap = propertiesInfo.getEmailProperties();
		EmailInfo emailInfo = new EmailInfo();
		for (Map.Entry<String, String> entry : emailInfoMap.entrySet()) {
			setEmailProperty(emailInfo, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, String> entry : propertiesInfo.getAppNameToRecipients().entrySet()) {
			String[] recipients = entry.getValue().split(",");
			if (recipients.length == 0 || recipients[0].isEmpty()) {
				JenkinsPlugInLogger.warning("No recepient(s) email provided in the configuration");
			} else {
				emailInfo.addAppToRecipients(entry.getKey(), Arrays.asList(recipients));
			}

		}
		EmailHelper.setEmailInfo(emailInfo);
	}

	private boolean readStrategiesConfiguration() {
		boolean isSuccess = true;
		try {
			readStrategiesAdditionalInformation();
			readComparisonStrategiesInformation();
			readOutputHandlerStrategiesInformation();
			readEmailInformation();
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
		for (Map.Entry<String, BuildInfo> entry : loadRunnerMetadata.getAppToBenchMarkBuildInfo().entrySet()) {
			long benchMarkEndTime = entry.getValue().getEndTime();
			long benchMarkStartTime = entry.getValue().getStartTime();
			if (currentEndTime < currentStartTime) {
				errorMessages
						.append("Error : Current Build's load runner end time is less than start time for the application "
								+ entry.getKey())
						.append(Constants.NEWLINE);
			}
			if (benchMarkEndTime < benchMarkStartTime) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages
						.append("Error : Benchmark Build's load runner end time is less than start time for the application "
								+ entry.getKey())
						.append(Constants.NEWLINE);
			}
			if (benchMarkStartTime > currentStartTime) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages
						.append("Error : Current Build's load runner start time is less than benchmark build's start time for the application "
								+ entry.getKey())
						.append(Constants.NEWLINE);
			}
			if (benchMarkEndTime > currentEndTime) {
				errorMessages.append(JenkinsPlugInLogger.getLevelString(3));
				errorMessages
						.append("Error : Current Build's load runner end time is less than benchmark build's end time for the application "
								+ entry.getKey())
						.append(Constants.NEWLINE);
			}
		}
	}

	private boolean readAPMConnectionConfiguration() {
		boolean isSuccess = true;
		try {
			APMConnectionInfo apmConnectionInfo = comparisonMetadata.getApmConnectionInfo();

			if (propertiesInfo.getEmURL() == null || propertiesInfo.getEmURL().isEmpty()) {
				comparisonMetadata.setMetadataInCorrect(true);
				JenkinsPlugInLogger.severe(" em.url property value is not found");
				JenkinsPlugInLogger.printLogOnConsole(2, " em.url property value is not found");
				isSuccess = false;
			} else {
				apmConnectionInfo.setEmURL(propertiesInfo.getEmURL());
				comparisonMetadata.addToCommonProperties(Constants.EMURL, propertiesInfo.getEmURL());
			}

			if (propertiesInfo.getEmAuthToken() == null || propertiesInfo.getEmAuthToken().isEmpty()) {
				comparisonMetadata.setMetadataInCorrect(true);
				JenkinsPlugInLogger.severe(" em.authtoken property value is not found");
				JenkinsPlugInLogger.printLogOnConsole(2, " em.authtoken property value is not found");
				isSuccess = false;
			} else {
				apmConnectionInfo.setEmAuthToken(propertiesInfo.getEmAuthToken());
				comparisonMetadata.addToCommonProperties(Constants.EMAUTHTOKEN, propertiesInfo.getEmAuthToken());
			}
			if (propertiesInfo.getEmTimeZone() == null || propertiesInfo.getEmTimeZone().isEmpty()) {

				JenkinsPlugInLogger.severe(" em.timezone property value is not found");
				JenkinsPlugInLogger.printLogOnConsole(2, " em.timezone property value is not found");
				isSuccess = false;
			} else {
				apmConnectionInfo.setEmTimeZone(propertiesInfo.getEmTimeZone());
				comparisonMetadata.addToCommonProperties(Constants.EMTIMEZONE, propertiesInfo.getEmTimeZone());
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

	private void readDOIProperties() {

		if (propertiesInfo.getDoiPropertyValue(Constants.DOITIMEZONE).toString() == null
				|| propertiesInfo.getDoiPropertyValue(Constants.DOITIMEZONE).toString().isEmpty()) {
			comparisonMetadata.addToCommonProperties(Constants.DOITIMEZONE, "");
		} else {
			comparisonMetadata.addToCommonProperties(Constants.DOITIMEZONE,
					propertiesInfo.getDoiPropertyValue(Constants.DOITIMEZONE));
		}

		if (propertiesInfo.getDoiPropertyValue(Constants.DOITENANTID) == null
				|| propertiesInfo.getDoiPropertyValue(Constants.DOITENANTID).isEmpty()) {
			comparisonMetadata.addToCommonProperties(Constants.DOITENANTID, "");
		} else {
			comparisonMetadata.addToCommonProperties(Constants.DOITENANTID,
					propertiesInfo.getDoiPropertyValue(Constants.DOITENANTID));
		}

		if (propertiesInfo.getDoiPropertyValue(Constants.JARVISENDPOINT) == null
				|| propertiesInfo.getDoiPropertyValue(Constants.JARVISENDPOINT).isEmpty()) {
			comparisonMetadata.addToCommonProperties(Constants.JARVISENDPOINT, "");
		} else {
			comparisonMetadata.addToCommonProperties(Constants.JARVISENDPOINT,
					propertiesInfo.getDoiPropertyValue(Constants.JARVISENDPOINT));
		}
	}

	private boolean readGenericConfiguration() {
		boolean isSuccess = true;
		try {

			comparisonMetadata.addToCommonProperties(Constants.EMWEBVIEWPORT,
					propertiesInfo.getCommonPropertyValue(Constants.EMWEBVIEWPORT));

			if (propertiesInfo.getCommonPropertyValue(Constants.METRICCLAMP) == null
					|| propertiesInfo.getCommonPropertyValue(Constants.METRICCLAMP).isEmpty()) {
				comparisonMetadata.addToCommonProperties(Constants.METRICCLAMP, defaultMetricClamp);

			} else {
				comparisonMetadata.addToCommonProperties(Constants.METRICCLAMP,
						propertiesInfo.getCommonPropertyValue(Constants.METRICCLAMP));
			}
			
			comparisonMetadata.addToCommonProperties(Constants.LOADGENERATORNAME,
					propertiesInfo.getCommonPropertyValue(Constants.LOADGENERATORNAME));
			
			MetricDataHelper.setMetricClamp(comparisonMetadata.getCommonPropertyValue(Constants.METRICCLAMP));
			comparisonMetadata.setDoiAppsToHostname(propertiesInfo.getDOIAppsToHostname());
			readDOIProperties();

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
		} else if (key.equals(Constants.EMAILPWD)) {
			emailInfo.setPassword(value);
		} else {
			setEmailRecipients(emailInfo, key, value);
		}
	}

	private void setEmailRecipients(EmailInfo emailInfo, String key, String value) {
		if (key.equals(Constants.EMAILCCRECIPIENTS)) {
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

	private boolean readIOUtilityConfiguration() {
		boolean isSuccess = true;
		try {
			JenkinsInfo jInfo = comparisonMetadata.getJenkinsInfo();
			String loggingFolder = jInfo.getBuildWorkSpaceFolder() + File.separator + jInfo.getJobName();
			int currentBuildNumber = jInfo.getCurrentBuildNumber();
			JenkinsPlugInLogger.printLogOnConsole(2, "Logging folder location is " + loggingFolder);

			if (propertiesInfo.getCommonPropertyValue(Constants.LOGGINGLEVEL).isEmpty()
					|| propertiesInfo.getCommonPropertyValue(Constants.LOGGINGLEVEL) == null) {
				FileHelper.initializeLog(Constants.DEFAULTLOGGINGLEVEL, loggingFolder, currentBuildNumber);
			} else {
				String loggingLevel = propertiesInfo.getCommonPropertyValue(Constants.LOGGINGLEVEL);
				FileHelper.initializeLog(loggingLevel, loggingFolder, currentBuildNumber);
			}

			String extensionsDirectory = propertiesInfo.getCommonPropertyValue(Constants.EXTENSIONSDIRECTORY);
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
		outputConfiguration.addToCommonProperties(Constants.LOADGENERATORNAME, 
				"" + comparisonMetadata.getCommonPropertyValue(Constants.LOADGENERATORNAME));
		outputConfiguration.addToCommonProperties(Constants.JENKINSJOBNAME,
				"" + comparisonMetadata.getJenkinsInfo().getJobName());
		outputConfiguration.addToCommonProperties(Constants.JENKINSCURRENTBUILD,
				"" + comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getNumber());
		outputConfiguration.setAppToBenchmarkBuildInfo(
				comparisonMetadata.getLoadRunnerMetadataInfo().getAppToBenchMarkBuildInfo());
		outputConfiguration.addToSCMRepoAttribs(Constants.JENKINSCURRENTBUILDSCMREPOPARAMS,
				comparisonMetadata.getLoadRunnerMetadataInfo().getCurrentBuildInfo().getSCMRepoParams());

		outputConfiguration.addToCommonProperties(Constants.EMURL,
				"" + comparisonMetadata.getApmConnectionInfo().getEmURL());
		outputConfiguration.addToCommonProperties(Constants.EMAUTHTOKEN,
				"" + comparisonMetadata.getApmConnectionInfo().getEmAuthToken());
		
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
