package com.ca.apm.jenkins.performancecomparatorplugin.outputhandler;

import java.util.List;

import com.ca.apm.jenkins.api.OutputHandler;
import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.EmailInfo;
import com.ca.apm.jenkins.core.helper.DataFormatHelper;
import com.ca.apm.jenkins.core.helper.EmailHelper;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.Constants;

/**
 * A basic implementation of Output-Strategy which takes the selected
 * comparison-strategy results and prepare a plain-text output and email the
 * report based upon the email configurations
 * 
 * @author Avinash Chandwani
 *
 */
public class PlainTextEmailOutputHandler implements OutputHandler<StrategyResult> {

	private OutputConfiguration outputConfiguration;

	public void setOutputConfiguration(OutputConfiguration outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

	public void publishOutput(List<StrategyResult> comparisonStrategyResults)
			throws BuildComparatorException, BuildExecutionException {

		JenkinsPlugInLogger.fine("Executing publishOutput of PlainTextEmailOutputHandler");
		String htmlOutput = DataFormatHelper.getHTMLTextOutput(outputConfiguration, comparisonStrategyResults);
		EmailInfo emailInfo = EmailHelper.getEMailInfo();
		emailInfo.setMessageContentType("text/html");
		emailInfo.setMessageSubject(
				"Build Performance Report for " + outputConfiguration.getCommonPropertyValue(Constants.jenkinsCurrentBuild));
		emailInfo.setMessageBody(htmlOutput);
		boolean emailSendStatus = false;
		try {

			emailSendStatus = EmailHelper.sendEmail();
			if (!emailSendStatus) {
				throw new BuildExecutionException("Plain Text Email Output Handler failed due to error in sending email");
			}
		} catch (BuildExecutionException e) {
			JenkinsPlugInLogger.severe("Error occured while sending email ", e);
			throw new BuildExecutionException("Plain Text Email Output Handler failed due to " + e.getMessage());
		}
		JenkinsPlugInLogger.fine("Email Send status is " + emailSendStatus);
		JenkinsPlugInLogger.fine("Execution of publishOutput of PlainTextEmailOutputHandler completed successfully");
	}
}