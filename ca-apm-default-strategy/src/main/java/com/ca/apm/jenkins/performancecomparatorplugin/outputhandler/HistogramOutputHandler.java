package com.ca.apm.jenkins.performancecomparatorplugin.outputhandler;

import java.io.File;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ca.apm.jenkins.api.OutputHandler;
import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.BuildPerformanceData;
import com.ca.apm.jenkins.core.entity.ComparisonMetadata;
import com.ca.apm.jenkins.core.entity.JenkinsAMChart;
import com.ca.apm.jenkins.core.helper.FileHelper;
import com.ca.apm.jenkins.core.helper.FormulaHelper;
import com.ca.apm.jenkins.core.helper.MetricDataHelper;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;

/**
 * An implementation of Output Handler to render the multiple builds results,
 * metric wise into histogram Charts
 *
 * @author Avinash Chandwani
 */
@SuppressWarnings("rawtypes")
public class HistogramOutputHandler implements OutputHandler<StrategyResult> {

	private ComparisonMetadata comparisonMetadata;
	private int currentBuildNumber;
	private OutputConfiguration outputConfiguration;
	private String workspaceFolder = null;
	private String jobName = null;
	private LinkedHashMap<String, LinkedHashMap<BuildInfo, Double>> metrictoBuildAvgValMap = new LinkedHashMap();
	private static final String DATAPROVIDER = "dataProvider";
	private static final String BUILDNUMBER = "BuildNumber";

	public void setComparisonMetadata(ComparisonMetadata comparisonMetadata) {
		this.comparisonMetadata = comparisonMetadata;
	}

	public void setOutputConfiguration(OutputConfiguration outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

	/**
	 * Entry method to generate buildtoBuildChart
	 *
	 * @param strategyResults
	 * @throws BuildComparatorException
	 * @throws BuildExecutionException
	 */
	public void publishOutput(List<StrategyResult> strategyResults) throws BuildExecutionException {
		List<BuildInfo> histogramBuildInfoList = outputConfiguration.getHistogramBuildInfoList();
		workspaceFolder = outputConfiguration.getCommonPropertyValue(Constants.WORKSPACEDIRECTORY);
		jobName = outputConfiguration.getCommonPropertyValue(Constants.JENKINSJOBNAME);
		getMetricData(histogramBuildInfoList, strategyResults);
	}

	/**
	 * The method to pull metrics from EM and builds a map for each metric path
	 * to build number and average value
	 *
	 * @param strategyResults
	 * @throws BuildExecutionException
	 */
	private void getMetricData(List<BuildInfo> buildInfoList, List<StrategyResult> strategyResults)
			throws BuildExecutionException {
		Map<String, StrategyConfiguration> comparisonStrategies = comparisonMetadata.getStrategiesInfo()
				.getComparisonStrategiesInfo();
		Map<BuildInfo, Map<String, Double>> strategyWiseBuildtoMetricAvgValMap = null;
		Set<String> metricPathSet = null;
		currentBuildNumber = buildInfoList.get(0).getNumber();
		for (int i = 0; i < strategyResults.size(); i++) {
			metricPathSet = new HashSet<>();
			strategyWiseBuildtoMetricAvgValMap = new LinkedHashMap<>();
			String strategyName = strategyResults.get(i).getStrategyName();
			StrategyConfiguration strategyConfiguration = comparisonStrategies.get(strategyName);
			String metricSpecifier = strategyConfiguration
					.getPropertyValue(strategyName + "." + Constants.METRICSPECIFIER);
			List<String> agentSpecifiers = strategyConfiguration.getAgentSpecifiers();
			for (String agentSpecifier : agentSpecifiers) {

				for (int j = 0; j < buildInfoList.size(); j++) {
					try {
						BuildPerformanceData buildPerformanceData = MetricDataHelper.getMetricData(agentSpecifier,
								metricSpecifier, buildInfoList.get(j));
						Map<String, Double> metricAverageValuesMap = FormulaHelper
								.getAverageValues(buildPerformanceData);
						for (String metricPath : metricAverageValuesMap.keySet()) {
							metricPathSet.add(metricPath);
						}
						strategyWiseBuildtoMetricAvgValMap.put(buildInfoList.get(j), metricAverageValuesMap);
					} catch (BuildComparatorException e) {
						JenkinsPlugInLogger.severe("An error has occured while collecting performance metrics for "
								+ strategyName + "from APM-> for agentSpecifier=" + agentSpecifier
								+ ",metricSpecifier =" + metricSpecifier + e.getMessage(), e);
					}
				}
			}

			getMetrictoBuildAvgValMap(strategyWiseBuildtoMetricAvgValMap, metricPathSet);
		}
		produceChartOutput(metrictoBuildAvgValMap);
	}

	/**
	 * The method to build a map for each metric path to build number and
	 * average value map
	 *
	 * @param strategyWiseBuildtoMetricAvgValMap
	 * @param metricPathSet
	 */
	private void getMetrictoBuildAvgValMap(Map<BuildInfo, Map<String, Double>> strategyWiseBuildtoMetricAvgValMap,
			Set<String> metricPathSet) {
		LinkedHashMap<BuildInfo, Double> buildtoAvgValMap = null;
		Iterator<String> it = metricPathSet.iterator();
		String metricPath = null;
		while (it.hasNext()) {
			metricPath = it.next();
			buildtoAvgValMap = new LinkedHashMap<>();
			for (Map.Entry<BuildInfo, Map<String, Double>> buildInfoEntry : strategyWiseBuildtoMetricAvgValMap
					.entrySet()) {
				Map<String, Double> metrictoAvgValMap = strategyWiseBuildtoMetricAvgValMap.get(buildInfoEntry.getKey());
				if (metrictoAvgValMap.containsKey(metricPath)) {
					buildtoAvgValMap.put(buildInfoEntry.getKey(), metrictoAvgValMap.get(metricPath));
				}
			}
			metrictoBuildAvgValMap.put(metricPath, buildtoAvgValMap);
		}
	}

	/**
	 * The method to produce chart
	 *
	 * @param metricPathSet
	 */
	@SuppressWarnings("rawtypes")
	private void produceChartOutput(Map<String, LinkedHashMap<BuildInfo, Double>> strategyWiseMetrictoBuildAvgValMap) {
		List<JenkinsAMChart> metricPathsChart = null;
		String emURL = outputConfiguration.getCommonPropertyValue(Constants.EMURL);
		String startTimeMillis = outputConfiguration.getCommonPropertyValue("runner.start");
		String endTimeMillis = outputConfiguration.getCommonPropertyValue("runner.end");
		String emWebViewPort = outputConfiguration.getCommonPropertyValue(Constants.EMWEBVIEWPORT);
		String appMapURL = null;
		if(Character.isDigit(emURL.charAt(emURL.lastIndexOf(':')+1))){
			 appMapURL = emURL.replace(emURL.substring(emURL.lastIndexOf(':') + 1, emURL.length() - 1), emWebViewPort)
					+ Constants.EMEXPVIEWURLPOSTFIX + "&ts1=" + startTimeMillis + "&ts2=" + endTimeMillis;
		}else{
			 appMapURL = emURL+"apm/appmap/"
					+ Constants.EMEXPVIEWURLPOSTFIX + "&ts1=" + startTimeMillis + "&ts2=" + endTimeMillis;
		}
		metricPathsChart = getChartsForMetricPaths(strategyWiseMetrictoBuildAvgValMap);
		String htmlOutput = null;
		htmlOutput = applyToVelocityTemplate(appMapURL, metricPathsChart);
		FileHelper
				.exportOutputToFile(
						workspaceFolder + File.separator + jobName + File.separator + currentBuildNumber
								+ File.separator + "chartOutput" + File.separator + "output",
						"buildtoBuild-chart-output.html", htmlOutput);
	}

	private static String applyToVelocityTemplate(String appMapURL, List<JenkinsAMChart> strategyCharts) {
		VelocityEngine ve = new VelocityEngine();
		StringWriter writer = new StringWriter();
		ve.setProperty("resource.loader", "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();
		Template t = ve.getTemplate("amHistogramChartsReport.vm");
		VelocityContext context = new VelocityContext();
		context.put("strategyCharts", strategyCharts);
		context.put("url", appMapURL);
		t.merge(context, writer);
		return writer.toString();
	}

	private static List<JenkinsAMChart> getChartsForMetricPaths(
			Map<String, LinkedHashMap<BuildInfo, Double>> strategyWiseMetrictoBuildAvgValMap) {
		List<JenkinsAMChart> amCharts = null;
		amCharts = new LinkedList<>();
		JenkinsAMChart amChart = null;
		int divId = 0;

		for (Map.Entry<String, LinkedHashMap<BuildInfo, Double>> metricPathEntry : strategyWiseMetrictoBuildAvgValMap
				.entrySet()) {

			JSONObject amChartJSON = generateAMChartsJSON(metricPathEntry.getKey(),
					strategyWiseMetrictoBuildAvgValMap.get(metricPathEntry.getKey()));
			if (!amChartJSON.get(DATAPROVIDER).equals("empty")) {
				amChart = new JenkinsAMChart();
				amChart.setChartJSONObject(amChartJSON);
				amChart.setDivId("div" + divId);
				amCharts.add(amChart);
				divId++;
			}
		}

		return amCharts;
	}

	private static JSONObject generateAMChartsJSON(String metricPath, Map<BuildInfo, Double> buildAvgValues) {

		String metricName = metricPath.substring(metricPath.lastIndexOf('|') + 1);
		JSONObject amCharts = new JSONObject();
		amCharts.put("type", "serial");
		amCharts.put("theme", "light");

		JSONArray dataProviderArray = new JSONArray();
		boolean isDataSet = false;
		JSONObject recordObj = null;
		for (Map.Entry<BuildInfo, Double> buildInfoEntry : buildAvgValues.entrySet()) {

			if (buildAvgValues.get(buildInfoEntry.getKey()) != 0) {
				recordObj = new JSONObject();
				isDataSet = true;
				recordObj.put(BUILDNUMBER, buildInfoEntry.getKey().getNumber());
				recordObj.put("AverageValue", buildAvgValues.get(buildInfoEntry.getKey()));
				if (!buildInfoEntry.getKey().getStatus().equalsIgnoreCase("SUCCESS")) {
					recordObj.put("color", "#ff0000");
				}
				dataProviderArray.put(recordObj);
			}
		}
		if (isDataSet) {
			amCharts.put(DATAPROVIDER, dataProviderArray);
		} else {
			amCharts.put(DATAPROVIDER, "empty");
		}

		JSONObject graphobj = new JSONObject();

		JSONObject valueAxis = new JSONObject();
		valueAxis.put("id", "ValueAxis-1");
		valueAxis.put("title", metricName);
		valueAxis.put("gridThickness", 0);

		JSONObject categoryAxis = new JSONObject();
		categoryAxis.put("startOnAxis", false);
		categoryAxis.put("title", BUILDNUMBER);
		categoryAxis.put("gridPosition", "start");
		categoryAxis.put("gridThickness", 0);
		categoryAxis.put("gridAlpha", 0);
		amCharts.put("categoryAxis", categoryAxis);

		JSONArray valueAxesArray = new JSONArray();
		valueAxesArray.put(valueAxis);
		amCharts.put("valueAxes", valueAxesArray);

		JSONObject titleObj = new JSONObject();
		titleObj.put("id", "Title-1");
		titleObj.put("text", metricPath);
		JSONArray titlesArray = new JSONArray();
		titlesArray.put(titleObj);

		amCharts.put("titles", titlesArray);

		amCharts.put("valueAxes", valueAxesArray);

		graphobj.put("fillAlphas", 0.8);
		graphobj.put("lineAlpha", 0.2);
		graphobj.put("fillColorsField", "color");
		graphobj.put("type", "column");
		// value field y-axis
		graphobj.put("valueField", "AverageValue");

		JSONArray graphArrayObj = new JSONArray();
		graphArrayObj.put(graphobj);

		amCharts.put("graphs", graphArrayObj);
		// category field x-axis
		amCharts.put("categoryField", BUILDNUMBER);

		JSONObject chartCursorobj = new JSONObject();
		chartCursorobj.put("categoryBalloonEnabled", true);
		chartCursorobj.put("cursorAlpha", 0);
		chartCursorobj.put("zoomable", false);
		amCharts.put("gridAboveGraphs", false);
		amCharts.put("chartCursor", chartCursorobj);

		return amCharts;
	}
}
