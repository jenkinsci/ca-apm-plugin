package com.ca.apm.jenkins.core.entity;

import java.util.List;

/**
 * This entity holds the Jenkins Specific Information for the use by Plug-in run
 * 
 * @author Avinash Chandwani
 *
 */
public class JenkinsInfo {

	private int currentBuildNumber;
	private int lastSuccessfulBuildNumber;
	private String buildWorkSpaceFolder;
	private String jobName;
	private List<String> histogramBuilds;

	public JenkinsInfo(int currentBuildNumber, int lastSuccessfulBuildNumber, List<String> histogramBuilds, String buildWorkSpaceFolder,
			String jobName) {
		super();
		this.currentBuildNumber = currentBuildNumber;
		this.lastSuccessfulBuildNumber = lastSuccessfulBuildNumber;
		this.buildWorkSpaceFolder = buildWorkSpaceFolder;
		this.jobName = jobName;
		this.histogramBuilds = histogramBuilds;
	}

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

	public List<String> getHistogramBuilds() {
		return histogramBuilds;
	}
	
	
}