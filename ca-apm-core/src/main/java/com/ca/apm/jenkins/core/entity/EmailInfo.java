package com.ca.apm.jenkins.core.entity;

import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

/**
 * This entity holds the email information which you want to send to the users on the completion of
 * Jenkins Plugin Run The basic fields will be populated by the plug-in based upon the
 * output-handler configuration provided by you. The additional fields have to be set by you.
 *
 * @author Avinash Chandwani
 */
public class EmailInfo {

  // Basic fields
  private String senderEmailId;
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
  private List<String> attachments;

  public EmailInfo() {
    super();
  }

  public void setRecipients(String recipientsList) {
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
}
