package com.ca.apm.jenkins.core.entity;

import org.json.JSONObject;

/**
 * Enity which holds information of one graph of chart output handler for a
 * given comparison-strategy
 * 
 * @author chaav03
 *
 */
public class JenkinsAMChart {

	private JSONObject chartJSONObject;
	private String divId;

	public JenkinsAMChart() {
		super();
	}

	public JSONObject getChartJSONObject() {
		return chartJSONObject;
	}

	public void setChartJSONObject(JSONObject chartJSONObject) {
		this.chartJSONObject = chartJSONObject;
	}

	public String getDivId() {
		return divId;
	}

	public void setDivId(String divId) {
		this.divId = divId;
	}
}