package com.ca.apm.jenkins.core.executor;

import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.helper.VertexAttributesUpdateHelper;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import hudson.model.TaskListener;
import java.util.logging.Level;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.ca.apm.jenkins.core.util.Constants;

/**
 * Main class to start the comparison strategy procedure
 *
 * @author Avinash Chandwani
 */
public class ComparisonRunner {

	private JenkinsInfo jenkinsInfo;
	private String performanceComparatorProperties;
	private TaskListener taskListener;
	
	public ComparisonRunner() {
		super();
	}

	public ComparisonRunner(JenkinsInfo jenkinsInfo, String performanceComparatorProperties,
			TaskListener taskListener) {
		this.jenkinsInfo = jenkinsInfo;
		this.performanceComparatorProperties = performanceComparatorProperties;
		this.taskListener = taskListener;
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

	/**
	 * Main method to start the comparator-plugin execution
	 *
	 * @return Returns a boolean value true or false based upon success or
	 *         failure of execution
	 * @throws BuildComparatorException
	 *             : In case of any error/exception during the execution occurs,
	 *              this exception is throw with appropriate message
	 * @throws BuildValidationException
	 */
	public boolean executeComparison()
			throws BuildComparatorException, BuildValidationException, BuildExecutionException {
		boolean isFailToBuild = false;

		PropertiesConfiguration properties = null;
		try {
			properties = new PropertiesConfiguration();
			properties.load(new FileInputStream(performanceComparatorProperties));

		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				JenkinsPlugInLogger.severe("The configuration file is not found ", e);
			}
		} catch (ConfigurationException e) {
			JenkinsPlugInLogger.severe("The configuration file has encountered some errors ", e);
		}

		if (jenkinsInfo.getCurrentBuildNumber() == 1) {
			JenkinsPlugInLogger.log(Level.INFO, "Current build number is first build, hence no comparison will happen");
			taskListener.getLogger().println("Current build number is first build, hence no comparison will happen");

		} else if (properties.containsKey(Constants.benchMarkBuildNumber)
				&& properties.getProperty(Constants.benchMarkBuildNumber).toString().isEmpty()
				&& jenkinsInfo.getLastSuccessfulBuildNumber() <= 0) {

			JenkinsPlugInLogger.log(Level.INFO,
					"There is no previous successful build, hence no comparison will happen");
			taskListener.getLogger().println("There is no previous successful build, hence no comparison will happen");

		} else {
			try {
				ComparisonMetadataLoader metadataLoader = new ComparisonMetadataLoader(jenkinsInfo,
						performanceComparatorProperties);
				metadataLoader.loadProperties();
				metadataLoader.validateConfigurations();
				ComparisonExecutor comparisonExecutor = new ComparisonExecutor(metadataLoader.getComparisonMetadata());
				OutputHandlingExecutor outputHandlingExecutor = new OutputHandlingExecutor(
						metadataLoader.getComparisonMetadata());
				comparisonExecutor.execute();

				BuildDecisionMaker decisionMaker = new BuildDecisionMaker(
						metadataLoader.getComparisonMetadata().getComparisonResult());
				if (metadataLoader.getComparisonMetadata().isFailTheBuild()) {
					isFailToBuild = decisionMaker.isFailed();
				}

				outputHandlingExecutor.execute(metadataLoader.getComparisonMetadata().getOutputConfiguration(),
						isFailToBuild);

				if (metadataLoader.getComparisonMetadata().isPublishBuildResulttoEM()) {
					VertexAttributesUpdateHelper vertexAttributesUpdateHelper = new VertexAttributesUpdateHelper(
							metadataLoader.getComparisonMetadata());
					vertexAttributesUpdateHelper.updateAttributeOfVertex(!isFailToBuild);
				}

			} catch (BuildComparatorException ex) {
				if (isFailToBuild) {
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
		return !isFailToBuild;
	}
}
