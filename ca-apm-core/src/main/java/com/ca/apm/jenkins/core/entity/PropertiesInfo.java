package com.ca.apm.jenkins.core.entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ca.apm.jenkins.api.entity.BuildInfo;
import com.ca.apm.jenkins.api.entity.StrategyConfiguration;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;

public class PropertiesInfo implements Serializable{

	//APMConnectionInfo
	 private String emURL;
	 private String emAuthToken;
	 private String emTimeZone;
	 private String emWebViewPort;
	 
	 //EmailInfo
	       // Basic fields
	/*  private String senderEmailId;
	  private String password;
	  private String smtpHost;
	  private boolean mailSmtpAuth;
	  private String mailMode;
	  private String gmailSmtpPort;
	  private String gmailSocketPort;
	  private List<String> toRecipients;
	  private List<String> ccRecipients;
	  private List<String> bccRecipients;

	       // Additional fields filled up by end user
	  private String messageSubject;
	  private String messageBody;
	  private String messageContentType;
	  private List<String> attachments;*/
	  
	  
	  //JenkinsInfo
	  private int currentBuildNumber;
	  private int lastSuccessfulBuildNumber;
	  private String buildWorkSpaceFolder;
	  private String jobName;
	  private List<BuildInfo> histogramBuildInfoList;
	 
		//StrategiesInfo
	    private Map<String, StrategyConfiguration> comparisonStrategiesInfo;
		private Map<String, OutputHandlerConfiguration> outputHandlersInfo;
		private Map<String, Set<String>> outputHandlerToComparisonStrategies;
		private Map<String, String> additionalProperties;
		private Set<String> nonMappedComparisonStrategies;
		
		//DOI properties benchmarkBuildNumber, APPLICATIONHOST, DOITIMEZONE, DOITENANTID, JARVISENDPOINT,
		 private Map<String, String> doiProperties = new HashMap<>();
		 
	   	//common properties logginglevel, extensionDirectory, buildPassorFail, publishBuildResultoEM, buildchangeeventtodoi
		 private Map<String, String> commonProperties = new HashMap<>(); 
		
		 
		  private Map<String, String> handlerSpecificProperties;
		  ////
		  private List<BuildInfo> histogramBuildInfoListProp;
		  private Map<String,Map<String, String>> scmRepoAttribs = new HashMap<>();
		
		  //BuildInfo
		  private int number;
		  private long startTime;
		  private long endTime;
		  private String status;

		  private Map<String, String> scmRepoParams = new HashMap<>();		 
		  
		  //StrategyConfig
		  Map<String, StrategyConfiguration>  strategyConfigProperty ;
		
		  //OutputHandler to ComparisonStrategy map
		  private Map<String, String> outputHandlerConfigProperties;
		  
		  //OutputHandlerConfiguration properties map
		  private Map<String, OutputHandlerConfiguration> outputHandlerConfig;
		  
		  
	 //APMConnectionInfo
	public String getEmURL() {
		return emURL;
	}
	public void setEmURL(String emURL) {
		this.emURL = emURL;
	}
	public String getEmAuthToken() {
		return emAuthToken;
	}
	public void setEmAuthToken(String emAuthToken) {
		this.emAuthToken = emAuthToken;
	}
	public String getEmTimeZone() {
		return emTimeZone;
	}
	public void setEmTimeZone(String emTimeZone) {
		this.emTimeZone = emTimeZone;
	}
	
	
	public String getEmWebViewPort() {
		return emWebViewPort;
	}
	public void setEmWebViewPort(String emWebViewPort) {
		this.emWebViewPort = emWebViewPort;
	}
	
	private Map<String, String> emailProperties = new HashMap<>(); 
		//EmailInfo
	    /* public void setRecipients(String recipientsList) {
		    String[] emailIds = recipientsList.split(",");
		    if (emailIds.length == 0) {
		      JenkinsPlugInLogger.warning(
		          "No Email id found in the output settings, hence email won't be sent");
		    } else {
		      for (String emailId : emailIds) {
		        toRecipients.add(emailId);
		      }
		    }
		  }

		  public String getSenderEmailId() {
		    return senderEmailId;
		  }

		  public void setSenderEmailId(String senderEmailId) {
		    this.senderEmailId = senderEmailId;
		  }

		  public String getPassword() {
		    return password;
		  }

		  public void setPassword(String password) {
		    this.password = password;
		  }

		  public String getSmtpHost() {
		    return smtpHost;
		  }

		  public void setSmtpHost(String smtpHost) {
		    this.smtpHost = smtpHost;
		  }

		  public boolean isMailSmtpAuth() {
		    return mailSmtpAuth;
		  }

		  public void setMailSmtpAuth(boolean mailSmtpAuth) {
		    this.mailSmtpAuth = mailSmtpAuth;
		  }

		  public String getMessageSubject() {
		    return messageSubject;
		  }

		  public void setMessageSubject(String messageSubject) {
		    this.messageSubject = messageSubject;
		  }

		  public String getMessageBody() {
		    return messageBody;
		  }

		  public void setMessageBody(String messageBody) {
		    this.messageBody = messageBody;
		  }

		  public String getMessageContentType() {
		    return messageContentType;
		  }

		  public void setMessageContentType(String messageContentType) {
		    this.messageContentType = messageContentType;
		  }

		  public List<String> getAttachments() {
		    return attachments;
		  }

		  public void addToAttachments(String attachment) throws FileNotFoundException {
		    File attachFile = new File(attachment);
		    if (!attachFile.exists()) {
		      throw new FileNotFoundException(attachment + " file does not exist, please check");
		    }
		    if (attachments == null) {
		      attachments = new LinkedList<>();
		    }
		    attachments.add(attachment);
		  }

		  public List<String> getCcRecipients() {
		    return ccRecipients;
		  }

		  public void setCcRecipients(List<String> ccRecipients) {
		    this.ccRecipients = ccRecipients;
		  }

		  public List<String> getBccRecipients() {
		    return bccRecipients;
		  }

		  public void setBccRecipients(List<String> bccRecipients) {
		    this.bccRecipients = bccRecipients;
		  }

		  public List<String> getToRecipients() {
		    return toRecipients;
		  }

		  public void setToRecipients(List<String> recipients) {
		    this.toRecipients = recipients;
		  }
		  public String getMailMode() {
			  return mailMode; 
		  }
		  
		  public void setMailMode(String mailMode) {
			    this.mailMode = mailMode;
		  }
		  public String getGmailSmtpPort() {
			  return gmailSmtpPort; 
		  }
		  
		  public void setGmailSmtpPort(String gmailSmtpPort) {
			    this.gmailSmtpPort = gmailSmtpPort;
		  }
		  public String getGmailSocketPort() {
			  return gmailSocketPort; 
		  }
		  
		  public void setGmailSocketPort(String gmailSocketPort) {
			    this.gmailSocketPort = gmailSocketPort;
		  }
		  */
		  //JenkinsInfo
		      public int getCurrentBuildNumber() {
			    return currentBuildNumber;
			  }

			  public void setCurrentBuildNumber(int buildNumber) {
			    this.currentBuildNumber = buildNumber;
			  }

			  public int getLastSuccessfulBuildNumber() {
			    return lastSuccessfulBuildNumber;
			  }

			  public void setLastSuccessfulBuildNumber(int lastSuccessfulBuildNumber) {
			    this.lastSuccessfulBuildNumber = lastSuccessfulBuildNumber;
			  }

			  public String getBuildWorkSpaceFolder() {
			    return buildWorkSpaceFolder;
			  }

			  public void setBuildWorkSpaceFolder(String buildWorkSpaceFolder) {
			    this.buildWorkSpaceFolder = buildWorkSpaceFolder;
			  }

			  public String getJobName() {
			    return jobName;
			  }

			  public void setJobName(String jobName) {
			    this.jobName = jobName;
			  }

			  public List<BuildInfo> getHistogramBuildInfoList() {
			    return histogramBuildInfoList;
			  }

			 public void setHistogramBuildInfoList(List<BuildInfo> histogramBuildInfoList) {
				this.histogramBuildInfoList = histogramBuildInfoList;
			 }
					
	    
			//StrategiesInfo
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
			
			
			public Map<String, String> getCommonProperties() {
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
                ////
			  public List<BuildInfo> getHistogramBuildInfoListProp() {
			    return histogramBuildInfoList;
			  }
               ////
			  public void setHistogramBuildInfoListProp(List<BuildInfo> histogramBuildInfoList) {
			    this.histogramBuildInfoList = histogramBuildInfoList;
			  }
			  
			  public void addToSCMRepoAttribs(String key, Map<String,String> value) {
				  scmRepoAttribs.put(key, value);
			  }

			  public Map<String,String> getSCMRepoAttribValue(String key) {
				    if (scmRepoAttribs.containsKey(key)) {
				      return scmRepoAttribs.get(key);
				    }
				    return null;
			  }
			  
	         //BuildInfo
			  public int getNumber() {
					return number;
				}

				public void setNumber(int buildNumber) {
					this.number = buildNumber;
				}

				public long getStartTime() {
					return startTime;
				}

				public void setStartTime(long buildStartTime) {
					this.startTime = buildStartTime;
				}

				public long getEndTime() {
					return endTime;
				}

				public void setEndTime(long buildEndTime) {
					this.endTime = buildEndTime;
				}

				public String getStatus() {
					return status;
				}

				public void setStatus(String status) {
					this.status = status;
				}

				public Map<String, String> getSCMRepoParams() {
					return scmRepoParams;
				}

				public void addToSCMRepoParams(String key, String value) {
					scmRepoParams.put(key, value);
				}
				
				//DOI Properties
				public Map<String, String> getDoiProperties() {
				    return doiProperties;
			    }

				  public void addToDoiProperties(String key, String value) {
				    doiProperties.put(key, value);
				  }

				  public String getDoiPropertyValue(String key) {
				    if (doiProperties.containsKey(key)) {
				      return doiProperties.get(key);
				    }
				    return null;
				 }
				  
				//outputHandler to ComparisonStrategy map			  
					  public Map<String, String> getOutputHandlerConfigProperties() {
						    return outputHandlerConfigProperties;
						  }

						  public String getOutputHandlerConfigPropertyValue(String key) {
						    if (outputHandlerConfigProperties.containsKey(key)) {
						      return outputHandlerConfigProperties.get(key);
						    }
						    return null;
						  }

						  public void addOutputHandlerConfigProperty(String key, String value) {
						    if (outputHandlerConfigProperties == null) {
						    	outputHandlerConfigProperties = new HashMap<>();
						    }
						    outputHandlerConfigProperties.put(key, value);
						  }
						  
					//StrategyConfiguration	  
						public Map<String, StrategyConfiguration> getStrategyConfigProperty() {
							return strategyConfigProperty;
						}
					 
						
						public void addStrategyConfigProperty(String key, StrategyConfiguration value) {
						    if (strategyConfigProperty == null) {
						    	strategyConfigProperty = new HashMap<>();
						    }
						    strategyConfigProperty.put(key, value);
						  }
						  
						  
					//OutputHandler Configuration
						
						public Map<String, OutputHandlerConfiguration> getOutputHandlerConfig() {
							return outputHandlerConfig;
						}
					 
						
						public void addOutputHandlerConfig(String key, OutputHandlerConfiguration value) {
						    if (outputHandlerConfig == null) {
						    	outputHandlerConfig = new HashMap<>();
						    }
						    outputHandlerConfig.put(key, value);
						  }
						
						public Map<String, String> getEmailProperties() {
						    return emailProperties;
						  }

						  public void addToEmailProperties(String key, String value) {
							  emailProperties.put(key, value);
						  }

						  public String getEmailPropertyValue(String key) {
						    if (emailProperties.containsKey(key)) {
						      return emailProperties.get(key);
						    }
						    return null;
						  }

}
