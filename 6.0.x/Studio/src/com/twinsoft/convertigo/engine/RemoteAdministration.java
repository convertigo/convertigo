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

package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.axis2.transport.mail.server.MailAddress;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SimpleCipher;
import com.twinsoft.tas.ApplicationException;
import com.twinsoft.tas.ParsingException;
import com.twinsoft.tas.SQLException;

public class RemoteAdministration {

	private boolean bAuthenticated = false;
	private String trial_username = null;

	public RemoteAdministration() {
	}

	public boolean login(String userName, String userPassword) throws EngineException, SQLException,
			ApplicationException, ParsingException {
		bAuthenticated = false;

		if (Engine.theApp == null) {
			Engine.start();
		}

		bAuthenticated = EnginePropertiesManager.checkProperty(PropertyName.ADMIN_USERNAME, userName)
				&& EnginePropertiesManager.checkProperty(PropertyName.ADMIN_PASSWORD, userPassword);

		File hackTrial = new File(Engine.CONFIGURATION_PATH + "/hackTrial.txt");
		if (!bAuthenticated && hackTrial.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(hackTrial));
				String line = br.readLine();
				br.close();
				bAuthenticated = ("ok, you can deploy !!".equals(line) && userName.matches(".+@.+\\.[a-z]+"))
						&& userName.equals(SimpleCipher.decode(userPassword));
				if (bAuthenticated) {
					trial_username = userName;
				}
			} catch (Exception e) {
			}
		}

		return bAuthenticated;
	}

	public void startEngine() throws EngineException {
		if (!bAuthenticated) {
			throw new EngineException("Not authenticated!");
		}

		Engine.start();
	}

	public void stopEngine() throws EngineException {
		if (!bAuthenticated) {
			throw new EngineException("Not authenticated!");
		}

		Engine.stop();
	}

	public void restartEngine() throws EngineException {
		if (!bAuthenticated) {
			throw new EngineException("Not authenticated!");
		}

		Engine.stop();
		Engine.start();
	}

	public boolean isEngineStarted() throws EngineException {
		if (!bAuthenticated) {
			throw new EngineException("Not authenticated!");
		}

		return (Engine.theApp != null);
	}

	public int getNumberOfContexts() throws EngineException {
		// if (!bAuthenticated) {
		// throw new EngineException("Not authenticated!");
		// }

		return Engine.theApp.contextManager.getNumberOfContexts();
	}

	public String[] getContextIDs() throws EngineException {
		if (!bAuthenticated)
			throw new EngineException("Not authenticated!");

		Collection<String> ctxids = Engine.theApp.contextManager.getContextIds();
		return ctxids.toArray(new String[ctxids.size()]);
	}

	public void removeContext(String contextID) throws EngineException {
		if (!bAuthenticated)
			throw new EngineException("Not authenticated!");

		Engine.theApp.contextManager.remove(contextID);
	}

	public void deployArchive(final String projectName, byte[] archiveFileContent) throws EngineException,
			IOException {
		if (!bAuthenticated)
			throw new EngineException("Not authenticated!");

		if (trial_username != null) {
			new Thread(new Runnable() {
				public void run() {
					try {
						Properties props = new Properties();

						props.put("mail.smtp.host",
								EnginePropertiesManager.getProperty(PropertyName.NOTIFICATIONS_SMTP_HOST));
						props.put("mail.smtp.socketFactory.port",
								EnginePropertiesManager.getProperty(PropertyName.NOTIFICATIONS_SMTP_PORT));
						props.put("mail.smtp.auth", "true");
						props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
						props.put("mail.smtp.socketFactory.fallback", "false");

						// Initializing
						Session mailSession = Session.getInstance(props, new Authenticator() {
							@Override
							public PasswordAuthentication getPasswordAuthentication() {
								return new PasswordAuthentication(EnginePropertiesManager
										.getProperty(PropertyName.NOTIFICATIONS_SMTP_USER),
										EnginePropertiesManager
												.getProperty(PropertyName.NOTIFICATIONS_SMTP_PASSWORD));
							}
						});
						MimeMessage message = new MimeMessage(mailSession);

						message.addRecipient(Message.RecipientType.TO, new MailAddress(EnginePropertiesManager
								.getProperty(PropertyName.NOTIFICATIONS_TARGET_EMAIL)));
						message.setSubject("[trial] deployment of " + projectName + " by " + trial_username);
						message.setText(message.getSubject() + "\n"
								+ "http://trial.convertigo.net/cems/projects/" + projectName + "\n"
								+ "https://trial.convertigo.net/cems/projects/" + projectName);
						Transport.send(message);
					} catch (MessagingException e1) {
					}
				}
			}).start();
		}

		String projectArchiveFileName = Engine.PROJECTS_PATH + "/" + projectName + ".car";
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(projectArchiveFileName);
			fos.write(archiveFileContent);
		} catch (IOException e) {
			Engine.logEngine
					.error("[RemoteAdministration] deployArchive() Unable to write the uploaded archive file content",
							e);
			throw new EngineException("Unable to write the uploaded archive file content!", e);
		} finally {
			if (fos != null)
				fos.close();
		}

		try {
			Engine.theApp.databaseObjectsManager.deployProject(projectArchiveFileName, true);
			Engine.logEngine.info("Project \"" + projectName + "\" has been successfully deployed");
		} catch (Exception e) {
			Engine.logEngine.error("[RemoteAdministration] deployArchive() Unable to deploy project \""
					+ projectName + "\"", e);
			throw new EngineException("Unable to deploy project \"" + projectName + "\"", e);
		}
	}

	public void deployArchive(String projectName, byte[] archiveFileContent, boolean bAssembleXsl)
			throws EngineException, IOException {
		if (!bAuthenticated) {
			throw new EngineException("Not authenticated!");
		}

		String projectArchiveFileName = Engine.PROJECTS_PATH + "/" + projectName + ".car";
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(projectArchiveFileName);
			fos.write(archiveFileContent);
		} catch (IOException e) {
			Engine.logEngine
					.error("[RemoteAdministration] deployArchive() Unable to write the uploaded archive file content",
							e);
			throw new EngineException("Unable to write the uploaded archive file content!", e);
		} finally {
			if (fos != null)
				fos.close();
		}

		try {
			Engine.theApp.databaseObjectsManager.deployProject(projectArchiveFileName, true, bAssembleXsl);
			Engine.logEngine.info("Project \"" + projectName + "\" has been successfully deployed");
		} catch (Exception e) {
			Engine.logEngine.error("[RemoteAdministration] deployArchive() Unable to deploy project \""
					+ projectName + "\"", e);
			throw new EngineException("Unable to deploy project \"" + projectName + "\"", e);
		}
	}

	public void syncProperties(String sProperties) throws EngineException {
		if (!bAuthenticated) {
			throw new EngineException("Not authenticated!");
		}

		try {
			Engine.logEngine.debug("[RemoteAdministration] syncProperties() Received properties:\n"
					+ sProperties);
			EnginePropertiesManager.load(new ByteArrayInputStream(sProperties.getBytes()));
			EnginePropertiesManager.saveProperties();
		} catch (Exception e) {
			Engine.logEngine.error("[RemoteAdministration] syncProperties() Unable to synchronize properties",
					e);
			throw new EngineException("Unable to synchronize properties!", e);
		}
	}

	public void syncCertificates(int type, String sProperties) throws EngineException {
		if (!bAuthenticated) {
			throw new EngineException("Not authenticated!");
		}

		try {
			Engine.logEngine.debug("[RemoteAdministration] syncCertificates() Received properties:\n"
					+ sProperties);

			File file = new File(Engine.CERTIFICATES_PATH + "/"
					+ CertificateManager.STORES_PROPERTIES_FILE_NAME);
			Properties storesProperties = new Properties();

			FileInputStream fis = new FileInputStream(file);
			storesProperties.load(fis);
			fis.close();

			Enumeration<String> enumKeys = GenericUtils.cast(storesProperties.keys());
			String key;

			// Certificates definitions
			if (type == 0) {
				while (enumKeys.hasMoreElements()) {
					key = enumKeys.nextElement();
					if (!key.startsWith("projects.") && !key.startsWith("tas.")) {
						Engine.logEngine.debug("[RemoteAdministration] syncCertificates() Removing property "
								+ key);
						storesProperties.remove(key);
					}
				}
			}
			// Certificates links
			else {
				while (enumKeys.hasMoreElements()) {
					key = enumKeys.nextElement();
					if (key.startsWith("projects.") || key.startsWith("tas.")) {
						Engine.logEngine.debug("[RemoteAdministration] syncCertificates() Removing property "
								+ key);
						storesProperties.remove(key);
					}
				}
			}

			storesProperties.load(new ByteArrayInputStream(sProperties.getBytes()));

			FileOutputStream fos = new FileOutputStream(file);
			storesProperties.store(fos, "");
			fos.flush();
			fos.close();
		} catch (Exception e) {
			Engine.logEngine.error(
					"[RemoteAdministration] syncCertificates() Unable to synchronize certificates", e);
			throw new EngineException("Unable to synchronize properties!", e);
		}
	}
}
