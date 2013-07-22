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

package com.twinsoft.convertigo.engine.admin.services.keys;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.tas.KeyManager;
import com.twinsoft.util.TWSKey;

@ServiceDefinition(
		name = "Update",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = "" ,
		cloud_forbidden = true)
public class Update extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();

		Element keysListElement = document.createElement("keys");
		rootElement.appendChild(keysListElement);

		Document post = null;

		post = XMLUtils.parseDOM(request.getInputStream());

		NodeList nl = post.getElementsByTagName("key");

		String tasRoot = EnginePropertiesManager.getProperty(PropertyName.CARIOCA_URL);

		for (int i = 0; i < nl.getLength(); i++) {
			String newKey = ((Element) nl.item(i)).getAttribute("text");
			if (newKey != null) {
				Element keyElement = document.createElement("key");
				String keyInfos;
				TWSKey twsKey = new TWSKey();
				twsKey.CreateKey(52);
				keyInfos = twsKey.decypherbis(newKey);

				if (keyInfos.length() == 0) {
					keyElement.setAttribute("errorMessage", "The key is not valid!");
					keyElement.setAttribute("valid", "false");
					keyElement.setAttribute("text", newKey);
					keysListElement.appendChild(keyElement);
				} else {
					try {
						StringTokenizer st = new StringTokenizer(keyInfos, ";");
						int product = Integer.parseInt(st.nextToken());
						if (product != 52) {
							keyElement.setAttribute("errorMessage", "The key is not a Convertigo key!");
							keyElement.setAttribute("valid", "false");
							keyElement.setAttribute("text", newKey);
							keysListElement.appendChild(keyElement);
						} else {
							st.nextToken();
							int cv = Integer.parseInt(st.nextToken());
							if (cv < 1) {
								keyElement.setAttribute("errorMessage", "The key is not valid (cv < 1)!");
								keyElement.setAttribute("valid", "false");
								keyElement.setAttribute("text", newKey);
								keysListElement.appendChild(keyElement);
							} else {
								Properties keysProperties = new Properties();
								keysProperties.load(new FileInputStream(tasRoot + "/Java/keys.txt"));
								
								// Check if key already exists
								if (keysProperties.getProperty(newKey) != null) {
									keyElement.setAttribute("errorMessage", "The key has already been added!");
									keyElement.setAttribute("valid", "false");
									keyElement.setAttribute("text", newKey);
									keysListElement.appendChild(keyElement);
								}
								else {
									keysProperties.setProperty(newKey, "");
									keysProperties.store(new FileOutputStream(tasRoot + "/Java/keys.txt"), null);
									KeyManager.addKey(newKey, keyInfos);
									keyElement.setAttribute("valid", "true");
									keyElement.setAttribute("text", newKey);
									keysListElement.appendChild(keyElement);
									/* The keys have been updated */
									Engine.logAdmin.info("The key '" + newKey + "' has been added");
								}
							}
						}
					} catch (Exception e) {
						keyElement.setAttribute("errorMessage", "The key is not valid!");
						keyElement.setAttribute("valid", "false");
						keyElement.setAttribute("text", newKey);
						keysListElement.appendChild(keyElement);
					}
				}
			}
		}
	}
}