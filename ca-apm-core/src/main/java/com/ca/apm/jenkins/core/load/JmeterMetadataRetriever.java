package com.ca.apm.jenkins.core.load;

import java.io.File;
import java.util.List;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.JenkinsInfo;
import com.ca.apm.jenkins.core.entity.LoadRunnerMetadata;
import com.ca.apm.jenkins.core.load.reader.JmeterCSVReader;
import com.ca.apm.jenkins.core.load.reader.JmeterXMLReader;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;

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
  JmeterCSVReader jmeterCSVMetadataReader = new JmeterCSVReader();
  JmeterXMLReader jmeterXMLMetadataRetriever = new JmeterXMLReader();
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
    List<BuildInfo> histogramBuildInfoList = this.loadRunnerMetadata.getJenkinsInfo().getHistogramBuildInfoList();
    int buildsInHistogram = histogramBuildInfoList.size();
    for (int i = 0; i < buildsInHistogram; i++)
    {
      int buildNumber = ((BuildInfo)histogramBuildInfoList.get(i)).getNumber();
      String filePath = this.jenkinsInfo.getBuildWorkSpaceFolder() + File.separator + this.jenkinsInfo.getJobName() + File.separator + buildNumber + File.separator + "jmeterOutput";
      if ((new File(filePath + ".csv").exists()) && (!new File(filePath + ".csv").isHidden()))
      {
        values = new long[2];
        jmeterCurrentRunOutputFile = (filePath + ".csv");
        values = jmeterCSVMetadataReader.getBuildTSFromOutputFile(this.jmeterCurrentRunOutputFile);
      }
      else if ((new File(filePath + ".xml").exists()) && (!new File(filePath + ".xml").isHidden()))
      {
        values = new long[2];
        jmeterCurrentRunOutputFile = (filePath + ".xml");
        values = jmeterXMLMetadataRetriever.getBuildTSFromOutputFile(this.jmeterCurrentRunOutputFile);
      }
      else
      {
        JenkinsPlugInLogger.info("jmeterOutput file of type csv or xml is not found in buildnumber " + buildNumber + " directory");
        JenkinsPlugInLogger.printLogOnConsole(2, "jmeterOutput file of type csv or xml is not found in buildnumber " + buildNumber + " directory");
        continue;
      }
      histogramBuildInfoList.get(i).setStartTime(this.values[0]);
      histogramBuildInfoList.get(i).setEndTime(this.values[1]);
    }
    this.loadRunnerMetadata.setHistogramBuildInfoList(histogramBuildInfoList);

    if (fileType.equalsIgnoreCase("csv")) {
     
      // Reading current build's jmeter csv output file

      jmeterCurrentRunOutputFile =
          jenkinsInfo.getBuildWorkSpaceFolder()
              + File.separator
              + jenkinsInfo.getJobName()
              + File.separator
              + jenkinsInfo.getCurrentBuildNumber()
              + File.separator
              + Constants.jmeterOutputFileName
              +"."+fileType.toLowerCase();
             
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
              + Constants.jmeterOutputFileName
              +"."+fileType.toLowerCase();
            
      values = new long[2];

      values = jmeterCSVMetadataReader.getBuildTSFromOutputFile(jmeterBenchmarkRunOutputFile);
      bStartTime = values[0];
      bEndTime = values[1];
      loadRunnerMetadata.setBenchMarBuildTimes(bStartTime, bEndTime);
    } else if (fileType.equalsIgnoreCase("xml")) {
    
      // Reading current build's jmeter xml output file

      jmeterCurrentRunOutputFile =
          jenkinsInfo.getBuildWorkSpaceFolder()
              + File.separator
              + jenkinsInfo.getJobName()
              + File.separator
              + jenkinsInfo.getCurrentBuildNumber()
              + File.separator
              + Constants.jmeterOutputFileName
              +"."+fileType.toLowerCase();
            
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
              + Constants.jmeterOutputFileName
              +"."+fileType.toLowerCase();
             
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
