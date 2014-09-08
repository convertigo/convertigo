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

package com.twinsoft.convertigo.engine.admin.services.database_objects;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.ClassUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.EnumUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@ServiceDefinition(name = "Set", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "the state of saving properties")
public class Set extends XmlService {
	private TwsCachedXPathAPI xpath;
	private Node postElt;
	
	private Object getPropertyValue(DatabaseObject object, String propertyName)
			throws TransformerException {
		Node nodetmp = xpath.selectSingleNode(postElt, "./property[@name=\"" + propertyName
				+ "\"]/*[1]");
				
		if (nodetmp == null)
			throw new IllegalArgumentException("Property '" + propertyName
					+ "' not found for object '" + object.getQName() + "'");
		
		Node nodeValue = xpath.selectSingleNode(nodetmp, "./@value");
		if (nodeValue == null)
			throw new IllegalArgumentException("Property '" + propertyName
					+ "' not found for object '" + object.getQName() + "'");

		String propertyValue = nodeValue.getNodeValue();
		
		return object.compileProperty(propertyName, propertyValue);
	}

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();
		Document post = null;
		Element response = document.createElement("response");
		
		try {
			Map<String, DatabaseObject> map = com.twinsoft.convertigo.engine.admin.services.projects.Get.getDatabaseObjectByQName(request);
			
			xpath = new TwsCachedXPathAPI();
			post = XMLUtils.parseDOM(request.getInputStream());
			postElt = document.importNode(post.getFirstChild(), true);

			String objectQName = xpath.selectSingleNode(postElt, "./@qname").getNodeValue();
			DatabaseObject object = map.get(objectQName);

//			String comment = getPropertyValue(object, "comment").toString();
//			object.setComment(comment);

			if (object instanceof Project) {
				Project project = (Project) object;
				
				String objectNewName = getPropertyValue(object, "name").toString();
				
				Engine.theApp.databaseObjectsManager.renameProject(project, objectNewName);
				
				map.remove(objectQName);
				map.put(project.getQName(), project);
			}

			BeanInfo bi = CachedIntrospector.getBeanInfo(object.getClass());

			PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();

			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				String propertyName = propertyDescriptor.getName();
				
				Method setter = propertyDescriptor.getWriteMethod();
				

				Class<?> propertyTypeClass = propertyDescriptor.getReadMethod().getReturnType();
				if (propertyTypeClass.isPrimitive()) {
					propertyTypeClass = ClassUtils.primitiveToWrapper(propertyTypeClass);
				}
				
				try{
					String propertyValue = getPropertyValue(object, propertyName).toString(); 

					Object oPropertyValue = createObject(propertyTypeClass, propertyValue);
	
					if (object.isCipheredProperty(propertyName)) {
						
						Method getter = propertyDescriptor.getReadMethod();
						String initialValue = (String) getter.invoke(object, (Object[]) null);
						
						if (oPropertyValue.equals(initialValue) || 
								DatabaseObject.encryptPropertyValue(initialValue).equals(oPropertyValue)) {
							oPropertyValue = initialValue;
						}else{
							object.hasChanged = true;
						}
					}
					
					if (oPropertyValue != null) {
						Object args[] = { oPropertyValue };
						setter.invoke(object, args);
					}
					
				} catch(IllegalArgumentException e){}
			}
			
			Engine.theApp.databaseObjectsManager.exportProject(object.getProject());
			response.setAttribute("state", "success");
			response.setAttribute("message", "Project have been successfully updated!");
		} catch(Exception e){
			Engine.logBeans.error("Error during saving the properties!\n"+e.getMessage());
			response.setAttribute("state", "error");
			response.setAttribute("message", "Error during saving the properties!");
			Element stackTrace = document.createElement("stackTrace");
			stackTrace.setTextContent(e.getMessage());
			root.appendChild(stackTrace);
		} finally {
			xpath.resetCache();
		}

		root.appendChild(response);
	}
	
	private Object createObject(Class<?> propertyClass, String value) throws ServiceException {
		Object oPropertyValue = null;

		if (Number.class.isAssignableFrom(propertyClass) ||
				Boolean.class.isAssignableFrom(propertyClass) ||
				String.class.isAssignableFrom(propertyClass)) {
			try {
				oPropertyValue = propertyClass.getConstructor(String.class).newInstance(value);	
			} catch (Exception e) {
				throw new ServiceException("Error when create the object:\n"+e.getMessage());
			}
		} else if (Enum.class.isAssignableFrom(propertyClass)) {
			oPropertyValue = EnumUtils.valueOf(propertyClass, value);
		}
		
		return oPropertyValue;
	}
}