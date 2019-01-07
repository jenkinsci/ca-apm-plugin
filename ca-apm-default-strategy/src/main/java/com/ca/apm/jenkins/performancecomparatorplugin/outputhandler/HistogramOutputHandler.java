package com.ca.apm.jenkins.performancecomparatorplugin.outputhandler;

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
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

/**
 * An implementation of Output Handler to render the multiple builds results, metric wise into
 * histogram Charts
 *
 * @author Avinash Chandwani
 */
@SuppressWarnings("rawtypes")
public class HistogramOutputHandler implements OutputHandler<StrategyResult> {

  private ComparisonMetadata comparisonMetadata;

  private int currentBuildNumber;
  private OutputConfiguration outputConfiguration;

  private static String applyToVelocityTemplate(List<JenkinsAMChart> strategyCharts) {
    VelocityEngine ve = new VelocityEngine();
    StringWriter writer = new StringWriter();
    ve.setProperty("resource.loader", "classpath");
    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    ve.init();
    Template t = ve.getTemplate("amHistogramChartsReport.vm");
    VelocityContext context = new VelocityContext();
    context.put("strategyCharts", strategyCharts);

    t.merge(context, writer);
    return writer.toString();
  }

  private static List<JenkinsAMChart> getChartsForMetricPaths(
      Map<String, Map<String, Map<String, Double>>> strategyWiseBuildAvgValues) {
    List<JenkinsAMChart> amCharts = null;
    amCharts = new LinkedList<JenkinsAMChart>();
    JenkinsAMChart amChart = null;
    int divId = 0;
    String metricSpecifier = null;
    List<String> buildNumbers = null;
    Set<String> metricPathSet = null;
    Map<String, Double> buildAvgValMap = new LinkedHashMap<String, Double>();
    for (String strategyName : strategyWiseBuildAvgValues.keySet()) {
      metricSpecifier = strategyName.substring(strategyName.indexOf('|') + 1);
      Map<String, Map<String, Double>> buildMerticAvgValMap =
          strategyWiseBuildAvgValues.get(strategyName);
      buildNumbers = new ArrayList(buildMerticAvgValMap.keySet());
      for (int i = 0; i < buildNumbers.size(); i++) {
        metricPathSet = new HashSet<String>();
        for (String metricPath : buildMerticAvgValMap.get(buildNumbers.get(i)).keySet()) {
          metricPathSet.add(metricPath);
        }
      }
      List<String> metricPathList = new ArrayList<String>(metricPathSet);
      Collections.sort(buildNumbers, Collections.reverseOrder());
      for (int j = 0; j < metricPathList.size(); j++) {
        String metricPathchart = null;
        for (int k = 0; k < buildNumbers.size(); k++) {

          for (String metricPath : buildMerticAvgValMap.get(buildNumbers.get(k)).keySet()) {

            if (metricPath.equals(metricPathList.get(j))) {
              metricPathchart = metricPath;
              buildAvgValMap.put(
                  buildNumbers.get(k),
                  buildMerticAvgValMap.get(buildNumbers.get(k)).get(metricPath));
            }
          }
        }
        JSONObject amChartJSON = generateAMChartsJSON(metricPathchart, buildAvgValMap);
        if (!amChartJSON.get("dataProvider").equals("empty")) {
          amChart = new JenkinsAMChart();
          amChart.setChartJSONObject(amChartJSON);
          amChart.setDivId("div" + divId);
          amCharts.add(amChart);
          divId++;
        }
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
    graphobj.put("lineAlpha", "0.2");
    graphobj.put("type", "column");
    graphobj.put("valueField", "AverageValue");

    JSONArray graphArrayObj = new JSONArray();
    graphArrayObj.put(graphobj);

    amCharts.put("graphs", graphArrayObj);

    amCharts.put("categoryField", "BuildNumber");

    JSONObject chartCursorobj = new JSONObject();
    chartCursorobj.put("fullWidth", "true");
    chartCursorobj.put("cursorAlpha", "0.1");
    amCharts.put("chartCursor", chartCursorobj);

    return amCharts;
  }

  public void setComparisonMetadata(ComparisonMetadata comparisonMetadata) {
    this.comparisonMetadata = comparisonMetadata;
  }

  public void setOutputConfiguration(OutputConfiguration outputConfiguration) {
    this.outputConfiguration = outputConfiguration;
  }

  public void publishOutput(List<StrategyResult> strategyResults)
      throws BuildComparatorException, BuildExecutionException {
    List<BuildInfo> histogramBuildInfoList = outputConfiguration.getHistogramBuildInfoList();
    String workspaceFolder =
        outputConfiguration.getCommonPropertyValue(Constants.workSpaceDirectory);
    String jobName = outputConfiguration.getCommonPropertyValue(Constants.jenkinsJobName);
    Map<String, Map<String, Map<String, Double>>> stgyBldMetricPathAverageValuesMap =
        getMetricData(histogramBuildInfoList);
    produceChartOutput(stgyBldMetricPathAverageValuesMap, workspaceFolder, jobName);
  }

  private Map<String, Map<String, Map<String, Double>>> getMetricData(List<BuildInfo> buildInfoList)
      throws BuildExecutionException {
    Set<String> comparisonStrategiesSet =
        comparisonMetadata
            .getStrategiesInfo()
            .getOutputHandlerToComparisonStrategies()
            .get(Constants.histogramoutputhtml);
    StrategyConfiguration strategyConfiguration;
    String comparisonStrategyName;
    List<String> comparisonStrategies = new ArrayList<String>(comparisonStrategiesSet);
    Map<String, Map<String, Double>> buildMetricPathAvgValuesMap = null;
    Map<String, Map<String, Map<String, Double>>> stgyBldMetricPathAverageValuesMap =
        new LinkedHashMap<String, Map<String, Map<String, Double>>>();
    String metricSpecifier = null;
    currentBuildNumber = buildInfoList.get(0).getNumber();
    for (int i = 0; i < comparisonStrategies.size(); i++) {
      comparisonStrategyName = comparisonStrategies.get(i);
      buildMetricPathAvgValuesMap = new LinkedHashMap<String, Map<String, Double>>();
      Map<String, StrategyConfiguration> comparisonStrategiesInfo =
          comparisonMetadata.getStrategiesInfo().getComparisonStrategiesInfo();
      strategyConfiguration = comparisonStrategiesInfo.get(comparisonStrategyName);
      List<String> agentSpecifiers = strategyConfiguration.getAgentSpecifiers();
      metricSpecifier =
          strategyConfiguration.getPropertyValue(
              comparisonStrategyName + "." + Constants.metricSpecifier);

      for (String agentSpecifier : agentSpecifiers) {

        try {
          for (int k = 0; k < buildInfoList.size(); k++) {
            BuildPerformanceData benchMarkPerformanceData =
                MetricDataHelper.getMetricData(
                    agentSpecifier, metricSpecifier, buildInfoList.get(k));
            Map<String, Double> metricPathAverageValuesMap =
                FormulaHelper.getAverageValues(benchMarkPerformanceData);

            buildMetricPathAvgValuesMap.put(
                String.valueOf(buildInfoList.get(k).getNumber()), metricPathAverageValuesMap);
          }
        } catch (BuildComparatorException e) {
          JenkinsPlugInLogger.severe(
              "An error has occured while collecting performance metrics for "
                  + comparisonStrategyName
                  + "from APM-> for agentSpecifier="
                  + agentSpecifier
                  + ",metricSpecifier ="
                  + metricSpecifier
                  + e.getMessage(),
              e);
        }
      }
      stgyBldMetricPathAverageValuesMap.put(
          comparisonStrategyName + "|" + metricSpecifier, buildMetricPathAvgValuesMap);
    }
    return stgyBldMetricPathAverageValuesMap;
  }

  @SuppressWarnings("rawtypes")
  private void produceChartOutput(
      Map<String, Map<String, Map<String, Double>>> stgyBldMetricPathAverageValuesMap,
      String workspaceFolder,
      String jobName) {
    List<JenkinsAMChart> metricPathsChart = null;
    metricPathsChart = getChartsForMetricPaths(stgyBldMetricPathAverageValuesMap);
    String htmlOutput = null;
    htmlOutput = applyToVelocityTemplate(metricPathsChart);
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
        "histogram-chart-output.html",
        htmlOutput);
  }
}
