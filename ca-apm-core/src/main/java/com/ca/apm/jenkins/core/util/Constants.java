package com.ca.apm.jenkins.core.util;

/**
 * Static Constants class for adding reusable strings
 *
 * @author Avinash Chandwani
 */
public class Constants {

  // EM REST Absolute URLs
  public static final String vertexIdByName = "/apm/appmap/vertex";
  public static final String attributeUpdate = "/apm/appmap/graph/vertex";
  public static final String queryMetricDataAPI = "/apm/appmap/private/apmData/query";
  // Arguments
  public static final String BEARER = "Bearer ";
  public static final String AUTHORIZATION = "Authorization";
  public static final String HTTP = "http://";
  public static final String COLON = ":";
  public static final String pipeSeperator = "|";
  public static final String ContentType = "Content-type";
  public static final String APPLICATION_JSON = "application/json";

  // Data Source
  public static final String CAAPM = "CA-APM";
  
  // Special Characters
  public static final String NewLine = "\n";
  public static final String lineBreak = "-------------------------------";
  // Comparison Status
  public static final String Passed = "PASSED!";
  public static final String Failed = "FAILED!";
  public static final String False = "false";
  public static final String SLOWER = "SLOWER";
  public static final String FASTER = "FASTER";
  public static final String SAME = "SAME";
  // Configuration Constants
  public static final String emURL = "em.url";
  public static final String emAuthToken = "em.authtoken";
  public static final String emTimeZone = "em.timezone";
  public static final String loadGeneratorName = "loadgenerator.name";
  
  public static final String benchMarkBuildNumber = "build.benchmarkbuildnumber";
  public static final String buildPassOrFail = "build.fail";
  public static final String isPublishBuildResulttoEM = "build.result.publishtoem";
 
  public static final String comparisonStrategiesList = "metric.list";
  public static final String comparisonStrategyName = "comparisonstrategy.name";
  public static final String comparatorClasssName = "comparator";
  
  public static final String meanLatencyStrategyName = "meanlatencystrategy";
  public static final String staticThresholdStrategyName = "staticthresholdstrategy";
   
  public static final String emailOutputHandlerName = "plaintextemail";
  public static final String jsonFileOutputHandlerName = "jsonfilestore";
  public static final String chartOutputHandlerName = "chartoutputhtml";
  public static final String histogramOutputHandlerName = "histogramoutputhtml";
  public static final String emailOutputHandlerClasssName = "PlainTextEmail";
  public static final String jsonFileOutputHandlerClasssName = "JSONFileStore";
  public static final String chartOutputHandlerClasssName = "Chart";
  public static final String histogramOutputHandlerClasssName = "Histogram";
  public static final String comparatorClassPath = "com.ca.apm.jenkins.performancecomparatorplugin.comparisonstrategy";
   
  public static final String agentSpecifier = "agentspecifier";
  public static final String metricSpecifier = "metricspecifier";
  public static final String threshold = "threshold";
  public static final String outputHandlers = "outputhandlers";
  public static final String outputHandlersList = "outputhandlers.list";
  public static final String outputHandlerClassName = "outputhandler";
  
  
  public static final String outputHandlerClassPath = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler";
  public static final String outputHandlerSuffix = "OutputHandler";
  
  public static final String emailMode = "email.mode";
  public static final String gmailSmtpPort = "email.gmailsmtpport";
  public static final String gmailSocketPort = "email.gmailsocketport";
  public static final String emailSMTPHost = "email.smtp.host";
  public static final String emailSMTPAuth = "email.smtp.auth";
  public static final String emailSenderId = "email.sender.id";
  public static final String emailPassword = "email.password";
  public static final String emailToRecipients = "email.recepients.to";
  public static final String emailCCRecipients = "email.recepients.cc";
  public static final String emailBccRecipients = "email.recepients.bcc";
  public static final String loggingLevel = "logging.level";
  public static final String defaultLoggingLevel = "INFO";
  public static final String extensionsDirectory = "extensions.directory";
  public static final String outputDirectory = "output.directory";
  public static final String workSpaceDirectory = "workspace.directory";
  public static final String jenkinsJobName = "jenkins.jobname";
  public static final String jenkinsCurrentBuild = "jenkins.currentbuild";
  public static final String jenkinsBenchMarkBuild = "jenkins.benchmarkbuild";
  // Run-Time Method Names
  public static final String comparatorConfigMethod = "setConfiguration";
  public static final String comparatorExecuteMethod = "doCompare";
  public static final String outputHandlerConfigMethod = "setOutputConfiguration";
  public static final String outputHandlerExecuteMethod = "publishOutput";
  public static final String emWebViewPort = "em.webview.port";
  public static final String emExpViewURLPostfix = "ApmServer/#/home?ep=0&g=-1&cha=0&cht=0&chs=0&m=L&l=ATC&fa=%5B%5D&u=UN1&view=%7B%22drillDown%22:%5B%5D%7D";
  public static final String applicationName = "application.name";
  public static final String metricClamp = "metric.clamp";
  public static final String buildsInHistogram = "histogram.builds";
  public static final String histogramoutputhtml = "histogramoutputhtml";
  public static final String ComparisonMetadataConfigMethod = "setComparisonMetadata";
  public static final String histogramBuildInfoList = "histogramBuildInfo";
   
  

  private Constants() {}
}
