package com.ca.apm.jenkins.core.load;

import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.LoadRunnerMetadata;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import com.ca.apm.jenkins.core.util.JenkinsPluginUtility;

/**
 * This class is used to fetch the metadata for manual load runner based on the
 * provided current, benchmark start and end time.
 * 
 */
public class ManualMetadataRetriever implements LoadRunnerMetadataRetriever {
	
	private LoadRunnerMetadata loadRunnerMetadata=null;
	
	public ManualMetadataRetriever(LoadRunnerMetadata loadRunnerMetadata) throws BuildComparatorException  {
		setLoadRunnerMetadata(loadRunnerMetadata);
		
	}
	
	public LoadRunnerMetadata getLoadRunnerMetadata() 
	{
		return loadRunnerMetadata;
	}

	public void setLoadRunnerMetadata(LoadRunnerMetadata loadRunnerMetadata)
	 {
		this.loadRunnerMetadata = loadRunnerMetadata;
	}
	
	/**
	 * This method is used to fetch metadata based on given current and
	 * benchmark start and end time for manual load runner.
	 * 
	 */
	public void fetchExtraMetadata() throws BuildValidationException {
		
		String loadRunnerName = loadRunnerMetadata.getLoadRunnerPropertyValue("loadgenerator.name");
		String currentBuildStartTime = loadRunnerMetadata
				.getLoadRunnerPropertyValue(loadRunnerName + ".currentrunloadstarttime");
		String currentBuildEndTime = loadRunnerMetadata
				.getLoadRunnerPropertyValue(loadRunnerName + ".currentrunloadendtime");
		String benchMarkBuildStartTime = loadRunnerMetadata
				.getLoadRunnerPropertyValue(loadRunnerName + ".benchmarkrunloadstarttime");
		String benchMarkBuildEndTime = loadRunnerMetadata
				.getLoadRunnerPropertyValue(loadRunnerName + ".benchmarkrunloadendtime");
				
		if (currentBuildEndTime == null ||currentBuildEndTime.isEmpty() || currentBuildStartTime == null || currentBuildStartTime.isEmpty() || benchMarkBuildEndTime == null ||
				benchMarkBuildEndTime.isEmpty() || benchMarkBuildStartTime == null || benchMarkBuildStartTime.isEmpty()) {
			throw new BuildValidationException(
					"The currentBuildEndTime, currentBuildStartTime, benchMarkBuildEndTime, benchMarkBuildStartTime should not be empty, hence abnormally terminating the plug-in");
		}
		long cStartTime = JenkinsPluginUtility.getLongTimeValue(currentBuildStartTime);
		long cEndTime = JenkinsPluginUtility.getLongTimeValue(currentBuildEndTime);
		long bStartTime = JenkinsPluginUtility.getLongTimeValue(benchMarkBuildStartTime);
		long bEndTime = JenkinsPluginUtility.getLongTimeValue(benchMarkBuildEndTime);
		
		  loadRunnerMetadata.setBenchMarBuildTimes(bStartTime, bEndTime);
			loadRunnerMetadata.setCurrentBuildTimes(cStartTime, cEndTime);
			JenkinsPlugInLogger
					.fine("Current Build Info while loading " + loadRunnerMetadata.getCurrentBuildInfo().toString());
			JenkinsPlugInLogger
					.fine("BenchMark Build Info while loading " + loadRunnerMetadata.getBenchMarkBuildInfo().toString());
	}
	
	
	
	
}
