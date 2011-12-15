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
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
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

	public static void main(String[] args) throws Exception {

		Engine.logBeans = Logger.getLogger(BeansDoc.class);
		// Engine.logBeans.setLevel(Level.TRACE);

		Document documentBeansDoc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		ProcessingInstruction pi = documentBeansDoc.createProcessingInstruction("xml",
				"version=\"1.0\" encoding=\"UTF-8\"");
		documentBeansDoc.appendChild(pi);


		Element dbdRoot = documentBeansDoc.createElement("database_objects");
		documentBeansDoc.appendChild(dbdRoot);

		DboExplorerManager manager = new DboExplorerManager();
		
		List<DboGroup> groups = new LinkedList<DboGroup>();
		List<DboCategory> categories = new LinkedList<DboCategory>();
		List<DboBeans> beansCategories = new LinkedList<DboBeans>();
		List<DboBean> beans = new LinkedList<DboBean>();
		
		groups = manager.getGroups();
		
		for (DboGroup group : groups) {
			Element dbdGroup = documentBeansDoc.createElement("group");
			dbdRoot.appendChild(dbdGroup);	
			Element dbdGroupName = documentBeansDoc.createElement("name");
			Text groupName = documentBeansDoc.createTextNode(group.getName());
			dbdGroupName.appendChild(groupName);
			dbdGroup.appendChild(dbdGroupName);
			categories = group.getCategories();
			for (DboCategory category : categories) {
				Element dbdCategory = documentBeansDoc.createElement("category");
				dbdGroup.appendChild(dbdCategory);	
				Element dbdCategoryName = documentBeansDoc.createElement("name");
				if( !( ("").equals(category.getName() ) ) ) {
					Text categoryName = documentBeansDoc.createTextNode(category.getName());
					dbdCategoryName.appendChild(categoryName);
					dbdCategory.appendChild(dbdCategoryName);
				}
				beansCategories = category.getBeans(); 
				for (DboBeans beansCategory : beansCategories) {
					Element dbdBeans = documentBeansDoc.createElement("beans");
					dbdCategory.appendChild(dbdBeans);	
					Element dbdBeansName = documentBeansDoc.createElement("name");
					if( !( ("").equals(beansCategory.getName() ) ) ) {
						Text beansName = documentBeansDoc.createTextNode(beansCategory.getName());
						dbdBeansName.appendChild(beansName);
						dbdBeans.appendChild(dbdBeansName);
					}
					beans = beansCategory.getBeans();
					addBean(beans,documentBeansDoc,dbdBeans);
				}
			}
		}		
		String sDocument = XMLUtils.prettyPrintDOM(documentBeansDoc);
		System.out.println(sDocument);
		FileWriter writer = new FileWriter("beans.xml");
		writer.write(sDocument);
		writer.close();
	}
	
	private static void addBean(List<DboBean> beans, Document documentBeansDoc, Element dbdParentElement) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IntrospectionException {
		for (DboBean bean : beans) {
			String databaseObjectClassName = bean.getClassName();
			if(bean.isEnable()) {
				if(bean.isDocumented()) {
					createBeanElement(databaseObjectClassName, documentBeansDoc, dbdParentElement,true);
				}
				else {
					createBeanElement(databaseObjectClassName, documentBeansDoc, dbdParentElement,false);
				}
			}
		}
	}

	private static void createBeanElement(String databaseObjectClassName,
			Document document, Element parentElement, Boolean bEnable)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, IntrospectionException {
		DatabaseObject databaseObject = (DatabaseObject) Class.forName(
				databaseObjectClassName).newInstance();

		boolean isExtractionRule = databaseObject instanceof ExtractionRule;

		Class<?> databaseObjectClass = databaseObject.getClass();
		BeanInfo beanInfo = Introspector.getBeanInfo(databaseObjectClass);
		BeanDescriptor databaseObjectBeanDescriptor = beanInfo
				.getBeanDescriptor();
		PropertyDescriptor[] propertyDescriptors = beanInfo
				.getPropertyDescriptors();
		int len = propertyDescriptors.length;
		

		Element elementBean = document.createElement("bean");
		parentElement.appendChild(elementBean);

		Element elementSub;
		Text elementText;

		elementSub = document.createElement("class");
		elementText = document.createTextNode(databaseObjectClassName);
		elementSub.appendChild(elementText);
		elementBean.appendChild(elementSub);

		elementSub = document.createElement("icon");
		elementText = document.createTextNode(MySimpleBeanInfo.getIconName(beanInfo, BeanInfo.ICON_COLOR_32x32));
		elementSub.appendChild(elementText);
		elementBean.appendChild(elementSub);

		elementSub = document.createElement("display_name");
		elementText = document.createTextNode(databaseObjectBeanDescriptor
				.getDisplayName());
		elementSub.appendChild(elementText);
		elementBean.appendChild(elementSub);

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
			elementText = document.createTextNode("Not yet documented.\nFor more information, do not hesitate to contact us in the forum in our Developer Network web site: http://www.convertigo.com/itcenter.html");
			elementSub.appendChild(elementText);
			elementBean.appendChild(elementSub);
		}
	}
}
