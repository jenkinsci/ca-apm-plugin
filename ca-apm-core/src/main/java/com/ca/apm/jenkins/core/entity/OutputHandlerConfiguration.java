package com.ca.apm.jenkins.core.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This entity holds all the configuration information about the output-handler which was configured
 * in the properties file
 *
 * @author Avinash Chandwani
 */
public class OutputHandlerConfiguration implements Serializable{

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
      properties = new HashMap<>();
    }
    properties.put(key, value);
  }
}
