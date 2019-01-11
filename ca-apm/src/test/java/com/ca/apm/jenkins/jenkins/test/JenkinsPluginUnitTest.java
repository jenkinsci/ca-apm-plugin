package com.ca.apm.jenkins.jenkins.test;

/**
 * The Class to execute JUnit Test cases during the build to make sure that code is not broken on
 * newer releases
 *
 * @author Avinash Chandwani
 */
public class JenkinsPluginUnitTest {

  public static void main(String[] args) {
    JenkinsPluginUnitTest unitTest = new JenkinsPluginUnitTest();
    // unitTest.testOne();
  }

  /*// This test to verify it fails if properties file are not found
  @Test
  public void testOne() {
  	PluginRunSimulation simulator = new PluginRunSimulation();
  	String path = "src/test/java/testdata/testOne/workspace/CIGNAOne";
  	try {
  		simulator.runPluginSimulation(path, "performance-comparato.properties");
  	} catch (BuildComparatorException e) {
  		String expectedMessage = "Input Properties file(s) defined in parameters does not exist, please check";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	} catch (BuildValidationException e) {
  		String expectedMessage = "Input Properties file(s) defined in parameters does not exist, please check";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}catch(BuildExecutionException ex){
  		String expectedMessage = "Input Properties file(s) defined in parameters does not exist, please check";
  		String actualMessage = ex.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}
  }

  @Test
  public void testTwo() {
  	PluginRunSimulation simulator = new PluginRunSimulation();
  	String path = "src/test/java/testdata/testTwo/workspace/CIGNAOne";
  	try {
  		simulator.runPluginSimulation(path, "performance-comparator.properties");
  	} catch (BuildComparatorException e) {
  		String expectedMessage = "   Error : Benchmark Build's load runner end time is less than start time\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	} catch (BuildValidationException e) {
  		String expectedMessage = "   Error : Benchmark Build's load runner end time is less than start time\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}catch (BuildExecutionException e) {
  		String expectedMessage = "   Error : Benchmark Build's load runner end time is less than start time\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}
  }

  @Test
  public void testThree() {
  	PluginRunSimulation simulator = new PluginRunSimulation();
  	String path = "src/test/java/testdata/testThree/workspace/CIGNAOne";
  	try {
  		simulator.runPluginSimulation(path, "performance-comparator.properties");
  	} catch (BuildComparatorException e) {
  		String expectedMessage = "   Error : Benchmark Build's load runner end time is less than start time\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	} catch (BuildValidationException e) {
  		String expectedMessage = "   Error : Benchmark Build's load runner end time is less than start time\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}
  	catch (BuildExecutionException e) {
  		String expectedMessage = "   Error : Benchmark Build's load runner end time is less than start time\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}

  }

  @Test
  public void testFour() {
  	PluginRunSimulation simulator = new PluginRunSimulation();
  	String path = "src/test/java/testdata/testFour/workspace/CIGNAOne";
  	try {
  		simulator.runPluginSimulation(path, "performance-comparator.properties");
  	} catch (BuildComparatorException e) {
  		String expectedMessage = "   Error : Comparison Strategy handler for meanLatencyStrategy is not defined\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	} catch (BuildValidationException e) {
  		String expectedMessage = "   Error : Comparison Strategy handler for meanLatencyStrategy is not defined\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}catch (BuildExecutionException e) {
  		String expectedMessage = "   Error : Comparison Strategy handler for meanLatencyStrategy is not defined\n";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}
  }

  @Test
  public void testFive() {
  	PluginRunSimulation simulator = new PluginRunSimulation();
  	String path = "src/test/java/testdata/testFive/workspace/CIGNAOne";
  	try {
  		simulator.runPluginSimulation(path, "performance-comparator.properties");
  	} catch (BuildComparatorException e) {
  		String expectedMessage = "No output-handler(s) defined in the configuration, hence exiting";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	} catch (BuildValidationException e) {
  		String expectedMessage = "No output-handler(s) defined in the configuration, hence exiting";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}catch(BuildExecutionException e){
  		String expectedMessage = "No output-handler(s) defined in the configuration, hence exiting";
  		String actualMessage = e.getMessage();
  		assertEquals(expectedMessage, actualMessage);
  	}
  }*/
}
