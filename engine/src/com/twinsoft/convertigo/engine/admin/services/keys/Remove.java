/*
* Copyright (c) 2001-2017 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/PushNotificationStep.java $
 * $Author: jmc $
 * $Revision: 43079 $
 * $Date: 2017-03-23 14:37:41 +0100 (jeu., 23 mars 2017) $
 */

package com.twinsoft.convertigo.engine.admin.services.keys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

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
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.tas.Key;
import com.twinsoft.tas.KeyManager;


@ServiceDefinition(
		name = "Remove",
		roles = { Role.WEB_ADMIN, Role.KEYS_CONFIG },
		parameters = {},
		returnValue = "",
		cloud_forbidden = true
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

		Properties keysProperties = new Properties();
		keysProperties.load(new FileInputStream(tasRoot + "/Java/keys.txt"));

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
		
		keysProperties.store(new FileOutputStream(tasRoot + "/Java/keys.txt"), null);
	}
}
