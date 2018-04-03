/*
 * Copyright (c) 2001-2018 Convertigo SA.
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

package com.twinsoft.convertigo.beans;
 
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.Component;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.dbo_explorer.DboParent;
import com.twinsoft.convertigo.engine.util.XMLUtils;


public class BeansDoc {
	//private static Map<String, Element> collision = new HashMap<String, Element>();
	private static final Pattern pDescription = Pattern.compile("(.*?)(?:\\|(.*))?");

	public static void main(String[] args) throws Exception {
		Document documentBeansDoc = CreateDocumentBeansDoc();
	
		String sDocument = XMLUtils.prettyPrintDOM(documentBeansDoc);
		System.out.println(sDocument);
		FileWriter writer = new FileWriter("beans.xml");
		writer.write(sDocument);
		writer.close();
	}
	
	public static Document CreateDocumentBeansDoc() throws Exception
	{
		// Loggers
		Engine.logBeans = Logger.getLogger(BeansDoc.class);
		Engine.logContext = Logger.getLogger(BeansDoc.class);
		Engine.logEngine = Logger.getLogger(BeansDoc.class);
		
		// Engine properties manager
		EnginePropertiesManager.initProperties();

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
						if(bean.isEnable()) {
							switch (bean.getDocumentedMode()) {
							case TRUE:
								createBeanElement(bean, documentBeansDoc, dbdBeans, true);
								break;
							case FALSE:
								createBeanElement(bean, documentBeansDoc, dbdBeans, false);
								break;
							default: break;
							}
						}
					}
				}
			}
		}
		handleMobileComponents(documentBeansDoc);
		return documentBeansDoc;
	}
	
	private static void handleMobileComponents(Document doc) {
		JXPathContext xpath = JXPathContext.newContext(doc);
		Element category = (Element) xpath.selectSingleNode("/database_objects/group[name = 'Mobile Application']/category[name = 'Components']");
		xpath = JXPathContext.newContext(category);
		Map<String, Element> beansMap = new HashMap<String, Element>();
		
		for (String group: ComponentManager.getGroups()) {
			String group_name = group.replaceFirst("s$", "") + " Components";
			Element beans = (Element) xpath.selectSingleNode("beans[name = '" + group_name + "']");
			if (beans == null) {
				beans = (Element) category.appendChild(doc.createElement("beans"));
				((Element) beans.appendChild(doc.createElement("name"))).setTextContent(group_name);
			}
			beansMap.put(group, beans);
		}
		
		for (Component component: ComponentManager.getComponentsByGroup()) {
			DatabaseObject dbo = ComponentManager.createBean(component);
			if (!(dbo instanceof UIDynamicElement)) {
				System.out.println("no UIDynamicElement but " + dbo.getClass());
				continue;
			}
			
			IonBean ionBean = ((UIDynamicElement) dbo).getIonBean();
			
			Element beans = beansMap.get(component.getGroup());
			
			JXPathContext xbeans = JXPathContext.newContext(beans);
			Element bean = (Element) xbeans.selectSingleNode("bean[display_name = '" + component.getLabel() + "']");
			if (bean != null) {
				System.out.println("remove existing " + component.getLabel());
				bean.getParentNode().removeChild(bean);
			}
			
			bean = (Element) beans.appendChild(doc.createElement("bean"));
			((Element) bean.appendChild(doc.createElement("class"))).setTextContent(ionBean.getClassName());
			((Element) bean.appendChild(doc.createElement("icon"))).setTextContent(ionBean.getIconColor32Path());
			//((Element) bean.appendChild(doc.createElement("display_name"))).setTextContent(ionBean.getDisplayName());
			((Element) bean.appendChild(doc.createElement("display_name"))).setTextContent(ionBean.getLabel());
			String description[] = ionBean.getDescription().split("\\|", 2);
			((Element) bean.appendChild(doc.createElement("short_description"))).setTextContent(description[0].trim());
			if (description.length > 1) {
				((Element) bean.appendChild(doc.createElement("long_description"))).setTextContent(description[1].trim());
			}
			
			SortedSet<IonProperty> properties = new TreeSet<IonProperty>(new Comparator<IonProperty>() {

				@Override
				public int compare(IonProperty o1, IonProperty o2) {
					int res = o1.getCategory().startsWith("@") ? (o2.getCategory().startsWith("@") ? 0 : -1) : (o2.getCategory().startsWith("@") ? 1 : 0);
					if (res == 0) {
						res = o1.getCategory().compareTo(o2.getCategory());
					}
					if (res == 0) {
						res = o1.getLabel().compareTo(o2.getLabel());
					}
					return res;
				}
			});
			properties.addAll(ionBean.getProperties().values());
			for (IonProperty prop: properties) {
				Element property = (Element) bean.appendChild(doc.createElement("property"));
				((Element) property.appendChild(doc.createElement("type"))).setTextContent(prop.getType());
				((Element) property.appendChild(doc.createElement("category"))).setTextContent(prop.getCategory());
				((Element) property.appendChild(doc.createElement("name"))).setTextContent(prop.getName());
				((Element) property.appendChild(doc.createElement("display_name"))).setTextContent(prop.getLabel());
				((Element) property.appendChild(doc.createElement("short_description"))).setTextContent(prop.getDescription());
			}
		}
		doc.toString();
	}
	
	private static void createBeanElement(DboBean bean, Document document, Element parentElement, Boolean bEnable)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IntrospectionException, XPathExpressionException {
		String databaseObjectClassName = bean.getClassName();
		DatabaseObject databaseObject = (DatabaseObject) Class.forName(
				databaseObjectClassName).newInstance();

		boolean isExtractionRule = databaseObject instanceof ExtractionRule;
		Class<?> databaseObjectClass = databaseObject.getClass();
		BeanInfo beanInfo = Introspector.getBeanInfo(databaseObjectClass);
		BeanDescriptor databaseObjectBeanDescriptor = beanInfo.getBeanDescriptor();
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		
		Arrays.sort(propertyDescriptors, new Comparator<PropertyDescriptor>() {
				
			@Override
			public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
				if(o1.isExpert() == o2.isExpert())
					return o1.getDisplayName().compareTo(o2.getDisplayName());
				else if(o1.isExpert())
					return 1;
				else 
					return -1;
			}				
		} );
		
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

		String description = databaseObjectBeanDescriptor.getShortDescription();
		
		String shortDescpription = description;
		
		String longDescpription = "";
		
		Matcher mDescription = pDescription.matcher(description);
		if (mDescription.matches()) {
			shortDescpription = mDescription.group(1);
			if (mDescription.group(2) != null) {
				longDescpription = mDescription.group(2);
			}
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
	
			for (PropertyDescriptor databaseObjectPropertyDescriptor : propertyDescriptors) {
				boolean skip = false;
				longDescpription = "";
				
				// Don't display hidden property descriptors
				if (databaseObjectPropertyDescriptor.isHidden()) {
					skip = true;
				}
				
				Method getter = databaseObjectPropertyDescriptor.getReadMethod();
				Method setter = databaseObjectPropertyDescriptor.getWriteMethod();
	
				// Only display read/write property descriptors
				if (getter == null || setter == null) {
					skip = true;
				}
				
				String blackListedForParentClass = (String) databaseObjectPropertyDescriptor.getValue("blackListedForParentClass");
				if (blackListedForParentClass != null) {
					// check
					for (DboParent parent: bean.getParents()) {
						String parentName = parent.getClassName();
						if (blackListedForParentClass.equals(parentName)) {
							skip = true;
							continue;
						}							
					}
				}
				
				if (skip) {
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
				
				mDescription = pDescription.matcher(description);
				
				if (mDescription.matches()) {
					shortDescpription = mDescription.group(1).trim();
					if (mDescription.group(2) != null) {
						longDescpription = mDescription.group(2).trim();
					}
				}
				
	
				Element elementProperty = document.createElement("property");
				elementBean.appendChild(elementProperty);
	
				elementSub = document.createElement("type");
				if (databaseObjectPropertyDescriptor.getValue("scriptable") != null && 
						databaseObjectPropertyDescriptor.getValue("scriptable").toString().equals("true")) {
					elementText = document
							.createTextNode("JS expression");
				} else {
					elementText = document
							.createTextNode(databaseObjectPropertyDescriptor
									.getPropertyType().getSimpleName());
				}
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
	
}