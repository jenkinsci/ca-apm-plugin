package com.ca.apm.jenkins.core.entity;

/**
 * TimeSliceValue represents one entry of value, min, max and count metric data metric values for
 * given metric-path
 *
 * @author Avinash Chandwani
 */
public class TimeSliceValue {

  private double value;
  private double max;
  private double min;
  private int count;
  private double frequency;

  public TimeSliceValue(double value, double max, double min, int count, double frequency) {
    super();
    this.value = value;
    this.max = max;
    this.min = min;
    this.count = count;
    this.frequency = frequency;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public double getMax() {
    return max;
  }

  public void setMax(double max) {
    this.max = max;
  }

  public double getMin() {
    return min;
  }

  public void setMin(double min) {
    this.min = min;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public double getfrequency() {
    return frequency;
  }

  public void setfrequency(double frequency) {
    this.frequency = frequency;
  }

  @Override
  public String toString() {
    return "TimeSliceValue [value="
        + value
        + ", max="
        + max
        + ", min="
        + min
        + ", count="
        + count
        + ", frequency="
        + frequency
        + "]";
  }
}
