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

package com.twinsoft.convertigo.engine.admin.services.cache;

import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;
import com.twinsoft.convertigo.engine.cache.DatabaseCacheManager;
import com.twinsoft.convertigo.engine.util.Crypto2;

@ServiceDefinition(
		name = "Clear",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class ShowProperties extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {		
		String cacheManagerFileType = "com.twinsoft.convertigo.engine.cache.FileCacheManager";	
		
		Element root = document.getDocumentElement();			
		Element cacheTypeElt= document.createElement("cacheType");	
		String cacheType=EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_CLASS);		
		cacheTypeElt.setTextContent(cacheType);
		root.appendChild(cacheTypeElt);
		
		if(!cacheType.equals(cacheManagerFileType)){
			
			Properties dbCacheProp = new Properties();
			String dbCachePropFileName = Engine.CONFIGURATION_PATH + DatabaseCacheManager.DB_PROP_FILE_NAME;		
			dbCacheProp.load(new FileInputStream(dbCachePropFileName.toString()));
			
			String url,databaseType,serverName,port,databaseName;
			
			url=dbCacheProp.getProperty("jdbc.url");
				
			databaseType=url.replaceFirst("://(.*)", "").replaceAll("(.*):", "");
			url=url.replaceFirst("(.*)://", "");
			serverName=url.replaceFirst("\\:(.*)", "");
			url=url.replaceFirst("(.*)\\:", "");
			port=url.replaceFirst("/(.*)", "");
			databaseName=url.replaceFirst("(.*)/", "");
			
			Element databaseTypeElt= document.createElement("databaseType");					
			databaseTypeElt.setTextContent(databaseType);
			root.appendChild(databaseTypeElt);
			
			Element serverNameElt= document.createElement("serverName");					
			serverNameElt.setTextContent(serverName);
			root.appendChild(serverNameElt);
			
			Element portElt= document.createElement("port");					
			portElt.setTextContent(port);
			root.appendChild(portElt);
			
			Element databaseNameElt= document.createElement("databaseName");					
			databaseNameElt.setTextContent(databaseName);
			root.appendChild(databaseNameElt);			
			
			Element name= document.createElement("userName");					
			name.setTextContent(dbCacheProp.getProperty("jdbc.user.name"));
			root.appendChild(name);
			
			Element password= document.createElement("userPassword");					
			password.setTextContent(Crypto2.decodeFromHexString(dbCacheProp.getProperty("jdbc.user.password")));
			root.appendChild(password);		
			
		}
		
       
	}
	
	
}	