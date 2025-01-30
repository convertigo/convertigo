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

package com.twinsoft.convertigo.engine.admin.services.keys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;

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


@ServiceDefinition(
		name = "Remove",
		roles = { Role.WEB_ADMIN, Role.KEYS_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Remove extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();

		/**
		 * get key to remove
		 */
		Element keysListElement = document.createElement("keys");
		rootElement.appendChild(keysListElement);

		Document post = XMLUtils.parseDOM(request.getInputStream());

		NodeList nl = post.getElementsByTagName("key");

		String tasRoot = EnginePropertiesManager.getProperty(PropertyName.CARIOCA_URL);
        KeyManager.init(tasRoot);
        
    	java.util.List<Key> keys = new ArrayList<Key>(GenericUtils.<Collection<Key>>cast(KeyManager.keys.values()));
    	Collections.sort(keys, new Comparator<Key>() {
			public int compare(Key o1, Key o2) {
				return o1.getQuickSortValue().toString().compareTo(o2.getQuickSortValue().toString());
			}
		});

		Properties keysProperties = PropertiesUtils.load(tasRoot + "/Java/keys.txt");
		
		for (int i = 0; i < nl.getLength(); i++) {
			String oldKey = ((Element) nl.item(i)).getAttribute("text");
			
			Element keyElement = document.createElement("key");
			
			// Check if key already exists
			if (keysProperties.getProperty(oldKey) != null) {
				
				// remove the key from properties
				keysProperties.remove(oldKey);
				
				// remove the key from KeyManager
				try {
					KeyManager.removeKey(oldKey); // remove key and save file
				} catch(Exception e) {					
				}
				
				keyElement.setAttribute("valid", "true");
				keyElement.setAttribute("text", oldKey);
				keysListElement.appendChild(keyElement);
				/* The keys have been updated */
				Engine.logAdmin.info("The key '" + oldKey + "' has been successfully removed!");
			} else {
				keyElement.setAttribute("valid", "false");
				keyElement.setAttribute("errorMessage", "Key was not found");
				keyElement.setAttribute("text", oldKey);
				keysListElement.appendChild(keyElement);
				/* The keys have been updated */
				Engine.logAdmin.info("The key '" + oldKey + "' could not be removed!");
			}
		}
		
		PropertiesUtils.store(keysProperties, tasRoot + "/Java/keys.txt");
	}
}
