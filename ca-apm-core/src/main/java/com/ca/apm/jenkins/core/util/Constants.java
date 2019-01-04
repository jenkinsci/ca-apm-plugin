package com.ca.apm.jenkins.core.util;

/**
 * Static Constants class for adding reusable strings
 * 
 * @author Avinash Chandwani
 *
 */
public class Constants {

	private Constants() {

	}

	// EM REST Absolute URLs
	public static final String restPath = "/apm/appmap/private/apmData/schema";
	public static final String tokenPath = "/apm/appmap/private/token/temporaryToken";
	public static final String vertexIdByName = "/apm/appmap/vertex";
	public static final String attributeUpdate = "/apm/appmap/graph/vertex";
	
	public static final String batchMetricData = "/apm/appmap/private/metric/batch";
	public static final String queryMetricDataAPI = "/apm/appmap/private/apmData/query";

	// Arguments
	public static final String BEARER = "Bearer ";
	public static final String AUTHORIZATION = "Authorization";
	public static final String HTTP = "http://";
	public static final String COLON = ":";
	public static final String pipeSeperator = "|";
	public static final String ContentType = "Content-type";
	public static final String APPLICATION_JSON = "application/json";

	// Metric Names
	public static final String AVG_RESPONSE_TIME_METRIC = "Average Response Time \\(ms\\)";
	public static final String STALL_COUNT_METRIC = "Health:Stall Count";

	// Data Source
	public static final String CAAPM = "CA-APM";

	// Load Source
	public static final String Blazemeter = "Blazemeter";

	// Comparison Strategy
	public static final String meanLatencyComparison = "Mean Latency Comparison";
	public static final String stallCountComparison = "Stall Count Comparison";

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
	public static final String emUserName = "em.username";
	public static final String emPassword = "em.password";
	public static final String emTimeZone = "em.timezone";

	public static final String loadGeneratorName = "loadgenerator.name";
	public static final String metadataReaderClassName = "metadatareader";

	public static final String benchMarkBuildNumber = "build.benchmarkbuildnumber";
	public static final String buildPassOrFail = "build.fail";
	public static final String isPublishBuildResulttoEM = "buildresult.em.publish";

	public static final String comparisonStrategiesList = "comparisonstrategies.list";
	public static final String comparisonStrategyName = "comparisonstrategy.name";
	public static final String comparatorClasssName = "comparator";
	public static final String agentSpecifier = "agentspecifier";
	public static final String metricSpecifier = "metricspecifier";
	public static final String threshold = "threshold";
	public static final String outputHandlers = "outputhandlers";

	public static final String outputHandlersList = "outputhandlers.list";
	public static final String outputHandlerClassName = "outputhandler";

	public static final String emailSMTPHost = "email.smtp.host";
	public static final String emailSMTPAuth = "email.smtp.auth";
	public static final String emailSenderId = "email.sender.id";
	public static final String emailPassword = "email.password";
	public static final String emailToRecipients = "email.recepients.to";
	public static final String emailCCRecipients = "email.recepients.cc";
	public static final String emailBccRecipients = "email.recepients.bcc";

	public static final String loggingLevel = "logging.level";
	public static final String extensionsDirectory = "extensions.directory";

	public static final String outputDirectory = "output.directory";
	public static final String workSpaceDirectory = "workspace.directory";
	public static final String jenkinsJobName = "jenkins.jobname";
	public static final String jenkinsCurrentBuild = "jenkins.currentbuild";
	public static final String jenkinsBenchMarkBuild = "jenkins.benchmarkbuild";

	// Run-Time Method Names
	public static final String loadRunnerMetadataExtractMethod = "fetchExtraMetadata";
	public static final String comparatorConfigMethod = "setConfiguration";
	public static final String comparatorExecuteMethod = "doCompare";
	public static final String outputHandlerConfigMethod = "setOutputConfiguration";
	public static final String outputHandlerExecuteMethod = "publishOutput";
	public static final String appMapURL = "appmap.url";
    public static final String applicationName = "application.name";
    public static final String metricClamp = "metric.clamp";
    public static final String buildsInHistogram = "histogram.builds";
    public static final String histogramoutputhtml = "histogramoutputhtml";
    public static final String ComparisonMetadataConfigMethod = "setComparisonMetadata";
    public static final String histogramBuildInfoList = "histogramBuildInfo";
}
