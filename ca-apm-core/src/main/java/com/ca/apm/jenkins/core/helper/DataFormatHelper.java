package com.ca.apm.jenkins.core.helper;

import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.core.entity.AgentComparisonResult;
import com.ca.apm.jenkins.core.entity.DefaultStrategyResult;
import com.ca.apm.jenkins.core.entity.MetricPathComparisonResult;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Utility class to provide you the facility to convert your custom output object into data-formats
 * like JSON, XML. It can also provide you plain-text output provided the input supplied to the
 * method is of DefaultStrategy type
 *
 * @author Avinash Chandwani
 */
public class DataFormatHelper {

  private DataFormatHelper() {
    super();
  }

  /**
   * Utility to Generate HTML Rich Text Output with Tables
   *
   * @param outputConfiguration
   * @return
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static String getHTMLTextOutput(
      OutputConfiguration outputConfiguration, List<StrategyResult> strategyResults) {

    List<StrategyResult<DefaultStrategyResult>> defaultStrategyResults =
        new LinkedList<StrategyResult<DefaultStrategyResult>>();
    for (StrategyResult<?> strategyResult : strategyResults) {
      if (strategyResult
          .getResult()
          .getClass()
          .getName()
          .equals("com.ca.apm.jenkins.core.entity.DefaultStrategyResult")) {
        defaultStrategyResults.add((StrategyResult<DefaultStrategyResult>) strategyResult);
      } else {
        JenkinsPlugInLogger.warning(
            "Your strategy Result is of "
                + strategyResult.getResult().getClass().getName()
                + " this cannot be processed by PlainTextEmailOutputHandler");
      }
    }

    if (defaultStrategyResults.isEmpty()) {
      JenkinsPlugInLogger.severe(
          "No Default Strategy Result Found in the handler, hence the output cannot be generated");
      return null;
    }

    JenkinsPlugInLogger.fine("****Preparing Rich Style Output for email content****");
    VelocityEngine ve = new VelocityEngine();
    ve.setProperty("resource.loader", "classpath");
    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    ve.init();
    Template t = ve.getTemplate("emailReporthtml.vm");
    VelocityContext context = new VelocityContext();
    context.put(
        "buildNumber", outputConfiguration.getCommonPropertyValue(Constants.jenkinsCurrentBuild));
    context.put(
        "benchmarkbuildnumber",
        outputConfiguration.getCommonPropertyValue(Constants.jenkinsBenchMarkBuild));
    context.put("duration", outputConfiguration.getCommonPropertyValue("runner.duration"));
    context.put("startTime", outputConfiguration.getCommonPropertyValue("runner.start"));
    context.put(
        "loadTestName", outputConfiguration.getCommonPropertyValue(Constants.loadGeneratorName));
    context.put("comparisonResults", defaultStrategyResults);
    context.put("newline", Constants.NewLine);
    StringWriter writer = new StringWriter();
    t.merge(context, writer);
    return writer.toString();
  }

  /**
   * Get the Plain Text Output for the comparison-strategy result you want to get If the
   * strategy-result is inheriting DefaultStrategyResult, then only we will be able to provide you
   * this output
   *
   * @param strategyResult : The argument should be of StrategyResult type The support is only for
   *     DefaultStrategyResult type of comparison-strategy result
   * @return Returns the string representation of the comparison-output
   */
  public static String generatePlainTextOutputForStrategy(StrategyResult<?> strategyResult) {
    StringBuilder builder = new StringBuilder();
    Object result = strategyResult.getResult();
    if (result instanceof DefaultStrategyResult) {
      DefaultStrategyResult dResult = (DefaultStrategyResult) result;
      Map<String, AgentComparisonResult> agentComparisonResult = dResult.getResult();
      for (String agentName : agentComparisonResult.keySet()) {
        int count = 1;
        AgentComparisonResult agentResult = agentComparisonResult.get(agentName);
        builder.append("Agent Specifier = " + agentName).append(Constants.NewLine);
        if (!agentResult.getSlowEntries().isEmpty()) {
          builder.append(" ").append(Constants.NewLine);
        }
        for (MetricPathComparisonResult mResult : agentResult.getSlowEntries()) {
          builder
              .append(
                  "  "
                      + count++
                      + ".Metric Path = "
                      + mResult.getMetricPath()
                      + ", expected value = "
                      + mResult.getExpectedValue()
                      + ", actual value = "
                      + mResult.getActualValue())
              .append(Constants.NewLine);
        }
        if (!agentResult.getSuccessEntries().isEmpty()) {
          builder.append(" ").append(Constants.NewLine);
        }
        count = 1;
        for (MetricPathComparisonResult mResult : agentResult.getSuccessEntries()) {
          builder
              .append(
                  "  "
                      + count++
                      + ".Metric Path = "
                      + mResult.getMetricPath()
                      + ", expected value = "
                      + mResult.getExpectedValue()
                      + ", actual value = "
                      + mResult.getActualValue())
              .append(Constants.NewLine);
        }
      }
    } else {
      JenkinsPlugInLogger.warning(
          "getPlainTextOutputForStrategy method is not supported for data formats except DefaultStrategyResult");
    }

    return builder.toString();
  }

  /**
   * The utility gives you a JSON string representation of your strategy result
   *
   * @param strategyResult You comparison-strategy result, any custom object is supported
   * @return Returns the String representation of the JSON Object of the <code>StrategyResult</code>
   *     object
   */
  public static String generateJSONOutputForStrategy(StrategyResult<?> strategyResult) {
    if (strategyResult == null) {
      return null;
    }
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(strategyResult);
  }

  /**
   * The utility gives you a XML string representation of your strategy result
   *
   * @param strategyResult You comparison-strategy result, any custom object is supported
   * @return Returns the String representation of the XML Object of the <code>StrategyResult</code>
   *     object
   */
  public static String generateXMLStrategyOutputResult(StrategyResult<?> strategyResult) {
    if (strategyResult == null) {
      return null;
    }
    XStream xstream = new XStream();
    return xstream.toXML(strategyResult);
  }
}
