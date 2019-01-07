package com.ca.apm.jenkins.api.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This entity holds the properties required in the Output-Handling stage. It contains the
 * properties like output directory, build run times, jenkins information, which can be used in the
 * Output-phase
 *
 * @author Avinash Chandwani
 */
public class OutputConfiguration {

  private Map<String, String> commonProperties;
  private Map<String, String> handlerSpecificProperties;
  private List<BuildInfo> histogramBuildInfoList;

  public OutputConfiguration() {
    super();
    commonProperties = new HashMap<String, String>();
    histogramBuildInfoList = new ArrayList<BuildInfo>();
  }

  public Map<String, String> getProperties() {
    return commonProperties;
  }

  public void addToCommonProperties(String key, String value) {
    commonProperties.put(key, value);
  }

  public String getCommonPropertyValue(String key) {
    if (commonProperties.containsKey(key)) {
      return commonProperties.get(key);
    }
    return null;
  }

  public String getHandlerSpecificPropertyValue(String key) {
    if (handlerSpecificProperties.containsKey(key)) {
      return handlerSpecificProperties.get(key);
    }
    return null;
  }

  public void setHandlerSpecificProperties(Map<String, String> handlerSpecificProperties) {
    this.handlerSpecificProperties = handlerSpecificProperties;
  }

  public List<BuildInfo> getHistogramBuildInfoList() {
    return histogramBuildInfoList;
  }

  public void setHistogramBuildInfoList(List<BuildInfo> histogramBuildInfoList) {
    this.histogramBuildInfoList = histogramBuildInfoList;
  }
}
