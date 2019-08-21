package com.ca.apm.jenkins.core.helper;

import com.ca.apm.jenkins.api.exception.BuildExecutionException;
import com.ca.apm.jenkins.core.entity.EmailInfo;
import com.ca.apm.jenkins.core.executor.CommonEncryptionProvider;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * This utility is provided to you to send email output. For this you have to
 * configure all email parameters in output-handler properties provided by you.
 * An EmailInfo object is provided to you in the OutputStrategy entity, you can
 * utilize it. If you want to send attachments, then add the file name to the
 * list
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
		return emailInfo;
	}

	private static void addToRecipients(MimeMessage message, EmailInfo emailInfo) throws MessagingException {
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
	}

	private static void updateMultipart(Multipart multipart) throws MessagingException {
		for (String filePath : emailInfo.getAttachments()) {
			MimeBodyPart attachPart = new MimeBodyPart();

			try {
				attachPart.attachFile(filePath);
			} catch (IOException ex) {
				JenkinsPlugInLogger.severe("Error in attaching the file to email " + ex.getMessage(), ex);
				continue;
			}
			multipart.addBodyPart(attachPart);
		}
	}

	/**
	 * This utility helps you to send email
	 *
	 * @return boolean flag stating whether email was sent successfully or not
	 * @throws BuildExecutionException
	 *             Throws BuildExecutionException if any error occurs during
	 *             sending email
	 */
	public static boolean sendEmail() throws BuildExecutionException {
		boolean isSent = false;
		Properties props = new Properties();
		props.put("mail.smtp.host", emailInfo.getSmtpHost());
		props.put("mail.smtp.auth", emailInfo.isMailSmtpAuth());
		props.put("mail.mode", emailInfo.getMailMode());
		if (emailInfo.getMailMode().equalsIgnoreCase("gmail")) {
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.protocol", "TLS");
			props.put("mail.smtp.socketFactory.port", emailInfo.getGmailSocketPort());
			props.put("mail.smtp.port", emailInfo.getGmailSmtpPort());
		}
		Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailInfo.getSenderEmailId(), emailInfo.getPassword());
			}
		});
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(emailInfo.getSenderEmailId()));
			if (emailInfo.getToRecipients() == null && emailInfo.getCcRecipients() == null
					&& emailInfo.getBccRecipients() == null) {
				JenkinsPlugInLogger
						.warning("No email Id provided in output configuration, hence no email will be sent.");
				return false;
			}
			addToRecipients(message, emailInfo);
			if (emailInfo.getToRecipients() != null) {
				message.setSubject(emailInfo.getMessageSubject());
				message.setContent(emailInfo.getMessageBody(), emailInfo.getMessageContentType());
				if (emailInfo.getAttachments() != null && !emailInfo.getAttachments().isEmpty()) {
					Multipart multipart = new MimeMultipart();
					updateMultipart(multipart);
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

	private static String encryptPassword(PropertiesConfiguration properties, String key, String value,
			String performanceComparatorProperties)
			throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
		CommonEncryptionProvider encryptProvider = new CommonEncryptionProvider();
		if (value.startsWith("ENC(")) {
			value = encryptProvider.decrypt(value.substring(4));
		} else {
			String encryptedPassword = encryptProvider.encrypt(value);
			String apmEncryptionPrefix = "ENC(";
			if (encryptedPassword != null && !encryptedPassword.equals("")
					&& !encryptedPassword.startsWith(apmEncryptionPrefix)) {
				encryptedPassword = apmEncryptionPrefix + encryptedPassword;
			}
			FileOutputStream out;
			try {
				out = new FileOutputStream(performanceComparatorProperties);
				properties.setProperty(key, encryptedPassword);
				properties.save(out);
				out.close();
			} catch (FileNotFoundException e) {
				JenkinsPlugInLogger.severe("Property file is not accessible or does not found", e);
			} catch (ConfigurationException e) {
				JenkinsPlugInLogger.severe("The requested encoding is not supported, try the default encoding.", e);
			} catch (IOException e) {
				JenkinsPlugInLogger.severe("Input-Output Operation Failed or Interrupted", e);
			}
		}
		return value;
	}

	public static String passwordEncrytion(PropertiesConfiguration properties, String key, String value,
			String performanceComparatorProperties) {

		try {
			value = encryptPassword(properties, key, value, performanceComparatorProperties);

		} catch (InvalidKeyException e) {
			JenkinsPlugInLogger.severe("Invalid Encrytion key", e);
		} catch (NoSuchAlgorithmException e) {
			JenkinsPlugInLogger.severe("Encrytion Algorithm is not available or not working", e);
		} catch (InvalidKeySpecException e) {
			JenkinsPlugInLogger.severe("Invalid Encrytion/Decryption Key", e);
		} catch (NoSuchPaddingException e) {
			JenkinsPlugInLogger.severe("Invalid Encrpyt/Decrypt Parameters", e);
		} catch (InvalidAlgorithmParameterException e) {
			JenkinsPlugInLogger.severe("Invalid or Incorrect AlgorithmParameter", e);
		} catch (BadPaddingException e) {
			JenkinsPlugInLogger.severe("Incorrect Encrpyt/Decrypt Parameters", e);
		} catch (IllegalBlockSizeException e) {
			JenkinsPlugInLogger.severe("Input data length not matching to block size", e);
		}
		return value;
	}

}
