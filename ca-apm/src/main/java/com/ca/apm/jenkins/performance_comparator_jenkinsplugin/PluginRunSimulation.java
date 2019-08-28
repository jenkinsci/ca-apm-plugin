package com.ca.apm.jenkins.performance_comparator_jenkinsplugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.executor.ComparisonRunner;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;

import hudson.model.TaskListener;

/**
 * This class is testing class for me for testing small-small functions
 *
 * @author Avinash Chandwani
 */
public class PluginRunSimulation {

	public static void main(String[] args) throws BuildValidationException, BuildExecutionException {
		PluginRunSimulation runSimulation = new PluginRunSimulation();
		runSimulation.runPluginSimulation("C:\\APM\\AutomicJenkins\\Jenkins\\single-properties-changes",
				"performance-comparator.properties");
	}

	public void runPluginSimulation(String path, String fileName)
			throws BuildValidationException, BuildExecutionException {
		List<BuildInfo> histogramBuilds = new ArrayList<>();
		BuildInfo histogramBuilInfo = new BuildInfo();
		histogramBuilInfo.setNumber(35);
		histogramBuilds.add(histogramBuilInfo);
		BuildInfo histogramBuilInfot = new BuildInfo();
		histogramBuilInfo.setNumber(34);
		histogramBuilds.add(histogramBuilInfot);
		BuildInfo histogramBuilInfon = new BuildInfo();
		histogramBuilInfo.setNumber(33);
		histogramBuilds.add(histogramBuilInfon);

		TaskListener taskListener = new TaskListenerMock();
		JenkinsPlugInLogger.setTaskListener(taskListener);
		BuildInfo currentBuilInfo = null;
		BuildInfo benchmarkBuildInfo = null;
		currentBuilInfo = new BuildInfo();
		currentBuilInfo.setNumber(35);

		benchmarkBuildInfo = new BuildInfo();
		benchmarkBuildInfo.setNumber(29);
		/*
		 * currentBuilInfo.setGitSHA("gidssiucsdlk");
		 * benchmarkBuildInfo.setGitSHA("jhfsljsaidjshdgs"); int
		 * nuOfHistogramBuilds = 5
		 */

		JenkinsInfo jenkinsInfo = new JenkinsInfo(currentBuilInfo.getNumber(), benchmarkBuildInfo.getNumber(),
				histogramBuilds, path + "workspace", "CIGNAOne");
		ComparisonRunner runner = new ComparisonRunner(currentBuilInfo, benchmarkBuildInfo, jenkinsInfo,
				path + File.separator + fileName, taskListener);
		runner.executeComparison();
	}
}
