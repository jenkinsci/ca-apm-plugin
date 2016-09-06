package com.ca.apm.swat.jenkins.caapm.utils;


import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.ca.apm.swat.jenkins.caapm.CAAPMBuildAction;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

public class EmailManager {
    
    private String smtpProtocol = null;
    private String smtpHost = null;
    private String smtpPort = null;
    private String username = null;
    private String password = null;
    private String timeout = "10000";
    private String recepient = null;
    Properties props = null;
    private AbstractBuild<?, ?> build;
    private BuildListener listener;
    private EnvVars envVars;
    private String projectName;
    
    
    private static final Logger LOGGER = Logger.getLogger(EmailManager.class.getName());

	public HashMap<String, String> globalConfigMap = null;

	public EmailManager ( AbstractBuild<?, ?> build, BuildListener listener ) {

	    props = new Properties(System.getProperties());
	    
	    this.build = build;
	    this.listener = listener;
	    
        try
        {
            envVars = build.getEnvironment(listener);
            
            LOGGER.log(Level.FINEST, "**** EmailManager::EmailManager envVars is " + envVars.get("mail.smtp.host"));
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (smtpHost == null) {
            smtpHost = envVars.get("mail.smtp.host");
        }
        if (smtpPort == null) {
            smtpPort = envVars.get("mail.smtp.port");
        }
        if (smtpProtocol == null) {
            smtpProtocol = envVars.get("mail.smtp.protocol");
        }
        if (username == null) {
            username = envVars.get("mail.smtp.username");
        }
       
        /*if (password == null) {
            password = envVars.get("mail.smtp.password");
        }
        if (recepient == null) {
            recepient = envVars.get("mail.smtp.recepient");
        }
        */
        if (projectName == null) {
            projectName = envVars.get("JOB_NAME");
        }
	}


	public void setRecepient( String recepient ) {
	    this.recepient = recepient;
	}
	
	public void setPassword( String password ) {
	    this.password = password;
	}
	
	
	public void sendMail( String buildNumber, String buildStatus, String buildReason ) {

	    if ( recepient == null || smtpHost == null || smtpPort == null || smtpProtocol == null 
	            || username == null || password == null )
	        return;
	    
	    LOGGER.log(Level.FINEST, "EmailManager::sendMail smtp host #" + smtpHost + "# smtpPort #" + smtpPort + 
	               "# recepient #" + recepient + "# password #" + password + "#smtpProtocol#"+smtpProtocol +"#username#" + username +"#");

		//Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.connectiontimeout", timeout);
        
		//if ( globalConfigMap.get("SMTPDebugEnabled").equals("true"))
		//	props.put("mail.debug", "true");
		
		//TLS - port 587
		
		if ( smtpProtocol.equals("TLS")){
			props.put("mail.smtp.starttls.enable", "true");
		}
		else if ( smtpProtocol.equals("SSL") ) {
			props.put("mail.smtp.socketFactory.port", smtpPort);
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.port", smtpPort);

		}

		
		
		/*
		//SSL - next three lines
*/		
		

		Session session = Session.getDefaultInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username,password);
			}
		});
		

		try {

			String[] recepients = recepient.split(",");
			String SMTPFrom = recepient;

			int numberOfRecepients = recepients.length; 

			for ( int i = 0;  i < numberOfRecepients; i++ ){

				String SMTPTo = recepients[i];

				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(SMTPFrom));
				message.setRecipients(Message.RecipientType.TO,
						InternetAddress.parse(SMTPTo));
		
				message.setSubject("Build " + buildNumber + " Of Jenkins Project \"" + projectName + "\" - " + buildStatus);

				

				// create the message part 
				MimeBodyPart mbp1 = new MimeBodyPart();
				
				if ( buildStatus.equals("SUCCESS"))
				    mbp1.setText("Dear " + SMTPTo.split("@")[0] + ", \n\n   Congratualtion. Build completed with Success \n\n\n Thanks" );
				else {
				    mbp1.setText("Dear " + SMTPTo.split("@")[0] + ", \n\n   Pls find below the reason for failure \n\n\n" +
	                        "\n\"" + buildReason +"\"\n"
	                        + "\n\nThanks");
				}


				Multipart mp = new MimeMultipart();
				mp.addBodyPart(mbp1);

				
				// add the Multipart to the message
				message.setContent(mp);

				Transport transport = null;
				
				transport = session.getTransport("smtp");
				transport.connect(smtpHost, new Integer(smtpPort), username, password);
				Transport.send(message);

//				System.out.println("Done");
			}

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

}