package com.ca.apm.jenkins.core.helper;

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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.core.entity.APMConnectionInfo;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.entity.ComparisonResult;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;

public class VertexAttributesUpdateHelper {

	private static APMConnectionInfo apmConnectionInfo;
	private static List<String> vertexAttributes;
	private static ComparisonMetadata comparisonMetadata;

	public static void setAPMConnectionInfo(APMConnectionInfo apmConnectionInfo) {
		VertexAttributesUpdateHelper.apmConnectionInfo = apmConnectionInfo;
	}

	private static String generateURL(String emURL, String relativeURL) {
		return emURL + relativeURL;
	}

	public VertexAttributesUpdateHelper(ComparisonMetadata comparisonMetadata) {
		this.comparisonMetadata = comparisonMetadata;
	}
	
	private static boolean callUpdateVertexAttribute(Map<String, Set<String>> vertexIdTsMap, Map attributesMap) {
		JenkinsPlugInLogger.info("Inside callUpdateVertexAttribute method");
		if (vertexIdTsMap.isEmpty()) {
			return false;
		}
		String attributeUpdURL = generateURL(apmConnectionInfo.getEmURL(), Constants.attributeUpdate);
		HttpClientBuilder client = HttpClientBuilder.create();
		HttpPatch request = new HttpPatch(attributeUpdURL);
		CloseableHttpResponse response = null;
		try {

			client = IgnoreSSLClient(client);
			request.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
			request.addHeader(Constants.AUTHORIZATION, Constants.BEARER + apmConnectionInfo.getEmAuthToken());
			request.addHeader("Accept", "application/json");
			Iterator iterator = vertexIdTsMap.entrySet().iterator();
			boolean isFirstID = true;
			StringBuffer payload = new StringBuffer("{ \"items\" : [");
			List<String> tsList = null;
			for (Map.Entry<String, Set<String>> entry : vertexIdTsMap.entrySet()) {
				tsList = new ArrayList<String>(entry.getValue());
				for (int ts = 0; ts < tsList.size(); ts++) {
					if (isFirstID) {
						payload.append("{" +

								"\"id\" : \"" + Integer.parseInt(entry.getKey()) + "\"," + "\"timestamp\" : \""
								+ tsList.get(ts) + "\"," + "\"attributes\": {" + "\"currentBuildNumber\":[\""
								+ attributesMap.get("currentBuildNumber") + "\"]," + "\"benchmarkBuildNumber\":[\""
								+ attributesMap.get("benchMarkBuildNumber") + "\"]," + "\"buildStatus\":[\""
								+ attributesMap.get("buildStatus") + "\"]," + "\"loadGeneratorName\":[\""
								+ attributesMap.get("loadGeneratorName") + "\"]," + "\"loadGeneratorStartTime\":[\""
								+ attributesMap.get("loadGeneratorStartTime") + "\"]," + "\"loadGeneratorEndTime\": [\""
								+ attributesMap.get("loadGeneratorEndTime") + "\"]" + "}" + "}");
						isFirstID = false;
					} else {
						payload.append(",{" +

								"\"id\" : \"" + Integer.parseInt(entry.getKey()) + "\"," + "\"timestamp\" : \""
								+ tsList.get(ts) + "\"," + "\"attributes\": {" + "\"currentBuildNumber\":[\""
								+ attributesMap.get("currentBuildNumber") + "\"]," + "\"benchmarkBuildNumber\":[\""
								+ attributesMap.get("benchMarkBuildNumber") + "\"]," + "\"buildStatus\":[\""
								+ attributesMap.get("buildStatus") + "\"]," + "\"loadGeneratorName\":[\""
								+ attributesMap.get("loadGeneratorName") + "\"]," + "\"loadGeneratorStartTime\":[\""
								+ attributesMap.get("loadGeneratorStartTime") + "\"]," + "\"loadGeneratorEndTime\": [\""
								+ attributesMap.get("loadGeneratorEndTime") + "\"]" + "}" + "}");
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
		} catch (UnsupportedEncodingException e) {
			JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
		} catch (ClientProtocolException e) {
			JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
		} catch (IOException e) {
			JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
		} catch (Exception e) {
			JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
		}

		if (Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()
				|| ((response.toString() != null) && response.toString().contains("Multi Status"))) {
			StringBuffer result = new StringBuffer();
			try {
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				
			} catch (IOException e) {
				JenkinsPlugInLogger.info("Error in callUpdateAttributeAPI ->" + e.getMessage());
			}
			JenkinsPlugInLogger.printLogOnConsole(2, "Successfully updated the vertex attributes");
			return true;
		}
		return false;
	}

	public boolean updateAttributeOfVertex(boolean isBuildSuccess) {
		JenkinsPlugInLogger.info("Entered updateAttributeOfVertex method");
		String buildStatus = isBuildSuccess == true ? "SUCCESS" : "FAIL";
		Map<String, String> attributesMap = new HashMap<String, String>();
		OutputConfiguration outputConfiguration = comparisonMetadata.getOutputConfiguration();
		attributesMap.put("currentBuildNumber",
				outputConfiguration.getCommonPropertyValue(Constants.jenkinsCurrentBuild));
		attributesMap.put("benchMarkBuildNumber",
				outputConfiguration.getCommonPropertyValue(Constants.jenkinsBenchMarkBuild));
		attributesMap.put("buildStatus", buildStatus);
		attributesMap.put("loadGeneratorName", outputConfiguration.getCommonPropertyValue(Constants.loadGeneratorName));
		long startTime = 0L;
		long endTime = 0L;
		try {
			startTime = Long.parseLong(outputConfiguration.getCommonPropertyValue("runner.start"));
			endTime = Long.parseLong(outputConfiguration.getCommonPropertyValue("runner.end"));
			String[] emDateTime = JenkinsPluginUtility.getEMTimeinDateFormat(startTime, endTime,
					apmConnectionInfo.getEmTimeZone());
			attributesMap.put("loadGeneratorStartTime", emDateTime[0]);
			attributesMap.put("loadGeneratorEndTime", emDateTime[1]);
		} catch (ParseException e) {
			JenkinsPlugInLogger.printLogOnConsole(2, e.getMessage());
		}
		ComparisonResult comparisonResult = comparisonMetadata.getComparisonResult();

		boolean processStatus = false;
		Set<String> vertexIds = null;
		String vertexId = null;
		
		String vertexIdsURL = generateURL(apmConnectionInfo.getEmURL(), Constants.vertexIdByName);

		HttpGet httpGet = null;

		Map<String, Set<String>> vertexIdTsMap = new HashMap<String, Set<String>>();
		Map<String, Set<String>> vertexIdTs = new HashMap<String, Set<String>>();
		String applicationName = comparisonMetadata.getCommonPropertyValue(Constants.applicationName);
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;

		client = HttpClients.createDefault();

		applicationName = applicationName.replace(" ", "%20C");

		httpGet = new HttpGet(vertexIdsURL + "?q=attributes.applicationName:" + applicationName);

		try {
			httpGet.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
			httpGet.addHeader(Constants.AUTHORIZATION, Constants.BEARER + apmConnectionInfo.getEmAuthToken());
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
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			JSONObject vertexJSON = new JSONObject(result.toString());
			if (!vertexJSON.isNull("_embedded")) {

				JSONObject embeddedObject = (JSONObject) vertexJSON.get("_embedded");
				JSONArray vertexArray = embeddedObject.getJSONArray("vertex");

				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				long millis = 0L;
				try {

					for (int i = 0; i < vertexArray.length(); i++) {
						JSONObject rowObj = (JSONObject) vertexArray.get(i);
						String id = (String) rowObj.get("id");
						String timestamp = (String) rowObj.get("timestamp");

						millis = dateFormat.parse(timestamp).getTime();

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

					}
					if (client != null)
						client.close();
					if (response != null)
						response.close();
				} catch (ParseException e) {
					JenkinsPlugInLogger.severe("Exception while parsing the timestamp " + e.getMessage());
				} catch (IOException e) {
					JenkinsPlugInLogger.severe("Exception while closing the http connection " + e.getMessage());
				}

			}

		}

		if (vertexIdTs.isEmpty()) {
			JenkinsPlugInLogger.severe("No vertices data is fetched for the application  " + applicationName);
			JenkinsPlugInLogger.printLogOnConsole(2, "No vertices data is fetched for the application  "+ applicationName);

		} else {
			processStatus = callUpdateVertexAttribute(vertexIdTs, attributesMap);
		}
		return processStatus;

	}

	private static HttpClientBuilder IgnoreSSLClient(HttpClientBuilder client) throws Exception {

		JenkinsPlugInLogger.finest("IgnoreSSLClient 1 ..");

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {
			}
		} };

		JenkinsPlugInLogger.finest("IgnoreSSLClient 2..");

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			javax.net.ssl.HostnameVerifier hnv = new javax.net.ssl.HostnameVerifier() {

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
}
