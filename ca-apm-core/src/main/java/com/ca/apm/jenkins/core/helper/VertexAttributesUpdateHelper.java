package com.ca.apm.jenkins.core.helper;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.core.entity.APMConnectionInfo;
import com.ca.apm.jenkins.core.entity.AgentComparisonResult;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.entity.ComparisonResult;
import com.ca.apm.jenkins.core.entity.DefaultStrategyResult;
import com.ca.apm.jenkins.core.entity.MetricPathComparisonResult;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

public class VertexAttributesUpdateHelper {

  public static final Set verticesAttributeSet = new LinkedHashSet();
  private static APMConnectionInfo apmConnectionInfo;
  private static List<String> vertexAttributes;
  private static ComparisonMetadata comparisonMetadata;

  public VertexAttributesUpdateHelper(ComparisonMetadata comparisonMetadata) {
    this.comparisonMetadata = comparisonMetadata;
  }

  public static void setAPMConnectionInfo(APMConnectionInfo apmConnectionInfo) {
    VertexAttributesUpdateHelper.apmConnectionInfo = apmConnectionInfo;
  }

  private static String generateURL(String emURL, String relativeURL) {
    return emURL + relativeURL;
  }

  private static String getTemporaryAuthToken() throws BuildComparatorException {
    if (apmConnectionInfo.getEmPassword().isEmpty()) {
      JenkinsPlugInLogger.fine(
          "Password is empty, hence considering username as auth token --:>"
              + apmConnectionInfo.getEmUserName());
      JenkinsPlugInLogger.fine("Length is --:>" + apmConnectionInfo.getEmUserName().length());
      return apmConnectionInfo.getEmUserName();
    }
    if (apmConnectionInfo.getAuthToken() == null) {
      JenkinsPlugInLogger.severe("Password is not empty, hence requesting temporary token from EM");
      String tokenUrl = generateURL(apmConnectionInfo.getEmURL(), Constants.tokenPath);
      CloseableHttpClient client = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(tokenUrl);
      JSONObject reqBody = new JSONObject();
      reqBody.put("username", apmConnectionInfo.getEmUserName());
      reqBody.put("password", apmConnectionInfo.getEmPassword());
      StringEntity entity = null;
      CloseableHttpResponse response = null;
      try {
        entity = new StringEntity(reqBody.toString());
        httpPost.setEntity(entity);
        httpPost.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
        response = client.execute(httpPost);
      } catch (UnsupportedEncodingException e) {
        JenkinsPlugInLogger.severe("Error while getting temporary token ->" + e.getMessage(), e);
      } catch (ClientProtocolException e) {
        JenkinsPlugInLogger.severe("Error while getting temporary token ->" + e.getMessage(), e);
      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Error while getting temporary token ->" + e.getMessage(), e);
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
          return null;
        }
        JSONObject responseObj = new JSONObject(result.toString());
        apmConnectionInfo.setAuthToken((String) responseObj.get("token"));
      } else {
        int statusCode = response.getStatusLine().getStatusCode();
        JenkinsPlugInLogger.severe(
            "Getting Auth token from CA-APM failed with status code " + statusCode);
        JenkinsPlugInLogger.severe("Detailed Response from EM is  " + response.toString());

        throw new BuildComparatorException(
            "Error occured while getting auth token from CA-APM with response code ->"
                + statusCode);
      }
    } else {
      return apmConnectionInfo.getAuthToken();
    }
    return apmConnectionInfo.getAuthToken();
  }

  private static boolean callUpdateVertexAttribute(
      Map<String, Set<String>> vertexIdTsMap, Map attributesMap) {
    JenkinsPlugInLogger.fine("Inside callUpdateVertexAttribute method");
    JenkinsPlugInLogger.printLogOnConsole(2, "Inside callUpdateVertexAttribute method");
    System.out.println("Inside callUpdateVertexAttribute method");
    if (vertexIdTsMap.isEmpty()) {
      return false;
    }
    String attributeUpdURL = generateURL(apmConnectionInfo.getEmURL(), Constants.attributeUpdate);
    HttpClientBuilder client = HttpClientBuilder.create();
    HttpPatch request = new HttpPatch(attributeUpdURL);
    CloseableHttpResponse response = null;
    // client.disableAutomaticRetries();
    // CloseableHttpClient client = HttpClients.createDefault();

    try {

      client = IgnoreSSLClient(client);
      request.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
      request.addHeader(
          Constants.AUTHORIZATION, Constants.BEARER + apmConnectionInfo.getAuthToken());
      request.addHeader("Accept", "application/json");
      Iterator iterator = vertexIdTsMap.entrySet().iterator();
      boolean isFirstID = true;
      StringBuffer payload = new StringBuffer("{ \"items\" : [");
      List<String> tsList = null;
      for (Map.Entry<String, Set<String>> entry : vertexIdTsMap.entrySet()) {
        tsList = new ArrayList<String>(entry.getValue());
        for (int ts = 0; ts < tsList.size(); ts++) {
          if (isFirstID) {
            payload.append(
                "{"
                    + "\"id\" : \""
                    + Integer.parseInt(entry.getKey())
                    + "\","
                    + "\"timestamp\" : \""
                    + tsList.get(ts)
                    + "\","
                    + "\"attributes\": {"
                    + "\"currentBuildNumber\":[\""
                    + attributesMap.get("currentBuildNumber")
                    + "\"],"
                    + "\"benchmarkBuildNumber\":[\""
                    + attributesMap.get("benchMarkBuildNumber")
                    + "\"],"
                    + "\"buildStatus\":[\""
                    + attributesMap.get("buildStatus")
                    + "\"],"
                    + "\"loadGeneratorName\":[\""
                    + attributesMap.get("loadGeneratorName")
                    + "\"],"
                    + "\"loadGeneratorStartTime\":[\""
                    + attributesMap.get("loadGeneratorStartTime")
                    + "\"],"
                    + "\"loadGeneratorEndTime\": [\""
                    + attributesMap.get("loadGeneratorEndTime")
                    + "\"]"
                    + "}"
                    + "}");
            isFirstID = false;
          } else {
            payload.append(
                ",{"
                    + "\"id\" : \""
                    + Integer.parseInt(entry.getKey())
                    + "\","
                    + "\"timestamp\" : \""
                    + tsList.get(ts)
                    + "\","
                    + "\"attributes\": {"
                    + "\"currentBuildNumber\":[\""
                    + attributesMap.get("currentBuildNumber")
                    + "\"],"
                    + "\"benchmarkBuildNumber\":[\""
                    + attributesMap.get("benchMarkBuildNumber")
                    + "\"],"
                    + "\"buildStatus\":[\""
                    + attributesMap.get("buildStatus")
                    + "\"],"
                    + "\"loadGeneratorName\":[\""
                    + attributesMap.get("loadGeneratorName")
                    + "\"],"
                    + "\"loadGeneratorStartTime\":[\""
                    + attributesMap.get("loadGeneratorStartTime")
                    + "\"],"
                    + "\"loadGeneratorEndTime\": [\""
                    + attributesMap.get("loadGeneratorEndTime")
                    + "\"]"
                    + "}"
                    + "}");
          }
        }
      }

      payload.append("] }");

      StringEntity entity = new StringEntity(payload.toString());
      request.setEntity(entity);
      response = client.build().execute(request);
      client.disableAutomaticRetries();
      client.disableConnectionState();
      client.build().close();
      System.out.println(response);

    } catch (UnsupportedEncodingException e) {
      JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
    } catch (ClientProtocolException e) {
      JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
    } catch (IOException e) {
      JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
    } catch (Exception e) {
      JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
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
        JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
      }
      return true;
    }
    return false;
  }

  private static void vertexAttrSet(Map<String, AgentComparisonResult> result) {
    JenkinsPlugInLogger.fine("inside vertexAttrSet method");
    JenkinsPlugInLogger.printLogOnConsole(2, "inside vertexAttrSet method");
    System.out.println("inside vertexAttrSet method");
    Iterator it = result.entrySet().iterator();
    while (it.hasNext()) {

      Map.Entry entry = (Map.Entry) it.next();
      List<MetricPathComparisonResult> successEntries =
          ((AgentComparisonResult) entry.getValue()).getSuccessEntries();
      String[] successEntriesList = successEntries.toString().split("ComparisonOutput");
      for (int i = 0; i < successEntriesList.length; i++) {

        if (successEntriesList[i].contains("metricPath")
            && successEntriesList[i].contains("Business Segment|")) {
          vertexAttributes = new ArrayList<String>();

          String[] metricPath =
              successEntriesList[i]
                  .substring(successEntriesList[i].indexOf("SuperDomain|"))
                  .split("[|]");

          vertexAttributes.add(metricPath[1]);
          vertexAttributes.add(metricPath[1] + "|" + metricPath[2] + "|" + metricPath[3]);
          vertexAttributes.add(metricPath[5]);
          vertexAttributes.add(metricPath[6].substring(0, metricPath[6].indexOf("via")).trim());
          verticesAttributeSet.add(vertexAttributes);
        }
      }
      List<MetricPathComparisonResult> slowEntries =
          ((AgentComparisonResult) entry.getValue()).getSlowEntries();
      String[] slowEntriesList = slowEntries.toString().split("ComparisonOutput");

      for (int i = 0; i < slowEntriesList.length; i++) {
        if (slowEntriesList[i].contains("metricPath")
            && slowEntriesList[i].contains("Business Segment|")) {
          vertexAttributes = new ArrayList<String>();
          String[] metricPath =
              slowEntriesList[i].substring(slowEntriesList[i].indexOf("SuperDomain|")).split("[|]");
          vertexAttributes.add(metricPath[1]);
          vertexAttributes.add(metricPath[1] + "|" + metricPath[2] + "|" + metricPath[3]);
          vertexAttributes.add(metricPath[5]);
          vertexAttributes.add(metricPath[6].substring(0, metricPath[6].indexOf("via")).trim());
          /*
           * // for(int j=0;j<vertexAttributes.size();j++){
           * System.out.print(vertexAttributes.get(vertexAttributes.
           * size() - 1) + "  "); // }
           */ verticesAttributeSet.add(vertexAttributes);
        }
      }
    }
  }

  private static HttpClientBuilder IgnoreSSLClient(HttpClientBuilder client) throws Exception {

    JenkinsPlugInLogger.finest("IgnoreSSLClient 1 ..");

    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(
                java.security.cert.X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {}

            public void checkServerTrusted(
                java.security.cert.X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {}
          }
        };

    JenkinsPlugInLogger.finest("IgnoreSSLClient 2..");

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new SecureRandom());
      javax.net.ssl.HostnameVerifier hnv =
          new javax.net.ssl.HostnameVerifier() {

            public boolean verify(String arg0, SSLSession session) {
              return true;
            }
          };
      SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory(sc, hnv);
      client.setSSLSocketFactory(sslcsf);

      return client;
    } catch (Exception ex) {
      JenkinsPlugInLogger.finest(" Exception in IgnoreSSLClient " + ex);
      JenkinsPlugInLogger.severe("Exception in IgnoreSSLClient", ex);

      return null;
    }
  }

  public boolean updateAttributeOfVertex(boolean isBuildSuccess) {
    JenkinsPlugInLogger.fine("inside updateAttributeOfVertex method");
    JenkinsPlugInLogger.printLogOnConsole(2, "Updating custom attributes of application vertices");
    System.out.println("inside updateAttributeOfVertex method");
    String buildStatus = isBuildSuccess == true ? "SUCCESS" : "FAIL";
    Map<String, String> attributesMap = new HashMap<String, String>();
    OutputConfiguration outputConfiguration = comparisonMetadata.getOutputConfiguration();
    attributesMap.put(
        "currentBuildNumber",
        outputConfiguration.getCommonPropertyValue(Constants.jenkinsCurrentBuild));
    attributesMap.put(
        "benchMarkBuildNumber",
        outputConfiguration.getCommonPropertyValue(Constants.jenkinsBenchMarkBuild));
    attributesMap.put("buildStatus", buildStatus);
    attributesMap.put(
        "loadGeneratorName",
        outputConfiguration.getCommonPropertyValue(Constants.loadGeneratorName));
    long startTime = 0L;
    long endTime = 0L;
    try {
      startTime = Long.parseLong(outputConfiguration.getCommonPropertyValue("runner.start"));
      endTime = Long.parseLong(outputConfiguration.getCommonPropertyValue("runner.end"));
      String[] emDateTime =
          JenkinsPluginUtility.getEMTimeinDateFormat(
              startTime, endTime, apmConnectionInfo.getEmTimeZone());
      attributesMap.put("loadGeneratorStartTime", emDateTime[0]);
      attributesMap.put("loadGeneratorEndTime", emDateTime[1]);
      JenkinsPlugInLogger.printLogOnConsole(2, "*****starttime******" + emDateTime[0]);
      JenkinsPlugInLogger.printLogOnConsole(2, "*****endTime*******" + emDateTime[1]);
    } catch (ParseException e) {
      JenkinsPlugInLogger.printLogOnConsole(2, e.getMessage());
    }
    ComparisonResult comparisonResult = comparisonMetadata.getComparisonResult();

    for (StrategyResult<?> strategyResult : comparisonResult.getStrategyResults()) {
      DefaultStrategyResult defaultStrategyResult =
          (DefaultStrategyResult) strategyResult.getResult();
      vertexAttrSet(defaultStrategyResult.getResult());
    }

    boolean processStatus = false;
    Set<String> vertexIds = null;
    String vertexId = null;
    if (!apmConnectionInfo.getEmPassword().isEmpty()) {
      JenkinsPlugInLogger.info(
          "User didn't provide auth token, hence generating a temporary token");
      apmConnectionInfo.setAuthToken(getTemporaryAuthToken());
    }
    String vertexIdsURL = generateURL(apmConnectionInfo.getEmURL(), Constants.vertexIdByName);
    CloseableHttpClient client = HttpClients.createDefault();

    HttpGet httpGet = null;

    Iterator vertexitr = verticesAttributeSet.iterator();
    Map<String, Set<String>> vertexIdTsMap = new HashMap<String, Set<String>>();
    Map<String, Set<String>> vertexIdTs = new HashMap<String, Set<String>>();
    while (vertexitr.hasNext()) {
      List<String> vertexList = (List<String>) vertexitr.next();
      String agent = vertexList.get(1).replace("|", "%7C").replace(" ", "%20C");
      String applicationName = vertexList.get(2);
      String transactionName = vertexList.get(3);

      httpGet =
          new HttpGet(
              vertexIdsURL
                  + "?q=attributes.applicationName:"
                  + applicationName
                  + "&attributes.transactionName:"
                  + transactionName
                  + "&attributes.agent:"
                  + agent);
      JenkinsPlugInLogger.info("*URL****" + httpGet.getURI().toString());

      CloseableHttpResponse response = null;
      try {
        httpGet.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
        httpGet.addHeader(
            Constants.AUTHORIZATION, Constants.BEARER + apmConnectionInfo.getAuthToken());
        response = client.execute(httpGet);

      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      } catch (ClientProtocolException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }

      if (Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()) {
        StringBuffer result = new StringBuffer();
        vertexIds = new HashSet<String>();

        try {
          BufferedReader rd =
              new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
          String line = "";
          while ((line = rd.readLine()) != null) {
            result.append(line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        JSONObject vertexJSON = new JSONObject(result.toString());
        if (vertexJSON.isNull("_embedded")) {
          continue;
        }
        JSONObject embeddedObject = (JSONObject) vertexJSON.get("_embedded");
        JSONArray vertexArray = embeddedObject.getJSONArray("vertex");

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        long millis = 0L;
        try {

          for (int i = 0; i < vertexArray.length(); i++) {
            JSONObject rowObj = (JSONObject) vertexArray.get(i);
            String id = (String) rowObj.get("id");
            String timestamp = (String) rowObj.get("timestamp");
            System.out.println("timestamp........" + timestamp);
            millis = dateFormat.parse(timestamp).getTime();
            System.out.println("millis........." + millis);
            //  if(millis>=startTime && millis<=endTime){
            if (vertexIdTsMap.containsKey(id)) {
              if (vertexIdTsMap.get(id).add(timestamp) == true) {
                if (vertexIdTs.containsKey(id)) {
                  vertexIdTs.get(id).add(timestamp);
                } else {
                  Set<String> tsSet = new HashSet<String>();
                  tsSet.add(timestamp);
                  vertexIdTs.put(id, tsSet);
                }
              }

            } else {
              Set<String> tsSet = new HashSet<String>();
              tsSet.add(timestamp);
              vertexIdTsMap.put(id, tsSet);
              vertexIdTs.put(id, tsSet);
            }
            // }
          }
        } catch (ParseException e) {
          JenkinsPlugInLogger.severe("Exception while parsing the timestamp " + e.getMessage());
        }
      }
    }

    processStatus = callUpdateVertexAttribute(vertexIdTs, attributesMap);
    return processStatus;
  }
}
