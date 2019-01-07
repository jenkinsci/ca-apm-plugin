package com.ca.apm.jenkins.core.load.reader;

import com.ca.apm.jenkins.api.LoadGenOPFileReader;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class is to fetch jmeter run metadata when a CSV file is provided at jmeter's workspace job
 * directory.
 */
public class JmeterCSVReader implements LoadGenOPFileReader {

  private static final String cvsSplitBy = ",";
  private static final String CSV = ".csv";

  /** This method is invoked to read the provided CSV file and return the start, end time. */
  public long[] getBuildTSFromOutputFile(String jmeterOutputFile) throws BuildValidationException {

    long[] buildValues = new long[2];
    if (jmeterOutputFile.endsWith(CSV)) {
      BufferedReader br = null;
      String resultLine = "";

      try {
        br = new BufferedReader(new FileReader(jmeterOutputFile));
        long minTs = 0;
        long maxTs = 0;
        if (br.readLine() == null)
          throw new BuildValidationException("The file " + jmeterOutputFile + " is empty");
        br.readLine();
        while ((resultLine = br.readLine()) != null) {

          // use comma as separator
          String[] timeStamp = resultLine.split(cvsSplitBy);

          long ts = Long.parseLong(timeStamp[0]);
          if (ts > maxTs) maxTs = ts;
          if (minTs == 0 || ts < minTs) {
            minTs = ts;
          }
        }
        JenkinsPlugInLogger.fine("JmeterOutputReader  minTs - " + minTs + "  maxTs - " + maxTs);
        buildValues[0] = minTs;
        buildValues[1] = maxTs;

      } catch (FileNotFoundException e) {

        throw new BuildValidationException(
            "The CSV file - "
                + jmeterOutputFile
                + " is not found under jenkins workspace's jobname directory");
      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Exception while reading the Jmeter's CSV file", e);
        throw new BuildValidationException(
            "Exception while reading the Jmeter's CSV file "
                + jmeterOutputFile
                + " "
                + e.getMessage());
      } finally {
        if (br != null) {
          try {
            br.close();
          } catch (IOException e) {
            JenkinsPlugInLogger.severe("Exception while closing the bufferedReader", e);
          }
        }
      }

      return buildValues;
    } else {
      throw new BuildValidationException(jmeterOutputFile + " is not a valid csv file ");
    }
  }
}
