package com.ca.apm.jenkins.performance_comparator_jenkinsplugin;

import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.executor.ComparisonRunner;
import hudson.model.TaskListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    List<String> histogramBuilds = new ArrayList<String>();
    // int nuOfHistogramBuilds = 5
    histogramBuilds.add(String.valueOf(35));
    histogramBuilds.add(String.valueOf(34));
    histogramBuilds.add(String.valueOf(33));
    TaskListener taskListener = new TaskListenerMock() ;
    JenkinsInfo jenkinsInfo =
        new JenkinsInfo(35, 29, histogramBuilds, path + "workspace", "CIGNAOne");
    ComparisonRunner runner = new ComparisonRunner(jenkinsInfo, path + File.separator + fileName,taskListener);
    runner.executeComparison();
  }
}
