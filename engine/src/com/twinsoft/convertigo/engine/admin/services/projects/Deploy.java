/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.projects;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.UploadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
	name = "Deploy",
	roles = { Role.WEB_ADMIN, Role.TRIAL, Role.PROJECTS_CONFIG },
	parameters = {
		@ServiceParameterDefinition(
			name = "bAssembleXsl",
			description = "assembling xsl"
		)
	},
	returnValue = ""
)
public class Deploy extends UploadService {

	boolean bAssembleXsl = false;

	@Override
	protected String getRepository() {
		return Engine.PROJECTS_PATH + "/";
	}

	@Override
	protected void doUpload(HttpServletRequest request, Document document, FileItem item) throws Exception {
		if (!item.getName().endsWith(".car")) {
			ServiceUtils.addMessage(document, document.getDocumentElement(), "The deployment of the project "
					+ item.getName() + " has failed. The archive file is not valid (.car required).", "error",
					false);
		}

		super.doUpload(request, document, item);

		// Depending on client browsers, according to the documentation,
		// item.getName() can either return a full path file name, or
		// simply a file name.
		String projectArchive = item.getName();

		// Bugfix #1425
		int i = projectArchive.lastIndexOf('/');
		if (i == -1) {
			i = projectArchive.lastIndexOf('\\');
			if (i != -1) {
				projectArchive = projectArchive.substring(i + 1);
			}
		} else {
			projectArchive = projectArchive.substring(i + 1);
		}
		
		Project project = Engine.theApp.databaseObjectsManager.deployProject(getRepository() + projectArchive, true, bAssembleXsl);
		
		String projectName = project.getName();
		Engine.theApp.schemaManager.clearCache(projectName);
		Project.executeAutoStartSequences(projectName);
		
		if (Boolean.parseBoolean(EnginePropertiesManager
				.getProperty(PropertyName.NOTIFICATIONS_NOTIFY_PROJECT_DEPLOYMENT))) {

			final String fUser = (String) request.getSession().getAttribute(SessionKey.ADMIN_USER.toString());
			final String fProjectName = projectName;

			Engine.execute(new Runnable() {
				public void run() {
					try {
						Properties props = new Properties();
						String smtpServer = EnginePropertiesManager.getProperty(PropertyName.NOTIFICATIONS_SMTP_HOST);
						String smtpPort = EnginePropertiesManager.getProperty(PropertyName.NOTIFICATIONS_SMTP_PORT);
						String smtpUsername = EnginePropertiesManager.getProperty(PropertyName.NOTIFICATIONS_SMTP_USER);
						String smtpPassword = EnginePropertiesManager.getProperty(PropertyName.NOTIFICATIONS_SMTP_PASSWORD);
						props.put("mail.transport.protocol", "smtps");
						props.put("mail.smtps.host", smtpServer);
						props.put("mail.smtps.port", smtpPort);
						props.put("mail.smtps.auth", "true");
						props.put("mail.smtps.ssl.protocols", "TLSv1.2");
						
						//Initializing
						Session mailSession = Session.getInstance(props, new Authenticator() {

							@Override
							protected PasswordAuthentication getPasswordAuthentication() {
								return new PasswordAuthentication(smtpUsername, smtpPassword);
							}
						});
						MimeMessage message = new MimeMessage(mailSession);
						message.setFrom(new InternetAddress("noreply@convertigo.com"));
						message.setSender(new InternetAddress("noreply@convertigo.com"));
						message.addRecipient(
								Message.RecipientType.TO,
								new InternetAddress(EnginePropertiesManager
										.getProperty(PropertyName.NOTIFICATIONS_TARGET_EMAIL)));
						message.setSubject("[trial] deployment of " + fProjectName + " by " + fUser);
						message.setText(message.getSubject() + "\n"
								+ "https://trial.convertigo.net/convertigo/projects/" + fProjectName);
						Transport transport = mailSession.getTransport("smtps");
						transport.connect(smtpServer, Integer.parseInt(smtpPort), smtpUsername, smtpPassword);
						transport.sendMessage(message, message.getAllRecipients());
						transport.close();
					} catch (MessagingException e1) {
					}
				}
			});
		}

		String message = "The project '" + projectName + "' has been successfully deployed.";
		Engine.logAdmin.info(message);
		ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
	}

	@Override
	protected void handleFormFields(HttpServletRequest request) {
		bAssembleXsl = Boolean.parseBoolean(request.getParameter("bAssembleXsl"));
	}

}
