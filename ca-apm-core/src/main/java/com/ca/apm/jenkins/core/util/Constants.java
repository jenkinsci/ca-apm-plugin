package com.ca.apm.jenkins.core.util;

/**
 * Static Constants class for adding reusable strings
 *
 * @author Avinash Chandwani
 */
public class Constants {

	// EM REST Absolute URLs

	// vertexIdByName, vertexUpdate
	public static final String ATTRIBUTEUPDATE = "/apm/appmap/graph/vertex";
	public static final String GETVERTEXIDBYNAME = "/apm/appmap/vertex";
	// attributeUpdate
	public static final String QUERYMETRICDATAAPIPRIVATE ="/atc/private/apmData/query";
	public static final String QUERYMETRICDATAAPIPRIVATE_BACKWARD ="/apm/appmap/private/apmData/query";

	// Arguments
	public static final String BEARER = "Bearer ";
	public static final String AUTHORIZATION = "Authorization";
	public static final String HTTP = "http://";
	public static final String COLON = ":";
	public static final String PIPESEPARATOR = "|";

	public static final String CONTENTTYPE = "Content-type";
	public static final String APPLICATION_JSON = "application/json";

	// Data Source
	public static final String CAAPM = "CA-APM";

	// Special Characters
	public static final String NEWLINE = "\n";

	// Comparison Status
	public static final String FAILED = "FAILED!";
	public static final String SLOWER = "SLOWER";
	public static final String FASTER = "FASTER";
	public static final String SAME = "SAME";
	// Configuration Constants
	public static final String EMURL = "em.url";
	public static final String EMAUTHTOKEN = "em.authtoken";
	public static final String EMTIMEZONE = "em.timezone";
	public static final String LOADGENERATORNAME = "loadgenerator.name";

	// DOI properties
	public static final String DOITIMEZONE = "doi.timezone";
	public static final String DOITENANTID = "doi.tenant.id";
	public static final String JARVISENDPOINT = "jarvis.endpoint";
	public static final String ISBUILDCHANGEEVENTTODOI = "build.changeevent.doi";
	public static final String APPLICATIONHOST = "application.host";

	public static final String BENCHMARKBUILDNUMBER = "build.benchmarkbuildnumber";
	public static final String BUILDPASSORFAIL = "build.fail";
	public static final String ISPUBLISHBUILDRESULTTOEM = "build.result.publishtoem";

	public static final String COMPARISONSTRATEGIESLIST = "metric.list";
	public static final String COMPARATORCLASSSNAME = "comparator";

	public static final String EMAILOUTPUTHANDLERNAME = "plaintextemail";
	public static final String JSONFILEOUTPUTHANDLERNAME = "jsonfilestore";
	public static final String CHARTOUTPUTHANDLERNAME = "chartoutputhtml";
	public static final String HISTOGRAMOUTPUTHANDLERNAME = "histogramoutputhtml";
	public static final String EMAILOUTPUTHANDLERCLASSSNAME = "PlainTextEmail";
	public static final String JSONFILEOUTPUTHANDLERCLASSSNAME = "JSONFileStore";
	public static final String CHARTOUTPUTHANDLERCLASSSNAME = "Chart";
	public static final String COMPARATORCLASSPATH = "com.ca.apm.jenkins.performancecomparatorplugin.comparisonstrategy";

	public static final String AGENTSPECIFIER = "agentspecifier";
	public static final String METRICSPECIFIER = "metricspecifier";
	public static final String THRESHOLD = "threshold";
	public static final String OUTPUTHANDLERS = "outputhandlers";
	public static final String OUTPUTHANDLERSLIST = "outputhandlers.list";

	public static final String OUTPUTHANDLERCLASSPATH = "com.ca.apm.jenkins.performancecomparatorplugin.outputhandler";
	public static final String OUTPUTHANDLERSUFFIX = "OutputHandler";

	public static final String EMAILMODE = "email.mode";
	public static final String GMAILSMTPPORT = "email.gmailsmtpport";
	public static final String GMAILSOCKETPORT = "email.gmailsocketport";
	public static final String EMAILSMTPHOST = "email.smtp.host";
	public static final String EMAILSMTPAUTH = "email.smtp.auth";
	public static final String EMAILSENDERID = "email.sender.id";
	public static final String EMAILTORECIPIENTS = "email.recepients.to";
	public static final String EMAILCCRECIPIENTS = "email.recepients.cc";
	public static final String EMAILBCCRECIPIENTS = "email.recepients.bcc";
	public static final String LOGGINGLEVEL = "logging.level";
	public static final String DEFAULTLOGGINGLEVEL = "INFO";
	public static final String EXTENSIONSDIRECTORY = "extensions.directory";
	public static final String OUTPUTDIRECTORY = "output.directory";
	public static final String WORKSPACEDIRECTORY = "workspace.directory";
	public static final String JENKINSJOBNAME = "jenkins.jobname";
	public static final String JENKINSCURRENTBUILD = "jenkins.currentbuild";
	public static final String JENKINSBENCHMARKBUILD = "jenkins.benchmarkbuild";

	public static final String JENKINSCURRENTBUILDSCMREPOPARAMS = "jenkins.currentbuildSCMRepoParams";
	public static final String JENKINSBENCHMARKBUILDSCMREPOPARAMS = "jenkins.benchmarkbuildSCMRepoParams";
	// Run-Time Method Names
	public static final String COMPARATORCONFIGMETHOD = "setConfiguration";
	public static final String COMPARATOREXECUTEMETHOD = "doCompare";
	public static final String OUTPUTHANDLERCONFIGMETHOD = "setOutputConfiguration";
	public static final String OUTPUTHANDLEREXECUTEMETHOD = "publishOutput";
	public static final String EMWEBVIEWPORT = "em.webview.port";
	public static final String EMEXPVIEWURLPOSTFIX_BACKWARD = "ApmServer/#/home?ep=0&g=-1&cha=0&cht=0&chs=0&m=L&l=ATC&fa=%5B%5D&u=UN1&view=%7B%22drillDown%22:%5B%5D%7D";
	public static final String EMEXPVIEWURLPOSTFIX = "apm/atc/#/home?ep=0&g=-1&cha=0&cht=0&chs=0&m=L&l=ATC&fa=%5B%5D&u=UN1&view=%7B%22drillDown%22:%5B%5D%7D";
	public static final String APPLICATIONNAME = "application.name";
	public static final String METRICCLAMP = "metric.clamp";
	public static final String BUILDSINHISTOGRAM = "histogram.builds";
	public static final String HISTOGRAMOUTPUTHTML = "histogramoutputhtml";
	public static final String COMPARISONMETADATACONFIGMETHOD = "setComparisonMetadata";

	private Constants() {
	}
}
