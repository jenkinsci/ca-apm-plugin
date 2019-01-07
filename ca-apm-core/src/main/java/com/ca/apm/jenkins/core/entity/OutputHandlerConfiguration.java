package com.ca.apm.jenkins.core.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * This entity holds all the configuration information about the output-handler which was configured
 * in the properties file
 *
 * @author Avinash Chandwani
 */
public class OutputHandlerConfiguration {

  private Map<String, String> properties;

  public OutputHandlerConfiguration() {
    super();
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public String getPropertyValue(String key) {
    if (properties.containsKey(key)) {
      return properties.get(key);
    }
    return null;
  }

  public void addProperty(String key, String value) {
    if (properties == null) {
      properties = new HashMap<String, String>();
    }
    properties.put(key, value);
  }
}
