package com.ca.apm.jenkins.core.helper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.APMConnectionInfo;
import com.ca.apm.jenkins.core.entity.BuildPerformanceData;
import com.ca.apm.jenkins.core.entity.MetricPerformanceData;
import com.ca.apm.jenkins.core.entity.TimeSliceValue;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;

/**
 * This utility helper class helps you to collect metrics information from APM
 * based on the agent specifier, metric specifier, metric name and the start
 * time and end time
 * 
 * @author Avinash Chandwani
 *
 */
public class MetricDataHelper {

	private static APMConnectionInfo apmConnectionInfo;
	private static String metricClamp;
	private static String applicationName;
	private static String comparisonStrategyName;

	private MetricDataHelper() {
		super();
	}

	public static void setAPMConnectionInfo(APMConnectionInfo apmConnectionInfo) {
		MetricDataHelper.apmConnectionInfo = apmConnectionInfo;
	}

	public static void setMetricClamp(String metricClamp) {
		MetricDataHelper.metricClamp = metricClamp;
	}

	public static void setApplicationName(String applicationName) {
		MetricDataHelper.applicationName = applicationName;
	}

	private static String generateURL(String emURL, String relativeURL) {
		return emURL + relativeURL;
	}

	private static String prepareSqlRequestBody(String agentSpecifier, String metricSpecifier, long startTime,
			long endTime) {
		String query = null;
		JSONObject requestBodyJSON = new JSONObject();
		query = "SELECT domain_name, agent_host,agent_process, agent_name, metric_path,  metric_attribute, ts, min_value, max_value, agg_value, value_count, frequency"
				+ " FROM metric_data WHERE ts >= " + startTime + " AND ts <= " + endTime
				+ " AND agent_name like_regex '" + agentSpecifier + "' AND metric_path like_regex '" + metricSpecifier
				+ "' limit " + Integer.parseInt(metricClamp);
		// JenkinsPlugInLogger.printLogOnConsole(1, "query..."+query);
		requestBodyJSON.put("query", query);
		return requestBodyJSON.toString();
	}

	/**
	 * This method is to parse the SQL API response from query API Checked with
	 * EM team, the records from /query API are not in any ordering Hence using
	 * a map to populate and at the end popluating it to prepare
	 * BuildPerformanceData Object
	 * 
	 * @param metricResponseJSON
	 * @return
	 */
	private static BuildPerformanceData parseSqlApiResponse(JSONObject metricResponseJSON) {
		BuildPerformanceData buildPerformanceData = new BuildPerformanceData();
		JSONArray rowArrays = (JSONArray) metricResponseJSON.get("rows");
		Map<String, MetricPerformanceData> performanceData = new HashMap<String, MetricPerformanceData>();
		for (int i = 0; i < rowArrays.length(); i++) {

			JSONArray rowArray = (JSONArray) rowArrays.get(i);
			String domainName = rowArray.getString(0);
			String agentHost = rowArray.getString(1);
			String agentProcess = rowArray.getString(2);
			String agentName = rowArray.getString(3);
			String applicationPath = rowArray.getString(4);
			String metricPath = domainName + Constants.pipeSeperator + agentHost + Constants.pipeSeperator
					+ agentProcess + Constants.pipeSeperator + agentName + Constants.pipeSeperator + applicationPath;
			MetricPerformanceData metricPerformanceData = null;
			double value = rowArray.getDouble(9);
			double max = rowArray.getDouble(8);
			double min = rowArray.getDouble(7);
			int count = rowArray.getInt(10);
			double frequency = rowArray.getDouble(11);
			TimeSliceValue timeSliceValue = new TimeSliceValue(value, max, min, count, frequency);
			if (performanceData.containsKey(metricPath)) {
				metricPerformanceData = performanceData.get(metricPath);
			} else {
				metricPerformanceData = new MetricPerformanceData();
				metricPerformanceData.setMetricPath(metricPath);
				performanceData.put(metricPath, metricPerformanceData);
			}

			metricPerformanceData.addToTimeSliceValue(timeSliceValue);
		}
		for (MetricPerformanceData metricPerformanceData : performanceData.values()) {
			buildPerformanceData.addToMetricPerformanceData(metricPerformanceData);
		}
		return buildPerformanceData;
	}

	/**
	 * This method return all the performance metrics for a given metric name
	 * for given agent specifier, metric specifier and the time range selected
	 * 
	 * @param agentSpecifier
	 *            A regex for agent specifier
	 * @param metricSpecifier
	 *            A regex for metric specifier The metric_path will be
	 *            constructed as .*metricRegex.*metricName
	 * @param build
	 *            Object BuildInfo
	 * @return it returns BuildPerformanceData wrapper object for a particular
	 *         build's performance metric-data from EM
	 * @throws BuildComparatorException
	 *             Throws when any error occurs during execution, proper message
	 *             is supplied to the exception
	 */
	public static BuildPerformanceData getMetricData(String agentSpecifier, String metricSpecifier, String strategyName,
			BuildInfo build) throws BuildComparatorException, BuildExecutionException {
		if (agentSpecifier == null || metricSpecifier == null) {
			throw new BuildComparatorException(
					"Mandatory parameters to the query are null. Please enter non null values for the arguments");
		}
		JenkinsPlugInLogger.finest("Entering fetchBatchMetricDataFromEM method");
		CloseableHttpClient httpClient = HttpClients.createDefault();
		comparisonStrategyName = strategyName;
		long startTime = build.getStartTime();
		long endTime = build.getEndTime();
		JenkinsPlugInLogger.fine("MetricDataHelper input query ->" + "agentSpecifier = " + agentSpecifier
				+ ", metricSpecifier=" + metricSpecifier + ", startTime=" + startTime + ",endTime=" + endTime);
		String body = prepareSqlRequestBody(agentSpecifier, metricSpecifier, startTime, endTime);
		JenkinsPlugInLogger.fine("Request Body = " + body);
		StringEntity bodyEntity;
		JSONObject metricDataJson = null;
		HttpPost httpPost = new HttpPost(generateURL(apmConnectionInfo.getEmURL(), Constants.queryMetricDataAPI));
		httpPost.addHeader(Constants.AUTHORIZATION, Constants.BEARER + apmConnectionInfo.getEmAuthToken());
		httpPost.addHeader(Constants.ContentType, Constants.APPLICATION_JSON);
		CloseableHttpResponse metricDataResponse = null;
		try {
			bodyEntity = new StringEntity(body);
			httpPost.setEntity(bodyEntity);
			metricDataResponse = httpClient.execute(httpPost);
			if (metricDataResponse.getStatusLine().getReasonPhrase().contains("BAD_REQUEST")) {
				body = body.substring(0, body.indexOf("limit") - 1).concat("\"}");
				JenkinsPlugInLogger.fine("Request Body = " + body);
				bodyEntity = new StringEntity(body);
				httpPost.setEntity(bodyEntity);
				metricDataResponse = httpClient.execute(httpPost);
			}

		} catch (UnsupportedEncodingException e1) {
			JenkinsPlugInLogger.severe("Error while fetching BuildPerformanceData ->" + e1.getMessage(), e1);
		} catch (ClientProtocolException e1) {
			JenkinsPlugInLogger.severe("Error while fetching BuildPerformanceData ->" + e1.getMessage(), e1);
		} catch (IOException e1) {
			if (e1.getCause().toString().contains("Connection refused")) {
				int apmHostNameIndex = apmConnectionInfo.getEmURL().indexOf("//") + 2;
				if (e1.getMessage().contains(apmConnectionInfo.getEmURL().substring(apmHostNameIndex,
						apmConnectionInfo.getEmURL().lastIndexOf(':')))) {
					JenkinsPlugInLogger.severe("Error while fetching BuildPerformanceData ->" + e1.getMessage(), e1);
					throw new BuildExecutionException(e1.getMessage().substring(0, e1.getMessage().lastIndexOf(':')));
				}
			} else {
				JenkinsPlugInLogger.severe("Error while fetching BuildPerformanceData ->" + e1.getMessage(), e1);
			}
		}
		if (metricDataResponse == null) {
			JenkinsPlugInLogger.severe("No response from APM REST API, hence returning null");
			return null;
		}
		if (Response.Status.OK.getStatusCode() == metricDataResponse.getStatusLine().getStatusCode()) {
			HttpEntity metricDataEntity = metricDataResponse.getEntity();
			ByteArrayOutputStream metricDataOs = new ByteArrayOutputStream();
			try {
				metricDataEntity.writeTo(metricDataOs);
			} catch (IOException e) {
				JenkinsPlugInLogger.severe("Error while fetching BuildPerformanceData ->" + e.getMessage(), e);
			}
			String metricDataContents = new String(metricDataOs.toByteArray());
			metricDataJson = new JSONObject(metricDataContents);
			JenkinsPlugInLogger.fine("Response JSON Body = " + metricDataJson);
		} else {
			if (metricDataResponse.getStatusLine().getReasonPhrase().contains("Unauthorized")) {
				throw new BuildExecutionException(
						metricDataResponse.getStatusLine().getReasonPhrase() + " APM Credentials");
			}
			int statusCode = metricDataResponse.getStatusLine().getStatusCode();
			JenkinsPlugInLogger.severe("Metric data capture from CA-APM failed with status code " + statusCode);
			JenkinsPlugInLogger.severe("Detailed Response from EM is  " + metricDataResponse.toString());

			throw new BuildComparatorException(
					"Error occured while fetching metrics from CA-APM with response code ->" + statusCode);
		}

		JenkinsPlugInLogger.finest("Exiting fetchBatchMetricDataFromEM method");
		BuildPerformanceData buildPerformanceData = parseSqlApiResponse(metricDataJson);
		if (buildPerformanceData.getMetricData().isEmpty()) {
			JenkinsPlugInLogger.severe("No metric data available in APM with the given arguments ->" + agentSpecifier
					+ "," + metricSpecifier);
		}
		return buildPerformanceData;
	}

	@SuppressWarnings("unused")
	private static BuildPerformanceData parseMetricResponse(JSONObject metricResponseJSON, String batchId) {
		JSONObject successfulBatches = (JSONObject) metricResponseJSON.get("successfulBatches");
		JSONArray batchInformation = (JSONArray) successfulBatches.get(batchId);
		BuildPerformanceData performanceData = new BuildPerformanceData();
		for (int i = 0; i < batchInformation.length(); i++) {
			JSONObject dataChunksArray = (JSONObject) batchInformation.get(i);
			JSONArray records = (JSONArray) dataChunksArray.get("dataChunks");
			MetricPerformanceData metricPerformanceData = new MetricPerformanceData();
			String metricPath = dataChunksArray.getString("id");
			metricPerformanceData.setMetricPath(metricPath);
			performanceData.getMetricData().add(metricPerformanceData);
			for (int j = 0; j < records.length(); j++) {
				JSONObject record = (JSONObject) records.get(j);
				JSONArray valuesArray = (JSONArray) record.get("values");
				JSONArray minArray = (JSONArray) record.get("mins");
				JSONArray maxArray = (JSONArray) record.get("maxes");
				JSONArray countArray = (JSONArray) record.get("counts");
				for (int k = 0; k < valuesArray.length(); k++) {
					double value = valuesArray.getDouble(k);
					double max = maxArray.getDouble(k);
					double min = minArray.getDouble(k);
					int count = countArray.getInt(k);
					double freqncy = 15000;
					TimeSliceValue sliceValue = new TimeSliceValue(value, max, min, count, freqncy);
					metricPerformanceData.addToTimeSliceValue(sliceValue);
				}
			}
		}
		return performanceData;
	}

	@SuppressWarnings("unused")
	private static JSONObject createMetricDataQueryRequestBody(String agentSpec, String metricRegex, long startTime,
			long endTime) throws JSONException {
		Collection<JSONObject> queries = new LinkedList<JSONObject>();

		String metricSpec = metricRegex;
		JSONObject query = new JSONObject();
		JSONObject agentSpecifier = new JSONObject();
		agentSpecifier.put("type", "REGEX");
		agentSpecifier.put("specifier", agentSpec);
		query.put("agentSpecifier", agentSpecifier);
		JSONObject metricSpecifier = new JSONObject();
		metricSpecifier.put("type", "REGEX");
		metricSpecifier.put("specifier", metricSpec);
		query.put("metricSpecifier", metricSpecifier);
		queries.add(query);
		JSONArray momFilterArray = new JSONArray(new JSONObject[] { null });
		query.put("momFilter", momFilterArray);
		JSONObject batchedMetrics = new JSONObject();
		batchedMetrics.put("metricQueries", new JSONArray(queries));

		JSONObject body = new JSONObject();
		body.put("batchedMetrics", batchedMetrics);
		body.put("batchId", 7);

		long duration = endTime - startTime;

		JSONObject queryRange = new JSONObject();
		queryRange.put("frequency", 15000);
		queryRange.put("rangeSize", duration);

		queryRange.put("endTime", JenkinsPluginUtility.getGMTTime(endTime));
		batchedMetrics.put("queryRange", queryRange);
		batchedMetrics.put("aggregate", "false");
		// batchedMetrics.put("clamp", Integer.parseInt(metricClamp));
		return body;
	}

}