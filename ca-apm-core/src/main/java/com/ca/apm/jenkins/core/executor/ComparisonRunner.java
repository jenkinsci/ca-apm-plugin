package com.ca.apm.jenkins.core.executor;

import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.helper.VertexAttributesUpdateHelper;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import java.util.logging.Level;

/**
 * Main class to start the comparison strategy procedure
 *
 * @author Avinash Chandwani
 */
public class ComparisonRunner {

  private JenkinsInfo jenkinsInfo;
  private String performanceComparatorProperties;

  public ComparisonRunner() {
    super();
  }

  public ComparisonRunner(JenkinsInfo jenkinsInfo, String performanceComparatorProperties) {
    this.jenkinsInfo = jenkinsInfo;
    this.performanceComparatorProperties = performanceComparatorProperties;
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
   * @return Returns a boolean value true or false based upon success or failure of execution
   * @throws BuildComparatorException : In case of any error/exception during the execution occurs,
   *     this exception is throw with appropriate message
   * @throws BuildValidationException
   */
  public boolean executeComparison()
      throws BuildComparatorException, BuildValidationException, BuildExecutionException {
    boolean isFailToBuild = false;

    if (jenkinsInfo.getCurrentBuildNumber() == 1) {
      JenkinsPlugInLogger.log(
          Level.INFO, "Current build number is first build, hence no comparison will happen");
    } else {
      try {
        ComparisonMetadataLoader metadataLoader =
            new ComparisonMetadataLoader(jenkinsInfo, performanceComparatorProperties);
        metadataLoader.loadProperties();
        metadataLoader.validateConfigurations();
        ComparisonExecutor comparisonExecutor =
            new ComparisonExecutor(metadataLoader.getComparisonMetadata());
        OutputHandlingExecutor outputHandlingExecutor =
            new OutputHandlingExecutor(metadataLoader.getComparisonMetadata());
        comparisonExecutor.execute();

        BuildDecisionMaker decisionMaker =
            new BuildDecisionMaker(metadataLoader.getComparisonMetadata().getComparisonResult());
        if (metadataLoader.getComparisonMetadata().isFailTheBuild()) {
          isFailToBuild = decisionMaker.isFailed();
        }

        outputHandlingExecutor.execute(
            metadataLoader.getComparisonMetadata().getOutputConfiguration(), isFailToBuild);

        if (metadataLoader.getComparisonMetadata().isPublishBuildResulttoEM()) {
          VertexAttributesUpdateHelper vertexAttributesUpdateHelper =
              new VertexAttributesUpdateHelper(metadataLoader.getComparisonMetadata());
          vertexAttributesUpdateHelper.updateAttributeOfVertex(!isFailToBuild);
        }

      } catch (BuildComparatorException ex) {
        if (isFailToBuild) {
          JenkinsPlugInLogger.printLogOnConsole(
              0, "Comparator Plugin Execution Completed with failures");
          throw new BuildComparatorException(ex.getMessage());

        } else {
          JenkinsPlugInLogger.severe(
              "Error occured in comparison ->"
                  + ex.getMessage()
                  + ", but you prefer not to fail the build, hence we are not going to fail the build");
        }
        JenkinsPlugInLogger.printLogOnConsole(
            0, "Comparator Plugin Execution Completed with failures");
      } catch (BuildValidationException ex) {
        throw new BuildValidationException(ex.getMessage());
      }
    }
    return !isFailToBuild;
  }
}
