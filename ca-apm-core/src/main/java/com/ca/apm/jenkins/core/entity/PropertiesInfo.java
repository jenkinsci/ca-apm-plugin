package com.ca.apm.jenkins.core.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;

public class PropertiesInfo implements Serializable {

	// APMConnectionInfo
	private String emURL;
	private String emAuthToken;
	private String emTimeZone;
	private String emWebViewPort;

	// JenkinsInfo
	private int currentBuildNumber;
	private int lastSuccessfulBuildNumber;
	private String buildWorkSpaceFolder;
	private String jobName;

	// StrategiesInfo
	private Map<String, Set<String>> outputHandlerToComparisonStrategies;
	private Set<String> nonMappedComparisonStrategies;

	// DOI properties DOITIMEZONE, DOITENANTID, JARVISENDPOINT,
	private Map<String, String> doiProperties;

	// publishBuildChangeEventtoDOI - application name to host map
	private Map<String, String> doiAppsToHostname;

	// common properties logginglevel, extensionDirectory, buildPassorFail,
	// publishBuildResultoEM, buildchangeeventtodoi
	private Map<String, String> commonProperties;

	// StrategyConfig
	Map<String, StrategyConfiguration> strategyConfigProperty;

	// OutputHandlerConfiguration properties map
	private Map<String, OutputHandlerConfiguration> outputHandlerConfig;

	// appToBenchmarkBuildInfo
	private Map<String, BuildInfo> appToBenchmarkBuildInfo;

	// publishBuildResulttoEM apps
	private List<String> appsToPublishBuildResultToEM;

	// emailProperties
	private Map<String, String> emailProperties;
	private Map<String, String> appNameToRecipients;

	// APMConnectionInfo
	public String getEmURL() {
		return emURL;
	}

	public void setEmURL(String emURL) {
		this.emURL = emURL;
	}

	public String getEmAuthToken() {
		return emAuthToken;
	}

	public void setEmAuthToken(String emAuthToken) {
		this.emAuthToken = emAuthToken;
	}

	public String getEmTimeZone() {
		return emTimeZone;
	}

	public void setEmTimeZone(String emTimeZone) {
		this.emTimeZone = emTimeZone;
	}

	public String getEmWebViewPort() {
		return emWebViewPort;
	}

	public void setEmWebViewPort(String emWebViewPort) {
		this.emWebViewPort = emWebViewPort;
	}

	// JenkinsInfo
	public int getCurrentBuildNumber() {
		return currentBuildNumber;
	}

	public void setCurrentBuildNumber(int buildNumber) {
		this.currentBuildNumber = buildNumber;
	}

	public int getLastSuccessfulBuildNumber() {
		return lastSuccessfulBuildNumber;
	}

	public void setLastSuccessfulBuildNumber(int lastSuccessfulBuildNumber) {
		this.lastSuccessfulBuildNumber = lastSuccessfulBuildNumber;
	}

	public String getBuildWorkSpaceFolder() {
		return buildWorkSpaceFolder;
	}

	public void setBuildWorkSpaceFolder(String buildWorkSpaceFolder) {
		this.buildWorkSpaceFolder = buildWorkSpaceFolder;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	// outputHandlerToComparisonStrategies
	public void addOutputHandlerToComparisonStrategies(String outputHandler, String comparisonStrategy) {
		if (outputHandlerToComparisonStrategies == null) {
			outputHandlerToComparisonStrategies = new LinkedHashMap<>();
		}
		Set<String> comparisonStrategies = null;
		if (outputHandlerToComparisonStrategies.containsKey(outputHandler)) {
			comparisonStrategies = outputHandlerToComparisonStrategies.get(outputHandler);
			comparisonStrategies.add(comparisonStrategy);
		} else {
			comparisonStrategies = new HashSet<>();
			comparisonStrategies.add(comparisonStrategy);
			outputHandlerToComparisonStrategies.put(outputHandler, comparisonStrategies);
		}
	}

	public Map<String, Set<String>> getOutputHandlerToComparisonStrategies() {
		return outputHandlerToComparisonStrategies;
	}

	public void addToNonMappedComparisonStrategies(String comparisonStrategy) {
		if (nonMappedComparisonStrategies == null) {
			nonMappedComparisonStrategies = new LinkedHashSet<>();
		}
		nonMappedComparisonStrategies.add(comparisonStrategy);
	}

	// CommonProperties
	public void addToCommonProperties(String key, String value) {
		if (commonProperties == null)
			commonProperties = new HashMap<>();
		commonProperties.put(key, value);
	}

	public String getCommonPropertyValue(String key) {
		if (commonProperties != null && commonProperties.containsKey(key)) {
			return commonProperties.get(key);
		}
		return null;
	}

	// DOI Properties
	public void addToDoiProperties(String key, String value) {
		if (doiProperties == null)
			doiProperties = new HashMap<>();
		doiProperties.put(key, value);
	}

	public Map<String, String> getDoiProperties() {
		return doiProperties;
	}

	public String getDoiPropertyValue(String key) {
		if (doiProperties != null && doiProperties.containsKey(key)) {
			return doiProperties.get(key);
		}
		return null;
	}

	// doiAppsToHostname
	public Map<String, String> getDOIAppsToHostname() {
		if (doiAppsToHostname != null)
			return doiAppsToHostname;
		return null;
	}

	public void addDOIAppsToHostname(String applicationName, String hostName) {
		if (doiAppsToHostname == null)
			doiAppsToHostname = new HashMap();
		doiAppsToHostname.put(applicationName, hostName);

	}

	// StrategyConfiguration
	public void addStrategyConfigProperty(String key, StrategyConfiguration value) {
		if (strategyConfigProperty == null) {
			strategyConfigProperty = new HashMap<>();
		}
		strategyConfigProperty.put(key, value);
	}

	public Map<String, StrategyConfiguration> getStrategyConfigProperty() {
		return strategyConfigProperty;
	}

	// OutputHandler Configuration
	public Map<String, OutputHandlerConfiguration> getOutputHandlerConfig() {
		return outputHandlerConfig;
	}

	public void addOutputHandlerConfig(String key, OutputHandlerConfiguration value) {
		if (outputHandlerConfig == null) {
			outputHandlerConfig = new HashMap<>();
		}
		outputHandlerConfig.put(key, value);
	}

	// appToBenchmarkBuildInfo
	public Map<String, BuildInfo> getAppToBenchmarkBuildInfo() {
		return appToBenchmarkBuildInfo;
	}

	public void setAppToBenchmarkBuildInfo(Map<String, BuildInfo> appToBuildInfo) {
		this.appToBenchmarkBuildInfo = appToBuildInfo;
	}

	// appsToPublishBuildResultToEM
	public List<String> getAppsToPublishBuildResultToEM() {
		return appsToPublishBuildResultToEM;
	}

	public void addAppsToPublishBuildResultToEM(String applicationName) {
		if (appsToPublishBuildResultToEM == null)
			appsToPublishBuildResultToEM = new ArrayList<>();
		appsToPublishBuildResultToEM.add(applicationName);

	}

	// emailProperties
	public Map<String, String> getEmailProperties() {
		return emailProperties;
	}

	public void addToEmailProperties(String key, String value) {
		if (emailProperties == null)
			emailProperties = new HashMap<>();
		emailProperties.put(key, value);
	}

	public String getEmailPropertyValue(String key) {
		if (emailProperties != null && emailProperties.containsKey(key)) {
			return emailProperties.get(key);
		}
		return null;
	}

	// appNameToRecipients
	public Map<String, String> getAppNameToRecipients() {
		return appNameToRecipients;
	}

	public void addAppNameToRecipients(String appName, String toRecipients) {
		if (appNameToRecipients == null)
			appNameToRecipients = new HashMap<>();
		appNameToRecipients.put(appName, toRecipients);

	}

}
