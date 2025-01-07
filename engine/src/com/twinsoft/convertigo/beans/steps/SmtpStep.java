/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SmtpStep extends Step implements IStepSourceContainer {

	private static final long serialVersionUID = 3915732415195665643L;

	public enum SmtpAuthType {
		none("None"),
		basic("Basic"),
		startTls("STARTTLS"),
		sslTls("SSL/TLS");
		
		private final String label;
		
		private SmtpAuthType(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	static private MailcapCommandMap mailcapCommandMap = null;
	
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	
	private String smtpServer = "xxx.xxx.xxx.xxx";
	private String smtpRecipients = "\"someone@domain.org\"";
	private String smtpSubject = "\"Convertigo sequence report\"";
	private String smtpUsername = "";
	private String smtpPassword = "";
	private String smtpPort = "25";
	private SmtpAuthType smtpAuthType = SmtpAuthType.none;
	private String smtpSender = "\"Convertigo <noreply@fakedomain.fake>\"";
	private String xslFilepath = "";
	private String contentType = "";
	private XMLVector<XMLVector<String>> attachments = new XMLVector<XMLVector<String>>();
	private boolean deliveryReceipt = false;
	private boolean readReceipt = false;
	private String sslProtocols = "";
	
	private transient String sMessageText = "";
	private transient String sContentType;
	private transient List<BodyPart> bodyParts = new LinkedList<BodyPart>();
	private transient String sSubject;
	private transient String sRecipients;
	private transient String sSender;
	
	public SmtpStep() {
		super();
	}

	@Override
    public SmtpStep clone() throws CloneNotSupportedException {
    	SmtpStep clonedObject = (SmtpStep) super.clone();
        return clonedObject;
    }

	@Override
	public SmtpStep copy() throws CloneNotSupportedException {
		SmtpStep copiedObject = (SmtpStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @(" + getLabel() + ")" : " @(??)";
		} catch (EngineException e) {}
		return smtpServer + label;
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
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
	
	public SmtpAuthType getSmtpAuthType() {
		return smtpAuthType;
	}

	public void setSmtpAuthType(SmtpAuthType newSmtpAuthType) {
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

	public XMLVector<XMLVector<String>> getAttachments() {
		return attachments;
	}

	public void setAttachments(XMLVector<XMLVector<String>> attachments) {
		this.attachments = attachments;
	}

	public boolean getDeliveryReceipt() {
		return deliveryReceipt;
	}
	
	public void setDeliveryReceipt(boolean deliveryReceipt) {
		this.deliveryReceipt = deliveryReceipt;
	}
	
	public boolean getReadReceipt() {
		return readReceipt;
	}
	
	public void setReadReceipt(boolean readReceipt) {
		this.readReceipt = readReceipt;
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					evaluate(javascriptContext, scope, this.smtpSubject, "smtpSubject", false);
					sSubject = evaluated instanceof Undefined ? "" : evaluated.toString();
					
					evaluate(javascriptContext, scope, this.smtpRecipients, "smtpRecipients", false);
					sRecipients = evaluated instanceof Undefined ? "" : evaluated.toString();
					
					evaluate(javascriptContext, scope, this.smtpSender, "smtpSender", false);
					sSender = evaluated instanceof Undefined ? "" : evaluated.toString();
					
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
						sContentType = fileXSL != null ? "text/html; charset=UTF-8" : "text/plain; charset=UTF-8";
					}

					Transformer transformer = fileXSL != null ?
							XMLUtils.getNewTransformer(new StreamSource(fileXSL)) : XMLUtils.getNewTransformer();
					
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
					transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
					StringWriter sw = new StringWriter();
					
					// make a clean copy, without step_id and copy_step attributes
					Document workingDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
					Element root = (Element) workingDoc.appendChild(workingDoc.createElement("root"));
					XMLCopyStep.createCopy(this, workingDoc, root);
					
					NodeList list = root.getChildNodes();
					if (list != null && list.getLength() > 0) {
						int nbNodes = list.getLength();
						for (int i = 0; i < nbNodes; i++) {
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
					
					bodyParts.clear();
					for (XMLVector<String> attachment : attachments) {
						String filepath = attachment.get(0);
						try {
							evaluate(javascriptContext, scope, filepath, "filepath", false);
							filepath = evaluated instanceof Undefined ? "" : evaluated.toString();
							if (filepath.length() > 0) {
								File file = new File(Engine.theApp.filePropertyManager.getFilepathFromProperty(filepath, getProject().getName()));
								if (file.exists()) {
									if (file.isFile()) {
										String filename = attachment.get(1);
										evaluate(javascriptContext, scope, filename, "filename", false);
										filename = evaluated instanceof Undefined ? file.getName() : evaluated.toString();
										
										MimeBodyPart bodyPart = new MimeBodyPart();
										bodyPart.attachFile(file);
										bodyPart.setFileName(filename);
										bodyParts.add(bodyPart);
									} else {
										Engine.logBeans.info("Unable attach a directory : " + file.getAbsolutePath());
									}
								} else {
									Engine.logBeans.info("Unable attach an unexisting file : " + file.getAbsolutePath());
								}
							} else {
								Engine.logBeans.info("Unable attach an empty filepath !!");
							}
						} catch (Exception e) {
							Engine.logBeans.info("Unable attach " + filepath, e);
						}
					}

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
			Address[] replies = {new InternetAddress(sSender)};
			String[] recipients = sRecipients.split(",|;");
			// Adding sender
			ret.setFrom(new InternetAddress(sSender));
			ret.setSender(new InternetAddress(sSender));
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
					if ("TO".equalsIgnoreCase(sRecipientType)) {
						ret.addRecipient(Message.RecipientType.TO, new InternetAddress(sRecipientAddress));
					} else if ("BCC".equalsIgnoreCase(sRecipientType)) {
						ret.addRecipient(Message.RecipientType.BCC, new InternetAddress(sRecipientAddress));
					} else {
						ret.addRecipient(Message.RecipientType.CC, new InternetAddress(sRecipientAddress));
					}
				} else {
					ret.addRecipient(Message.RecipientType.CC, new InternetAddress(recipients[i]));
				}
			}

			//Add receipts
			if (getDeliveryReceipt()) {
				message.setHeader("Return-Receipt-To", new InternetAddress(sSender).toString());
			}
			if (getReadReceipt()) {
				message.setHeader("Disposition-Notification-To", new InternetAddress(sSender).toString());
			}
			
			
			//Adding mail subject
			ret.setSubject(sSubject);

			//Adding content
			if (bodyParts.size() > 0) {
				Multipart multipart = new MimeMultipart();
				
				MimeBodyPart msgPart = new MimeBodyPart();
				msgPart.setContent(sMessageText, sContentType);
				multipart.addBodyPart(msgPart);
				
				for (BodyPart bodyPart : bodyParts) {
					multipart.addBodyPart(bodyPart);
				}
				
				ret.setContent(multipart);
			} else {
				ret.setContent(sMessageText, sContentType);
			}
		} catch (Exception e1) {
			Engine.logBeans.error("(SmtpStep) An error occured while trying to build e-mail : " + e1.getStackTrace().toString(), e1);
		}
		return ret;
	}
	
	private boolean sendMess() {
		boolean ret = true;
		try {
			Engine.execute(new Runnable(){
				public void run() {
					Properties props = new Properties();
					try {
						if (mailcapCommandMap == null) {
							MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
							synchronized (mc) {
								if (mailcapCommandMap == null) {
									mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
									mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
									mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
									mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
									mc.addMailcap("multipart/mixed;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
									mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822");
								}
								mailcapCommandMap = mc;
							}
						}
						
						String sslProtocols = SmtpStep.this.sslProtocols.isBlank() ? "TLSv1.2" : SmtpStep.this.sslProtocols;
						
						if (smtpAuthType == SmtpAuthType.sslTls) {
							props.put("mail.transport.protocol", "smtps");
							props.put("mail.smtps.host", smtpServer);
							props.put("mail.smtps.port", smtpPort);
							props.put("mail.smtps.auth", "true");
							props.put("mail.smtps.ssl.protocols", sslProtocols);

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
						} else if(smtpAuthType == SmtpAuthType.basic) {
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
						} else if (smtpAuthType == SmtpAuthType.startTls){
							props.put("mail.transport.protocol", "smtp");
							props.put("mail.smtp.host", smtpServer);
							props.put("mail.smtp.port", smtpPort);
							props.put("mail.smtp.auth", "true");
							props.put("mail.smtp.starttls.enable", "true");
							props.put("mail.smtp.starttls.required", "true");
							props.put("mail.smtp.ssl.protocols", sslProtocols);

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
			});
		} catch (Exception e2) {
			Engine.logBeans.error("(SmtpStep) An error occured while trying to send e-mail : " + e2.getStackTrace().toString(), e2);
		}
		return ret;
	}
	
    @Override
	public void preconfigure(Element element) throws Exception {
    	super.preconfigure(element);

    	String version = element.getAttribute("version");
    	TwsCachedXPathAPI xpath = getXPathAPI();

    	if (version != null && VersionUtils.compareMigrationVersion(version, ".m005") < 0) {
    		Engine.logBeans.warn("[SmtpStep] The object \"" + getName() + "\" (subject and recipients) has been updated to m005");

    		Element prop = (Element) xpath.selectSingleNode(element, "property[@name='smtpSubject']/*");
    		prop.setAttribute("value", "\"" + prop.getAttribute("value") + "\"");
    		prop = (Element) xpath.selectSingleNode(element, "property[@name='smtpRecipients']/*");
    		prop.setAttribute("value", "\"" + prop.getAttribute("value") + "\"");
    		
    		hasChanged = true;
    	}

    	if (version != null && VersionUtils.compareProductVersion(version, "7.5.0") < 0) {
    		Engine.logBeans.warn("[SmtpStep] The object \"" + getName() + "\" (sender) has been updated to 7.5.0");
    		
    		Element prop = (Element) xpath.selectSingleNode(element, "property[@name='smtpSender']/*");
    		prop.setAttribute("value", "\"" + prop.getAttribute("value") + "\"");
    		
    		hasChanged = true;
    	}
    }
    
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("smtpPassword".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("smtpPassword".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}

	public String getSslProtocols() {
		return sslProtocols;
	}

	public void setSslProtocols(String sslProtocols) {
		this.sslProtocols = sslProtocols;
	}
}
