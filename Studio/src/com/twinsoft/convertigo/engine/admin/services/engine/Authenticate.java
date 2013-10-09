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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.SimpleCipher;

@ServiceDefinition(
		name = "Authenticate",
		roles = { Role.ANONYMOUS },
		parameters = {
				@ServiceParameterDefinition(
						name = "authType",
						description = "the authentication type required: login | logout"
					),
				@ServiceParameterDefinition(
						name = "authUserName",
						description = "the authentication user name"
					),
				@ServiceParameterDefinition(
						name = "authPassword",
						description = "the authentication user password (in clear text)"
					)
		},
		returnValue = "the authentication status"
	)
	
public class Authenticate extends XmlService {

	/*
	 * Defines the number of seconds the service will block account in case of
	 * successive wrong authentication attempts.
	 */
	private static final int AUTHENTICATION_DELAY = 300;

	/*
	 * Defines the number max of wrong authentication attempts before
	 * introducing a wait delay
	 */
	private static final int MAX_NUMBER_OF_WRONG_AUTHENTICATION_ATTEMPTS = 5;

	/*
	 * Defines the map of the number of currently wrong authentication attempts
	 * with a given user ID
	 */
	private static Map<String, AuthenticationAttempt> authenticationAttempts = new HashMap<String, AuthenticationAttempt>();

	private class AuthenticationAttempt {
		public int numberOfFailedRequests = 0;
		public long accountBlockedUntil = 0;
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		boolean logIn = "login".equals(ServiceUtils.getRequiredParameter(request, "authType"));

		HttpSession httpSession = request.getSession();

		// Login
		if (logIn) {

			String user = ServiceUtils.getRequiredParameter(request, "authUserName");
			String password = ServiceUtils.getRequiredParameter(request, "authPassword");

			httpSession.setAttribute(SessionKey.ADMIN_USER.toString(), user);
			Engine.logAdmin.info("User '" + user + "' is trying to login");

			// Check authentication attempts
			AuthenticationAttempt authenticationAttempt = Authenticate.authenticationAttempts.get(user);

			if (authenticationAttempt != null && authenticationAttempt.accountBlockedUntil != 0) {
				long now = System.currentTimeMillis();
				if (now > authenticationAttempt.accountBlockedUntil) {
					// Unblock the account
					authenticationAttempt.accountBlockedUntil = 0;
					authenticationAttempt.numberOfFailedRequests = 0;
				} else {
					// Continue blocking the account
					Engine.logAdmin.warn("Detected possible brute force attack: user '" + user
							+ "' has failed to login too many times; authentication request is blocked.");
					Engine.authenticatedSessionManager.removeAuthenticatedSession(httpSession);
					
					long secondsRemaining = (authenticationAttempt.accountBlockedUntil - now) / 1000;
					
					ServiceUtils.addMessage(document, document.getDocumentElement(), "The '" + user
							+ "' account is blocked during " + secondsRemaining
							+ " seconds, due to too many failed authentication attempts.\n\n"
							+ "Please wait and retry it later.", "error", false);

					return;
				}
			}

			Role[] roles = null;

			// Legacy authentication
			if (EnginePropertiesManager.getProperty(PropertyName.ADMIN_USERNAME).equals(user)
					&& EnginePropertiesManager.checkProperty(PropertyName.ADMIN_PASSWORD, password)) {
				roles = new Role[] { Role.WEB_ADMIN, Role.TEST_PLATFORM, Role.AUTHENTICATED };
			} else if (EnginePropertiesManager.getProperty(PropertyName.TEST_PLATFORM_USERNAME).equals(user)
					&& EnginePropertiesManager.checkProperty(PropertyName.TEST_PLATFORM_PASSWORD, password)) {
				roles = new Role[] { Role.TEST_PLATFORM, Role.AUTHENTICATED };
			}
			// Trial authentication
			else {
				File hackTrial = new File(Engine.CONFIGURATION_PATH + "/hackTrial.txt");
				if (hackTrial.exists()) {
					try {
						BufferedReader br = new BufferedReader(new FileReader(hackTrial));
						String line = br.readLine();
						br.close();
						if (!"ok, you can deploy !!".equals(line)) {
							Engine.logAdmin.error("Trial authentication failure: wrong internal data!");
						} else if (user.matches(".+@.+\\.[a-z]+")
								&& user.equals(SimpleCipher.decode(password))) {
							roles = new Role[] { Role.TRIAL };
							httpSession.setAttribute("trial_user", true);
						} else {
							Engine.logAdmin.error("Trial authentication failure: wrong username/password");
						}
					} catch (Exception e) {
						Engine.logAdmin.error("Trial authentication failure: wrong internal data!", e);
					}
				}
			}

			if (roles == null) {
				if (authenticationAttempt == null) {
					//The AuthenticationAttempt object does not exist; we must create a new one
					authenticationAttempt = new AuthenticationAttempt();
					Authenticate.authenticationAttempts.put(user, authenticationAttempt);
				}

				Engine.logAdmin.error("Invalid password or user name '" + user + "' (attempt #"
						+ authenticationAttempt.numberOfFailedRequests + ")");

				authenticationAttempt.numberOfFailedRequests++;

				Engine.authenticatedSessionManager.removeAuthenticatedSession(httpSession);

				if (authenticationAttempt.numberOfFailedRequests > Authenticate.MAX_NUMBER_OF_WRONG_AUTHENTICATION_ATTEMPTS) {
					Engine.logAdmin.warn("Detected possible brute force attack: user '" + user
							+ "' has failed to login too many times; authentication request is blocked.");

					if (authenticationAttempt.accountBlockedUntil == 0) {
						authenticationAttempt.accountBlockedUntil = System.currentTimeMillis()
								+ Authenticate.AUTHENTICATION_DELAY * 1000;
					}

					long now = System.currentTimeMillis();
					long secondsRemaining = (authenticationAttempt.accountBlockedUntil - now) / 1000;
					
					ServiceUtils.addMessage(document, document.getDocumentElement(), "The '" + user
							+ "' account is blocked during " + secondsRemaining
							+ " seconds, due to too many failed authentication attempts.\n\n"
							+ "Please wait and retry it later.", "error", false);
				} else {
					ServiceUtils.addMessage(document, document.getDocumentElement(),
							"Invalid authentication!\n\nPlease verify your user ID and/or your password.",
							"error", false);
				}
			} else {
				Authenticate.authenticationAttempts.remove(user);

				Engine.authenticatedSessionManager.addAuthenticatedSession(httpSession, roles);

				ServiceUtils.addMessage(document, document.getDocumentElement(), "", "success");
				ServiceUtils.addMessage(document, document.getDocumentElement(),
						"" + httpSession.getAttribute(SessionKey.ADMIN_USER.toString()), "user", false);
				ServiceUtils.addRoleNodes(document.getDocumentElement(), roles);

				httpSession.setAttribute("authenticatedUser", "c8o:admin");

				Engine.logAdmin.info("User '" + user + "' has been successfully authenticated");
			}
		}
		// Logout
		else {
			Engine.authenticatedSessionManager.removeAuthenticatedSession(httpSession);
			httpSession.removeAttribute("authenticatedUser");
			ServiceUtils.addMessage(document, document.getDocumentElement(), "", "success");
		}
	}
}
