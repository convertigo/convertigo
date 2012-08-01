/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.StringWriter;
import java.security.Provider;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SmtpStep extends Step implements IStepSourceContainer, ITagsProperty {

	private static final long serialVersionUID = 3915732415195665643L;

	protected XMLVector<String> sourceDefinition = new XMLVector<String>();
	
	private String smtpServer = "xxx.xxx.xxx.xxx";
	private String smtpRecipients = "someone@domain.org";
	private String smtpSubject = "Convertigo sequence report";
	private String smtpUsername = "";
	private String smtpPassword = "";
	private String smtpPort = "25";
	private String smtpAuthType = "None";
	private String smtpSender = "Convertigo <noreply@fakedomain.fake>";
	private String xslFilepath = "";
	private String contentType = "";

	private transient StepSource source = null;
	private transient String sMessageText = "";
	private transient String sContentType;
	
	public SmtpStep() {
		super();
	}

	@Override
    public SmtpStep clone() throws CloneNotSupportedException {
    	SmtpStep clonedObject = (SmtpStep) super.clone();
    	clonedObject.source = null;
        return clonedObject;
    }

	@Override
	public SmtpStep copy() throws CloneNotSupportedException {
		SmtpStep copiedObject = (SmtpStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @(" + getLabel() + ")" : " @(??)";
		} catch (EngineException e) {}
		return smtpServer + label + (!text.equals("") ? " // " + text : "");
	}

	protected boolean workOnSource() {
		return true;
	}
	
	protected StepSource getSource() {
		if (source == null) {
			source = new StepSource(this, sourceDefinition);
		}
		return source;
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
		source = new StepSource(this, sourceDefinition);
	}

	public String getSmtpServer() {
		return smtpServer;
	}

	public void setSmtpServer(String newSmtpServer) {
		this.smtpServer = newSmtpServer;
	}
	
	public String getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(String newSmtpPort) {
		this.smtpPort = newSmtpPort;
	}
	
	public String getSmtpAuthType() {
		return smtpAuthType;
	}

	public void setSmtpAuthType(String newSmtpAuthType) {
		this.smtpAuthType = newSmtpAuthType;
	}
	
	public String getSmtpSender() {
		return smtpSender;
	}

	public void setSmtpSender(String newSmtpSender) {
		this.smtpSender = newSmtpSender;
	}
	
	public String getSmtpRecipients() {
		return smtpRecipients;
	}

	public void setSmtpRecipients(String newSmtpRecipients) {
		this.smtpRecipients = newSmtpRecipients;
	}
	
	public String getSmtpSubject() {
		return smtpSubject;
	}

	public void setSmtpSubject(String newSmtpSubject) {
		this.smtpSubject = newSmtpSubject;
	}
	
	public String getSmtpUsername() {
		return smtpUsername;
	}

	public void setSmtpUsername(String newSmtpUsername) {
		this.smtpUsername = newSmtpUsername;
	}
	
	public String getSmtpPassword() {
		return smtpPassword;
	}

	public void setSmtpPassword(String newSmtpPassword) {
		this.smtpPassword = newSmtpPassword;
	}
	
	public String getXslFilepath() {
		return xslFilepath;
	}

	public void setXslFilepath(String xslFilepath) {
		this.xslFilepath = xslFilepath;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		String[] result = new String[0];
		if(propertyName.equals("smtpAuthType")){
			String[] authTypes = {"None","Basic","STARTTLS","SSL/TLS"};
			result = authTypes;
		}
		return result;
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					evaluate(javascriptContext, scope, this.xslFilepath, "xslFilepath", false);
					String xslFilepath = evaluated instanceof Undefined ? "" : evaluated.toString();
					evaluate(javascriptContext, scope, contentType, "contentType", false);
					sContentType = evaluated instanceof Undefined ? "" : evaluated.toString();
					File fileXSL = null;
					if (xslFilepath.length() > 0) {
						fileXSL = new File(Engine.theApp.filePropertyManager.getFilepathFromProperty(xslFilepath, getProject().getName()));
						if (!fileXSL.exists()) {
							throw new EngineException("The defined xslFilepath doesn't exist : " + fileXSL.getAbsolutePath());
						}
					}
					
					if (sContentType.length() == 0) {
						sContentType = fileXSL != null ? "text/html" : "text/plain";
					}

					Transformer transformer = fileXSL != null ?
							XMLUtils.getNewTransformer(new StreamSource(fileXSL)) : XMLUtils.getNewTransformer();

					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
					transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
					StringWriter sw = new StringWriter();

					StepSource stepSource = getSource();
					NodeList list = stepSource.inError() ? null : stepSource.getContextOutputNodes();
					if (list != null) {
						for (int i = 0; i < list.getLength(); i++) {
							Node currentNode = list.item(i);
							switch (currentNode.getNodeType()) {
							case Node.ATTRIBUTE_NODE: 
							case Node.TEXT_NODE:
							case Node.CDATA_SECTION_NODE: 
							case Node.COMMENT_NODE:
								if (fileXSL == null) {
									sw.write(currentNode.getNodeValue());
								}
								break;
							case Node.ELEMENT_NODE:
								transformer.transform(new DOMSource(currentNode), new StreamResult(sw));
								break;
							default:
								transformer.transform(new DOMSource(currentNode), new StreamResult(sw));
							}
						}
					} else {
						transformer.transform(new DOMSource(sequence.context.outputDocument), new StreamResult(sw));
					}
					sMessageText = sw.toString();

					sendMess();
					return true;
				} catch (EngineException e) {
					throw e;
				} catch (Exception e) {
					throw new EngineException(e.getClass().getSimpleName() + " during the mail creation", e);
				}
			}
		}
		return false;
	}
	
	public String toJsString() {
		return "";
	}
	
	private class SMTPAuthenticator extends javax.mail.Authenticator {

		@Override
		public PasswordAuthentication getPasswordAuthentication() {
			String username = smtpUsername;
			String password = smtpPassword;
			return new PasswordAuthentication(username, password);
		}
	}
	
	private MimeMessage buildMail(MimeMessage message) {
		MimeMessage ret = message;
		try {
			Address[] replies = {new InternetAddress(smtpSender)};
			smtpRecipients = smtpRecipients.replaceAll(",|;", "µ");
			String[] recipients = smtpRecipients.split("µ");
			// Adding sender
			ret.setFrom(new InternetAddress(smtpSender));
			ret.setSender(new InternetAddress(smtpSender));
			ret.setReplyTo(replies);
			
			//Adding recipients
			if (recipients[0].indexOf(":") != -1) {
				ret.addRecipient(Message.RecipientType.TO, new InternetAddress(recipients[0].split(":")[1]));
			} else {
				ret.addRecipient(Message.RecipientType.TO, new InternetAddress(recipients[0]));
			}
			for (int i = 1; i < recipients.length; i++) {
				if (recipients[i].indexOf(":") != -1) {
					String sRecipientType  = recipients[i].split(":")[0];
					String sRecipientAddress  = recipients[i].split(":")[1];
					if (sRecipientType.toUpperCase() == "TO") {
						ret.addRecipient(Message.RecipientType.TO, new InternetAddress(sRecipientAddress));
					} else if (sRecipientType.toUpperCase() == "BCC") {
						ret.addRecipient(Message.RecipientType.BCC, new InternetAddress(sRecipientAddress));
					} else {
						ret.addRecipient(Message.RecipientType.CC, new InternetAddress(sRecipientAddress));
					}
				} else {
					ret.addRecipient(Message.RecipientType.CC, new InternetAddress(recipients[i]));
				}
			}
			
			//Adding mail subject
			ret.setSubject(smtpSubject);
			
			//Adding content
			ret.setContent(sMessageText, sContentType);
		} catch (Exception e1) {
			Engine.logBeans.error("(SmtpStep) An error occured while trying to build e-mail : " + e1.getStackTrace().toString(), e1);
		}
		return ret;
	}
	
	private boolean sendMess() {
		boolean ret = true;
		try {
			new Thread(new Runnable(){
				public void run() {
					Properties props = new Properties();
					try {
						if (smtpAuthType.equals("SSL/TLS")){
							Provider provider = (Provider) Class.forName("com.sun.net.ssl.internal.ssl.Provider").newInstance();
							java.security.Security.addProvider(provider);
							props.put("mail.transport.protocol", "smtps");
							props.put("mail.smtps.host", smtpServer);
							props.put("mail.smtps.port", smtpPort);
							props.put("mail.smtps.auth", "true");

							//Initializing
							SMTPAuthenticator auth = new SMTPAuthenticator();
							Session mailSession = Session.getInstance(props, auth);
							MimeMessage message = new MimeMessage(mailSession);

							// Building message
							message = buildMail(message);

							//Sending e-mail
							Engine.logBeans.info("(SmtpStep) Sending e-mail with SSL authentication");
							Transport transport = mailSession.getTransport("smtps");
							transport.connect(smtpServer, Integer.parseInt(smtpPort), smtpUsername, smtpPassword);
							transport.sendMessage(message, message.getAllRecipients());
							transport.close();
						} else if(smtpAuthType.equals("Basic")) {
							props.put("mail.transport.protocol", "smtp");
							props.put("mail.smtp.host", smtpServer);
							props.put("mail.smtp.port", smtpPort);
							props.put("mail.smtp.auth", "true");

							//Initializing
							Session mailSession = Session.getInstance(props, null);
							MimeMessage message = new MimeMessage(mailSession);

							// Building message
							message = buildMail(message);

							//Sending e-mail
							Engine.logBeans.info("(SmtpStep) Sending e-mail with Basic authentication");
							Transport transport = mailSession.getTransport("smtp");
							//Engine.logBeans.info("(SmtpSend)usr='" + smtpUsername + "'/pwd='" + smtpPassword + "'");
							transport.connect(smtpServer, Integer.parseInt(smtpPort), smtpUsername, smtpPassword);
							transport.sendMessage(message, message.getAllRecipients());
							transport.close();
						} else if (smtpAuthType.equals("STARTTLS")){
							props.put("mail.transport.protocol", "smtp");
							props.put("mail.smtp.host", smtpServer);
							props.put("mail.smtp.port", smtpPort);
							props.put("mail.smtp.auth", "true");
							props.put("mail.smtp.starttls.enable", "true");

							//Initializing
							Session mailSession = Session.getInstance(props);
							MimeMessage message = new MimeMessage(mailSession);

							// Building message
							message = buildMail(message);

							//Sending e-mail
							Engine.logBeans.info("(SmtpStep) Sending e-mail with STARTTLS authentication");
							Transport transport = mailSession.getTransport("smtp");
							transport.connect(smtpServer, smtpUsername, smtpPassword);
							transport.sendMessage(message, message.getAllRecipients());
							transport.close();
						} else {
							props.put("mail.transport.protocol", "smtp");
							props.put("mail.smtp.host", smtpServer);
							props.put("mail.smtp.port", smtpPort);
							props.put("mail.smtp.auth", "false");

							//Initializing
							Session mailSession = Session.getInstance(props);
							MimeMessage message = new MimeMessage(mailSession);

							// Building message
							message = buildMail(message);

							//Sending e-mail
							Engine.logBeans.info("(SmtpStep) Sending e-mail with no authentication");
							Transport transport = mailSession.getTransport("smtp");
							Transport.send(message);
							transport.close();
						}
					} catch (Exception e1) {
						Engine.logBeans.error("(SmtpStep) An error occured while trying to send e-mail : " + e1.getStackTrace().toString(), e1);
					}
				}
			}).start();
		} catch (Exception e2) {
			Engine.logBeans.error("(SmtpStep) An error occured while trying to send e-mail : " + e2.getStackTrace().toString(), e2);
		}
		return ret;
	}
}
