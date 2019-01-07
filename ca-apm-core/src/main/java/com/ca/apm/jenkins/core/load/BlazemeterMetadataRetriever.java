package com.ca.apm.jenkins.core.load;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.core.entity.LoadRunnerMetadata;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import okhttp3.Credentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation of LoadRunnerMetadataRetreiver specifically for Blazemeter Load Runner Blazemeter
 * provides a rich set of APIs from which metadata about a load-test can be fetched. In case you are
 * using Blazemeter as a load-runner tool, you can use configure this class as
 * metadatareaderclassname. Rest will be taken care for you.
 *
 * @author Avinash Chandwani
 */
public class BlazemeterMetadataRetriever implements LoadRunnerMetadataRetriever {

  private String blazemeterRestURL;
  private String blazemeterApiKey;
  private String blazemeterApiKeySecret;
  private String testId;
  private String componentName;
  private LoadRunnerMetadata loadRunnerMetadata = null;

  public BlazemeterMetadataRetriever(LoadRunnerMetadata loadRunnerMetadata)
      throws BuildComparatorException {
    setLoadRunnerMetadata(loadRunnerMetadata);
    testId = loadRunnerMetadata.getLoadRunnerPropertyValue("blazemeter.testid");
    blazemeterRestURL = loadRunnerMetadata.getLoadRunnerPropertyValue("blazemeter.resturl");
    blazemeterApiKey = loadRunnerMetadata.getLoadRunnerPropertyValue("blazemeter.apikeyid");
    blazemeterApiKeySecret =
        loadRunnerMetadata.getLoadRunnerPropertyValue("blazemeter.apikeysecret");
  }

  public LoadRunnerMetadata getLoadRunnerMetadata() {
    return loadRunnerMetadata;
  }

  public void setLoadRunnerMetadata(LoadRunnerMetadata loadRunnerMetadata) {
    this.loadRunnerMetadata = loadRunnerMetadata;
  }

  public void fetchExtraMetadata() throws BuildComparatorException {
    init();
  }

  private void init() throws BuildComparatorException {
    JenkinsPlugInLogger.printLogOnConsole(3, "Inside init() of BlazemeterMetadataRetriever");
    JenkinsPlugInLogger.info("****Initializing BlazemeterMetadataRetriever****");
    fetchTestRunDurations();
    fetchTestName();
    fetchTestUsersAndTestFileName();
    fetchBusinessSegmentInfo();
    JenkinsPlugInLogger.info("****Initialization of BlazemeterMetadataRetriever Completed****");
  }

  private void fetchTestName() throws BuildComparatorException {
    String mastersSummaryURL = blazemeterRestURL + "/tests/" + testId;
    CloseableHttpClient client = HttpClients.createDefault();
    HttpGet httpGet = new HttpGet(mastersSummaryURL);
    httpGet.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
    httpGet.addHeader(
        Constants.AUTHORIZATION, Credentials.basic(blazemeterApiKey, blazemeterApiKeySecret));
    CloseableHttpResponse response = null;
    try {
      response = client.execute(httpGet);
    } catch (UnsupportedEncodingException e) {
      JenkinsPlugInLogger.severe("Error while reading REST Response", e);
      throw new BuildComparatorException("Error while reading Test Name->" + e.getMessage());
    } catch (ClientProtocolException e) {
      JenkinsPlugInLogger.severe("Error while reading REST Response", e);
      throw new BuildComparatorException("Error while reading Test Name->" + e.getMessage());
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error while reading REST Response", e);
      throw new BuildComparatorException("Error while reading Test Name->" + e.getMessage());
    }
    if (Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()) {
      StringBuffer result = new StringBuffer();
      try {
        BufferedReader rd =
            new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }

      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Error while reading REST Response", e);
        throw new BuildComparatorException("Error while reading Test Name->" + e.getMessage());
      }
      JSONObject testMetadataResponse = new JSONObject(result.toString());
      JSONObject resultNode = (JSONObject) testMetadataResponse.get("result");
      String loadRunnerTestName = resultNode.getString("name");
      loadRunnerMetadata.addToLoadRunnerProperties("testname", loadRunnerTestName);
    } else if (Response.Status.NOT_FOUND.getStatusCode()
        == response.getStatusLine().getStatusCode()) {
      JenkinsPlugInLogger.severe("Test ID not present in the Blazemeter System");
      throw new BuildComparatorException("Test ID not present in the Blazemeter System");
    } else {
      JenkinsPlugInLogger.severe("Internal Server error has occured while processing your request");
      throw new BuildComparatorException(
          "Internal Server error has occured while processing your request");
    }
  }

  private void fetchTestUsersAndTestFileName() {
    String mastersSummaryURL = blazemeterRestURL + "/tests/" + testId + "/info?force=false";
    CloseableHttpClient client = HttpClients.createDefault();
    HttpGet httpGet = new HttpGet(mastersSummaryURL);
    httpGet.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
    httpGet.addHeader(
        Constants.AUTHORIZATION,
        Credentials.basic(
            loadRunnerMetadata.getLoadRunnerPropertyValue("blazemeter.apikeyid"),
            loadRunnerMetadata.getLoadRunnerPropertyValue("blazemeter.apikeysecret")));
    CloseableHttpResponse response = null;
    try {
      response = client.execute(httpGet);
    } catch (UnsupportedEncodingException e) {
      JenkinsPlugInLogger.severe("Error while reading REST Response");
      return;
    } catch (ClientProtocolException e) {
      JenkinsPlugInLogger.severe("Error while reading REST Response");
      return;
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error while reading REST Response");
      return;
    }
    if (Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()) {
      StringBuffer result = new StringBuffer();
      try {
        BufferedReader rd =
            new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }

      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Error while reading REST Response");
        return;
      }
      JSONObject testMetadataResponse = new JSONObject(result.toString());
      JSONObject resultNode = (JSONObject) testMetadataResponse.get("result");
      int numberOfUsers = resultNode.getInt("threads");
      if (!resultNode.isNull("jmx")) {
        JSONObject jmxObject = (JSONObject) resultNode.get("jmx");
        String jmxFileName = jmxObject.getString("filename");
        loadRunnerMetadata.addToLoadRunnerProperties("loadrunner.filename", jmxFileName);
      } else {
        JenkinsPlugInLogger.severe(
            "Internal Server error has occured while processing your request : JMX file is not found.");
      }
      loadRunnerMetadata.addToLoadRunnerProperties("loadrunner.users", "" + numberOfUsers);
    } else if (Response.Status.NOT_FOUND.getStatusCode()
        == response.getStatusLine().getStatusCode()) {
      JenkinsPlugInLogger.severe("Test ID not present in the Blazemeter System");
    } else {
      JenkinsPlugInLogger.severe("Internal Server error has occured while processing your request");
    }
  }

  private void fetchTestRunDurations() throws BuildComparatorException {
    List<String> histogramBuilds = loadRunnerMetadata.getJenkinsInfo().getHistogramBuilds();
    int buildsInHistogram = histogramBuilds.size();
    int limit = 0;
    int leastHistogramBuild = Integer.parseInt(histogramBuilds.get(buildsInHistogram - 1));
    if (leastHistogramBuild < loadRunnerMetadata.getBenchMarkBuildNumber()) {
      limit = loadRunnerMetadata.getCurrentBuildNumber() - leastHistogramBuild + 1;
    } else {
      limit =
          loadRunnerMetadata.getCurrentBuildNumber()
              - loadRunnerMetadata.getBenchMarkBuildNumber()
              + 1;
    }
    String mastersSummaryURL =
        blazemeterRestURL + "/tests/" + testId + "/masters-summaries?limit=" + limit;
    CloseableHttpClient client = HttpClients.createDefault();
    HttpGet httpGet = new HttpGet(mastersSummaryURL);
    httpGet.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
    httpGet.addHeader(
        Constants.AUTHORIZATION, Credentials.basic(blazemeterApiKey, blazemeterApiKeySecret));
    CloseableHttpResponse response = null;
    try {
      response = client.execute(httpGet);
    } catch (UnsupportedEncodingException e) {
      JenkinsPlugInLogger.severe("Error fetching Test Run Durations ->" + e.getMessage());
      throw new BuildComparatorException("Error fetching Test Run Durations ->" + e.getMessage());
    } catch (ClientProtocolException e) {
      JenkinsPlugInLogger.severe("Error fetching Test Run Durations ->" + e.getMessage());
      throw new BuildComparatorException("Error fetching Test Run Durations ->" + e.getMessage());
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error fetching Test Run Durations ->" + e.getMessage());
      throw new BuildComparatorException("Error fetching Test Run Durations ->" + e.getMessage());
    }
    if (Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()) {
      StringBuffer result = new StringBuffer();
      try {
        BufferedReader rd =
            new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }
      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Error fetching Test Run Durations ->" + e.getMessage());
        throw new BuildComparatorException("Error fetching Test Run Durations ->" + e.getMessage());
      }
      JSONObject masterIdsResponse = new JSONObject(result.toString());
      JSONObject resultObj = (JSONObject) masterIdsResponse.get("result");
      JSONArray labelsArray = (JSONArray) resultObj.get("labels");
      JSONObject currentInformation = (JSONObject) labelsArray.get(0);
      int benchmarkTestNumber =
          loadRunnerMetadata.getCurrentBuildNumber() - loadRunnerMetadata.getBenchMarkBuildNumber();
      if (labelsArray.length() > benchmarkTestNumber) {
        JSONObject benchMarkInformation = (JSONObject) labelsArray.get(benchmarkTestNumber);
        long benchMarkBZRunId = benchMarkInformation.getLong("id");
        loadRunnerMetadata.addToLoadRunnerProperties(
            "benchMarkBuildBZRunId", "" + benchMarkBZRunId);
        long benchMarkBuildStartTime =
            ((JSONObject) benchMarkInformation.get("session")).getLong(("created")) * 1000;
        long benchMarkBuildEndTime =
            ((JSONObject) benchMarkInformation.get("session")).getLong(("updated")) * 1000;
        loadRunnerMetadata.setBenchMarBuildTimes(benchMarkBuildStartTime, benchMarkBuildEndTime);
      }
      long currentBZRunId = currentInformation.getLong("id");

      loadRunnerMetadata.addToLoadRunnerProperties("currentBuildBZRunId", "" + currentBZRunId);
      long currentBuildStartTime =
          ((JSONObject) currentInformation.get("session")).getLong(("created")) * 1000;
      long currentBuildEndTime =
          ((JSONObject) currentInformation.get("session")).getLong(("updated")) * 1000;
      loadRunnerMetadata.setCurrentBuildTimes(currentBuildStartTime, currentBuildEndTime);

      List<BuildInfo> histogramBuildInfoList = new ArrayList<BuildInfo>();
      BuildInfo histBuildInfo = null;
      histogramBuildInfoList.add(0, loadRunnerMetadata.getCurrentBuildInfo());
      int histogramlength = 0;
      if (labelsArray.length() > buildsInHistogram) {
        histogramlength = buildsInHistogram;
      } else {
        histogramlength = labelsArray.length();
      }
      for (int i = 1; i < histogramlength; i++) {
        histBuildInfo = new BuildInfo();
        JSONObject histogramBuildInformation =
            (JSONObject)
                labelsArray.get(
                    loadRunnerMetadata.getCurrentBuildNumber()
                        - Integer.parseInt(histogramBuilds.get(i)));
        long histogramBuildStartTime =
            ((JSONObject) histogramBuildInformation.get("session")).getLong(("created")) * 1000;
        long histogramBuildEndTime =
            ((JSONObject) histogramBuildInformation.get("session")).getLong(("updated")) * 1000;
        histBuildInfo.setNumber(Integer.parseInt(histogramBuilds.get(i)));
        histBuildInfo.setStartTime(histogramBuildStartTime);
        histBuildInfo.setEndTime(histogramBuildEndTime);
        histogramBuildInfoList.add(i, histBuildInfo);
      }

      loadRunnerMetadata.setHistogramBuildInfo(histogramBuildInfoList);
    } else if (Response.Status.NOT_FOUND.getStatusCode()
        == response.getStatusLine().getStatusCode()) {
      JenkinsPlugInLogger.severe(
          "Test Information could not be found in the Blazemeter System for test id:" + testId);
      throw new BuildComparatorException(
          "Test Information could not be found in the Blazemeter System for test id:" + testId);
    } else if (Response.Status.UNAUTHORIZED
        .getReasonPhrase()
        .equals(response.getStatusLine().getReasonPhrase())) {
      JenkinsPlugInLogger.severe(
          "Internal Server error has occured while processing request for Blazemeter : "
              + response.getStatusLine().getReasonPhrase()
              + " Blazemeter credentials");
      throw new BuildComparatorException(
          "Internal Server error has occured while fetching Test Run Durations for Blazemeter : "
              + response.getStatusLine().getReasonPhrase()
              + " Blazemeter credentials");
    } else {
      JenkinsPlugInLogger.severe("Internal Server error has occured while processing your request");
      throw new BuildComparatorException(
          "Internal Server error has occured while fetching Test Run Durations");
    }
  }

  private void copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
    byte[] buf = new byte[bufferSize];
    int n = input.read(buf);
    while (n >= 0) {
      output.write(buf, 0, n);
      n = input.read(buf);
    }
    output.flush();
  }

  private String getReportLogURL() throws BuildComparatorException {
    String reportLogURL = null;
    String mastersSessionIdURL =
        blazemeterRestURL
            + "/masters/"
            + loadRunnerMetadata.getLoadRunnerPropertyValue("currentBuildBZRunId")
            + "/reports";
    CloseableHttpClient client = HttpClients.createDefault();
    HttpGet httpGet = new HttpGet(mastersSessionIdURL);
    httpGet.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
    httpGet.addHeader(
        Constants.AUTHORIZATION, Credentials.basic(blazemeterApiKey, blazemeterApiKeySecret));
    CloseableHttpResponse response = null;
    try {
      response = client.execute(httpGet);
    } catch (UnsupportedEncodingException e) {
      JenkinsPlugInLogger.severe("Error while fetching Report Log URL->" + e.getMessage());
      throw new BuildComparatorException("Error while fetching Report Log URL->" + e.getMessage());
    } catch (ClientProtocolException e) {
      JenkinsPlugInLogger.severe("Error while fetching Report Log URL->" + e.getMessage());
      throw new BuildComparatorException("Error while fetching Report Log URL->" + e.getMessage());
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error while fetching Report Log URL->" + e.getMessage());
      throw new BuildComparatorException("Error while fetching Report Log URL->" + e.getMessage());
    }
    if (Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()) {
      StringBuffer result = new StringBuffer();
      try {
        BufferedReader rd =
            new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }
      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Error while fetching Report Log URL->" + e.getMessage());
        throw new BuildComparatorException(
            "Error while fetching Report Log URL->" + e.getMessage());
      }
      JSONObject responseObj = new JSONObject(result.toString());
      JSONArray resultsArray = (JSONArray) responseObj.get("result");

      for (int i = 0; i < resultsArray.length(); i++) {
        JSONObject rowObj = (JSONObject) resultsArray.get(i);
        String fileName = rowObj.getString("fileName");
        if (fileName.endsWith(".zip")) {
          reportLogURL = rowObj.getString("dataUrl");
          break;
        }
      }
    }
    return reportLogURL;
  }

  private String getZipURL(String reportLogURL) throws BuildComparatorException {
    JenkinsPlugInLogger.finest("Report Log URL is :" + reportLogURL);
    CloseableHttpClient client = HttpClients.createDefault();
    String zipDownloadURL = null;
    HttpGet httpGet = new HttpGet(reportLogURL);
    httpGet.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
    httpGet.addHeader(
        Constants.AUTHORIZATION, Credentials.basic(blazemeterApiKey, blazemeterApiKeySecret));
    CloseableHttpResponse response = null;
    try {
      response = client.execute(httpGet);
    } catch (UnsupportedEncodingException e) {
      JenkinsPlugInLogger.severe("Error while fetching getZipURL->" + e.getMessage());
      throw new BuildComparatorException("Error while fetching getZipURL->" + e.getMessage());
    } catch (ClientProtocolException e) {
      JenkinsPlugInLogger.severe("Error while fetching getZipURL->" + e.getMessage());
      throw new BuildComparatorException("Error while fetching getZipURL->" + e.getMessage());
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error while fetching getZipURL->" + e.getMessage());
      throw new BuildComparatorException("Error while fetching getZipURL->" + e.getMessage());
    }
    if (Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()) {
      StringBuffer result = new StringBuffer();
      try {
        BufferedReader rd =
            new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }
      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Error while fetching Report Log URL->" + e.getMessage());
        throw new BuildComparatorException("Error while fetching getZipURL->" + e.getMessage());
      }
      JSONObject responseObj = new JSONObject(result.toString());
      JSONObject resultsArray = (JSONObject) responseObj.get("result");
      JSONArray dataArray = (JSONArray) resultsArray.get("data");
      for (int i = 0; i < dataArray.length(); i++) {
        JSONObject rowObj = (JSONObject) dataArray.get(i);
        String title = (String) rowObj.get("title");
        if (title.endsWith("Zip")) {
          zipDownloadURL = rowObj.getString("dataUrl");
          break;
        }
      }
    }
    return zipDownloadURL;
  }

  private StringBuilder downloadZipAndExtractJTL(String zipDownloadURL)
      throws BuildComparatorException {
    URL url = null;
    String jenkinsWorkspaceFolder =
        loadRunnerMetadata.getJenkinsInfo().getBuildWorkSpaceFolder()
            + File.separator
            + loadRunnerMetadata.getJenkinsInfo().getJobName()
            + File.separator
            + loadRunnerMetadata.getJenkinsInfo().getCurrentBuildNumber();
    try {
      url = new URL(zipDownloadURL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      InputStream in = connection.getInputStream();
      FileOutputStream out =
          new FileOutputStream(jenkinsWorkspaceFolder + File.separator + "load_metadata.zip");
      copy(in, out, 1024);
      out.close();
    } catch (MalformedURLException e) {
      JenkinsPlugInLogger.severe("Error while downloadZipAndExtractJTL->" + e.getMessage());
      // throw new BuildComparatorException("Error while downloadZipAndExtractJTL->" +
      // e.getMessage());
    } catch (ProtocolException e) {
      JenkinsPlugInLogger.severe("Error while downloadZipAndExtractJTL->" + e.getMessage());
      // throw new BuildComparatorException("Error while downloadZipAndExtractJTL->" +
      // e.getMessage());
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error while downloadZipAndExtractJTL->" + e.getMessage());
      // throw new BuildComparatorException("Error while downloadZipAndExtractJTL->" +
      // e.getMessage());
    }
    ZipFile zipFile;
    StringBuilder result = new StringBuilder();
    try {
      zipFile = new ZipFile(jenkinsWorkspaceFolder + File.separator + "load_metadata.zip");
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.getName().endsWith(".jtl")) {
          InputStream stream = zipFile.getInputStream(entry);
          try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(stream));
            String line = "";
            while ((line = rd.readLine()) != null) {
              result.append(line);
            }
          } catch (IOException e) {
            JenkinsPlugInLogger.severe("Error while downloadZipAndExtractJTL->" + e.getMessage());
            // throw new BuildComparatorException("Error while downloadZipAndExtractJTL->" +
            // e.getMessage());
          }
          stream.close();
        }
      }
      zipFile.close();
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error while downloadZipAndExtractJTL->" + e.getMessage());
      // throw new BuildComparatorException("Error while downloadZipAndExtractJTL->" +
      // e.getMessage());
    }
    return result;
  }

  private String getBusinessSegmentName(String query) {
    if (query.contains("$bs")) {
      int index = query.indexOf("$bs=");
      return query.substring(index + 4, query.indexOf(";", index));
    }
    return null;
  }

  private void processJTLXMLFile(StringBuilder jtlXMLData) throws BuildComparatorException {
    try {
      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      InputSource is = new InputSource();
      is.setCharacterStream(new StringReader(jtlXMLData.toString()));
      Document doc = db.parse(is);
      NodeList resultNode = doc.getElementsByTagName("httpSample");
      outer:
      for (int i = 0; i < resultNode.getLength(); i++) {
        Element element = (Element) resultNode.item(i);
        NodeList urlList = element.getChildNodes();
        for (int j = 0; j < urlList.getLength(); j++) {
          Node urlNode = urlList.item(j);
          if (urlNode.getNodeName().equals("java.net.URL")) {
            String urlStr = urlNode.getTextContent();
            URL url = new URL(urlStr);
            String query = url.getQuery();
            if (query != null) {
              componentName = getBusinessSegmentName(query);
            }
            if (componentName != null) {
              break outer;
            }
          }
        }
      }
      loadRunnerMetadata.addToLoadRunnerProperties("componentname", componentName);
    } catch (ParserConfigurationException e) {
      JenkinsPlugInLogger.severe("Error while processJTLXMLFile->" + e);
      // JenkinsPlugInLogger.severe("Error while processJTLXMLFile->" + e.getMessage());
      // throw new BuildComparatorException("Error while processJTLXMLFile->" + e.getMessage());
    } catch (SAXException e) {
      JenkinsPlugInLogger.severe("Error while processJTLXMLFile->" + e);
      // JenkinsPlugInLogger.severe("Error while processJTLXMLFile->" + e.getMessage());
      // throw new BuildComparatorException("Error while processJTLXMLFile->" + e.getMessage());
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error while processJTLXMLFile->" + e);
      // JenkinsPlugInLogger.severe("Error while processJTLXMLFile->" + e.getMessage());
      // throw new BuildComparatorException("Error while processJTLXMLFile->" + e.getMessage());
    }
  }

  private void fetchBusinessSegmentInfo() throws BuildComparatorException {

    String reportLogURL = getReportLogURL();
    String zipDownloadURL = getZipURL(reportLogURL);
    if (zipDownloadURL != null) {
      StringBuilder jtlXMLData = downloadZipAndExtractJTL(zipDownloadURL);
      processJTLXMLFile(jtlXMLData);
    }
  }
}
