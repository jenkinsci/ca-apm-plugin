package com.ca.apm.jenkins.performancecomparatorplugin.outputhandler;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ca.apm.jenkins.api.OutputHandler;
import com.ca.apm.jenkins.api.entity.OutputConfiguration;
import com.ca.apm.jenkins.api.entity.StrategyResult;
import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.helper.DataFormatHelper;
import com.ca.apm.jenkins.core.helper.FileHelper;
import com.ca.apm.jenkins.core.util.Constants;

/**
 * An implementation of Output-Strategy which takes the selected
 * comparison-strategy results and prepare a JSON output and stores into the
 * configured output.directory If output.directory is not provided, it will
 * flush to Jenkins Current Build workspace folder
 * 
 * @author Avinash Chandwani
 *
 */
@SuppressWarnings("rawtypes")
public class JSONFileStoreOutputHandler implements OutputHandler<StrategyResult> {

	private OutputConfiguration outputConfiguration;

	public void setOutputConfiguration(OutputConfiguration outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

	public void publishOutput(List<StrategyResult> comparisonStrategyResults)
			throws BuildComparatorException, BuildExecutionException {
		String outputPath = outputConfiguration.getCommonPropertyValue(Constants.outputDirectory);
		JSONObject outputObject = new JSONObject();
		JSONArray resultsArray = new JSONArray();
		outputObject.put("strategy_results", resultsArray);
		for (StrategyResult<?> strategyResult : comparisonStrategyResults) {
			String json = DataFormatHelper.generateJSONOutputForStrategy((StrategyResult<?>) strategyResult);
			JSONObject strategyJson = new JSONObject(json);
			resultsArray.put(strategyJson);
		}
		FileHelper.exportOutputToFile(outputPath, "json-file-store-output-handler.json", outputObject.toString(1));
	}
}