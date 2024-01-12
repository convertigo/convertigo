/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.keys;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.tas.Key;
import com.twinsoft.tas.KeyManager;
import com.twinsoft.util.DESKey;
import com.twinsoft.util.TWSKey;

@ServiceDefinition(
		name = "Update",
		roles = { Role.WEB_ADMIN, Role.KEYS_CONFIG },
		parameters = {},
		returnValue = ""
	)
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
							} else if (twsKey.hasExpired(newKey)) {
								keyElement.setAttribute("errorMessage", "The key is already expired!");
								keyElement.setAttribute("valid", "false");
								keyElement.setAttribute("text", newKey);
								keysListElement.appendChild(keyElement);
							} else {
								Properties keysProperties = PropertiesUtils.load(tasRoot + "/Java/keys.txt");
								
								// Check if key already exists
								if (keysProperties.getProperty(newKey) != null) {
									keyElement.setAttribute("errorMessage", "The key has already been added!");
									keyElement.setAttribute("valid", "false");
									keyElement.setAttribute("text", newKey);
									keysListElement.appendChild(keyElement);
								} else {
									keysProperties.setProperty(newKey, "");
									PropertiesUtils.store(keysProperties, tasRoot + "/Java/keys.txt");
									
									KeyManager.addKey(newKey, keyInfos);
									keyElement.setAttribute("valid", "true");
									keyElement.setAttribute("text", newKey);
									keysListElement.appendChild(keyElement);
									/* The keys have been updated */
									Engine.logAdmin.info("The key '" + newKey + "' has been added");

									/* update key file */
									updateKeyFile();
									Engine.logAdmin.info("The key file updated");
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
	
	boolean isActiveKey(String sKey) {
		Map<String, Key> keys = GenericUtils.cast(KeyManager.keys);
		Iterator<Key> iter = keys.values().iterator();
		while (iter.hasNext()) {
			Key k = iter.next();
			String str = k.sKey; 
			if (str.equals(sKey))
				return true;
		}
		return false;
	}
	
	void updateKeyFile() {
		try {
			boolean changed = false;
			String tasRoot = EnginePropertiesManager.getProperty(PropertyName.CARIOCA_URL);
			Properties keysProperties = PropertiesUtils.load(tasRoot + "/Java/keys.txt");
			Enumeration<Object> enumeration = keysProperties.keys();
			String sEval = DESKey.encodeToHexString("eval").toUpperCase(); // A4E2F2A4A778C2C1
			String sKey;
			while (enumeration.hasMoreElements()) {
				sKey = (String) enumeration.nextElement();
				// if first run date
				if (sKey.equals(sEval)) 
					continue;
				// if key commented out
				if (sKey.startsWith("#")) 
					continue;
				
				if (!isActiveKey(sKey)) {
					changed |= true;
					keysProperties.remove(sKey);
				}
			}
			
			if (changed) {
				PropertiesUtils.store(keysProperties, tasRoot + "/Java/keys.txt");
			}
		} catch(Exception e) {
			Engine.logAdmin.info("The key file cannot be updated");
		}
	}
}
