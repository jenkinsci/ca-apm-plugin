package com.ca.apm.jenkins.core.entity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.core.util.IOUtility;

/**
 * The class is responsible to read all the configurations provided by the user
 * APM Connection Details Jenkins Environment Variables usable by plugin Load
 * Runner Information configured Comparison Strategies and output handlers
 * defined by user
 * 
 * @author Avinash Chandwani
 *
 */
public class ComparisonMetadata {

	private JenkinsInfo jenkinsInfo;
	private APMConnectionInfo apmConnectionInfo;
	private LoadRunnerMetadata loadRunnerMetadataInfo;
	private StrategiesInfo strategiesInfo;
	private ComparisonResult comparisonResult;
	private OutputConfiguration outputConfiguration;
	private IOUtility ioUtility;
	private boolean isMetadataIsIncorrect;
	private String jobWorkSpaceFolder;
	private boolean failTheBuild;
	private Map<String, String> commonProperties;
	private boolean isPublishBuildResulttoEM;

	public ComparisonMetadata(JenkinsInfo jenkinsInfo) {
		this.jenkinsInfo = jenkinsInfo;
		ioUtility = new IOUtility();
		apmConnectionInfo = new APMConnectionInfo();
		loadRunnerMetadataInfo = new LoadRunnerMetadata();
		strategiesInfo = new StrategiesInfo();
		outputConfiguration = new OutputConfiguration();
		comparisonResult = new ComparisonResult();
		jobWorkSpaceFolder = jenkinsInfo.getBuildWorkSpaceFolder() + File.separator + jenkinsInfo.getJobName()
				+ File.separator;
		commonProperties = new HashMap<String, String>();
	}

	public JenkinsInfo getJenkinsInfo() {
		return jenkinsInfo;
	}

	public void setJenkinsInfo(JenkinsInfo jenkinsInfo) {
		this.jenkinsInfo = jenkinsInfo;
	}

	public APMConnectionInfo getCaapmAuthInfo() {
		return apmConnectionInfo;
	}

	public LoadRunnerMetadata getLoadRunnerMetadataInfo() {
		return loadRunnerMetadataInfo;
	}

	public StrategiesInfo getStrategiesInfo() {
		return strategiesInfo;
	}

	public ComparisonResult getComparisonResult() {
		return comparisonResult;
	}

	public void setComparisonResult(ComparisonResult comparisonResult) {
		this.comparisonResult = comparisonResult;
	}

	public IOUtility getIoUtility() {
		return ioUtility;
	}

	public APMConnectionInfo getApmConnectionInfo() {
		return apmConnectionInfo;
	}

	public OutputConfiguration getOutputConfiguration() {
		return outputConfiguration;
	}

	public boolean isMetadataInCorrect() {
		return isMetadataIsIncorrect;
	}

	public void setMetadataInCorrect(boolean isMetadataCorrect) {
		this.isMetadataIsIncorrect = isMetadataCorrect;
	}

	public String getJobWorkSpaceFolder() {
		return jobWorkSpaceFolder;
	}

	public void setJobWorkSpaceFolder(String jobWorkSpaceFolder) {
		this.jobWorkSpaceFolder = jobWorkSpaceFolder;
	}

	public boolean isFailTheBuild() {
		return failTheBuild;
	}

	public void setFailTheBuild(boolean failTheBuild) {
		this.failTheBuild = failTheBuild;
	}

	public void addToStrategyResults(StrategyResult<?> strategyResult) {
		comparisonResult.addToComparisonStrategyResult(strategyResult);
	}
	
	public void addToCommonProperties(String key, String value) {
		commonProperties.put(key, value);
	}

	public String getCommonPropertyValue(String key) {
		if (commonProperties.containsKey(key)) {
			return commonProperties.get(key);
		}
		return null;
	}

	public boolean isPublishBuildResulttoEM() {
		return isPublishBuildResulttoEM;
	}

	public void setPublishBuildResulttoEM(boolean isPublishBuildResulttoEM) {
		this.isPublishBuildResulttoEM = isPublishBuildResulttoEM;
	}

	@Override
	public String toString() {
		return "ComparisonMetadata [jenkinsInfo=" + jenkinsInfo + ", apmConnectionInfo=" + apmConnectionInfo
				+ ", loadRunnerMetadataInfo=" + loadRunnerMetadataInfo + ", strategiesInfo=" + strategiesInfo
				+ ", comparisonResult=" + comparisonResult + ", outputConfiguration=" + outputConfiguration
				+ ", ioUtility=" + ioUtility + ", isMetadataIsIncorrect=" + isMetadataIsIncorrect
				+ ", jobWorkSpaceFolder=" + jobWorkSpaceFolder + ", failTheBuild=" + failTheBuild
				+ ", commonProperties=" + commonProperties 
				+", isPublishBuildResulttoEM=" + isPublishBuildResulttoEM+ "]";
	}


		
	
}
