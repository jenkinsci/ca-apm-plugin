package com.ca.apm.jenkins.core.executor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.helper.DOIHelper;
import com.ca.apm.jenkins.core.helper.VertexAttributesUpdateHelper;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;

import hudson.model.TaskListener;

/**
 * Main class to start the comparison strategy procedure
 *
 * @author Avinash Chandwani
 */
public class ComparisonRunner {

	private JenkinsInfo jenkinsInfo;
	private BuildInfo currentBuildInfo;
	private BuildInfo benchmarkBuildInfo;
	private String performanceComparatorProperties;
	private TaskListener taskListener;

	public ComparisonRunner() {
		super();
	}

	public ComparisonRunner(BuildInfo currentBuildInfo, BuildInfo benchmarkBuildInfo, JenkinsInfo jenkinsInfo,
			String performanceComparatorProperties, TaskListener taskListener) {
		this.jenkinsInfo = jenkinsInfo;
		this.performanceComparatorProperties = performanceComparatorProperties;
		this.taskListener = taskListener;
		this.currentBuildInfo = currentBuildInfo;
		this.benchmarkBuildInfo = benchmarkBuildInfo;
	}

	public JenkinsInfo getJenkinsInfo() {
		return jenkinsInfo;
	}

	public void setJenkinsInfo(JenkinsInfo jenkinsInfo) {
		this.jenkinsInfo = jenkinsInfo;
	}

	public String getPerformanceComparatorProperties() {
		return performanceComparatorProperties;
	}

	public void setPerformanceComparatorProperties(String performanceComparatorProperties) {
		this.performanceComparatorProperties = performanceComparatorProperties;
	}

	public BuildInfo getCurrentBuildInfo() {
		return currentBuildInfo;
	}

	public void setCurrentBuildInfo(BuildInfo currentBuildInfo) {
		this.currentBuildInfo = currentBuildInfo;
	}

	public BuildInfo getBenchmarkBuildInfo() {
		return benchmarkBuildInfo;
	}

	public void setBenchmarkBuildInfo(BuildInfo benchmarkBuildInfo) {
		this.benchmarkBuildInfo = benchmarkBuildInfo;
	}

	private void execute(ComparisonMetadataLoader metadataLoader, boolean isFailtheBuild,
			OutputHandlingExecutor outputHandlingExecutor) {

		metadataLoader.getComparisonMetadata().getLoadRunnerMetadataInfo().getCurrentBuildInfo()
				.setStatus(isFailtheBuild ? "FAILURE" : "SUCCESS");
		outputHandlingExecutor.execute(metadataLoader.getComparisonMetadata().getOutputConfiguration(), isFailtheBuild);

		if (metadataLoader.getComparisonMetadata().isPublishBuildResulttoEM()) {
			VertexAttributesUpdateHelper vertexAttributesUpdateHelper = new VertexAttributesUpdateHelper(
					metadataLoader.getComparisonMetadata());
			vertexAttributesUpdateHelper.updateAttributeOfVertex(!isFailtheBuild);
		}

		if (metadataLoader.getComparisonMetadata().isBuildChangeEventtoDOI()) {
			DOIHelper doiHelper = new DOIHelper(metadataLoader.getComparisonMetadata());
			doiHelper.sendBuildChangeEventtoDOI();
		}

	}

	private PropertiesConfiguration loadProperties() {
		PropertiesConfiguration properties = null;
		try {
			properties = new PropertiesConfiguration();
			properties.load(new FileInputStream(performanceComparatorProperties));

		} catch (IOException e) {

			JenkinsPlugInLogger.severe("The configuration file is not found ", e);

		} catch (ConfigurationException e) {
			JenkinsPlugInLogger.severe("The configuration file has encountered some errors ", e);
		}
		return properties;
	}

	/**
	 * Main method to start the comparator-plugin execution
	 *
	 * @return Returns a boolean value true or false based upon success or
	 *         failure of execution
	 * @throws BuildComparatorException
	 *             : In case of any error/exception during the execution occurs,
	 *             this exception is throw with appropriate message
	 * @throws BuildValidationException
	 */
	public boolean executeComparison() throws BuildValidationException, BuildExecutionException {
		boolean isFailtheBuild = false;

		PropertiesConfiguration properties = loadProperties();
		if (jenkinsInfo.getCurrentBuildNumber() == 1) {
			JenkinsPlugInLogger.log(Level.INFO, "Current build number is first build, hence no comparison will happen");
			taskListener.getLogger().println("Current build number is first build, hence no comparison will happen");

		} else if (properties.containsKey(Constants.BENCHMARKBUILDNUMBER)
				&& properties.getProperty(Constants.BENCHMARKBUILDNUMBER).toString().isEmpty()
				&& jenkinsInfo.getLastSuccessfulBuildNumber() <= 0) {

			JenkinsPlugInLogger.log(Level.INFO,
					"There is no previous successful build, hence no comparison will happen");
			taskListener.getLogger().println("There is no previous successful build, hence no comparison will happen");

		} else {
			try {
				ComparisonMetadataLoader metadataLoader = new ComparisonMetadataLoader(currentBuildInfo,
						benchmarkBuildInfo, jenkinsInfo, performanceComparatorProperties);

				metadataLoader.loadProperties();
				metadataLoader.validateConfigurations();
				ComparisonExecutor comparisonExecutor = new ComparisonExecutor(metadataLoader.getComparisonMetadata());
				OutputHandlingExecutor outputHandlingExecutor = new OutputHandlingExecutor(
						metadataLoader.getComparisonMetadata());
				comparisonExecutor.execute();

				BuildDecisionMaker decisionMaker = new BuildDecisionMaker(
						metadataLoader.getComparisonMetadata().getComparisonResult());
				if (metadataLoader.getComparisonMetadata().isFailTheBuild()) {
					isFailtheBuild = decisionMaker.isFailed();
				}
				execute(metadataLoader, isFailtheBuild, outputHandlingExecutor);
			} catch (BuildComparatorException ex) {
				if (isFailtheBuild) {
					JenkinsPlugInLogger.printLogOnConsole(0, "Comparator Plugin Execution Completed with failures");
					throw new BuildComparatorException(ex.getMessage());

				} else {
					JenkinsPlugInLogger.severe("Error occured in comparison ->" + ex.getMessage()
							+ ", but you prefer not to fail the build, hence we are not going to fail the build");
				}
				JenkinsPlugInLogger.printLogOnConsole(0, "Comparator Plugin Execution Completed with failures");
			} catch (BuildValidationException ex) {
				throw new BuildValidationException(ex.getMessage());
			}
		}
		return !isFailtheBuild;
	}
}
