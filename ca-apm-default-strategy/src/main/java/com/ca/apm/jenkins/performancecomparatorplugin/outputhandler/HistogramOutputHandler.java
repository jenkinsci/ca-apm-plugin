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
 * An implementation of Output Handler to render the multiple builds results, metric wise into
 * histogram Charts
 *
 * @author Avinash Chandwani
 */
@SuppressWarnings("rawtypes")
public class HistogramOutputHandler implements OutputHandler<StrategyResult> {

  private static final String HISTOGRAM = "HistogramOutputHandler";
  private ComparisonMetadata comparisonMetadata;

  private int currentBuildNumber;
  private OutputConfiguration outputConfiguration;
  private String workspaceFolder = null;
  private String jobName = null;

  private LinkedHashMap<String, LinkedHashMap<String, Double>> metrictoBuildAvgValMap =
      new LinkedHashMap<String, LinkedHashMap<String, Double>>();

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
  public void publishOutput(List<StrategyResult> strategyResults)
      throws BuildComparatorException, BuildExecutionException {
    List<BuildInfo> histogramBuildInfoList = outputConfiguration.getHistogramBuildInfoList();
    workspaceFolder = outputConfiguration.getCommonPropertyValue(Constants.workSpaceDirectory);
    jobName = outputConfiguration.getCommonPropertyValue(Constants.jenkinsJobName);
    getMetricData(histogramBuildInfoList, strategyResults);
  }

  /**
   * The method to pull metrics from EM and builds a map for each metric path to build number and
   * average value
   *
   * @param strategyResults
   * @throws BuildExecutionException
   */
  private void getMetricData(List<BuildInfo> buildInfoList, List<StrategyResult> strategyResults)
      throws BuildExecutionException {
    Map<String, StrategyConfiguration> comparisonStrategies =
        comparisonMetadata.getStrategiesInfo().getComparisonStrategiesInfo();
    Map<String, Map<String, Double>> strategyWiseBuildtoMetricAvgValMap = null;
    Set<String> metricPathSet = null;
    currentBuildNumber = buildInfoList.get(0).getNumber();
    for (int i = 0; i < strategyResults.size(); i++) {
      metricPathSet = new HashSet<String>();
      strategyWiseBuildtoMetricAvgValMap = new LinkedHashMap<String, Map<String, Double>>();
      String strategyName = strategyResults.get(i).getStrategyName();
      StrategyConfiguration strategyConfiguration = comparisonStrategies.get(strategyName);
      String metricSpecifier =
          strategyConfiguration.getPropertyValue(strategyName + "." + Constants.metricSpecifier);
      List<String> agentSpecifiers = strategyConfiguration.getAgentSpecifiers();
      for (String agentSpecifier : agentSpecifiers) {

        for (int j = 0; j < buildInfoList.size(); j++) {
          try {
            BuildPerformanceData buildPerformanceData =
                MetricDataHelper.getMetricData(
                    agentSpecifier, metricSpecifier, "HISTOGRAM", buildInfoList.get(j));
            Map<String, Double> metricAverageValuesMap =
                FormulaHelper.getAverageValues(buildPerformanceData);
            for (String metricPath : metricAverageValuesMap.keySet()) {
              metricPathSet.add(metricPath);
            }
            strategyWiseBuildtoMetricAvgValMap.put(
                String.valueOf(buildInfoList.get(j).getNumber()), metricAverageValuesMap);
          } catch (BuildComparatorException e) {
            JenkinsPlugInLogger.severe(
                "An error has occured while collecting performance metrics for "
                    + strategyName
                    + "from APM-> for agentSpecifier="
                    + agentSpecifier
                    + ",metricSpecifier ="
                    + metricSpecifier
                    + e.getMessage(),
                e);
          }
        }
      }

      getMetrictoBuildAvgValMap(strategyWiseBuildtoMetricAvgValMap, metricPathSet);
    }
    produceChartOutput(metrictoBuildAvgValMap);
  }

  /**
   * The method to build a map for each metric path to build number and average value map
   *
   * @param strategyWiseBuildtoMetricAvgValMap
   * @param metricPathSet
   */
  private void getMetrictoBuildAvgValMap(
      Map<String, Map<String, Double>> strategyWiseBuildtoMetricAvgValMap,
      Set<String> metricPathSet) {
    LinkedHashMap<String, Double> buildtoAvgValMap = null;
    Iterator<String> it = metricPathSet.iterator();
    String metricPath = null;
    while (it.hasNext()) {
      metricPath = it.next();
      buildtoAvgValMap = new LinkedHashMap<String, Double>();
      for (String buildNumber : strategyWiseBuildtoMetricAvgValMap.keySet()) {
        Map<String, Double> metrictoAvgValMap = strategyWiseBuildtoMetricAvgValMap.get(buildNumber);
        if (metrictoAvgValMap.containsKey(metricPath)) {
          buildtoAvgValMap.put(buildNumber, metrictoAvgValMap.get(metricPath));
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
  private void produceChartOutput(
      Map<String, LinkedHashMap<String, Double>> strategyWiseMetrictoBuildAvgValMap) {
    List<JenkinsAMChart> metricPathsChart = null;
    String emURL = outputConfiguration.getCommonPropertyValue(Constants.emURL);
    String startTimeMillis = outputConfiguration.getCommonPropertyValue("runner.start");
    String endTimeMillis = outputConfiguration.getCommonPropertyValue("runner.end");
    String emWebViewPort = outputConfiguration.getCommonPropertyValue(Constants.emWebViewPort);
    String appMapURL = emURL.replace(emURL.substring(emURL.lastIndexOf(':')+1, emURL.length()-1),emWebViewPort) + Constants.emExpViewURLPostfix + "&ts1=" + startTimeMillis + "&ts2=" + endTimeMillis;
   // String appMapURL = outputConfiguration.getCommonPropertyValue(Constants.atcViewURL);
    metricPathsChart = getChartsForMetricPaths(strategyWiseMetrictoBuildAvgValMap);
    String htmlOutput = null;
    htmlOutput = applyToVelocityTemplate(appMapURL, metricPathsChart);
    FileHelper.exportOutputToFile(
        workspaceFolder
            + File.separator
            + jobName
            + File.separator
            + currentBuildNumber
            + File.separator
            + "chartOutput"
            + File.separator
            + "output",
        "buildtoBuildStrategy-chart-output.html",
        htmlOutput);
  }

  private static String applyToVelocityTemplate(
      String appMapURL, List<JenkinsAMChart> strategyCharts) {
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
      Map<String, LinkedHashMap<String, Double>> strategyWiseMetrictoBuildAvgValMap) {
    List<JenkinsAMChart> amCharts = null;
    amCharts = new LinkedList<JenkinsAMChart>();
    JenkinsAMChart amChart = null;
    int divId = 0;

    for (String metricPath : strategyWiseMetrictoBuildAvgValMap.keySet()) {

      JSONObject amChartJSON =
          generateAMChartsJSON(metricPath, strategyWiseMetrictoBuildAvgValMap.get(metricPath));
      if (!amChartJSON.get("dataProvider").equals("empty")) {
        amChart = new JenkinsAMChart();
        amChart.setChartJSONObject(amChartJSON);
        amChart.setDivId("div" + divId);
        amCharts.add(amChart);
        divId++;
      }
    }

    return amCharts;
  }

  private static JSONObject generateAMChartsJSON(
      String metricPath, Map<String, Double> buildAvgValues) {

    String metricName = metricPath.substring(metricPath.lastIndexOf('|') + 1);
    JSONObject amCharts = new JSONObject();
    amCharts.put("type", "serial");
    amCharts.put("theme", "light");
        
    JSONArray dataProviderArray = new JSONArray();
    boolean isDataSet = false;
    JSONObject recordObj = null;
    for (String buildNumber : buildAvgValues.keySet()) {

      if (buildAvgValues.get(buildNumber) != 0) {
        recordObj = new JSONObject();
        isDataSet = true;
        recordObj.put("BuildNumber", Integer.parseInt(buildNumber));
        recordObj.put("AverageValue", buildAvgValues.get(buildNumber));
        //recordObj.put("color", "#CD0D74");

        dataProviderArray.put(recordObj);
      }
    }
    if (isDataSet) {
      amCharts.put("dataProvider", dataProviderArray);
    } else {
      amCharts.put("dataProvider", "empty");
    }

    JSONObject graphobj = new JSONObject();

    JSONObject valueAxis = new JSONObject();
    valueAxis.put("id", "ValueAxis-1");
    valueAxis.put("title", metricName);

    JSONObject categoryAxis = new JSONObject();
    categoryAxis.put("startOnAxis", true);
    categoryAxis.put("title", "BuildNumber");
    categoryAxis.put("gridPosition", "start");
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

    graphobj.put("fillAlphas", 0.9);
    graphobj.put("lineAlpha", 0.2);
    graphobj.put("fillColorsField", "color");
    graphobj.put("type", "column");
    graphobj.put("valueField", "AverageValue");

    JSONArray graphArrayObj = new JSONArray();
    graphArrayObj.put(graphobj);

    amCharts.put("graphs", graphArrayObj);

    amCharts.put("categoryField", "BuildNumber");

    JSONObject chartCursorobj = new JSONObject();
    chartCursorobj.put("categoryBalloonEnabled", true);
    chartCursorobj.put("cursorAlpha", 0);
    chartCursorobj.put("zoomable", false);
    amCharts.put("chartCursor", chartCursorobj);

    return amCharts;
  }
}
