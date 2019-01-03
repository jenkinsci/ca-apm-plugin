package com.ca.apm.jenkins.api.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This entity holds the configuration of a comparison strategy which is
 * configured in strategyProperties file
 * 
 * @author Avinash Chandwani
 *
 */
public class StrategyConfiguration {

	private List<String> agentSpecifiers;
	private Map<String, String> properties;

	public StrategyConfiguration() {
		super();
	}

	public List<String> getAgentSpecifiers() {
		return agentSpecifiers;
	}

	public void setAgentSpecifiers(List<String> agentSpecifiers) {
		this.agentSpecifiers = agentSpecifiers;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void addProperty(String key, String value) {
		if (properties == null) {
			properties = new HashMap<String, String>();
		}
		properties.put(key, value);
	}

	public String getPropertyValue(String key) {
		if (properties.containsKey(key)) {
			return properties.get(key);
		}
		return null;
	}
}