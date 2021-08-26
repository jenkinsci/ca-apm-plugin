package com.ca.apm.jenkins.performance_comparator_jenkinsplugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.entity.OutputHandlerConfiguration;
import com.ca.apm.jenkins.core.entity.PropertiesInfo;
import com.ca.apm.jenkins.core.executor.ComparisonRunner;
import com.ca.apm.jenkins.core.helper.EmailHelper;
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
import hudson.model.Result;
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
import jline.internal.InputStreamReader;
import net.sf.json.JSONObject;

/** @author Avinash Chandwani */
public class CAAPMPerformanceComparator extends Recorder implements SimpleBuildStep, Serializable {

	/** */
	private static final long serialVersionUID = -440923159278868167L;

	private int buildsInHistogram;
	private String loadGeneratorStartTime;
	private String loadGeneratorEndTime;
	private String loadGeneratorName;
	private Map<String, String> attribsMap;
	private PropertiesInfo propertiesInfo;
	private JenkinsInfo jenkinsInfo;
	private Map<String, BuildInfo> appToBenchmarkBuildInfo;
	private String configFilesPath;

	private static final String PLUGINTASKFAILEDDUETO = "Plugin Task failed due to :";
	private static final String LODGENSTARTTIME = "loadGeneratorStartTime";
	private static final String LOADGENENDTIME = "loadGeneratorEndTime";
	private static final String SUCCESS = "SUCCESS";
	private static final String FAILURE = "FAILURE";
	private static final String CURRENTBUILDSCMPARAMS = " currentBuildScmParams = ";

	@DataBoundConstructor
	public CAAPMPerformanceComparator(String performanceComparatorProperties, String loadGeneratorStartTime,
			String loadGeneratorEndTime, String loadGeneratorName, String attribsStr) {
		this.configFilesPath = performanceComparatorProperties;
		this.loadGeneratorStartTime = loadGeneratorStartTime;
		this.loadGeneratorEndTime = loadGeneratorEndTime;
		this.loadGeneratorName = loadGeneratorName;
		convertAttribsStrToMap(attribsStr);
	}

	private void convertAttribsStrToMap(String attribsList) {
		attribsMap = new HashMap<>();
		if (attribsList != null && !attribsList.isEmpty()) {
			attribsList = attribsList.substring(1, attribsList.length() - 1);
			String[] keyValuePairs = attribsList.split(",");
			for (int i = 0; i < keyValuePairs.length; i++) {
				String[] entry = keyValuePairs[i].split(":");
				attribsMap.put(entry[0].trim(), entry[1].trim());
			}
		}

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

	private boolean runAction(BuildInfo currentBuildInfo, int previousSuccessfulBuild,
			List<BuildInfo> histogramBuildInfoList, String workspaceFolder, String jobName, TaskListener taskListener,
			PropertiesInfo propertiesInfo) throws BuildValidationException, BuildExecutionException {
		boolean comparisonRunStatus = false;
		jenkinsInfo = new JenkinsInfo(currentBuildInfo.getNumber(), previousSuccessfulBuild, histogramBuildInfoList,
				workspaceFolder, jobName);
		ComparisonRunner runner = new ComparisonRunner(currentBuildInfo, jenkinsInfo, taskListener, propertiesInfo);
		comparisonRunStatus = runner.executeComparison();
		return comparisonRunStatus;
	}

	private Callable<StringBuilder, IOException> executeComparison(final BuildInfo currentBuildInfo,
			final int previousSuccessfulBuildNumber, final List<BuildInfo> histogramBuildInfoList,
			final String workspaceFolder, final String jobName, final TaskListener taskListener) {
		return new Callable<StringBuilder, IOException>() {

			private static final long serialVersionUID = 1L;

			StringBuilder consoleLogString = new StringBuilder();

			public StringBuilder call() throws IOException {
				doExecute(currentBuildInfo, previousSuccessfulBuildNumber, histogramBuildInfoList, workspaceFolder,
						jobName, taskListener);
				return consoleLogString;
			}

			@Override
			public void checkRoles(RoleChecker arg0) {
				// Do Nothing
			}
		};
	}

	private void doExecute(BuildInfo currentBuildInfo, int previousSuccessfulBuildNumber,
			List<BuildInfo> histogramBuildInfoList, String workspaceFolder, String jobName, TaskListener taskListener)
			throws AbortException {
		boolean isSuccessful = false;
		try {
			JenkinsPlugInLogger.setTaskListener(taskListener);
			isSuccessful = runAction(currentBuildInfo, previousSuccessfulBuildNumber, histogramBuildInfoList,
					workspaceFolder, jobName, taskListener, propertiesInfo);
		} catch (BuildComparatorException | BuildValidationException | BuildExecutionException e) {
			taskListener.getLogger().println(PLUGINTASKFAILEDDUETO + e.getMessage());
			throw new AbortException(
					"*******Performance Comparison FAILED*******due to" + Constants.NEWLINE + e.getMessage());
		}
		if (isSuccessful) {
			taskListener.getLogger().println("CA-APM Jenkins Plugin execution has completed successfully");
		} else {
			taskListener.getLogger().println("Plugin Task is not completed");
			throw new AbortException(
					"*******Performance Comparison FAILED******* due to error in getting no strategy results or performance crossed the threshold mark, please review results for more details");
		}
	}

	public void addOrReplaceParamValue(Run run, String name, String value) {
		ParametersAction oldParam = run.getAction(ParametersAction.class);
		ParametersAction newParam = null;
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

	private void setBenchmarkBuildInfo(Run<?, ?> run, int currentBuildNumber, BuildInfo benchmarkBuildInfo,
			TaskListener taskListener) {
		int benchmarkBuildNumber = benchmarkBuildInfo.getNumber();

		Run benchmarkRun = (Run) (run.getParent().getBuilds().limit(currentBuildNumber - benchmarkBuildNumber + 1)
				.toArray()[currentBuildNumber - benchmarkBuildNumber]);
		String benchMarkBuildStartTime;
		String benchMarkBuildEndTime;

		if (benchmarkRun != null) {
			benchmarkBuildInfo.setNumber(benchmarkRun.getNumber());
			ParametersAction paramAction = benchmarkRun.getAction(ParametersAction.class);
			benchMarkBuildStartTime = getParamValue(paramAction, LODGENSTARTTIME);
			benchMarkBuildEndTime = getParamValue(paramAction, LOADGENENDTIME);
			benchmarkBuildInfo.setStartTime(Long.parseLong(benchMarkBuildStartTime));
			benchmarkBuildInfo.setEndTime(Long.parseLong(benchMarkBuildEndTime));
			taskListener.getLogger()
					.println("benchmarkBuildNumber = " + benchmarkRun.getNumber() + " benchMarkBuildStartTime = "
							+ benchMarkBuildStartTime + ", bemnchMarkBuildEndTime = " + benchMarkBuildEndTime
							+ ", benchMarkBuildScmParams = " + benchmarkBuildInfo.getSCMRepoParams());

			JenkinsPlugInLogger.log(Level.INFO,
					"benchmarkBuildNumber = " + benchmarkRun.getNumber() + " benchMarkBuildStartTime = "
							+ benchMarkBuildStartTime + ", bemnchMarkBuildEndTime = " + benchMarkBuildEndTime
							+ ", benchMarkBuildscmRepoParams = " + benchmarkBuildInfo.getSCMRepoParams());
			try {
				Result benchmarkResult = benchmarkRun.getResult();
				if (null != benchmarkResult) {
					if (benchmarkResult.toString().contains(SUCCESS)) {
						benchmarkBuildInfo.setStatus(SUCCESS);
					} else {
						benchmarkBuildInfo.setStatus(FAILURE);
					}

				}
			} catch (Exception ex) {
				JenkinsPlugInLogger
						.severe("Error during the execution of setBenchmarkBuildInfo() in  CAAPMPerformanceComparator: "
								+ ex.getMessage());
			}
		}
	}

	private void setBenchmarkBuildInfo(Run<?, ?> run, int currentBuildNumber, int previousSuccessfulBuildNumber,
			Map<String, BuildInfo> appToBenchmarkBuildInfo, TaskListener taskListener) throws AbortException {
		for (Map.Entry<String, BuildInfo> entry : appToBenchmarkBuildInfo.entrySet()) {
			int benchmarkBuildNumber = entry.getValue().getNumber();
			BuildInfo benchmarkBuildInfo = entry.getValue();
			if (benchmarkBuildNumber == 0) {
				if (previousSuccessfulBuildNumber > 0) {
					entry.getValue().setNumber(previousSuccessfulBuildNumber);
					benchmarkBuildNumber = entry.getValue().getNumber();
				}
			} else if (benchmarkBuildNumber >= currentBuildNumber || previousSuccessfulBuildNumber == 0) {
				JenkinsPlugInLogger.log(Level.INFO,
						"There is no valid benchmarkbuild or previous successful build, hence no comparison will happen. ");
				taskListener.getLogger().println(
						"There is no valid benchmarkbuild or previous successful build, hence no comparison will happen ");
				throw new AbortException(
						"There is no valid benchmarkbuild or previous successful build, hence no comparison will happen ");
			}
			if (benchmarkBuildNumber < currentBuildNumber) {
				setBenchmarkBuildInfo(run, currentBuildNumber, benchmarkBuildInfo, taskListener);
			}

			if (benchmarkBuildInfo.getStartTime() == 0 && benchmarkBuildInfo.getEndTime() == 0) {
				JenkinsPlugInLogger.log(Level.INFO,
						"There is no test time durations for benchmark build, hence no comparison will happen. ");
				taskListener.getLogger().println(
						"There is no test time durations for benchmark build, hence no comparison will happen. ");
			}

		}

	}

	private void setBuildParams(Run<?, ?> run) throws IOException {
		InputStream in = run.getLogInputStream();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String str = null;
			String gitCommitMessage = null;
			while ((str = br.readLine()) != null) {
				if (str.contains("Commit message")) {
					gitCommitMessage = str.substring(str.indexOf('"') + 1, str.lastIndexOf('"'));
					JenkinsPlugInLogger.printLogOnConsole(3, "...commit message..." + gitCommitMessage);
				}

			}
			if (gitCommitMessage != null) {
				attribsMap.put("GIT_COMMIT_MESSAGE", gitCommitMessage);
			}

			addOrReplaceParamValue(run, LODGENSTARTTIME, loadGeneratorStartTime);
			addOrReplaceParamValue(run, LOADGENENDTIME, loadGeneratorEndTime);
			if (!attribsMap.isEmpty()) {
				for (Map.Entry<String, String> scmRepoEntry : attribsMap.entrySet()) {
					addOrReplaceParamValue(run, scmRepoEntry.getKey(), scmRepoEntry.getValue());
				}
			}
		}
	}

	private BuildInfo readAttribsMap(Run<?, ?> run, BuildInfo currentBuildInfo, TaskListener taskListener) {
		String currentBuildStartTime;
		String currentBuildEndTime;
		if (run != null) {
			currentBuildInfo.setNumber(run.getNumber());
			ParametersAction paramAction = run.getAction(ParametersAction.class);
			currentBuildStartTime = getParamValue(paramAction, LODGENSTARTTIME);
			currentBuildEndTime = getParamValue(paramAction, LOADGENENDTIME);
			if (!attribsMap.isEmpty()) {
				for (Map.Entry<String, String> scmRepoEntry : attribsMap.entrySet()) {
					currentBuildInfo.addToSCMRepoParams(scmRepoEntry.getKey(),
							getParamValue(paramAction, scmRepoEntry.getKey()));
				}
			}

			currentBuildInfo.setStartTime(Long.parseLong(currentBuildStartTime));
			currentBuildInfo.setEndTime(Long.parseLong(currentBuildEndTime));
			taskListener.getLogger().println("currentBuildNumber = " + run.getNumber() + " currentBuildStartTime = "
					+ currentBuildStartTime + ", currentBuildEndTime = " + currentBuildEndTime);
			if (!currentBuildInfo.getSCMRepoParams().isEmpty()) {
				taskListener.getLogger().println(CURRENTBUILDSCMPARAMS + currentBuildInfo.getSCMRepoParams());
			}

			JenkinsPlugInLogger.log(Level.INFO, "currentBuildNumber = " + run.getNumber() + " currentBuildStartTime = "
					+ currentBuildStartTime + ", currentBuildEndTime = " + currentBuildEndTime);

			if (!currentBuildInfo.getSCMRepoParams().isEmpty()) {
				JenkinsPlugInLogger.log(Level.INFO, CURRENTBUILDSCMPARAMS + currentBuildInfo.getSCMRepoParams());
			}

		}
		return currentBuildInfo;
	}

	private int assignPreviousSuccessfulBuild(int previousSuccessfulBuildNumber, Run<?, ?> run) {
		try {
			if (run != null) {
				Run<?, ?> prevSuccessfulBuild = run.getPreviousSuccessfulBuild();
				if (prevSuccessfulBuild != null) {
					previousSuccessfulBuildNumber = prevSuccessfulBuild.getNumber();
				} else {
					previousSuccessfulBuildNumber = 0;
				}
			}
		} catch (Exception ex) {
			JenkinsPlugInLogger
					.severe("Error during the execution of assignPreviousSuccessfulBuild() in  CAAPMPerformanceComparator: "
							+ ex.getMessage());
		}
		return previousSuccessfulBuildNumber;
	}

	private void setHistogramBuildInfoSetStatus(BuildInfo histogramBuildInfo, Run<?, ?> run) {
		try {
			Result runResult = run.getResult();
			if (runResult != null) {
				if (runResult.toString().contains(SUCCESS)) {
					histogramBuildInfo.setStatus(SUCCESS);
				} else if (runResult.toString().contains(FAILURE)) {
					histogramBuildInfo.setStatus(FAILURE);
				}
			}

		} catch (Exception ex) {
			JenkinsPlugInLogger
					.severe("Error during the execution of setHistogramBuildInfoSetStatus() in  CAAPMPerformanceComparator: "
							+ ex.getMessage());
		}
	}

	private void setHistogramBuildInfo(Run<?, ?> run, BuildInfo histogramBuildInfo,
			List<BuildInfo> histogramBuildInfoList) {
		String histogramBuildStartTime;
		String histogramBuildEndTime;
		if (run != null) {

			ParametersAction paramAction = run.getAction(ParametersAction.class);
			if (paramAction != null) {
				histogramBuildStartTime = getParamValue(paramAction, LODGENSTARTTIME);
				histogramBuildEndTime = getParamValue(paramAction, LOADGENENDTIME);
				histogramBuildInfo.setStartTime(Long.parseLong(histogramBuildStartTime));
				histogramBuildInfo.setEndTime(Long.parseLong(histogramBuildEndTime));
			}
			setHistogramBuildInfoSetStatus(histogramBuildInfo, run);
			histogramBuildInfoList.add(histogramBuildInfo);
		}
	}

	@Override
	public void perform(Run<?, ?> run, FilePath filePath, Launcher launcher, TaskListener taskListener)
			throws InterruptedException, IOException {

		taskListener.getLogger().println("Attributes Map received from Jenkins" + attribsMap);
		// set logger
		JenkinsPlugInLogger.setTaskListener(taskListener);
		int currentBuildNumber = run.getNumber();
		BuildInfo currentBuildInfo = null;
		// BuildInfo benchmarkBuildInfo = null;
		BuildInfo histogramBuildInfo = null;
		String jobName = filePath.getBaseName();
		JenkinsPlugInLogger.info("jobName:" + jobName);
		String workspaceFolder = "" + filePath.getParent();
		int previousSuccessfulBuildNumber = 0;

		List<BuildInfo> histogramBuildInfoList = new ArrayList<>();
		// benchmarkBuildInfo = new BuildInfo();
		currentBuildInfo = new BuildInfo();
		setBuildParams(run);
		taskListener.getLogger().println("loading config files : ");

		loadConfiguration();

		if (currentBuildNumber == 1) {
			JenkinsPlugInLogger.log(Level.INFO, "Current build number is first build, hence no comparison will happen");
			taskListener.getLogger().println("Current build number is first build, hence no comparison will happen");
			taskListener.getLogger().println("CA-APM Jenkins Plugin execution has completed successfully");
			return;
		}
		
		if(appToBenchmarkBuildInfo != null){
			int appsCount = 0;
			for(Map.Entry<String, BuildInfo> entry : appToBenchmarkBuildInfo.entrySet()) {
				if(entry.getValue().getNumber() <=0 && run.getPreviousSuccessfulBuild() == null){
					taskListener.getLogger().println("There is no benchmark build provided and no previous successful build, hence no comparison will happen for the application "+entry.getKey());
				}else{
					appsCount++;
				}
				
			}
			if (appsCount==0)
				return;
		}

		previousSuccessfulBuildNumber = assignPreviousSuccessfulBuild(previousSuccessfulBuildNumber, run);
		readAttribsMap(run, currentBuildInfo, taskListener);
		histogramBuildInfoList.add(currentBuildInfo);
		setBenchmarkBuildInfo(run, currentBuildNumber, previousSuccessfulBuildNumber, appToBenchmarkBuildInfo,
				taskListener);

		for (int i = 1; i < this.buildsInHistogram; i++) {

			if (run != null && run.getPreviousBuild() == null) {
				break;
			}
			histogramBuildInfo = new BuildInfo();
			if (run != null) {
				run = run.getPreviousBuild();
				int buildNumber = run.number;
				histogramBuildInfo.setNumber(buildNumber);
			}

			setHistogramBuildInfo(run, histogramBuildInfo, histogramBuildInfoList);

		}
		propertiesInfo.setAppToBenchmarkBuildInfo(appToBenchmarkBuildInfo);

		boolean isRemoteExecution = filePath.isRemote();
		if (isRemoteExecution) {
			taskListener.getLogger().println("Launching in slave machine");
			Callable<StringBuilder, IOException> callable = executeComparison(currentBuildInfo,
					previousSuccessfulBuildNumber, histogramBuildInfoList, workspaceFolder, jobName, taskListener);
			VirtualChannel channel = launcher.getChannel();
			channel.call(callable);
		} else {
			taskListener.getLogger().println("Launching in master machine");
			doExecute(currentBuildInfo, previousSuccessfulBuildNumber, histogramBuildInfoList, workspaceFolder, jobName,
					taskListener);
		}
	}

	public String getParamValue(ParametersAction pAction, String paramName) {
		if (pAction != null) {
			ParameterValue parameterValue = pAction.getParameter(paramName);
			if (parameterValue != null)
				return (String) parameterValue.getValue();
		}
		return "0";
	}

	private void readBuildsInHistogram(PropertiesConfiguration properties) {
		if (properties.containsKey(Constants.BUILDSINHISTOGRAM)) {
			String nuOfBuildsInHistogram = properties.getString(Constants.BUILDSINHISTOGRAM);
			if (nuOfBuildsInHistogram == null || nuOfBuildsInHistogram.isEmpty()
					|| Integer.parseInt(nuOfBuildsInHistogram) <= 1 || Integer.parseInt(nuOfBuildsInHistogram) > 10)
				buildsInHistogram = 10;
			else
				buildsInHistogram = Integer.parseInt(nuOfBuildsInHistogram);

		} else {
			buildsInHistogram = 10;
		}
	}

	private void readBenchmarkBuildNumber(PropertiesConfiguration properties) throws BuildValidationException {
		if (appToBenchmarkBuildInfo == null)
			appToBenchmarkBuildInfo = new HashMap<String, BuildInfo>();
		BuildInfo benchmarkBuildInfo = new BuildInfo();
		if (properties.containsKey(Constants.BENCHMARKBUILDNUMBER)) {
			if (properties.getProperty(Constants.BENCHMARKBUILDNUMBER).toString() != null
					&& !properties.getProperty(Constants.BENCHMARKBUILDNUMBER).toString().isEmpty()) {
				if (Integer.parseInt(properties.getProperty(Constants.BENCHMARKBUILDNUMBER).toString()) > 0) {
					int benchmarkBuildNumber = Integer
							.parseInt(properties.getProperty(Constants.BENCHMARKBUILDNUMBER).toString());

					benchmarkBuildInfo.setNumber(benchmarkBuildNumber);
					
				}
			} else if (properties.getProperty(Constants.BENCHMARKBUILDNUMBER).toString() != null && (properties
					.getProperty(Constants.BENCHMARKBUILDNUMBER).toString().isEmpty()
					|| Integer.parseInt(properties.getProperty(Constants.BENCHMARKBUILDNUMBER).toString()) <= 0)) {
				benchmarkBuildInfo.setNumber(0);
			}

		} else if (properties.getString(Constants.APPLICATIONNAME) != null
				&& !properties.getString(Constants.APPLICATIONNAME).isEmpty()) {
			benchmarkBuildInfo.setNumber(0);
			JenkinsPlugInLogger.log(Level.INFO, "Please provide valid benchmark build number ");
			// throw new BuildValidationException("Please provide valid
			// benchmark build number ");

		}
		appToBenchmarkBuildInfo.put(properties.getString(Constants.APPLICATIONNAME), benchmarkBuildInfo);
	}

	private void loadFile(File file) throws AbortException {
		try {
			JenkinsPlugInLogger.printLogOnConsole(1, "Loading config file  " + file.getPath());
			InputStream input = new FileInputStream(file.toString());
			PropertiesConfiguration properties = new PropertiesConfiguration();
			properties.load(input);
			readConfigFiles(properties, file.toString());
		} catch (FileNotFoundException | ConfigurationException | AbortException e) {
			throw new AbortException(e.getMessage());
		}
	}

	private void getFiles(File configFolder) throws AbortException {

		if (configFolder.listFiles() != null) {

			for (File file : configFolder.listFiles()) {
				if (!file.isDirectory()) {
					loadFile(file);
					break;
				}
			}
			for (File file : configFolder.listFiles()) {
				if (file.isDirectory()) {
					for (File appfile : file.listFiles()) {
						loadFile(appfile);
					}
				}
			}

		} else {
			JenkinsPlugInLogger.severe("There are no configuration files provided ");
			return;
		}
	}

	private void loadConfiguration() throws AbortException {

		File configFolder = new File(configFilesPath);
		propertiesInfo = new PropertiesInfo();
		getFiles(configFolder);

	}

	private void readConfigFiles(PropertiesConfiguration properties, String performanceComparatorProperties)
			throws AbortException {
		try {
			readBuildsInHistogram(properties);
			readBenchmarkBuildNumber(properties);
			readCommonProperties(properties);
			readDOIProperties(properties);
			readStrategiesConfiguration(properties);
			readEmailProperties(properties, performanceComparatorProperties);
		} catch (BuildValidationException e) {
			JenkinsPlugInLogger.severe("The configuration file is not found or configuration error ", e);
			// fail the build if configuration error
			throw new AbortException(e.getMessage());
		}

	}

	private void readCommonProperties(PropertiesConfiguration properties) {

		// APMConnectionInfo
		if (properties.getString(Constants.EMURL) != null) {
			propertiesInfo.setEmURL(properties.getString(Constants.EMURL));
		}

		if (properties.getString(Constants.EMAUTHTOKEN) != null) {
			propertiesInfo.setEmAuthToken(properties.getString(Constants.EMAUTHTOKEN));
		}

		if (properties.getString(Constants.EMTIMEZONE) != null) {
			propertiesInfo.setEmTimeZone(properties.getString(Constants.EMTIMEZONE));
		}

		propertiesInfo.addToCommonProperties(Constants.LOADGENERATORNAME, loadGeneratorName);
		if (properties.getString(Constants.LOGGINGLEVEL) != null) {
			propertiesInfo.addToCommonProperties(Constants.LOGGINGLEVEL, properties.getString(Constants.LOGGINGLEVEL));
		}
		if (properties.getString(Constants.EXTENSIONSDIRECTORY) != null) {
			propertiesInfo.addToCommonProperties(Constants.EXTENSIONSDIRECTORY,
					properties.getString(Constants.EXTENSIONSDIRECTORY));
		}

		if (properties.getString(Constants.METRICCLAMP) != null) {
			propertiesInfo.addToCommonProperties(Constants.METRICCLAMP, properties.getString(Constants.METRICCLAMP));
		}
		if (properties.getString(Constants.BUILDPASSORFAIL) != null) {
			if (properties.getString(Constants.BUILDPASSORFAIL).equals("true")) {
				propertiesInfo.addToCommonProperties(Constants.BUILDPASSORFAIL, "true");
			} else if (properties.getString(Constants.BUILDPASSORFAIL) != null && (properties.getString(Constants.BUILDPASSORFAIL).equals("false"))) {
				if (propertiesInfo.getCommonPropertyValue(Constants.BUILDPASSORFAIL) == null)
					propertiesInfo.addToCommonProperties(Constants.BUILDPASSORFAIL, "false");
			}
		}

		if (properties.getString(Constants.ISPUBLISHBUILDRESULTTOEM) != null
				&& properties.getString(Constants.ISPUBLISHBUILDRESULTTOEM).equals("true")) {
			propertiesInfo.addAppsToPublishBuildResultToEM(properties.getString(Constants.APPLICATIONNAME));
		}
		if (properties.getString(Constants.ISBUILDCHANGEEVENTTODOI) != null) {
			propertiesInfo.addToCommonProperties(Constants.ISBUILDCHANGEEVENTTODOI,
					properties.getString(Constants.ISBUILDCHANGEEVENTTODOI));
		}
		if (properties.getString(Constants.ISBUILDCHANGEEVENTTODOI) != null && !properties.getString(Constants.ISBUILDCHANGEEVENTTODOI).isEmpty()
				&& properties.getString(Constants.ISBUILDCHANGEEVENTTODOI).equals("true")) {
			if (properties.getString(Constants.APPLICATIONHOST) == null
					|| properties.getString(Constants.APPLICATIONHOST).isEmpty()) {
				JenkinsPlugInLogger.printLogOnConsole(1,
						"Please provide valid host name to publish build change event to DOI for the application "+properties.getString(Constants.APPLICATIONNAME));
				JenkinsPlugInLogger.severe("Please provide valid host name to publish build change event to DOI "+properties.getString(Constants.APPLICATIONNAME));
			} else {
				propertiesInfo.addDOIAppsToHostname(properties.getString(Constants.APPLICATIONNAME),
						properties.getString(Constants.APPLICATIONHOST));
			}
		}
		if (properties.getString(Constants.EMWEBVIEWPORT) != null) {
			propertiesInfo.addToCommonProperties(Constants.EMWEBVIEWPORT,
					properties.getString(Constants.EMWEBVIEWPORT));
		}

	}

	private void readDOIProperties(PropertiesConfiguration properties) {
		if (properties.getString(Constants.DOITIMEZONE) != null) {
			propertiesInfo.addToDoiProperties(Constants.DOITIMEZONE, properties.getString(Constants.DOITIMEZONE));
		}
		if (properties.getString(Constants.DOITENANTID) != null) {
			propertiesInfo.addToDoiProperties(Constants.DOITENANTID, properties.getString(Constants.DOITENANTID));
		}
		if (properties.getString(Constants.JARVISENDPOINT) != null) {
			propertiesInfo.addToDoiProperties(Constants.JARVISENDPOINT, properties.getString(Constants.JARVISENDPOINT));
		}
	}

	private void readStrategiesConfiguration(PropertiesConfiguration properties) {
		try {

			readComparisonStrategiesInformation(properties);
			readOutputHandlerStrategiesInformation(properties);

		} catch (NoSuchElementException ex) {

			JenkinsPlugInLogger.severe("Required property not found ", ex);
			JenkinsPlugInLogger.printLogOnConsole(2, "Missing strategies property, please check logs for more details");
		}

	}

	private void readComparisonStrategiesInformation(PropertiesConfiguration properties) {

		if (properties.getStringArray(Constants.COMPARISONSTRATEGIESLIST) != null) {
			String[] comparisonStrategies = properties.getStringArray(Constants.COMPARISONSTRATEGIESLIST);

			StrategyConfiguration strategyConfiguration;
			String applicationName = properties.getString(Constants.APPLICATIONNAME);
			for (String comparisonStrategy : comparisonStrategies) {
				Iterator<String> strategyKeys = properties.getKeys(comparisonStrategy);
				strategyConfiguration = new StrategyConfiguration();
				String appNameCompStrategy = applicationName + "." + comparisonStrategy;
				strategyConfiguration.addProperty("name", appNameCompStrategy);
				while (strategyKeys.hasNext()) {
					String key = strategyKeys.next();
					if (key.endsWith("." + Constants.OUTPUTHANDLERS)) {
						String[] outputHandlers = properties.getStringArray(key);
						addToOutputHandlerToComparisonStrategies(outputHandlers, appNameCompStrategy);
					} else if (key.endsWith(Constants.AGENTSPECIFIER)) {
						// It is mandatory field
						strategyConfiguration.setAgentSpecifiers(Arrays.asList(properties.getStringArray(key)));
					} else {
						strategyConfiguration.addProperty(applicationName + "." + key, properties.getString(key));
					}
				}

				propertiesInfo.addStrategyConfigProperty(appNameCompStrategy, strategyConfiguration);

			}
		}
	}

	private void addToOutputHandlerToComparisonStrategies(String[] outputHandlers, String comparisonStrategy) {
		for (String outputHandler : outputHandlers) {
			if (outputHandler.isEmpty()) {
				propertiesInfo.addToNonMappedComparisonStrategies(comparisonStrategy);
				continue;
			}
			propertiesInfo.addOutputHandlerToComparisonStrategies(outputHandler, comparisonStrategy);
		}
	}

	private void readOutputHandlerStrategiesInformation(PropertiesConfiguration properties) {

		if (properties.getStringArray(Constants.OUTPUTHANDLERSLIST) != null) {
			String[] outputStrategies = properties.getStringArray(Constants.OUTPUTHANDLERSLIST);
			OutputHandlerConfiguration outputHandlerInfo;
			if (outputStrategies.length == 1 && outputStrategies[0].length() == 0) {
				JenkinsPlugInLogger.severe("No Output Handler Defined in the configuration");
				return;
			}
			for (String outputHandler : outputStrategies) {
				outputHandlerInfo = new OutputHandlerConfiguration();
				Iterator<String> strategyKeys = properties.getKeys(outputHandler);
				while (strategyKeys.hasNext()) {
					String key = strategyKeys.next();
					outputHandlerInfo.addProperty(key, properties.getString(key));
				}
				outputHandlerInfo.addProperty("name", outputHandler);
				propertiesInfo.addOutputHandlerConfig(outputHandler, outputHandlerInfo);

			}
		}
	}

	private void readEmailProperties(PropertiesConfiguration properties, String performanceComparatorProperties) {
		if (properties.getKeys("email") != null) {
			Iterator<String> keys = properties.getKeys("email");
			while (keys.hasNext()) {
				String key = keys.next();
				String value = properties.getString(key);
				if (key.equals("email.recepients.to")) {
					if (properties.getString(Constants.APPLICATIONNAME) != null
							&& !properties.getString(Constants.APPLICATIONNAME).isEmpty())
						propertiesInfo.addAppNameToRecipients(properties.getString(Constants.APPLICATIONNAME), value);
					else
						propertiesInfo.addAppNameToRecipients("default", value);
					continue;
				}
				if (key.equals("email.password")) {
					value = EmailHelper.passwordEncrytion(properties, performanceComparatorProperties, key, value);
				}
				propertiesInfo.addToEmailProperties(key, value);

			}
		}
	}

	@Extension
	@Symbol("caapmplugin")
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		private String loadGeneratorStartTime;
		private String loadGeneratorEndTime;
		private String loadGeneratorName;
		private String paramsMap;

		@Override
		public CAAPMPerformanceComparator newInstance(StaplerRequest req, JSONObject formData) throws FormException {

			try {
				String performanceComparatorProperties = formData.getString("performanceComparatorProperties");
				CAAPMPerformanceComparator caAPMPublisher = new CAAPMPerformanceComparator(
						performanceComparatorProperties, this.loadGeneratorStartTime, this.loadGeneratorEndTime,
						this.loadGeneratorName, this.paramsMap);
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

		@Override
		public String getDisplayName() {
			return "Jenkins Plugin for CA APM";
		}
	}
}
