package com.ca.apm.jenkins.api.entity;

import java.io.Serializable;

/**
 * A wrapper class for one comparison-strategy's result. The return type of each
 * comparison-strategy has to be of this type. You can define your own T and
 * create an implementation of your own output. You can refer
 * DefaultStrategyResult for creating your own strategy-result of your defined
 * type T.
 *
 * <p>
 * You can create your own object of type T. In case you want to convert your
 * custom output to JSON or XML, you can use DataFormatHelper class to do the
 * required transformation
 *
 * @author Avinash Chandwani
 * @param <T>
 *            You can define your own T as mentioned above
 */
public class StrategyResult<T> implements Serializable {

	private String strategyName;
	private T result;
	private double frequency;

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String stategyName) {
		this.strategyName = stategyName;
	}

	public double getFrequency() {
		return frequency;
	}

	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}

	@Override
	public String toString() {
		return "StrategyResult [strategyName=" + strategyName + ", result=" + result + ", frequency=" + frequency + "]";
	}
}
