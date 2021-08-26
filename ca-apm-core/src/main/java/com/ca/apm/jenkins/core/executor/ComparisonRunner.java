package com.ca.apm.jenkins.core.executor;

import java.util.logging.Level;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.entity.PropertiesInfo;
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
	private TaskListener taskListener;
	private PropertiesInfo propertiesInfo;
	
	public ComparisonRunner() {
		super();
	}

	public ComparisonRunner(BuildInfo currentBuildInfo, JenkinsInfo jenkinsInfo,
			 TaskListener taskListener, PropertiesInfo propertiesInfo) {
		this.jenkinsInfo = jenkinsInfo;
		this.taskListener = taskListener;
		this.currentBuildInfo = currentBuildInfo;
		this.propertiesInfo = propertiesInfo;
							
	}
	
	
	public JenkinsInfo getJenkinsInfo() {
		return jenkinsInfo;
	}

	public void setJenkinsInfo(JenkinsInfo jenkinsInfo) {
		this.jenkinsInfo = jenkinsInfo;
	}

	private void execute(ComparisonMetadataLoader metadataLoader, boolean isFailtheBuild,
			OutputHandlingExecutor outputHandlingExecutor) {

		metadataLoader.getComparisonMetadata().getLoadRunnerMetadataInfo().getCurrentBuildInfo()
				.setStatus(isFailtheBuild ? "FAILURE" : "SUCCESS");
		outputHandlingExecutor.execute(metadataLoader.getComparisonMetadata().getOutputConfiguration(), isFailtheBuild);

		if (metadataLoader.getComparisonMetadata().getAppsToPublishBuildResultToEM()!=null) {
			VertexAttributesUpdateHelper vertexAttributesUpdateHelper = new VertexAttributesUpdateHelper(
					metadataLoader.getComparisonMetadata());
			vertexAttributesUpdateHelper.updateAttributeOfVertex(!isFailtheBuild);
		}

		if (metadataLoader.getComparisonMetadata().getDoiAppsToHostname() != null && !metadataLoader.getComparisonMetadata().getDoiAppsToHostname().isEmpty()) {
			DOIHelper doiHelper = new DOIHelper(metadataLoader.getComparisonMetadata());
			doiHelper.sendBuildChangeEventtoDOI();
		}

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

		if (jenkinsInfo.getCurrentBuildNumber() == 1) {
			JenkinsPlugInLogger.log(Level.INFO, "Current build number is first build, hence no comparison will happen");
			taskListener.getLogger().println("Current build number is first build, hence no comparison will happen");

		} else {
			try {
				ComparisonMetadataLoader metadataLoader = new ComparisonMetadataLoader(currentBuildInfo,
						 jenkinsInfo, propertiesInfo);

				metadataLoader.loadProperties();
				metadataLoader.validateConfigurations();
				ComparisonExecutor comparisonExecutor = new ComparisonExecutor(metadataLoader.getComparisonMetadata());
				OutputHandlingExecutor outputHandlingExecutor = new OutputHandlingExecutor(
						metadataLoader.getComparisonMetadata());
				comparisonExecutor.execute();

				BuildDecisionMaker decisionMaker = new BuildDecisionMaker(
						metadataLoader.getComparisonMetadata().getComparisonResult());
				if (metadataLoader.getComparisonMetadata().isFailTheBuild() || decisionMaker.isFailed()) {
					isFailtheBuild = true;
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
