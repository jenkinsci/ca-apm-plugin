package com.ca.apm.jenkins.core.entity;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ca.apm.jenkins.api.entity.StrategyConfiguration;

/**
 * Entity which holds the complete comparison-strategies and output-handlers
 * information and their mapping
 *
 * @author Avinash Chandwani
 */
public class StrategiesInfo {

	private Map<String, StrategyConfiguration> comparisonStrategiesInfo;
	private Map<String, OutputHandlerConfiguration> outputHandlersInfo;
	private Map<String, Set<String>> outputHandlerToComparisonStrategies;
	private Map<String, String> additionalProperties;
	private Set<String> nonMappedComparisonStrategies;

	public StrategiesInfo() {
		super();
	}

	public void addToOutputHandlerToComparisonStrategies(String outputHandler, String comparisonStrategy) {
		if (outputHandlerToComparisonStrategies == null) {
			outputHandlerToComparisonStrategies = new LinkedHashMap<>();
		}
		Set<String> comparisonStrategies = null;
		if (outputHandlerToComparisonStrategies.containsKey(outputHandler)) {
			comparisonStrategies = outputHandlerToComparisonStrategies.get(outputHandler);
			comparisonStrategies.add(comparisonStrategy);
		} else {
			comparisonStrategies = new HashSet<>();
			comparisonStrategies.add(comparisonStrategy);
			outputHandlerToComparisonStrategies.put(outputHandler, comparisonStrategies);
		}
	}

	public void addComparisonStrategyInfo(String comparisonStrategyName, StrategyConfiguration comparisonStrategyInfo) {
		if (comparisonStrategiesInfo == null) {
			comparisonStrategiesInfo = new LinkedHashMap<>();
		}
		comparisonStrategiesInfo.put(comparisonStrategyName, comparisonStrategyInfo);
	}

	public void addOutputHandlersInfo(String outputHandler, OutputHandlerConfiguration outputHandlerInfo) {
		if (outputHandlersInfo == null) {
			outputHandlersInfo = new LinkedHashMap<>();
		}
		outputHandlersInfo.put(outputHandler, outputHandlerInfo);
	}

	public void addAdditionalProperties(String key, String value) {
		if (additionalProperties == null) {
			additionalProperties = new LinkedHashMap<>();
		}
		additionalProperties.put(key, value);
	}

	public Map<String, Set<String>> getOutputHandlerToComparisonStrategies() {
		return outputHandlerToComparisonStrategies;
	}

	public String getPropertyValue(String key) {
		return additionalProperties.get(key);
	}

	public Map<String, StrategyConfiguration> getComparisonStrategiesInfo() {
		return comparisonStrategiesInfo;
	}

	public Map<String, OutputHandlerConfiguration> getOutputHandlersInfo() {
		return outputHandlersInfo;
	}

	public Set<String> getMappedComparisonStrategies(String outputHandlers) {
		return outputHandlerToComparisonStrategies.get(outputHandlers);
	}

	public boolean isComparisonStrategyNonMapped(String comparisonStrategy) {
		boolean isComparisonStrategyNonMapped = false;

		if (nonMappedComparisonStrategies != null && nonMappedComparisonStrategies.contains(comparisonStrategy)) {
			isComparisonStrategyNonMapped = true;
		}
		return isComparisonStrategyNonMapped;
	}

	public void addToNonMappedComparisonStrategies(String comparisonStrategy) {
		if (nonMappedComparisonStrategies == null) {
			nonMappedComparisonStrategies = new LinkedHashSet<>();
		}
		nonMappedComparisonStrategies.add(comparisonStrategy);
	}
}
