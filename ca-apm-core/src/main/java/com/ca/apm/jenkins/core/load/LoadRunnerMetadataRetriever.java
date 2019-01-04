package com.ca.apm.jenkins.core.load;

import com.ca.apm.jenkins.api.exception.BuildComparatorException;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.entity.LoadRunnerMetadata;

/**
 * This is the default implementation of configuring load-runner metadata You
 * are free to extend this entity, but make sure that you populate the
 * LoadRunnerMetadata entity appropriately Also makes sure if you extend this
 * entity, you place the class in the extensions folder and mention the path of
 * the folder where the jar file exist,
 * 
 * @author Avinash Chandwani
 *
 */

public interface LoadRunnerMetadataRetriever {

	
	public LoadRunnerMetadata getLoadRunnerMetadata(); 
	
	public void setLoadRunnerMetadata(LoadRunnerMetadata loadRunnerMetadata);
	

	/**
	 * This method is invoked when this object is initialized.
	 *
	 * @throws BuildComparatorException
	 *             : In case any exception occurs while fetching data it throws
	 *             BuildComparatorException
	 */
	public void fetchExtraMetadata() throws BuildValidationException;
}