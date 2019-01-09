package com.ca.apm.jenkins.performance_comparator_jenkinsplugin;

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
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/** @author Avinash Chandwani */
public class CAAPMPerformanceComparator extends Recorder implements SimpleBuildStep, Serializable {

  /** */
  private static final long serialVersionUID = -440923159278868167L;

  private String performanceComparatorProperties;
  private int buildsInHistogram;

  @DataBoundConstructor
  public CAAPMPerformanceComparator(String performanceComparatorProperties) {
    this.performanceComparatorProperties = performanceComparatorProperties;
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

  private boolean runAction(
      int currentBuildNumber,
      int previousSuccessfulBuild,
      List<String> histogramBuilds,
      String workspaceFolder,
      String jobName,
      TaskListener taskListener)
      throws BuildComparatorException, BuildValidationException, BuildExecutionException {
    boolean comparisonRunStatus = false;
    JenkinsInfo jenkinsInfo =
        new JenkinsInfo(
            currentBuildNumber, previousSuccessfulBuild, histogramBuilds, workspaceFolder, jobName);
    ComparisonRunner runner =
        new ComparisonRunner(jenkinsInfo, this.performanceComparatorProperties,taskListener);
    comparisonRunStatus = runner.executeComparison();
    return comparisonRunStatus;
  }

  private Callable<StringBuilder, IOException> executeComparison(
      final int currentBuildNumber,
      final int previousSuccessfulBuildNumber,
      final List<String> histogramBuilds,
      final String workspaceFolder,
      final String jobName,
      final TaskListener taskListener)
      throws IOException, InterruptedException {
    return new Callable<StringBuilder, IOException>() {

      /** */
      private static final long serialVersionUID = 1L;

      StringBuilder consoleLogString = new StringBuilder();

      public StringBuilder call() throws IOException {
        consoleLogString =
            doExecute(
                currentBuildNumber,
                previousSuccessfulBuildNumber,
                histogramBuilds,
                workspaceFolder,
                jobName,
                taskListener);
        return consoleLogString;
      }

      @Override
      public void checkRoles(RoleChecker arg0) throws SecurityException {}
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

  private StringBuilder doExecute(
      int currentBuildNumber,
      int previousSuccessfulBuildNumber,
      List<String> histogramBuilds,
      String workspaceFolder,
      String jobName,
      TaskListener taskListener)
      throws AbortException {
    boolean isSuccessful = false;
    StringBuilder consoleLogString = JenkinsPlugInLogger.getConsoleLogString();
    try {
      isSuccessful =
          runAction(
              currentBuildNumber,
              previousSuccessfulBuildNumber,
              histogramBuilds,
              workspaceFolder,
              jobName,
              taskListener);
    } catch (BuildComparatorException e) {
      consoleLogString.append("Plugin Task failed due to :" + e.getMessage());
      clearAndStopLogging(taskListener, consoleLogString);
      throw new AbortException(
          "*******Performance Comparison Failed*******due to" + Constants.NewLine + e.getMessage());
    } catch (BuildValidationException ex) {
      consoleLogString.append("Plugin Task failed due to :" + ex.getMessage());
      clearAndStopLogging(taskListener, consoleLogString);
      throw new AbortException(
          "*******Performance Comparison Failed******* due to"
              + Constants.NewLine
              + ex.getMessage());
    } catch (BuildExecutionException ex) {
      consoleLogString.append("Plugin Task failed due to :" + ex.getMessage());
      clearAndStopLogging(taskListener, consoleLogString);
      throw new AbortException(
          "*******Performance Comparison Failed******* due to"
              + Constants.NewLine
              + ex.getMessage());
    }
    if (isSuccessful) {
      JenkinsPlugInLogger.printLogOnConsole(
          0, "CA-APM Jenkins Plugin execution has completed successfully");
    } else {
      consoleLogString.append("Plugin Task is not completed");
      clearAndStopLogging(taskListener, consoleLogString);
      throw new AbortException(
          "*******Performance Comparison Failed******* due to performance crossed the threshold mark, please review results for more details");
    }
    return consoleLogString;
  }

  private void clearAndStopLogging(TaskListener taskListener, StringBuilder output) {
    taskListener.getLogger().println(output.toString());
    output.setLength(0);
    JenkinsPlugInLogger.closeLogger();
  }

  @Override
  public void perform(
      Run<?, ?> run, FilePath filePath, Launcher launcher, TaskListener taskListener)
      throws InterruptedException, IOException {
    int currentBuilderNumber = run.number;

    String jobName = filePath.getBaseName();
    JenkinsPlugInLogger.info("jobName:" + jobName);
    JenkinsPlugInLogger.info("run.getDisplayName:" + run.getDisplayName());
    JenkinsPlugInLogger.info("run.getFullDisplayName:" + run.getFullDisplayName());
    File rootDir = run.getRootDir();
    FilePath jobPath = new FilePath(new File(rootDir.getParentFile().getParent()));
    JenkinsPlugInLogger.info("rootDir.getParent():" + rootDir.getParentFile().getParent());
    JenkinsPlugInLogger.info("jobPath:" + jobPath + Constants.NewLine);
    JenkinsPlugInLogger.info("jobPath.getRemote():" + jobPath.getRemote());
    JenkinsPlugInLogger.info("filePath.getRemote():" + filePath.getRemote());
    FilePath sourceFilePath = new FilePath(new File(filePath.getRemote()));
    JenkinsPlugInLogger.info("sourceFilePath.getName():" + sourceFilePath.getName());
    String workspaceFolder = "" + filePath.getParent();
    int previousSuccessfulBuildNumber = 0;

    List<String> histogramBuilds = new ArrayList<String>();

    if (run.getPreviousSuccessfulBuild() == null) {
      previousSuccessfulBuildNumber = 0;
    } else {
      previousSuccessfulBuildNumber = run.getPreviousSuccessfulBuild().getNumber();
    }
    histogramBuilds.add(String.valueOf(currentBuilderNumber));
    taskListener.getLogger().println("loading config file : " + this.performanceComparatorProperties);
    try {
      loadConfiguration();
    } catch (ConfigurationException | IOException e) {
      JenkinsPlugInLogger.severe("The configuration file is not found or configuration error ", e);
      // fail the build if configuration error
      throw new AbortException(e.getMessage());
    }
    /*for (int i = 1; i < buildsInHistogram; i++) {
      if (run.getPreviousBuild() != null) {
        run = run.getPreviousBuild();
        histogramBuilds.add(String.valueOf(run.number));
        JenkinsPlugInLogger.info("Histogram build ids.........." + histogramBuilds.get(i));
      } else break;
    }*/
    boolean isRemoteExecution = filePath.isRemote();
    StringBuilder output = null;
    if (isRemoteExecution) {
      taskListener.getLogger().println("Launching in slave machine");
      Callable<StringBuilder, IOException> callable =
          executeComparison(
              currentBuilderNumber,
              previousSuccessfulBuildNumber,
              histogramBuilds,
              workspaceFolder,
              jobName,
              taskListener);
      VirtualChannel channel = launcher.getChannel();
      output = channel.call(callable);
    } else {
      taskListener.getLogger().println("Launching in master machine");
      output =
          doExecute(
              currentBuilderNumber,
              previousSuccessfulBuildNumber,
              histogramBuilds,
              workspaceFolder,
              jobName,
              taskListener);
    }
    clearAndStopLogging(taskListener, output);
  }

  private void loadConfiguration() throws ConfigurationException, IOException {

    PropertiesConfiguration properties = new PropertiesConfiguration();
    InputStream input;
    input = new FileInputStream(this.performanceComparatorProperties);
    properties.load(input);
    if (properties.containsKey(Constants.buildsInHistogram)) {
      String nuOfBuildsForHistogram = properties.getString(Constants.buildsInHistogram);
      if (nuOfBuildsForHistogram == null
          || nuOfBuildsForHistogram.isEmpty()
          || Integer.parseInt(nuOfBuildsForHistogram) <= 1
          || Integer.parseInt(nuOfBuildsForHistogram) > 10) buildsInHistogram = 10;
      else buildsInHistogram = Integer.parseInt(nuOfBuildsForHistogram);

    } else {
      buildsInHistogram = 10;
    }
  }

  @Extension
  @Symbol("caapmplugin")
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private String performanceComparatorProperties;

    @Override
    public CAAPMPerformanceComparator newInstance(StaplerRequest req, JSONObject formData)
        throws FormException {

      try {
        this.performanceComparatorProperties =
            formData.getString("performanceComparatorProperties");
        CAAPMPerformanceComparator caAPMPublisher =
            new CAAPMPerformanceComparator(this.performanceComparatorProperties);
        save();
        return caAPMPublisher;
      } catch (Exception ex) {
        return null;
      }
    }

    @Override
    public boolean isApplicable(
        @SuppressWarnings("rawtypes") Class<? extends AbstractProject> arg0) {
      return true;
    }

    public String getDisplayName() {
      return "Jenkins Plugin for CA APM";
    }
  }
}
