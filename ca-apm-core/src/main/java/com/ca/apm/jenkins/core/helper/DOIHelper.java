package com.ca.apm.jenkins.core.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;

public class DOIHelper {

	private ComparisonMetadata comparisonMetadata;

	public DOIHelper(ComparisonMetadata comparisonMetadata) {
		this.comparisonMetadata = comparisonMetadata;
	}

	public void sendBuildChangeEventtoDOI() {
		String jarvisEndpoint = comparisonMetadata.getCommonPropertyValue(Constants.JARVISENDPOINT);
		HttpPost httpPost = new HttpPost(jarvisEndpoint);
		httpPost.addHeader(Constants.CONTENTTYPE, Constants.APPLICATION_JSON);
		JenkinsPlugInLogger.info("DOI httpPost.getRequestLine().." + httpPost.getRequestLine());

		CloseableHttpResponse changeEventOIResponse = null;

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

			StringEntity bodyEntity;
			for(Map.Entry<String, String> entry : comparisonMetadata.getDoiAppsToHostname().entrySet()){
				String body = createBuildChangeEventDataRequestBody(entry.getKey(), entry.getValue()).toString();
				bodyEntity = new StringEntity(body);
				httpPost.setEntity(bodyEntity);
				changeEventOIResponse = httpClient.execute(httpPost);
				InputStream is = changeEventOIResponse.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder out = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					out.append(line);
				}
				reader.close();
			}
		} catch (Exception ex) {
			JenkinsPlugInLogger.severe("Error while pushing the build change event data to OI ->" + ex.getMessage(),
					ex);
		}
	}

	@SuppressWarnings("unused")
	private JSONObject createBuildChangeEventDataRequestBody(String applicationName, String applicationHost) {
		Map<String, String> scmRepoVarsMap = null;
		OutputConfiguration outputConfiguration = comparisonMetadata.getOutputConfiguration();
		StringBuilder message = new StringBuilder();
		if (outputConfiguration.getSCMRepoAttribValue(Constants.JENKINSCURRENTBUILDSCMREPOPARAMS) != null)
			scmRepoVarsMap = outputConfiguration.getSCMRepoAttribValue(Constants.JENKINSCURRENTBUILDSCMREPOPARAMS);
		String currentBuildNumber = outputConfiguration.getCommonPropertyValue(Constants.JENKINSCURRENTBUILD);
		JenkinsPlugInLogger.info(" SCMRepoVarsMap in DOIHELPER : " + scmRepoVarsMap);

		String doiTenantID = comparisonMetadata.getCommonPropertyValue(Constants.DOITENANTID);
		String doiTimeZone = comparisonMetadata.getCommonPropertyValue(Constants.DOITIMEZONE);
		
		String doiTimeStamp = JenkinsPluginUtility.getOITimeinDateFormat(doiTimeZone);
		
		String event_unique_id = "Jenkins-" + UUID.randomUUID().toString() + "-" + (new Date().getTime());
		JenkinsPlugInLogger.info("event_unique_id   : " + event_unique_id);
		JSONObject header = new JSONObject();
		header.put("product_id", "ao");
		header.put("tenant_id", doiTenantID);
		header.put("doc_type_id", "itoa_events_change_custom");
		header.put("doc_type_version", "1");

		JSONObject bodyObj = new JSONObject();
		bodyObj.put("change_type", "JenkinsBuildChangeEvent");		
		bodyObj.put("event_unique_id", event_unique_id);
		bodyObj.put("host", applicationHost);
		bodyObj.put("timestamp", doiTimeStamp);
		bodyObj.put("ci_unique_id", "100892");
		bodyObj.put("severity", "information");
		bodyObj.put("summary", "Change event is generated with Jenkins build");
		bodyObj.put("product", "Jenkins");
		message.append("BuildNumber : ").append(currentBuildNumber).append(", ").append("ApplicationName : ")
				.append(applicationName);
		if (scmRepoVarsMap != null)
			for (Entry<String, String> entry : scmRepoVarsMap.entrySet()) {
				message.append(", ").append(entry.getKey()).append(" : ").append(entry.getValue());
			}
		bodyObj.put("message", message);
		bodyObj.put("status", "NEW");

		JSONArray bodyArray = new JSONArray();
		bodyArray.put(bodyObj);

		JSONObject documentObj = new JSONObject();
		documentObj.put("header", header);
		documentObj.put("body", bodyArray);

		JSONObject buildChangeEventJsonObj = new JSONObject();
		JSONArray documents = new JSONArray();
		documents.put(documentObj);
		buildChangeEventJsonObj.put("documents", documents);
	
		JenkinsPlugInLogger.info("Metric JSON data..str...." + buildChangeEventJsonObj);
		return buildChangeEventJsonObj;
	}

}
