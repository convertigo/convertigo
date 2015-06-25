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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.tas.Key;
import com.twinsoft.tas.KeyManager;


@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = "",
		cloud_forbidden = true
	)
public class List extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();
        
        String tasRoot = EnginePropertiesManager.getProperty(PropertyName.CARIOCA_URL);
        
        KeyManager.init(tasRoot);
        
    	java.util.List<Key> keys = new ArrayList<Key>(GenericUtils.<Collection<Key>>cast(KeyManager.keys.values()));
    	Collections.sort(keys, new Comparator<Key>() {
			public int compare(Key o1, Key o2) {
				return o1.getQuickSortValue().toString().compareTo(o2.getQuickSortValue().toString());
			}
		});
    	
    	Iterator<Key> iKey = keys.iterator();
    	int nbValidKey = 0;
    	if (iKey.hasNext()) {
        	Key key = iKey.next();
        	while (key != null) {
            	int total = 0;
        		int emulatorID = key.emulatorID;
        		String emulatorName = KeyManager.getEmulatorName(emulatorID);

        		// Count number of valid keys
        		nbValidKey += KeyManager.hasExpired(emulatorID) ? 0 : (key.bDemo ? 0 : 1);

        		Element keysElement = document.createElement("keys");
        		
        		do {
        			total += key.cv;
        			
        			Element keyElement = document.createElement("key");
        			keyElement.setAttribute("text", key.sKey);
        			keyElement.setAttribute("value", Integer.toString( key.cv + (key.emulatorID == com.twinsoft.api.Session.EmulIDSE ? 990000000 : 0 ) ) );
        			keyElement.setAttribute("evaluation",key.bDemo ? "true" : "false");
        			keyElement.setAttribute("expired",key.cv == 0 ? "true" : "false");
        			keysElement.appendChild(keyElement);
        			
        			key = iKey.hasNext() ? iKey.next() : null;
        		} while (key != null && emulatorID == key.emulatorID);
        		
        		Element emulatorNameElement = document.createElement("category");    	    	    	
        		emulatorNameElement.setAttribute("name", emulatorName);
        		emulatorNameElement.setAttribute("remaining", Integer.toString(KeyManager.getCV(emulatorID)));
        		emulatorNameElement.setAttribute("total", Integer.toString(total));
        		
        		emulatorNameElement.appendChild(keysElement);
        		
        		rootElement.appendChild(emulatorNameElement);
        	}    		
    	}
    	
    	//We add the number of valid into the XML response
    	Element countValidKey = document.createElement("nb_valid_key");
    	countValidKey.setTextContent(nbValidKey + "");
		rootElement.appendChild(countValidKey);
	}
}
