package com.ca.apm.jenkins.performance_comparator_jenkinsplugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
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

  public static void main(String[] args)
      throws BuildComparatorException, BuildValidationException, BuildExecutionException {
    PluginRunSimulation runSimulation = new PluginRunSimulation();
    String path = "C:\\APM\\AutomicJenkins\\Jenkins\\single-properties-changes";
    runSimulation.runPluginSimulation(path, "performance-comparator.properties");
  }

  public void runPluginSimulation(String path, String fileName)
      throws BuildComparatorException, BuildValidationException, BuildExecutionException {
    List<BuildInfo> histogramBuilds = new ArrayList<BuildInfo>();
    BuildInfo histogramBuilInfo = new BuildInfo();
    histogramBuilInfo.setNumber(35);
    histogramBuilds.add(histogramBuilInfo);
    BuildInfo histogramBuilInfot = new BuildInfo();
    histogramBuilInfo.setNumber(34);
    histogramBuilds.add(histogramBuilInfot);
    BuildInfo histogramBuilInfon = new BuildInfo();
    histogramBuilInfo.setNumber(33);
    histogramBuilds.add(histogramBuilInfon);
    // int nuOfHistogramBuilds = 5
   
    TaskListener taskListener = new TaskListenerMock() ;
    JenkinsPlugInLogger.setTaskListener(taskListener);
    BuildInfo currentBuilInfo , benchmarkBuildInfo = null;
    currentBuilInfo= new BuildInfo();
    currentBuilInfo.setNumber(35);
    benchmarkBuildInfo= new BuildInfo();
    benchmarkBuildInfo.setNumber(29);
    
    JenkinsInfo jenkinsInfo =
        new JenkinsInfo(currentBuilInfo.getNumber(), benchmarkBuildInfo.getNumber(), histogramBuilds, path + "workspace", "CIGNAOne", "LoadGeneratorName");
    ComparisonRunner runner = new ComparisonRunner(currentBuilInfo, benchmarkBuildInfo, jenkinsInfo, path + File.separator + fileName,taskListener);
    runner.executeComparison();
  }
}
