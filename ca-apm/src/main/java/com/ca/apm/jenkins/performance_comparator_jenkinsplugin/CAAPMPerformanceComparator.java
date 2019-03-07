package com.ca.apm.jenkins.performance_comparator_jenkinsplugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.executor.ComparisonRunner;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/** @author Avinash Chandwani */
public class CAAPMPerformanceComparator extends Recorder implements SimpleBuildStep, Serializable {

	/** */
	private static final long serialVersionUID = -440923159278868167L;

	private String performanceComparatorProperties;
	private int buildsInHistogram;
	private int benchmarkBuildNumber;
	private String loadGeneratorStartTime;
	private String loadGeneratorEndTime;
	private String loadGeneratorName;
	private String isReadJenkinsLoadRunnerStageDuration = "true";

	@DataBoundConstructor
	public CAAPMPerformanceComparator(String performanceComparatorProperties, String loadGeneratorStartTime,
			String loadGeneratorEndTime, String loadGeneratorName) {
		this.performanceComparatorProperties = performanceComparatorProperties;
		this.loadGeneratorStartTime = loadGeneratorStartTime;
		this.loadGeneratorEndTime = loadGeneratorEndTime;
		this.loadGeneratorName = loadGeneratorName;

	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {

		listener.getLogger().println("Inside perform method");
		return true;
	}

	public String getPerformanceComparatorProperties() {
		return performanceComparatorProperties;
	}

	public void setPerformanceComparatorProperties(String performanceComparatorProperties) {
		this.performanceComparatorProperties = performanceComparatorProperties;
	}

	private boolean runAction(BuildInfo currentBuildInfo, BuildInfo benchmarkBuildInfo, int previousSuccessfulBuild,
			List<BuildInfo> histogramBuildInfoList, String workspaceFolder, String jobName, TaskListener taskListener,
			String loadGeneratorName)
			throws BuildComparatorException, BuildValidationException, BuildExecutionException {
		boolean comparisonRunStatus = false;
		JenkinsInfo jenkinsInfo = new JenkinsInfo(currentBuildInfo.getNumber(), previousSuccessfulBuild,
				histogramBuildInfoList, workspaceFolder, jobName, loadGeneratorName);
		ComparisonRunner runner = new ComparisonRunner(currentBuildInfo, benchmarkBuildInfo, jenkinsInfo,
				this.performanceComparatorProperties, taskListener);
		comparisonRunStatus = runner.executeComparison();
		return comparisonRunStatus;
	}

	private Callable<StringBuilder, IOException> executeComparison(final BuildInfo currentBuildInfo,
			final BuildInfo benchmarkBuildInfo, final int previousSuccessfulBuildNumber,
			final List<BuildInfo> histogramBuildInfoList, final String workspaceFolder, final String jobName,
			final TaskListener taskListener, final String loadGeneratorName) throws IOException, InterruptedException {
		return new Callable<StringBuilder, IOException>() {

			/** */
			private static final long serialVersionUID = 1L;

			StringBuilder consoleLogString = new StringBuilder();

			public StringBuilder call() throws IOException {
				doExecute(currentBuildInfo, benchmarkBuildInfo, previousSuccessfulBuildNumber, histogramBuildInfoList,
						workspaceFolder, jobName, taskListener, loadGeneratorName);
				return consoleLogString;
			}

			@Override
			public void checkRoles(RoleChecker arg0) throws SecurityException {
			}
		};
	}

	/*
	 * private FilePath prepareLocalWorkspace(FilePath masterJobLocation,
	 * FilePath remoteWorkspace, TaskListener taskListener) throws
	 * InterruptedException { try { FilePath localFilePath = new FilePath(new
	 * File(masterJobLocation.getParent() + File.separator +
	 * masterJobLocation.getBaseName() + File.separator + "properties"));
	 * FilePath remotePath = new FilePath( new File(remoteWorkspace.getParent()
	 * + File.separator + remoteWorkspace.getBaseName())); //
	 * filePath.getParent() + File.separator + filePath.getBaseName() + //
	 * File.separator taskListener.getLogger().println("Local-workspace is :" +
	 * masterJobLocation.getParent());
	 * taskListener.getLogger().println("Local-path is :" +
	 * localFilePath.getParent());
	 * taskListener.getLogger().println("Local-workspace exists? " +
	 * masterJobLocation.exists());
	 * taskListener.getLogger().println("Local-path exists? " +
	 * localFilePath.exists());
	 * taskListener.getLogger().println("Remote-workspace is :" +
	 * remoteWorkspace.getParent());
	 * taskListener.getLogger().println("Remote-path is :" + remotePath);
	 * taskListener.getLogger().println("Remote-workspace exists? " +
	 * remoteWorkspace.exists());
	 * taskListener.getLogger().println("Remote-path exists? " +
	 * remotePath.exists());
	 * taskListener.getLogger().println("Local File Path  " +
	 * localFilePath.getParent()); File apmPropertiesFile = new File(
	 * masterJobLocation.getParent() + File.separator +
	 * masterJobLocation.getBaseName() + File.separator + "properties" +
	 * File.separator + "apm.properties"); FilePath apmfP = new
	 * FilePath(apmPropertiesFile); apmfP.copyTo(remotePath); int numberCopied =
	 * localFilePath.copyRecursiveTo(remotePath);
	 * taskListener.getLogger().println("Number of files copied " +
	 * numberCopied); return masterJobLocation; } catch (IOException e) {
	 * taskListener.getLogger()
	 * .print("Error occured while copying files, remoteWorkSpace :" +
	 * remoteWorkspace.getParent() + ", localWorkspace is : " +
	 * masterJobLocation.getParent() + "::" + e.getMessage());
	 * e.printStackTrace(); } return null; }
	 */

	/*
	 * private void doCopyIfRemote(FilePath workspace, Run<?, ?> run,
	 * TaskListener taskListener) throws InterruptedException {
	 *
	 * if (workspace.isRemote()) { FilePath masterJobLocation = new
	 * FilePath(run.getParent().getRootDir());
	 * taskListener.getLogger().println("Root dir is " +
	 * run.getParent().getRootDir()); workspace =
	 * prepareLocalWorkspace(masterJobLocation, workspace, taskListener);
	 * taskListener.getLogger().print("Copied successfully"); } else {
	 * taskListener.getLogger().print(
	 * "[WARNING] Remote workspace detected. Please consider enabling Master/slave mode in the plugin settings"
	 * ); } }
	 */

	private void doExecute(BuildInfo currentBuildInfo, BuildInfo benchMarkBuildInfo, int previousSuccessfulBuildNumber,
			List<BuildInfo> histogramBuildInfoList, String workspaceFolder, String jobName, TaskListener taskListener,
			String loadGeneratorName) throws AbortException {
		boolean isSuccessful = false;
		try {
			isSuccessful = runAction(currentBuildInfo, benchMarkBuildInfo, previousSuccessfulBuildNumber,
					histogramBuildInfoList, workspaceFolder, jobName, taskListener, loadGeneratorName);
		} catch (BuildComparatorException e) {
			taskListener.getLogger().println("Plugin Task failed due to :" + e.getMessage());
			throw new AbortException(
					"*******Performance Comparison Failed*******due to" + Constants.NewLine + e.getMessage());
		} catch (BuildValidationException ex) {
			taskListener.getLogger().println("Plugin Task failed due to :" + ex.getMessage());
			throw new AbortException(
					"*******Performance Comparison Failed******* due to" + Constants.NewLine + ex.getMessage());
		} catch (BuildExecutionException ex) {
			taskListener.getLogger().println("Plugin Task failed due to :" + ex.getMessage());
			throw new AbortException(
					"*******Performance Comparison Failed******* due to" + Constants.NewLine + ex.getMessage());
		}
		if (isSuccessful) {
			taskListener.getLogger().println("CA-APM Jenkins Plugin execution has completed successfully");
		} else {
			taskListener.getLogger().println("Plugin Task is not completed");
			throw new AbortException(
					"*******Performance Comparison Failed******* due to performance crossed the threshold mark, please review results for more details");
		}
	}

	public void addOrReplaceParamValue(Run run, String name, String value) {
		ParametersAction oldParam = run.getAction(ParametersAction.class);
		ParametersAction newParam = run.getAction(ParametersAction.class);
		ArrayList paramsList = new ArrayList<StringParameterValue>();
		paramsList.add(new StringParameterValue(name, value));
		if (oldParam != null) {
			run.removeAction(oldParam);
			newParam = oldParam.createUpdated(paramsList);
		} else {
			newParam = new hudson.model.ParametersAction(paramsList);
		}
		run.addAction(newParam);
	}

	@Override
	public void perform(Run<?, ?> run, FilePath filePath, Launcher launcher, TaskListener taskListener)
			throws InterruptedException, IOException {
		// set logger
		JenkinsPlugInLogger.setTaskListener(taskListener);
		int currentBuildNumber = run.getNumber();
		BuildInfo currentBuildInfo, benchmarkBuildInfo, histogramBuildInfo = null;
		String jobName = filePath.getBaseName();
		JenkinsPlugInLogger.info("jobName:" + jobName);
		String workspaceFolder = "" + filePath.getParent();
		int previousSuccessfulBuildNumber = 0;

		List<BuildInfo> histogramBuildInfoList = new ArrayList<BuildInfo>();
		histogramBuildInfo = new BuildInfo();
		benchmarkBuildInfo = new BuildInfo();
		currentBuildInfo = new BuildInfo();
		addOrReplaceParamValue(run, "loadGeneratorStartTime", loadGeneratorStartTime);
		addOrReplaceParamValue(run, "loadGeneratorEndTime", loadGeneratorEndTime);
		taskListener.getLogger().println("loading config file : " + this.performanceComparatorProperties);
		try {
			loadConfiguration();
		} catch (ConfigurationException | IOException e) {
			JenkinsPlugInLogger.severe("The configuration file is not found or configuration error ", e);
			// fail the build if configuration error
			throw new AbortException(e.getMessage());
		} catch (BuildValidationException e) {
			throw new AbortException(e.getMessage());
		}
		if (currentBuildNumber == 1 || (benchmarkBuildNumber == 0 && run.getPreviousSuccessfulBuild() == null)) {
			JenkinsPlugInLogger.log(Level.INFO, "Current build number is first build, hence no comparison will happen");
			taskListener.getLogger().println("Current build number is first build, hence no comparison will happen");
			taskListener.getLogger().println("CA-APM Jenkins Plugin execution has completed successfully");
			return;
		}
		if (run.getPreviousSuccessfulBuild() == null) {
			previousSuccessfulBuildNumber = 0;
		} else {
			previousSuccessfulBuildNumber = run.getPreviousSuccessfulBuild().getNumber();
		}

		String currentBuildStartTime;
		String currentBuildEndTime;

		if (run != null) {

			currentBuildInfo.setNumber(run.getNumber());
			if (this.isReadJenkinsLoadRunnerStageDuration.equalsIgnoreCase("true")) {
				ParametersAction paramAction = run.getAction(ParametersAction.class);
				currentBuildStartTime = getParamValue(paramAction, "loadGeneratorStartTime");
				currentBuildEndTime = getParamValue(paramAction, "loadGeneratorEndTime");
				currentBuildInfo.setStartTime(Long.parseLong(currentBuildStartTime));
				currentBuildInfo.setEndTime(Long.parseLong(currentBuildEndTime));
				taskListener.getLogger().println("currentBuildNumber = " + run.getNumber() + " currentBuildStartTime = "
						+ currentBuildStartTime + ", currentBuildEndTime = " + currentBuildEndTime);
				JenkinsPlugInLogger.log(Level.INFO,
						"currentBuildNumber = " + run.getNumber() + " currentBuildStartTime = " + currentBuildStartTime
								+ ", currentBuildEndTime = " + currentBuildEndTime);
			}
		}
		histogramBuildInfoList.add(currentBuildInfo);
		if (benchmarkBuildNumber == 0) {
			if (previousSuccessfulBuildNumber > 0) {
				benchmarkBuildNumber = previousSuccessfulBuildNumber;
			}
		} else if (previousSuccessfulBuildNumber == 0 || benchmarkBuildNumber >= currentBuildNumber) {
			JenkinsPlugInLogger.log(Level.INFO,
					"There is no valid benchmarkbuild or previous successful build, hence no comparison will happen. ");
			taskListener.getLogger().println(
					"There is no valid benchmarkbuild or previous successful build, hence no comparison will happen ");
			throw new AbortException(
					"There is no valid benchmarkbuild or previous successful build, hence no comparison will happen ");
		}
		if (benchmarkBuildNumber < currentBuildNumber) {
			Run benchmarkRun = (Run) (run.getParent().getBuilds().limit(currentBuildNumber - benchmarkBuildNumber + 1)
					.toArray()[currentBuildNumber - benchmarkBuildNumber]);
			String benchMarkBuildStartTime;
			String benchMarkBuildEndTime;

			if (benchmarkRun != null) {
				benchmarkBuildInfo.setNumber(benchmarkRun.getNumber());
				if (this.isReadJenkinsLoadRunnerStageDuration.equalsIgnoreCase("true")) {
					ParametersAction paramAction = benchmarkRun.getAction(ParametersAction.class);
					benchMarkBuildStartTime = getParamValue(paramAction, "loadGeneratorStartTime");
					benchMarkBuildEndTime = getParamValue(paramAction, "loadGeneratorEndTime");
					benchmarkBuildInfo.setStartTime(Long.parseLong(benchMarkBuildStartTime));
					benchmarkBuildInfo.setEndTime(Long.parseLong(benchMarkBuildEndTime));
					taskListener.getLogger()
							.println("benchmarkBuildNumber = " + benchmarkRun.getNumber()
									+ " benchMarkBuildStartTime = " + benchMarkBuildStartTime
									+ ", bemnchMarkBuildEndTime = " + benchMarkBuildEndTime);
					JenkinsPlugInLogger.log(Level.INFO,
							"benchmarkBuildNumber = " + benchmarkRun.getNumber() + " benchMarkBuildStartTime = "
									+ benchMarkBuildStartTime + ", bemnchMarkBuildEndTime = " + benchMarkBuildEndTime);
				}
			}

			if (benchmarkRun.getResult().toString().contains("SUCCESS")) {
				benchmarkBuildInfo.setStatus("SUCCESS");
			} else if (benchmarkRun.getResult().toString().contains("FAILURE")) {
				benchmarkBuildInfo.setStatus("FAILURE");
			}
		}
		if (benchmarkBuildInfo.getStartTime() == 0 && benchmarkBuildInfo.getEndTime() == 0) {
			JenkinsPlugInLogger.log(Level.INFO,
					"There is no test time durations for benchmark build, hence no comparison will happen. ");
			taskListener.getLogger()
					.println("There is no test time durations for benchmark build, hence no comparison will happen. ");
		}
		String histogramBuildStartTime;
		String histogramBuildEndTime;
		for (int i = 1; i < this.buildsInHistogram; i++) {
			if (run.getPreviousBuild() == null) {
				break;
			}
			run = run.getPreviousBuild();
			histogramBuildInfo = new BuildInfo();
			histogramBuildInfo.setNumber(run.number);
			if (this.isReadJenkinsLoadRunnerStageDuration.equalsIgnoreCase("true")) {
				ParametersAction paramAction = run.getAction(ParametersAction.class);
				if (paramAction != null) {
					histogramBuildStartTime = getParamValue(paramAction, "loadGeneratorStartTime");
					histogramBuildEndTime = getParamValue(paramAction, "loadGeneratorEndTime");
					histogramBuildInfo.setStartTime(Long.parseLong(histogramBuildStartTime));
					histogramBuildInfo.setEndTime(Long.parseLong(histogramBuildEndTime));
				}
			}
			if (run.getResult().toString().contains("SUCCESS")) {
				histogramBuildInfo.setStatus("SUCCESS");
			} else if (run.getResult().toString().contains("FAILURE")) {
				histogramBuildInfo.setStatus("FAILURE");
			}

			histogramBuildInfoList.add(histogramBuildInfo);

		}

		boolean isRemoteExecution = filePath.isRemote();
		StringBuilder output = null;
		if (isRemoteExecution) {
			taskListener.getLogger().println("Launching in slave machine");
			Callable<StringBuilder, IOException> callable = executeComparison(currentBuildInfo, benchmarkBuildInfo,
					previousSuccessfulBuildNumber, histogramBuildInfoList, workspaceFolder, jobName, taskListener,
					loadGeneratorName);
			VirtualChannel channel = launcher.getChannel();
			output = channel.call(callable);
		} else {
			taskListener.getLogger().println("Launching in master machine");
			doExecute(currentBuildInfo, benchmarkBuildInfo, previousSuccessfulBuildNumber, histogramBuildInfoList,
					workspaceFolder, jobName, taskListener, loadGeneratorName);
		}
	}

	public String getParamValue(ParametersAction pAction, String paramName) {
		ParameterValue parameterValue = pAction.getParameter(paramName);
		if (parameterValue != null)
			return (String) parameterValue.getValue();
		return "0";
	}

	private void loadConfiguration() throws ConfigurationException, IOException, BuildValidationException {

		PropertiesConfiguration properties = new PropertiesConfiguration();
		InputStream input;
		input = new FileInputStream(this.performanceComparatorProperties);
		properties.load(input);
		if (properties.containsKey(Constants.buildsInHistogram)) {
			String nuOfBuildsForHistogram = properties.getString(Constants.buildsInHistogram);
			if (nuOfBuildsForHistogram == null || nuOfBuildsForHistogram.isEmpty()
					|| Integer.parseInt(nuOfBuildsForHistogram) <= 1 || Integer.parseInt(nuOfBuildsForHistogram) > 10)
				buildsInHistogram = 10;
			else
				buildsInHistogram = Integer.parseInt(nuOfBuildsForHistogram);

		} else {
			buildsInHistogram = 10;
		}
		if (properties.containsKey(Constants.benchMarkBuildNumber)) {
			if (!properties.getProperty(Constants.benchMarkBuildNumber).toString().isEmpty()) {
				if (!(Integer.parseInt(properties.getProperty(Constants.benchMarkBuildNumber).toString()) <= 0 ? true
						: false)) {
					benchmarkBuildNumber = Integer
							.parseInt(properties.getProperty(Constants.benchMarkBuildNumber).toString());
					JenkinsPlugInLogger.printLogOnConsole(1,
							"benchmarkbuild number...." + String.valueOf(benchmarkBuildNumber));
				} else {
					JenkinsPlugInLogger.log(Level.INFO, "Please provide valid benchmark build number ");
					throw new BuildValidationException("Please provide valid benchmark build number ");

				}
			}
		}
		if (properties.containsKey(Constants.isReadJenkinsLoadRunnerStageDuration)
				&& !properties.getProperty(Constants.isReadJenkinsLoadRunnerStageDuration).toString().isEmpty()) {
			this.isReadJenkinsLoadRunnerStageDuration = (String) properties
					.getProperty(Constants.isReadJenkinsLoadRunnerStageDuration);
		}
	}

	@Extension
	@Symbol("caapmplugin")
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private String performanceComparatorProperties;
		private String loadGeneratorStartTime;
		private String loadGeneratorEndTime;
		private String loadGeneratorName;

		@Override
		public CAAPMPerformanceComparator newInstance(StaplerRequest req, JSONObject formData) throws FormException {

			try {
				this.performanceComparatorProperties = formData.getString("performanceComparatorProperties");
				CAAPMPerformanceComparator caAPMPublisher = new CAAPMPerformanceComparator(
						this.performanceComparatorProperties, this.loadGeneratorStartTime, this.loadGeneratorEndTime,
						this.loadGeneratorName);
				save();
				return caAPMPublisher;
			} catch (Exception ex) {
				return null;
			}
		}

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> arg0) {
			return true;
		}

		public String getDisplayName() {
			return "Jenkins Plugin for CA APM";
		}
	}
}
