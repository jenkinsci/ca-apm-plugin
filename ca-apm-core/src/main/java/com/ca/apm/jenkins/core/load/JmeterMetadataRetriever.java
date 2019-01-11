package com.ca.apm.jenkins.core.load;

import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.entity.LoadRunnerMetadata;
import com.ca.apm.jenkins.core.load.reader.JmeterCSVReader;
import com.ca.apm.jenkins.core.load.reader.JmeterXMLReader;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import java.io.File;

/**
 * This class is to fetch jmeter run metadata when a file is provided at jmeter's workspace job
 * directory.
 */
public class JmeterMetadataRetriever implements LoadRunnerMetadataRetriever {

  String loadRunnerName = null;
  String jmeterCurrentRunOutputFile = null;
  String jmeterBenchmarkRunOutputFile = null;
  JenkinsInfo jenkinsInfo = null;
  long cStartTime, cEndTime, bStartTime, bEndTime = 0;
  long[] values;
  private LoadRunnerMetadata loadRunnerMetadata = null;

  public JmeterMetadataRetriever(LoadRunnerMetadata loadRunnerMetadata)
      throws BuildComparatorException {
    setLoadRunnerMetadata(loadRunnerMetadata);
  }

  public LoadRunnerMetadata getLoadRunnerMetadata() {
    return loadRunnerMetadata;
  }

  public void setLoadRunnerMetadata(LoadRunnerMetadata loadRunnerMetadata) {
    this.loadRunnerMetadata = loadRunnerMetadata;
  }

  /**
   * This method is used to fetch jmeter metadata when the output file of jmeter is provided for the
   * current run and benchmark run as well.
   */
  public void fetchExtraMetadata() throws BuildValidationException {

    this.loadRunnerName = loadRunnerMetadata.getLoadRunnerPropertyValue("loadgenerator.name");

    this.jmeterCurrentRunOutputFile =
        loadRunnerMetadata.getLoadRunnerPropertyValue(loadRunnerName + ".currentrunoutputfile");

    this.jmeterBenchmarkRunOutputFile =
        loadRunnerMetadata.getLoadRunnerPropertyValue(loadRunnerName + ".benchmarkrunoutputfile");

    this.jenkinsInfo = loadRunnerMetadata.getJenkinsInfo();

    readLoadGenFile();
  }

  /** This method is used to read jmeter output file . Throws BuildValidationException */
  private void readLoadGenFile() throws BuildValidationException {
    String fileType = null;
    fileType = loadRunnerMetadata.getLoadRunnerPropertyValue(loadRunnerName + ".filetype");
    JenkinsPlugInLogger.fine(
        "Current Build Info while loading " + loadRunnerMetadata.getCurrentBuildInfo().toString());
    JenkinsPlugInLogger.fine(
        "BenchMark Build Info while loading "
            + loadRunnerMetadata.getBenchMarkBuildInfo().toString());
    if (fileType.equalsIgnoreCase("csv")) {
      JmeterCSVReader jmeterCSVMetadataReader = new JmeterCSVReader();

      // Reading current build's jmeter csv output file

      jmeterCurrentRunOutputFile =
          jenkinsInfo.getBuildWorkSpaceFolder()
              + File.separator
              + jenkinsInfo.getJobName()
              + File.separator
              + jenkinsInfo.getCurrentBuildNumber()
              + File.separator
              + jmeterCurrentRunOutputFile;

      values = new long[2];

      values = jmeterCSVMetadataReader.getBuildTSFromOutputFile(jmeterCurrentRunOutputFile);
      cStartTime = values[0];
      cEndTime = values[1];
      loadRunnerMetadata.setCurrentBuildTimes(cStartTime, cEndTime);

      // Reading benchmark build's jmeter csv output file

      jmeterBenchmarkRunOutputFile =
          jenkinsInfo.getBuildWorkSpaceFolder()
              + File.separator
              + jenkinsInfo.getJobName()
              + File.separator
              + loadRunnerMetadata.getBenchMarkBuildNumber()
              + File.separator
              + jmeterBenchmarkRunOutputFile;

      values = new long[2];

      values = jmeterCSVMetadataReader.getBuildTSFromOutputFile(jmeterBenchmarkRunOutputFile);
      bStartTime = values[0];
      bEndTime = values[1];
      loadRunnerMetadata.setBenchMarBuildTimes(bStartTime, bEndTime);
    } else if (fileType.equalsIgnoreCase("xml")) {
      JmeterXMLReader jmeterXMLMetadataRetriever = new JmeterXMLReader();

      // Reading current build's jmeter xml output file

      jmeterCurrentRunOutputFile =
          jenkinsInfo.getBuildWorkSpaceFolder()
              + File.separator
              + jenkinsInfo.getJobName()
              + File.separator
              + jenkinsInfo.getCurrentBuildNumber()
              + File.separator
              + jmeterCurrentRunOutputFile;

      values = new long[2];

      values = jmeterXMLMetadataRetriever.getBuildTSFromOutputFile(jmeterCurrentRunOutputFile);
      cStartTime = values[0];
      cEndTime = values[1];
      loadRunnerMetadata.setCurrentBuildTimes(cStartTime, cEndTime);

      // Reading benchmark build's jmeter csv output file

      jmeterBenchmarkRunOutputFile =
          jenkinsInfo.getBuildWorkSpaceFolder()
              + File.separator
              + jenkinsInfo.getJobName()
              + File.separator
              + loadRunnerMetadata.getBenchMarkBuildNumber()
              + File.separator
              + jmeterBenchmarkRunOutputFile;

      values = new long[2];

      values = jmeterXMLMetadataRetriever.getBuildTSFromOutputFile(jmeterBenchmarkRunOutputFile);
      bStartTime = values[0];
      bEndTime = values[1];
      loadRunnerMetadata.setBenchMarBuildTimes(bStartTime, bEndTime);
    } else {
      throw new BuildValidationException("jmeter.filetype value should be either csv or xml ");
    }
  }
}
