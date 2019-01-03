package com.ca.apm.jenkins.performancecomparatorplugin.outputhandler;

import java.text.SimpleDateFormat;
import java.util.List;

import com.ca.apm.jenkins.api.OutputHandler;
import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.helper.AMChartHelper;
import com.ca.apm.jenkins.core.util.Constants;

/**
 * A basic implementation Output Handler to render the comparison-results into
 * Google Charts
 * 
 * @author Avinash Chandwani
 *
 */
public class ChartOutputHandler implements OutputHandler<StrategyResult> {

	private OutputConfiguration outputConfiguration;

	public void setOutputConfiguration(OutputConfiguration outputConfiguration) {
		this.outputConfiguration = outputConfiguration;

	}

	public void publishOutput(List<StrategyResult> strategyResults)
			throws BuildComparatorException, BuildExecutionException {
		SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss z");
		String currentBuildNumber = outputConfiguration.getCommonPropertyValue(Constants.jenkinsCurrentBuild);
		String benchMarkBuildNumber = outputConfiguration.getCommonPropertyValue(Constants.jenkinsBenchMarkBuild);
		String workspaceFolder = outputConfiguration.getCommonPropertyValue(Constants.workSpaceDirectory);
		String jobName = outputConfiguration.getCommonPropertyValue(Constants.jenkinsJobName);
		String appMapURL = outputConfiguration.getCommonPropertyValue(Constants.appMapURL);
		String startTimeMillis = outputConfiguration.getCommonPropertyValue("runner.start");
		String endTimeMillis = outputConfiguration.getCommonPropertyValue("runner.end");
		appMapURL = appMapURL.concat("&ts1="+startTimeMillis+"&ts2="+endTimeMillis);
		String startDateTime= format.format(Long.parseLong(startTimeMillis));
		String endDateTime = format.format(Long.parseLong(endTimeMillis));
		AMChartHelper.produceChartOutput(strategyResults, workspaceFolder, jobName, benchMarkBuildNumber,
				currentBuildNumber, appMapURL, startDateTime, endDateTime);
	}

}
