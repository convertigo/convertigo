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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.CompilablePropertyException;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@ServiceDefinition(name = "Set", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "the state of saving properties")
public class Set extends XmlService {
	private TwsCachedXPathAPI xpath;
	private Node postElt;

	private Object getPropertyValue(DatabaseObject object, String propertyName)
			throws TransformerException, CompilablePropertyException {
		Node nodetmp = xpath.selectSingleNode(postElt, "./property[@name=\"" + propertyName
				+ "\"]/*[1]/@value");
		String propertyValue = null;
		if (nodetmp == null)
			throw new IllegalArgumentException("Property '" + propertyName
					+ "' not found for object '" + object.getQName() + "'");

		propertyValue = nodetmp.getNodeValue();

		return DatabaseObject.compileProperty(object, propertyName, propertyValue);
	}

//	private String getPropertyValue(String propertyName) throws TransformerException {
//		Node nodetmp = xpath.selectSingleNode(postElt, "./property[@name=\"" + propertyName
//				+ "\"]/*[1]/@value");
//		if (nodetmp == null)
//			return "";
//		return nodetmp.getNodeValue();
//	}

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

//			String comment = getPropertyValue("comment");
			String comment = getPropertyValue(object, "comment").toString();
			object.setComment(comment);

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

				String propertyTypeString = xpath.selectSingleNode(postElt,
						"./property[@name=\"" + propertyName + "\"]/*[1]")
						.getNodeName();
				
//				String propertyValue = getPropertyValue(propertyName);
				try{
					String propertyValue = getPropertyValue(object, propertyName).toString();
					
					Object oPropertyValue = createObject(propertyTypeString, propertyValue);
	
					if (object.isCipheredProperty(propertyName)) {
						oPropertyValue = DatabaseObject.encryptPropertyValue(oPropertyValue);
					}
					
					if (oPropertyValue != null) {
						Object args[] = { oPropertyValue };
						setter.invoke(object, args);
					}
				} catch(IllegalArgumentException e){
					
				}
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

	private Object createObject(String propertyTypeString, Object propertyValue) {
		Object oPropertyValue = null;
		if (propertyTypeString.equals(String.class.toString().split("\\s")[1])) {
			oPropertyValue = propertyValue;
		} else if (propertyTypeString.equals(Character.class.toString().split(" ")[1])) {
			oPropertyValue = new Character(((String) propertyValue).charAt(0));
		} else if (propertyTypeString.equals(Boolean.class.toString().split(" ")[1])) {
			oPropertyValue = new Boolean(((String) propertyValue));
		} else if (propertyTypeString.equals(Integer.class.toString().split(" ")[1])) {
			oPropertyValue = new Integer(((String) propertyValue));
		} else if (propertyTypeString.equals(Long.class.toString().split(" ")[1])) {
			oPropertyValue = new Long(((String) propertyValue));
		} else if (propertyTypeString.equals(Float.class.toString().split(" ")[1])) {
			oPropertyValue = new Float(((String) propertyValue));
		} else if (propertyTypeString.equals(Double.class.toString().split(" ")[1])) {
			oPropertyValue = new Double(((String) propertyValue));
		} else if (propertyTypeString.equals(Byte.class.toString().split(" ")[1])) {
			oPropertyValue = new Byte(((String) propertyValue));
		} else if (propertyTypeString.equals(Short.class.toString().split(" ")[1])) {
			oPropertyValue = new Short(((String) propertyValue));
		}
		return oPropertyValue;
	}

}