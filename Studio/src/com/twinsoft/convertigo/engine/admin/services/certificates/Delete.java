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

package com.twinsoft.convertigo.engine.admin.services.certificates;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.util.FileAndProperties;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Delete",
		roles = { Role.WEB_ADMIN },
		parameters = {
				@ServiceParameterDefinition(
						name = "certificateName_{i}",
						description = "the name of the certificate i (from 1) to delete"
					)				
		},
		returnValue = ""
	)
public class Delete extends XmlService {
	
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();
      
        //String classToCreate = ServiceUtils.getRequiredParameter(request, "classToCreate");
			
        FileAndProperties rep=ServiceUtils.startCertificate();
		File file=rep.getF();
		Properties storesProperties=rep.getP();
		 
	 	String certificateName;

	 	int i = 1;	  	
	 	ArrayList<String> mappingsToDelete = new ArrayList<String>();
 		while ( (certificateName = (String)request.getParameter("certificateName_"+i)) != null) {
 			Iterator<Object> it = storesProperties.keySet().iterator();
			while (it.hasNext()) {
				String propertyName = (String) it.next();
				String propertyValue = storesProperties.getProperty(propertyName); 	
				if( propertyValue.equals(certificateName)){ 				 
					mappingsToDelete.add(propertyName); 				    
				}
			}
			for(int j = 0; j < mappingsToDelete.size(); j++){ 					
				//delete found mappings
				ServiceUtils.deleteMapping(storesProperties, mappingsToDelete.get(j), document, rootElement);
			}
 			if(storesProperties.remove(certificateName)!= null){
 				ServiceUtils.addMessage(document,rootElement,"Certificate "+certificateName+" has successfully been deleted.","message"); 				
 			}
 			else
 				throw new ServiceException("Certificate "+certificateName+" didn't exist");
 			storesProperties.remove(certificateName+".type");
 			storesProperties.remove(certificateName+".group");
 			i++;
 		}

 		FileOutputStream fos = new FileOutputStream(file);
 		storesProperties.store(fos , "");
 		fos.flush();
 		fos.close();
	}
}	