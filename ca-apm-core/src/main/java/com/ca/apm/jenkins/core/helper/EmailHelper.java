package com.ca.apm.jenkins.core.helper;

import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.EmailInfo;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import java.io.IOException;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * This utility is provided to you to send email output. For this you have to configure all email
 * parameters in output-handler properties provided by you. An EmailInfo object is provided to you
 * in the OutputStrategy entity, you can utilize it. If you want to send attachments, then add the
 * file name to the list
 *
 * @author Avinash Chandwani
 */
public class EmailHelper {

  private static EmailInfo emailInfo;

  private EmailHelper() {

    super();
  }

  public static void setEmailInfo(EmailInfo emailInfo) {
    EmailHelper.emailInfo = emailInfo;
  }

  public static EmailInfo getEMailInfo() {
    if (emailInfo == null) {}
    return emailInfo;
  }

  /**
   * This utility helps you to send email
   *
   * @return boolean flag stating whether email was sent successfully or not
   * @throws BuildExecutionException Throws BuildExecutionException if any error occurs during
   *     sending email
   */
  public static boolean sendEmail() throws BuildExecutionException {
    boolean isSent = false;
    Properties props = new Properties();
    props.put("mail.smtp.host", emailInfo.getSmtpHost());
    props.put("mail.smtp.auth", emailInfo.isMailSmtpAuth());
    Session session =
        Session.getDefaultInstance(
            props,
            new javax.mail.Authenticator() {
              @Override
              protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    emailInfo.getSenderEmailId(), emailInfo.getPassword());
              }
            });
    try {
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailInfo.getSenderEmailId()));
      if (emailInfo.getToRecipients() == null
          && emailInfo.getCcRecipients() == null
          && emailInfo.getBccRecipients() == null) {
        JenkinsPlugInLogger.warning(
            "No email Id provided in output configuration, hence no email will be sent.");
        return false;
      }
      if (emailInfo.getToRecipients() != null) {
        for (String toRecepient : emailInfo.getToRecipients()) {
          message.addRecipient(Message.RecipientType.TO, new InternetAddress(toRecepient));
        }
      }
      if (emailInfo.getCcRecipients() != null && !emailInfo.getCcRecipients().isEmpty()) {
        for (String ccRecepient : emailInfo.getCcRecipients()) {
          message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccRecepient));
        }
      }
      if (emailInfo.getBccRecipients() != null) {
        for (String bccRecepient : emailInfo.getCcRecipients()) {
          message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccRecepient));
        }
      }

      if (emailInfo.getToRecipients() != null) {
        message.setSubject(emailInfo.getMessageSubject());
        message.setContent(emailInfo.getMessageBody(), emailInfo.getMessageContentType());
        if (emailInfo.getAttachments() != null && !emailInfo.getAttachments().isEmpty()) {
          Multipart multipart = new MimeMultipart();

          for (String filePath : emailInfo.getAttachments()) {
            MimeBodyPart attachPart = new MimeBodyPart();

            try {
              attachPart.attachFile(filePath);
            } catch (IOException ex) {
              JenkinsPlugInLogger.severe(
                  "Error in attaching the file to email " + ex.getMessage(), ex);
              continue;
            }
            multipart.addBodyPart(attachPart);
          }
          message.setContent(multipart);
        }
      }
      Transport.send(message);
      JenkinsPlugInLogger.info("Email Sent Successfully");
      isSent = true;
    } catch (MessagingException e) {
      JenkinsPlugInLogger.severe("Error occured while sending email ->" + e.getMessage(), e);
      throw new BuildExecutionException(
          "An error occured during sending mail, please check logs for more details");
    }
    return isSent;
  }
}
