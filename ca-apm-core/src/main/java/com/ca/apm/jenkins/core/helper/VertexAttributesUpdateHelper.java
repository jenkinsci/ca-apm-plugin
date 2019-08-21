package com.ca.apm.jenkins.core.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;

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

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.core.entity.APMConnectionInfo;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;

public class VertexAttributesUpdateHelper {

	private static APMConnectionInfo apmConnectionInfo;
	private ComparisonMetadata comparisonMetadata;
	private static final String ERRORINCALLUPDATEATTRIBUTEAPI = "Error in callUpdateAttributeAPI ->";

	public static void setAPMConnectionInfo(APMConnectionInfo apmConnectionInfo) {
		VertexAttributesUpdateHelper.apmConnectionInfo = apmConnectionInfo;
	}

	private static String generateURL(String emURL, String relativeURL) {
		return emURL + relativeURL;
	}

	public VertexAttributesUpdateHelper(ComparisonMetadata comparisonMetadataInfo) {
		comparisonMetadata = comparisonMetadataInfo;
	}

	private static String getPayload(Map<String, Set<String>> vertexIdTsMap, Map<String, String> attributesMap) {
		List<String> tsList = null;
		boolean isFirstID = true;
		StringBuilder payload = new StringBuilder("{ \"items\" : [");
		for (Map.Entry<String, Set<String>> entry : vertexIdTsMap.entrySet()) {
			tsList = new ArrayList<>(entry.getValue());

			for (int ts = 0; ts < tsList.size(); ts++) {

				int attribsCount = 1;
				StringBuilder attribs = new StringBuilder();
				for (Map.Entry<String, String> attribsEntry : attributesMap.entrySet()) {
					if (attribsCount != attributesMap.size()) {
						attribs = attribs
								.append("\"" + attribsEntry.getKey() + "\":[\"" + attribsEntry.getValue() + "\"],");
						attribsCount++;
					} else {
						attribs = attribs
								.append("\"" + attribsEntry.getKey() + "\":[\"" + attribsEntry.getValue() + "\"]}}");
					}
				}
				if (isFirstID) {
					payload.append("{" +

							"\"id\" : \"" + Integer.parseInt(entry.getKey()) + "\"," + "\"timestamp\" : \""
							+ tsList.get(ts) + "\"," + "\"attributes\": {" + attribs.toString());
					isFirstID = false;
				} else {
					payload.append(",{" +

							"\"id\" : \"" + Integer.parseInt(entry.getKey()) + "\"," + "\"timestamp\" : \""
							+ tsList.get(ts) + "\"," + "\"attributes\": {" + attribs.toString());
				}

			}

		}
		payload.append("] }");
		JenkinsPlugInLogger.printLogOnConsole(3, "........payloadString..." + payload.toString());
		return payload.toString();
	}

	private static boolean callUpdateVertexAttribute(Map<String, Set<String>> vertexIdTsMap,
			Map<String, String> attributesMap) {
		JenkinsPlugInLogger.info("Inside callUpdateVertexAttribute method");
		if (vertexIdTsMap.isEmpty()) {
			return false;
		}
		String attributeUpdURL = generateURL(apmConnectionInfo.getEmURL(), Constants.ATTRIBUTEUPDATE);
		HttpClientBuilder client = HttpClientBuilder.create();
		HttpPatch request = new HttpPatch(attributeUpdURL);
		CloseableHttpResponse response = null;
		try {

			client = ignoreSSLClient(client);
			request.addHeader(Constants.CONTENTTYPE, Constants.APPLICATION_JSON);
			request.addHeader(Constants.AUTHORIZATION, Constants.BEARER + apmConnectionInfo.getEmAuthToken());
			request.addHeader("Accept", "application/json");
			String payload = getPayload(vertexIdTsMap, attributesMap);
			StringEntity entity = new StringEntity(payload);
			request.setEntity(entity);
			if (client != null) {
				response = client.build().execute(request);
				client.disableAutomaticRetries();
				client.disableConnectionState();
				client.build().close();
			}
		} catch (Exception e) {
			JenkinsPlugInLogger.info(ERRORINCALLUPDATEATTRIBUTEAPI + e.getMessage());
		}

		if (response != null && (Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()
				|| ((response.toString() != null) && response.toString().contains("Multi Status")))) {
			StringBuilder result = new StringBuilder();
			try {
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}

			} catch (IOException e) {
				JenkinsPlugInLogger.info(ERRORINCALLUPDATEATTRIBUTEAPI + e.getMessage());
			}
			JenkinsPlugInLogger.printLogOnConsole(2, "Successfully updated the vertex attributes");
			return true;
		}
		return false;
	}

	private JSONObject createVertexFilterQueryRequestBody(String applicationName) {
		JSONObject andItemsObj = new JSONObject();

		andItemsObj.put("itemType", "attributeFilter");
		andItemsObj.put("attributeName", "applicationName");
		andItemsObj.put("attributeOperator", "IN");
		String[] valuesArr = new String[] { applicationName };
		andItemsObj.put("values", valuesArr);

		JSONArray andItemsArray = new JSONArray();
		andItemsArray.put(andItemsObj);

		JSONObject orItemsObj = new JSONObject();
		orItemsObj.put("andItems", andItemsArray);

		JSONArray orItemsArray = new JSONArray();
		orItemsArray.put(orItemsObj);

		JSONObject bodyObj = new JSONObject();
		bodyObj.put("includeStartPoint", false);
		bodyObj.put("orItems", orItemsArray);

		return bodyObj;

	}

	private CloseableHttpResponse getVertexIds(String applicationName) {
		CloseableHttpResponse response = null;
		String vertexIdsURL = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(vertexIdsURL);
			vertexIdsURL = generateURL(apmConnectionInfo.getEmURL(), Constants.ATTRIBUTEUPDATE);
			httpPost.addHeader(Constants.CONTENTTYPE, Constants.APPLICATION_JSON);
			httpPost.addHeader(Constants.AUTHORIZATION, Constants.BEARER + apmConnectionInfo.getEmAuthToken());
			String body = createVertexFilterQueryRequestBody(applicationName).toString();
			JenkinsPlugInLogger.printLogOnConsole(3, "vertex query " + body);
			StringEntity bodyEntity = new StringEntity(body);
			httpPost.setEntity(bodyEntity);
			response = httpClient.execute(httpPost);
			if (response == null) {
				vertexIdsURL = generateURL(apmConnectionInfo.getEmURL(), Constants.GETVERTEXIDBYNAME);
				HttpGet httpGet = new HttpGet(vertexIdsURL + "?q=attributes.applicationName:" + applicationName);
				httpGet.addHeader(Constants.CONTENTTYPE, Constants.APPLICATION_JSON);
				httpGet.addHeader(Constants.AUTHORIZATION, Constants.BEARER + apmConnectionInfo.getEmAuthToken());
				response = httpClient.execute(httpGet);
			}
		} catch (Exception e) {
			JenkinsPlugInLogger.severe("Error in executing getVertexIds(String applicationName) : " + e.getMessage());

		} finally {
			try {
				httpClient.close();
			} catch (IOException ie) {
				JenkinsPlugInLogger.severe("Error in closing httpClient in getVertexIds() : " + ie.getMessage());

			}
		}

		return response;
	}

	private static boolean parseforVertexIds(JSONArray vertexArray, Map<String, String> attributesMap,
			String applicationName) {
		boolean processStatus = false;
		Map<String, Set<String>> vertexIdTs = new HashMap<>();
		Map<String, Set<String>> vertexIdTsMap = new HashMap<>();
		for (int i = 0; i < vertexArray.length(); i++) {
			JSONObject rowObj = (JSONObject) vertexArray.get(i);
			String id = (String) rowObj.get("id");
			String timestamp = (String) rowObj.get("timestamp");
			if (vertexIdTsMap.containsKey(id)) {
				if (vertexIdTsMap.get(id).add(timestamp)) {
					if (vertexIdTs.containsKey(id)) {
						vertexIdTs.get(id).add(timestamp);
					} else {
						Set<String> tsSet = new HashSet<>();
						tsSet.add(timestamp);
						vertexIdTs.put(id, tsSet);
					}
				}

			} else {
				Set<String> tsSet = new HashSet<>();
				tsSet.add(timestamp);
				vertexIdTsMap.put(id, tsSet);
				vertexIdTs.put(id, tsSet);
			}
		}
		for (Map.Entry<String, Set<String>> vertexid : vertexIdTs.entrySet()) {
			JenkinsPlugInLogger.printLogOnConsole(3, "VertexIDs...." + vertexid.getKey());
		}

		if (vertexIdTs.isEmpty()) {
			JenkinsPlugInLogger.severe("No vertices data is fetched for the application  " + applicationName);
			JenkinsPlugInLogger.printLogOnConsole(2,
					"No vertices data is fetched for the application  " + applicationName);

		} else {
			processStatus = callUpdateVertexAttribute(vertexIdTs, attributesMap);
		}
		return processStatus;
	}

	private boolean readResponse(CloseableHttpResponse response, boolean processStatus,
			Map<String, String> attributesMap, String applicationName) {
		StringBuilder result = new StringBuilder();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			response.close();

			JSONObject vertexJSON = new JSONObject(result.toString());
			if (!vertexJSON.isNull("_embedded")) {
				JSONObject embeddedObject = (JSONObject) vertexJSON.get("_embedded");
				JSONArray vertexArray = embeddedObject.getJSONArray("vertex");

				processStatus = parseforVertexIds(vertexArray, attributesMap, applicationName);

			}
		} catch (IOException e) {
			JenkinsPlugInLogger.severe("Exception while closing the http connection " + e.getMessage());
		}
		return processStatus;
	}

	private boolean getVertexIds(Map<String, String> attributesMap, String applicationName,
			OutputConfiguration outputConfiguration) {

		boolean processStatus = false;
		long startTime = Long.parseLong(outputConfiguration.getCommonPropertyValue("runner.start"));
		long endTime = Long.parseLong(outputConfiguration.getCommonPropertyValue("runner.end"));
		try {
			String[] emDateTime = JenkinsPluginUtility.getEMTimeinDateFormat(startTime, endTime,
					apmConnectionInfo.getEmTimeZone());
			attributesMap.put("loadGeneratorStartTime", emDateTime[0]);
			attributesMap.put("loadGeneratorEndTime", emDateTime[1]);
			applicationName = applicationName.replace(" ", "%20C");
			CloseableHttpResponse response = getVertexIds(applicationName);
			if (response != null && Response.Status.OK.getStatusCode() == response.getStatusLine().getStatusCode()) {

				processStatus = readResponse(response, processStatus, attributesMap, applicationName);

			}
		} catch (Exception e) {
			JenkinsPlugInLogger.printLogOnConsole(2, e.getMessage());
		}
		return processStatus;
	}

	public boolean updateAttributeOfVertex(boolean isBuildSuccess) {
		JenkinsPlugInLogger.info("Entered updateAttributeOfVertex method");
		String buildStatus = isBuildSuccess ? "SUCCESS" : "FAIL";
		Map<String, String> attributesMap = new HashMap<>();
		OutputConfiguration outputConfiguration = comparisonMetadata.getOutputConfiguration();
		attributesMap.put("currentBuildNumber",
				outputConfiguration.getCommonPropertyValue(Constants.JENKINSCURRENTBUILD));
		attributesMap.put("benchMarkBuildNumber",
				outputConfiguration.getCommonPropertyValue(Constants.JENKINSBENCHMARKBUILD));

		if (outputConfiguration.getSCMRepoAttribValue(Constants.JENKINSCURRENTBUILDSCMREPOPARAMS) != null) {
			for (Map.Entry<String, String> scmRepoEntry : outputConfiguration
					.getSCMRepoAttribValue(Constants.JENKINSCURRENTBUILDSCMREPOPARAMS).entrySet()) {
				attributesMap.put(scmRepoEntry.getKey(), scmRepoEntry.getValue());
			}
		}

		JenkinsPlugInLogger.printLogOnConsole(3, "...vertex update..git attribs. : "
				+ outputConfiguration.getSCMRepoAttribValue(Constants.JENKINSCURRENTBUILDSCMREPOPARAMS));
		if (outputConfiguration.getSCMRepoAttribValue(Constants.JENKINSBENCHMARKBUILDSCMREPOPARAMS) != null) {
			for (Map.Entry<String, String> scmRepoEntry : outputConfiguration
					.getSCMRepoAttribValue(Constants.JENKINSBENCHMARKBUILDSCMREPOPARAMS).entrySet()) {
				attributesMap.put(scmRepoEntry.getKey(), scmRepoEntry.getValue());
			}
		}
		attributesMap.put("buildStatus", buildStatus);
		attributesMap.put("loadGeneratorName", outputConfiguration.getCommonPropertyValue(Constants.LOADGENERATORNAME));

		String applicationName = comparisonMetadata.getCommonPropertyValue(Constants.APPLICATIONNAME);
		return getVertexIds(attributesMap, applicationName, outputConfiguration);

	}

	private static HttpClientBuilder ignoreSSLClient(HttpClientBuilder client) {

		JenkinsPlugInLogger.finest("IgnoreSSLClient 1 ..");

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
				// Do nothing
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
				// Do nothing
			}
		} };

		JenkinsPlugInLogger.finest("IgnoreSSLClient 2..");

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			javax.net.ssl.HostnameVerifier hnv = new javax.net.ssl.HostnameVerifier() {

				public boolean verify(String arg0, SSLSession session) {
					boolean isVerified = false;
					if (session != null)
						isVerified = true;
					return isVerified;
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
