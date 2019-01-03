package com.ca.apm.jenkins.core.entity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is the default implementation of one comparison-strategy's result This
 * contains the map of agent-wise performance-comparison result.
 * 
 * This entity is used by our default comparison-strategies. The output type of
 * every default comparison-strategy is this entity.
 * 
 * The example of our DefaultStrategyResult is like: StrategyResult of type
 * DefaultStrategyResult is the return type for our default provided
 * comparison-strategies
 * 
 * You can refer to our default output-handlers as to how they receive
 * DefaultStrategyResult Object as their input and how processing happen
 * 
 * @author Avinash Chandwani
 *
 */
public class DefaultStrategyResult {

	private Map<String, AgentComparisonResult> result = new LinkedHashMap<String, AgentComparisonResult>();

	public Map<String, AgentComparisonResult> getResult() {
		return result;
	}

	public void addOneResult(String agentSpecifier, AgentComparisonResult agentComparisonResult) {
		getResult().put(agentSpecifier, agentComparisonResult);
	}
}