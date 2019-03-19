package com.ca.apm.jenkins.core.helper;

import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.core.entity.AgentComparisonResult;
import com.ca.apm.jenkins.core.entity.DefaultStrategyResult;
import com.ca.apm.jenkins.core.entity.JenkinsAMChart;
import com.ca.apm.jenkins.core.entity.MetricPathComparisonResult;
import com.ca.apm.jenkins.core.entity.TimeSliceValue;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.IOUtility;
import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This utility helper class provides you to create charts and graphs
 *
 * @author chaav03
 */
public class AMChartHelper {

  private AMChartHelper() {
    super();
  }

  private static void preProcessChartOutputDirectory(
      String workspaceFolder, String jobName, String currentBuildNumber) {
    FileHelper.createDirectory(workspaceFolder + File.separator + jobName);
    FileHelper.createDirectory(
        workspaceFolder + File.separator + jobName + File.separator + currentBuildNumber);
    FileHelper.createDirectory(
        workspaceFolder
            + File.separator
            + jobName
            + File.separator
            + currentBuildNumber
            + File.separator
            + "chartOutput");
    File targetZipFile =
        new File(
            workspaceFolder
                + File.separator
                + jobName
                + File.separator
                + currentBuildNumber
                + File.separator
                + "chartOutput"
                + File.separator);
    IOUtility ioUtility = new IOUtility();
    ioUtility.extractZipFromClassPath("amcharts.zip", targetZipFile);
  }

  private static String generateHomePageHtmlContent(List<StrategyResult> strategyResults) {
    VelocityEngine ve = new VelocityEngine();
    StringWriter writer = new StringWriter();
    ve.setProperty("resource.loader", "classpath");
    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    ve.init();
    Template t = ve.getTemplate("amChartHomePage.vm");
    VelocityContext context = new VelocityContext();
    List<HashMap<String, String>> strategies = new LinkedList<HashMap<String, String>>();
    String name = "buildtoBuildStrategy";
    String link = "chartOutput/output/"+name+"-chart-output.html";
    HashMap<String, String> strategyMap = new HashMap<String, String>();
    strategyMap.put("name", name);
    strategyMap.put("link", link);
    strategies.add(strategyMap);
    for (StrategyResult<?> strategyResult : strategyResults) {
        String startegyName = strategyResult.getStrategyName();
        String strategyLink = "chartOutput/output/" + startegyName + "-chart-output.html";
        HashMap<String, String> strategyResultMap = new HashMap<String, String>();
        strategyResultMap.put("name", startegyName);
        strategyResultMap.put("link", strategyLink);
        strategies.add(strategyResultMap);
      }   
    context.put("strategies", strategies);
    t.merge(context, writer);
    return writer.toString();
  }

  private static String applyToVelocityTemplate(
      List<JenkinsAMChart> strategyCharts,
      String appMapURL,
      String startDateTime,
      String endDateTime,
      String frequency) {
    VelocityEngine ve = new VelocityEngine();
    StringWriter writer = new StringWriter();
    ve.setProperty("resource.loader", "classpath");
    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    ve.init();
    Template t = ve.getTemplate("amChartsReport.vm");
    VelocityContext context = new VelocityContext();
    context.put("strategyCharts", strategyCharts);
    context.put("url", appMapURL);
    context.put("startDateTime", startDateTime);
    context.put("endDateTime", endDateTime);
    context.put("frequency", frequency);
    t.merge(context, writer);
    return writer.toString();
  }

  private static String getMetricName(String metricPath) {
    return metricPath.substring(metricPath.lastIndexOf('|') + 1);
  }

  private static String getTransactionName(String metricPath) {
    return metricPath.substring(0, metricPath.lastIndexOf('|'));
  }

  private static JSONArray getTimeSliceFormattedArray(
      String categoryField,
      List<TimeSliceValue> benchMarkSlices,
      List<TimeSliceValue> currentBuildSlices) {
    JSONArray dataProviderArray = new JSONArray();
    int size = currentBuildSlices.size();
    for (int i = 0; i < benchMarkSlices.size(); i++) {
      int categoryId = i + 1;
      JSONObject recordObj = new JSONObject();
      recordObj.put(categoryField, categoryId);
      recordObj.put("BenchMark_MetricValue", benchMarkSlices.get(i).getValue());
      double actualValue = 0.0;
      if (i <= size - 1) {
        actualValue = currentBuildSlices.get(i).getValue();
      }
      recordObj.put("CurrentBuild_MetricValue", actualValue);
      dataProviderArray.put(recordObj);
    }
    return dataProviderArray;
  }

  /** "categoryAxis": { "gridPosition": "start" }, */
  private static JSONObject generateAMChartsJSON(String metricPath,
      String metricName,
      String benchMarkBuildNumber,
      String currentBuildNumber,
      List<TimeSliceValue> benchMarkSlices,
      List<TimeSliceValue> currentBuildSlices) {
    JSONObject amCharts = new JSONObject();
    amCharts.put("type", "serial");
    amCharts.put("categoryField", "FrequencyInterval");
    amCharts.put("startDuration", 1);
    amCharts.put("balloon", new JSONObject());
    amCharts.put("trendLines", new JSONArray());
    amCharts.put("guides", new JSONArray());
    amCharts.put("allLabels", new JSONArray());

    JSONObject categoryAxisObj = new JSONObject();
    categoryAxisObj.put("gridPosition", "start");

    amCharts.put("categoryAxis", categoryAxisObj);

    JSONObject exportObj = new JSONObject();
    exportObj.put("enabled", true);
    String fileName = metricName+"_"+metricPath.substring(metricPath.lastIndexOf('|')+1);
    exportObj.put("fileName", fileName);
    exportObj.put("pageOrigin", false);
    
    amCharts.put("export", exportObj);

    JSONObject valueAxis = new JSONObject();
    valueAxis.put("id", "ValueAxis-1");
    valueAxis.put("title", metricName);
    JSONArray valueAxesArray = new JSONArray();
    valueAxesArray.put(valueAxis);

    amCharts.put("valueAxes", valueAxesArray);

    JSONObject legendObj = new JSONObject();
    legendObj.put("enabled", true);
    legendObj.put("useGraphSettings", true);

    amCharts.put("legend", legendObj);

    JSONObject titleObj = new JSONObject();
    titleObj.put("id", "Title-1");
    titleObj.put("text", metricPath);
    JSONArray titlesArray = new JSONArray();
    titlesArray.put(titleObj);

    amCharts.put("titles", titlesArray);

    JSONObject benchMarkgraphObj = new JSONObject();
    benchMarkgraphObj.put("balloonText", "[[title]] of [[FrequencyInterval]]:[[value]]");
    benchMarkgraphObj.put("bullet", "round");
    benchMarkgraphObj.put("id", "AmGraph-1");
    benchMarkgraphObj.put("title", "Build " + benchMarkBuildNumber);
    benchMarkgraphObj.put("valueField", "BenchMark_MetricValue");

    JSONObject currentgraphObj = new JSONObject();
    currentgraphObj.put("balloonText", "[[title]] of [[FrequencyInterval]]:[[value]]");
    currentgraphObj.put("bullet", "square");
    currentgraphObj.put("id", "AmGraph-2");
    currentgraphObj.put("title", "Build " + currentBuildNumber);
    currentgraphObj.put("valueField", "CurrentBuild_MetricValue");

    JSONArray graphArrayObj = new JSONArray();
    graphArrayObj.put(benchMarkgraphObj);
    graphArrayObj.put(currentgraphObj);

    amCharts.put("graphs", graphArrayObj);

    JSONArray dataProviderArray =
        getTimeSliceFormattedArray("FrequencyInterval", benchMarkSlices, currentBuildSlices);

    amCharts.put("dataProvider", dataProviderArray);
    return amCharts;
  }

  private static List<JenkinsAMChart> getChartsForOneStrategy(String strategyName,
      DefaultStrategyResult defaultStrategyResult,
      String benchMarkBuildNumber,
      String currentBuildNumber) {
    List<JenkinsAMChart> amCharts = null;
    if (defaultStrategyResult != null) {
      amCharts = new LinkedList<JenkinsAMChart>();
      int divId = 1;
      for (String agentSpecifier : defaultStrategyResult.getResult().keySet()) {
        AgentComparisonResult agentComparisonResult =
            defaultStrategyResult.getResult().get(agentSpecifier);
        for (MetricPathComparisonResult comparisonResult : agentComparisonResult.getSlowEntries()) {

          String metricName = getMetricName(comparisonResult.getMetricPath());
          String transaction = getTransactionName(comparisonResult.getMetricPath());
          JSONObject amChartJSON =
              generateAMChartsJSON(transaction,
                  metricName,
                  benchMarkBuildNumber,
                  currentBuildNumber,
                  comparisonResult.getBenchMarkBuildTimeSliceValues(),
                  comparisonResult.getCurrentBuildTimeSliceValues());

          JenkinsAMChart amChart = new JenkinsAMChart();
          amChart.setChartJSONObject(amChartJSON);
          amChart.setDivId("div" + divId);
          amCharts.add(amChart);
          divId++;
        }
        for (MetricPathComparisonResult comparisonResult :
            agentComparisonResult.getSuccessEntries()) {
          String metricName = getMetricName(comparisonResult.getMetricPath());
          String transaction = getTransactionName(comparisonResult.getMetricPath());
          JSONObject amChartJSON =
              generateAMChartsJSON(transaction,
                  metricName,
                  benchMarkBuildNumber,
                  currentBuildNumber,
                  comparisonResult.getBenchMarkBuildTimeSliceValues(),
                  comparisonResult.getCurrentBuildTimeSliceValues());
          JenkinsAMChart amChart = new JenkinsAMChart();
          amChart.setChartJSONObject(amChartJSON);
          amChart.setDivId("div" + divId);
          amCharts.add(amChart);
          divId++;
        }
      }
    }
    return amCharts;
  }

  /**
   * This method generates the chart output for all the strategy results provided to it It generates
   * one chart per comparison-strategy in the chartoutput folder current build's workspace directory
   * The HTML output file is present in output folder inside chartoutput folder
   *
   * @param strategyResults The list of strategy results which are mapped to this output-handler
   * @param workspaceFolder The Jenkins workspace folder
   * @param jobName The Jenkins Job Name
   * @param benchMarkBuildNumber Benchmarck build number
   * @param currentBuildNumber Current Build Number
   */
  @SuppressWarnings("rawtypes")
  public static void produceChartOutput(
      List<StrategyResult> strategyResults,
      String workspaceFolder,
      String jobName,
      String benchMarkBuildNumber,
      String currentBuildNumber,
      String appMapURL,
      String startDateTime,
      String endDateTime,
      String frequency) {
    Map<String, List<JenkinsAMChart>> strategyWiseCharts = null;

    strategyWiseCharts = new LinkedHashMap<String, List<JenkinsAMChart>>();
    for (StrategyResult<?> strategyResult : strategyResults) {
      String strategyName = strategyResult.getStrategyName();
      if (strategyResult
          .getResult()
          .getClass()
          .getName()
          .equals("com.ca.apm.jenkins.core.entity.DefaultStrategyResult")) {
        if (strategyResult.getResult() == null) {

          JenkinsPlugInLogger.warning(
              strategyName
                  + " is not a default strategy implementation, hence the result will not be presented on chart output");
          continue;
        } else {

          List<JenkinsAMChart> strategyChart = null;
          DefaultStrategyResult defaultStrategyResult =
              (DefaultStrategyResult) strategyResult.getResult();
          strategyChart =
              getChartsForOneStrategy(strategyName,
                  defaultStrategyResult, benchMarkBuildNumber, currentBuildNumber);
          strategyWiseCharts.put(strategyName, strategyChart);
        }

      } else {
        JenkinsPlugInLogger.warning(
            "Your strategy Result is of "
                + strategyResult.getResult().getClass().getName()
                + " this cannot be processed by ChartOutputHandler");
      }
    }
    if (!strategyWiseCharts.isEmpty()) {

      preProcessChartOutputDirectory(workspaceFolder, jobName, currentBuildNumber);
      String homePageHtmlOutput = generateHomePageHtmlContent(strategyResults);
      FileHelper.exportOutputToFile(
          workspaceFolder + File.separator + jobName + File.separator + currentBuildNumber,
          "chart-output.html",
          homePageHtmlOutput);
      for (String comparisonStrategyName : strategyWiseCharts.keySet()) {
        String htmlOutput = null;
        List<JenkinsAMChart> strategyCharts = strategyWiseCharts.get(comparisonStrategyName);
        htmlOutput =
            applyToVelocityTemplate(
                strategyCharts, appMapURL, startDateTime, endDateTime, frequency);
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
            comparisonStrategyName + "-chart-output.html",
            htmlOutput);
      }
    }
  }
}
