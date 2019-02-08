/*
 * Copyright (c) 2001-2019 Convertigo SA.
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
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean.DocumentedMode;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.dbo_explorer.DboParent;
import com.twinsoft.convertigo.engine.util.StringUtils;


public class BeansDoc {
	private static final Pattern pDescription = Pattern.compile("(.*?)(?:\\|\\s*(.*))?");
	
	private int max;
	private int count = 0;
	private File outputDirectory;
	private File imageDirectory;
	private String groupName;
	private String categoryName;
	private String beansCategoryName;
	
	public static void main(String[] args) throws Exception {
		int max = -1;
		switch (args.length) {
		case 0:
			System.err.println("Missing arguments: <output directory> (optional: max objects)");
			System.exit(1);
		case 2:
			max = Integer.parseInt(args[1]);
		}
		File dir = new File(args[0]);
		if (!new File(dir, "_config.yml").exists()) {
			System.err.println("<output directory>: " + dir.getAbsolutePath() + "\nShould have a _config.yml file.");
		}
		File outputDirectory = new File(dir, "_object");
		File imageDirectory = new File(dir, "images/beans");
		System.out.println("Generating in: " + dir.getAbsolutePath());
		if (max >= 0) {
			System.out.println("Generating max " + max + " objects");
		}
		long start = System.currentTimeMillis();
		int count = new BeansDoc(outputDirectory, imageDirectory, max).run();
		long total = System.currentTimeMillis() - start;
		System.out.println("\nGenerated in: " + dir.getAbsolutePath());
		System.out.println("Generated " + count + " objects in: " + total + " ms (" + (total * 1.0f / count) + " ms / object)");
	}
	
	private BeansDoc(File outputDirectory, File imageDirectory, int max) {
		this.outputDirectory = outputDirectory;
		this.imageDirectory = imageDirectory;
		this.max = max;
	}
	
	private int run()  throws Exception {
		FileUtils.deleteQuietly(outputDirectory);
		FileUtils.deleteQuietly(imageDirectory);
		// Loggers
		Engine.logBeans = Logger.getLogger(BeansDoc.class);
		Engine.logContext = Logger.getLogger(BeansDoc.class);
		Engine.logEngine = Logger.getLogger(BeansDoc.class);
		
		// Engine properties manager
		EnginePropertiesManager.initProperties();

		DboExplorerManager manager = new DboExplorerManager();
		
		List<DboGroup> groups = manager.getGroups();
		for (DboGroup group : groups) {
			groupName = group.getName();
			List<DboCategory> categories = group.getCategories();
			for (DboCategory category : categories) {
				categoryName = category.getName();
				List<DboBeans> beansCategories = category.getBeans(); 
				for (DboBeans beansCategory : beansCategories) {
					beansCategoryName = beansCategory.getName();
					List<DboBean> beans = beansCategory.getBeans();
					for (DboBean bean : beans) {
						if(bean.isEnable()) {
							if (count == max) {
								return count;
							} else {
								count++;
							}
							System.out.println("handle /" + groupName + "/" + categoryName + "/" + beansCategoryName + "/" + bean.getClassName());
							createBeanElement(bean, bean.getDocumentedMode() == DocumentedMode.TRUE);
						}
					}
				}
			}
		}
//		handleMobileComponents(documentBeansDoc);
		return count;
	}
//	
//	private static void handleMobileComponents(Document doc) {
//		JXPathContext xpath = JXPathContext.newContext(doc);
//		Element category = (Element) xpath.selectSingleNode("/database_objects/group[name = 'Mobile Application']/category[name = 'Components']");
//		xpath = JXPathContext.newContext(category);
//		Map<String, Element> beansMap = new HashMap<String, Element>();
//		
//		for (String group: ComponentManager.getGroups()) {
//			String group_name = group.replaceFirst("s$", "") + " Components";
//			Element beans = (Element) xpath.selectSingleNode("beans[name = '" + group_name + "']");
//			if (beans == null) {
//				beans = (Element) category.appendChild(doc.createElement("beans"));
//				((Element) beans.appendChild(doc.createElement("name"))).setTextContent(group_name);
//			}
//			beansMap.put(group, beans);
//		}
//		
//		for (Component component: ComponentManager.getComponentsByGroup()) {
//			DatabaseObject dbo = ComponentManager.createBean(component);
//			if (!(dbo instanceof UIDynamicElement)) {
//				System.out.println("no UIDynamicElement but " + dbo.getClass());
//				continue;
//			}
//			
//			IonBean ionBean = ((UIDynamicElement) dbo).getIonBean();
//			
//			Element beans = beansMap.get(component.getGroup());
//			
//			JXPathContext xbeans = JXPathContext.newContext(beans);
//			Element bean = (Element) xbeans.selectSingleNode("bean[display_name = '" + component.getLabel() + "']");
//			if (bean != null) {
//				System.out.println("remove existing " + component.getLabel());
//				bean.getParentNode().removeChild(bean);
//			}
//			
//			bean = (Element) beans.appendChild(doc.createElement("bean"));
//			((Element) bean.appendChild(doc.createElement("class"))).setTextContent(ionBean.getClassName());
//			((Element) bean.appendChild(doc.createElement("icon"))).setTextContent(ionBean.getIconColor32Path());
//			//((Element) bean.appendChild(doc.createElement("display_name"))).setTextContent(ionBean.getDisplayName());
//			((Element) bean.appendChild(doc.createElement("display_name"))).setTextContent(ionBean.getLabel());
//			String description[] = ionBean.getDescription().split("\\|", 2);
//			((Element) bean.appendChild(doc.createElement("short_description"))).setTextContent(description[0].trim());
//			if (description.length > 1) {
//				((Element) bean.appendChild(doc.createElement("long_description"))).setTextContent(description[1].trim());
//			}
//			
//			SortedSet<IonProperty> properties = new TreeSet<IonProperty>(new Comparator<IonProperty>() {
//
//				@Override
//				public int compare(IonProperty o1, IonProperty o2) {
//					int res = o1.getCategory().startsWith("@") ? (o2.getCategory().startsWith("@") ? 0 : -1) : (o2.getCategory().startsWith("@") ? 1 : 0);
//					if (res == 0) {
//						res = o1.getCategory().compareTo(o2.getCategory());
//					}
//					if (res == 0) {
//						res = o1.getLabel().compareTo(o2.getLabel());
//					}
//					return res;
//				}
//			});
//			properties.addAll(ionBean.getProperties().values());
//			for (IonProperty prop: properties) {
//				Element property = (Element) bean.appendChild(doc.createElement("property"));
//				((Element) property.appendChild(doc.createElement("type"))).setTextContent(prop.getType());
//				((Element) property.appendChild(doc.createElement("category"))).setTextContent(prop.getCategory());
//				((Element) property.appendChild(doc.createElement("name"))).setTextContent(prop.getName());
//				((Element) property.appendChild(doc.createElement("display_name"))).setTextContent(prop.getLabel());
//				((Element) property.appendChild(doc.createElement("short_description"))).setTextContent(prop.getDescription());
//			}
//		}
//		doc.toString();
//	}
//	
	private void createBeanElement(DboBean bean, boolean bEnable) throws Exception {
		String databaseObjectClassName = bean.getClassName();
		DatabaseObject databaseObject = (DatabaseObject) Class.forName(databaseObjectClassName).newInstance();
		boolean isExtractionRule = databaseObject instanceof ExtractionRule;
		Class<?> databaseObjectClass = databaseObject.getClass();
		BeanInfo beanInfo = Introspector.getBeanInfo(databaseObjectClass);
		BeanDescriptor databaseObjectBeanDescriptor = beanInfo.getBeanDescriptor();
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		
		Arrays.sort(propertyDescriptors, new Comparator<PropertyDescriptor>() {
				
			@Override
			public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
				if (o1.isExpert() == o2.isExpert()) {
					return o1.getDisplayName().compareTo(o2.getDisplayName());
				} else if (o1.isExpert()) {
					return 1;
				} else { 
					return -1;
				}
			}				
		} );
		String displayName = databaseObjectBeanDescriptor.getDisplayName();
		String normalizedName = StringUtils.normalize(displayName).toLowerCase().replaceAll("_","-");
		String normalizedGroup = StringUtils.normalize(groupName).toLowerCase().replaceAll("_","-");
		String normalizedCatname = StringUtils.normalize(beansCategoryName).toLowerCase().replaceAll("_","-");		
		String iconName = MySimpleBeanInfo.getIconName(beanInfo, BeanInfo.ICON_COLOR_32x32);
		String iconPath = iconName.replaceFirst(".*/beans/", "");
		try (InputStream is = getClass().getResourceAsStream(iconName)) {
			FileUtils.copyInputStreamToFile(is, new File(imageDirectory, iconPath));
		}
		StringBuilder sb = new StringBuilder();
		String permalink;
		if(normalizedCatname == "")			
			permalink = "reference-manual/" + normalizedGroup + "/" + normalizedName + "/index.html";
		else
			permalink = "reference-manual/" + normalizedGroup + "/" + normalizedCatname + "/" + normalizedName + "/index.html";
		sb.append(
			"---\n" +
			"layout: page\n" +
			"title: " + displayName + "\n" +
			"sidebar: c8o_sidebar\n" +
			"permalink: " + permalink + "\n" +
			"ObjGroup: " + groupName + "\n" +
			"ObjCatName: " + normalizedCatname + "\n" +
			"ObjName: " + displayName + "\n" +
			"ObjClass: " + databaseObjectClassName + "\n" +
			"ObjIcon: /images/beans/" + iconPath + "\n" +
			"topnav: topnavobj" + "\n" +
			"---\n"
		);
		
		if (bEnable) {
			String description = databaseObjectBeanDescriptor.getShortDescription();
			
			String shortDescription = description;
			
			String longDescription = "";
			
			Matcher mDescription = pDescription.matcher(description);
			if (mDescription.matches()) {
				shortDescription = mDescription.group(1);
				if (mDescription.group(2) != null) {
					longDescription = mDescription.group(2);
					longDescription.replaceAll("\\n", "\n\n");
				}
			}
			
			sb.append(
				"##### " + shortDescription + "\n\n" + longDescription + "\n"
			);
			
			SortedMap<String, String> properties = new TreeMap<>();
			
			for (PropertyDescriptor databaseObjectPropertyDescriptor : propertyDescriptors) {
				boolean skip = false;
				longDescription = "";
				
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
							break;
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
				
				description = databaseObjectPropertyDescriptor.getShortDescription();
				
				mDescription = pDescription.matcher(description);
				
				if (mDescription.matches()) {
					description = mDescription.group(1).trim();
					if (mDescription.group(2) != null) {
						description += "<br/>" + mDescription.group(2).trim();
					}
				}
				
				String type = databaseObjectPropertyDescriptor.getPropertyType().getSimpleName();
				if ("true".equals("" + databaseObjectPropertyDescriptor.getValue("scriptable"))) {
					type = "JS expression";
				}
				
				String propDisplayName = databaseObjectPropertyDescriptor.getDisplayName();
				
				String line = propDisplayName + " | " +
						type + " | " +
						category + " | " +
						description + "\n";
				if (category.equals("standard")) {
					category = "0";
				}
				properties.put(category + "_" + propDisplayName, line);
			}
			
			if (!properties.isEmpty()) {
				sb.append("\nProperty | Type | Category | Description\n--- | --- | --- | ---\n");
				for (String line: properties.values()) {
					sb.append(line);
				}
			}
		} else {
			sb.append("##### Not yet documented.\nFor more information, do not hesitate to contact us in the forum in our Developer Network website: http://www.convertigo.com/itcenter.html\n");
		}
		String toWrite = sb.toString();
		if (!"\n".equals(System.lineSeparator())) {
			toWrite = toWrite.replace("\n", System.lineSeparator());
		}
		FileUtils.write(new File(outputDirectory, normalizedGroup + "/" + normalizedCatname + "/" + normalizedName + ".md"), toWrite, "UTF-8");
	}
}