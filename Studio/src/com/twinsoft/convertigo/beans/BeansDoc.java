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

package com.twinsoft.convertigo.beans;
 
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class BeansDoc {
	private static Map<String, Element> collision = new HashMap<String, Element>();

	public static void main(String[] args) throws Exception {

		// Pseudo initialization of the C8O engine...

		// Loggers
		Engine.logBeans = Logger.getLogger(BeansDoc.class);
		Engine.logContext = Logger.getLogger(BeansDoc.class);
		Engine.logEngine = Logger.getLogger(BeansDoc.class);

		// Starting the DBO database exploration...
		Document documentBeansDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		ProcessingInstruction pi = documentBeansDoc.createProcessingInstruction("xml",
				"version=\"1.0\" encoding=\"UTF-8\"");
		documentBeansDoc.appendChild(pi);

		Element dbdRoot = documentBeansDoc.createElement("database_objects");
		documentBeansDoc.appendChild(dbdRoot);

		DboExplorerManager manager = new DboExplorerManager();
		
		List<DboGroup> groups = manager.getGroups();
		
		for (DboGroup group : groups) {
			Element dbdGroup = documentBeansDoc.createElement("group");
			dbdRoot.appendChild(dbdGroup);	
			Element dbdGroupName = documentBeansDoc.createElement("name");
			Text groupName = documentBeansDoc.createTextNode(group.getName());
			dbdGroupName.appendChild(groupName);
			dbdGroup.appendChild(dbdGroupName);
			List<DboCategory> categories = group.getCategories();
			for (DboCategory category : categories) {
				String categoryName = category.getName();
				Element dbdCategory = documentBeansDoc.createElement("category");
				dbdGroup.appendChild(dbdCategory);	
				Element dbdCategoryName = documentBeansDoc.createElement("name");
				if (!"".equals(categoryName)) {
					dbdCategoryName.appendChild(documentBeansDoc.createTextNode(categoryName));
					dbdCategory.appendChild(dbdCategoryName);
				}
				List<DboBeans> beansCategories = category.getBeans(); 
				for (DboBeans beansCategory : beansCategories) {
					String beansCategoryName = beansCategory.getName();
					Element dbdBeans = documentBeansDoc.createElement("beans");
					dbdCategory.appendChild(dbdBeans);	
					Element dbdBeansName = documentBeansDoc.createElement("name");
					if( !"".equals(beansCategoryName)) {
						Text beansName = documentBeansDoc.createTextNode(beansCategoryName);
						dbdBeansName.appendChild(beansName);
						dbdBeans.appendChild(dbdBeansName);
					}
					List<DboBean> beans = beansCategory.getBeans();
					for (DboBean bean : beans) {
						String databaseObjectClassName = bean.getClassName();
						if(bean.isEnable()) {
							switch (bean.getDocumentedMode()) {
							case TRUE:
								createBeanElement(databaseObjectClassName, documentBeansDoc, dbdBeans, true);
								break;
							case FALSE:
								createBeanElement(databaseObjectClassName, documentBeansDoc, dbdBeans, false);
								break;
							default: break;
							}
						}
					}
				}
			}
		}		
		String sDocument = XMLUtils.prettyPrintDOM(documentBeansDoc);
		System.out.println(sDocument);
		FileWriter writer = new FileWriter("beans.xml");
		writer.write(sDocument);
		writer.close();
	}

	private static void createBeanElement(String databaseObjectClassName, Document document, Element parentElement, Boolean bEnable)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IntrospectionException {
		DatabaseObject databaseObject = (DatabaseObject) Class.forName(
				databaseObjectClassName).newInstance();

		boolean isExtractionRule = databaseObject instanceof ExtractionRule;

		Class<?> databaseObjectClass = databaseObject.getClass();
		BeanInfo beanInfo = Introspector.getBeanInfo(databaseObjectClass);
		BeanDescriptor databaseObjectBeanDescriptor = beanInfo.getBeanDescriptor();
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		int len = propertyDescriptors.length;
		
		Element elementBean = document.createElement("bean");
		parentElement.appendChild(elementBean);

		Element elementSub;
		Text elementText;
		String displayName = databaseObjectBeanDescriptor.getDisplayName();

		elementSub = document.createElement("class");
		elementText = document.createTextNode(databaseObjectClassName);
		elementSub.appendChild(elementText);
		elementBean.appendChild(elementSub);

		elementSub = document.createElement("icon");
		elementText = document.createTextNode(MySimpleBeanInfo.getIconName(beanInfo, BeanInfo.ICON_COLOR_32x32));
		elementSub.appendChild(elementText);
		elementBean.appendChild(elementSub);

		elementSub = document.createElement("display_name");
		elementText = document.createTextNode(displayName);
		elementSub.appendChild(elementText);
		elementBean.appendChild(elementSub);
		
		// Name collision detection
		if (collision.containsKey(displayName)) {
			Element otherBean = collision.get(displayName);
			if (otherBean != null) {
				changeName(otherBean);
			}
			changeName(elementBean);
			collision.put(displayName, null);
		} else {
			collision.put(displayName, elementBean);
		}
		

		String description = databaseObjectBeanDescriptor.getShortDescription();
		int idx = description.indexOf(" | ");
		String shortDescpription = description;
		String longDescpription = "";

		if (idx != -1) {
			shortDescpription = description.substring(0, idx);
			longDescpription = description.substring(idx + 3);
		}
		if (bEnable) {
			elementSub = document.createElement("short_description");
			elementText = document.createTextNode(shortDescpription);
			elementSub.appendChild(elementText);
			elementBean.appendChild(elementSub);
	
			elementSub = document.createElement("long_description");
			longDescpription.replaceAll("\\n", "\n");
			elementText = document.createTextNode(longDescpription);
			elementSub.appendChild(elementText);
			elementBean.appendChild(elementSub);
	
			for (int i = 0; i < len; i++) {
				PropertyDescriptor databaseObjectPropertyDescriptor = propertyDescriptors[i];
	
				// Don't display hidden property descriptors
				if (databaseObjectPropertyDescriptor.isHidden()) {
					continue;
				}

				Method getter = databaseObjectPropertyDescriptor.getReadMethod();
				Method setter = databaseObjectPropertyDescriptor.getWriteMethod();
	
				// Only display read/write property descriptors
				if (getter == null || setter == null) {
					continue;
				}
	
				String category = "standard";
				if (isExtractionRule) {
					category = "configuration";
					if (databaseObjectPropertyDescriptor.isExpert()) {
						category = "selection";
					}
				} else if (databaseObjectPropertyDescriptor.isExpert()) {
					category = "expert";
				}
	
				description = databaseObjectPropertyDescriptor
						.getShortDescription();
				idx = description.indexOf(" | ");
				shortDescpription = description;
				longDescpription = "";
	
				if (idx != -1) {
					shortDescpription = description.substring(0, idx);
					longDescpription = description.substring(idx + 3);
				}
	
				Element elementProperty = document.createElement("property");
				elementBean.appendChild(elementProperty);
	
				elementSub = document.createElement("type");
				elementText = document
						.createTextNode(databaseObjectPropertyDescriptor
								.getPropertyType().getSimpleName());
				elementSub.appendChild(elementText);
				elementProperty.appendChild(elementSub);
	
				elementSub = document.createElement("category");
				elementText = document.createTextNode(category);
				elementSub.appendChild(elementText);
				elementProperty.appendChild(elementSub);
	
				elementSub = document.createElement("name");
				elementText = document
						.createTextNode(databaseObjectPropertyDescriptor.getName());
				elementSub.appendChild(elementText);
				elementProperty.appendChild(elementSub);
	
				elementSub = document.createElement("display_name");
				elementText = document
						.createTextNode(databaseObjectPropertyDescriptor
								.getDisplayName());
				elementSub.appendChild(elementText);
				elementProperty.appendChild(elementSub);
	
				elementSub = document.createElement("short_description");
				elementText = document.createTextNode(shortDescpription);
				elementSub.appendChild(elementText);
				elementProperty.appendChild(elementSub);
	
				elementSub = document.createElement("long_description");
				elementText = document.createTextNode(longDescpription);
				elementSub.appendChild(elementText);
				elementProperty.appendChild(elementSub);
			}
		}
		else {
			elementSub = document.createElement("short_description");
			elementText = document.createTextNode("Not yet documented.\nFor more information, do not hesitate to contact us in the forum in our Developer Network website: http://www.convertigo.com/itcenter.html");
			elementSub.appendChild(elementText);
			elementBean.appendChild(elementSub);
		}
	}
	
	private static void changeName(Element bean) {
		try {
			Element nameElement = (Element) XPathAPI.selectSingleNode(bean, "display_name");
			String name = nameElement.getTextContent();
			String groupName = (XPathAPI.selectSingleNode(bean, "ancestor::category/name[text()='Variables']") != null) ?
				XPathAPI.selectSingleNode(bean, "ancestor::beans/name/text()").getNodeValue() : // case of Variables
				XPathAPI.selectSingleNode(bean, "ancestor::group/name/text()").getNodeValue(); // default case
			
			nameElement.setTextContent(name + " (" + groupName + ")");
		} catch (TransformerException e) {
			System.err.println("Unexpected exception in changeName of BeansDoc");
			e.printStackTrace(System.err);
		}
	}
}
