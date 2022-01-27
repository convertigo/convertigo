/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.Component;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean.DocumentedMode;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.dbo_explorer.DboParent;


public class BeansDoc {
	private static final Pattern pDescription = Pattern.compile("(.*?)(?:\\|\\s*(.*))?");
	
	private int max;
	private int count = 0;
	private File outputDirectory;
	private File imageDirectory;
	private String groupName;
	private String categoryName;
	private String beansCategoryName;
	private static Map<String, String> fullnames = new HashMap<String, String>();
	
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
		File outputDirectory = new File(dir, "reference-manual/convertigo-objects");
		File imageDirectory = new File(dir, "images/beans");
		System.out.println("Generating in: " + dir.getAbsolutePath());
		if (max >= 0) {
			System.out.println("Generating max " + max + " objects");
		}
		long start = System.currentTimeMillis();
		int count = new BeansDoc(outputDirectory, imageDirectory, max).run();
		
		File sidebar = new File(dir, "_data/sidebars/c8o_sidebar.yml");
		if (sidebar.exists()) {
			String content = FileUtils.readFileToString(sidebar, "UTF-8");
			content = content.replaceFirst("(version: ).*", "$1" + ProductVersion.helpVersion);
			FileUtils.write(sidebar, content, "UTF-8");
		}
		
		long total = System.currentTimeMillis() - start;
		System.out.println("\nGenerated in: " + dir.getAbsolutePath());
		System.out.println("Generated " + count + " objects in: " + total + " ms (" + (total * 1.0f / count) + " ms / object)");
	}
	
	private BeansDoc(File outputDirectory, File imageDirectory, int max) {
		this.outputDirectory = outputDirectory;
		this.imageDirectory = imageDirectory;
		this.max = max;
		fullnames.put("convertigo-objects", "Convertigo Objects");
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
						if (bean.isEnable()) {
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
		generateMobileComponentsMd(outputDirectory);
		makeListingPages(outputDirectory);		
		return count;
	}
	
	private void generateMobileComponentsMd(File outputDirectory)
	{		
		List<Component> grpBeans = ComponentManager.getComponentsByGroup();	
		Map<String,IonBean> map = ComponentManager.getIonBeans();		
		
		for(Component beanMB: grpBeans)
		{
			String grpName = beanMB.getGroup();
			String objName = beanMB.getName();
			String objLabel = beanMB.getLabel();
			String imgPath = beanMB.getImagePath();
			
			String imgPathNormalized = imgPath.replaceFirst("/com/twinsoft/convertigo/beans/", "");
			String classPathNormalized = "ion_objects.json"; // We are changing it if the bean is not an IonBean just below
			String grpNameNormalized = grpName;
			
			Boolean isIonBean = false;
			
			// Here we are checking that our bean is not in an IonBean
			// If it is we set isIonBean to true;
			for (Map.Entry<String, IonBean> ionbean : map.entrySet()) {
				if(objName == ionbean.getKey()) {
					isIonBean = true;
				}				
			}
			// If this bean is not an IonBean we simply add this class
			if(isIonBean == false) {
				classPathNormalized = "com.twinsoft.convertigo.beans.mobile.components.res."+objName;
			}				
			
			if(!grpName.contains("Components")) {
				grpNameNormalized = grpName.concat(" Components");	
			}
			grpNameNormalized = mbNormalize(grpNameNormalized);
			
			String objLabelNormalized = mbNormalize(objLabel);
			String path = "/mobile-application/components/" + grpNameNormalized + "/";
			String permalink = "reference-manual/convertigo-objects" + path + objLabelNormalized + "/";
			
			String metadesc = beanMB.getDescription();
			metadesc = metadesc.replaceAll("<[a-zA-Z]*>|<\\/[a-zA-Z]*>|<br\\/>", " ");
			metadesc = metadesc.replaceAll(":", " ");
			metadesc = metadesc.replaceAll("\\|", " ");
			
			if(metadesc.length() >= 150)
				metadesc = metadesc.substring(0, 150);
			
			StringBuilder sb = new StringBuilder();
			sb.append("---\n" +
					"layout: page\n" +
					"title: " + objLabel + "\n" +
					"sidebar: c8o_sidebar\n" +
					"permalink: " + permalink + "\n" +
					"metadesc: " + metadesc + "\n" +
					"ObjGroup: " + grpName + "\n" +
					"ObjCatName: " + grpNameNormalized + "\n" +
					"ObjName: " + objName + "\n" +
					"ObjClass: " + classPathNormalized + "\n" +
					"ObjIcon: /images/beans/" + imgPathNormalized + "\n" +
					"topnav: topnavobj" + "\n" +
					"---\n");
			String description = beanMB.getDescription();
			description = description.replaceAll("\\|", "<br/>\n");
			description = description.replaceAll("<br\\/>", "<br/>\n");
			description = description.replaceAll("Defines| Defines", "##### Defines");
			sb.append(description + "\n\n");
			
			// Prepare properties to create a markdown table
			String properties = beanMB.getPropertiesDescription();
			properties = properties.replaceAll("</i></br>"," | ");
			properties = properties.replaceAll("<ul>|</ul>","");
			properties = properties.replaceAll("<li><i>","");
			properties = properties.replaceAll("</li>","\n");
			
			if(!properties.isEmpty()) {
				sb.append("Name | Description \n");
				sb.append("--- | ---\n");				
				sb.append(properties + "\n");
			}			
			
			String toWrite = sb.toString();
			if (!"\n".equals(System.lineSeparator())) {
				toWrite = toWrite.replace("\n", System.lineSeparator());
			}
			try (InputStream is = getClass().getResourceAsStream(imgPath)) {
				System.out.println("imgPath : " + imgPath);
				String imgPathModified = imgPath.replaceFirst(".*\\w\\/beans", "");
				FileUtils.copyInputStreamToFile(is, new File(imageDirectory, imgPathModified));
				System.out.println("Image generated at : " + imageDirectory + imgPathModified);
			} catch (Exception e) {
				System.out.println("Unable to copy the image file");
			}
			try {
				FileUtils.write(new File(outputDirectory, path + objLabelNormalized + ".md"), toWrite, "UTF-8");
				System.out.println("Generated md for : " + objLabel);
			}
			catch (IOException e) {
				System.out.println("Unable to write the file");
			}
		}
	}
	
	private String mbNormalize(String str) {
		String normalized = str.toLowerCase();
		normalized = normalized.replaceAll(" / ", " ");
		normalized = normalized.replaceAll(" ", "-");		
		if(!normalized.contains("miscellaneous")) {
			normalized = normalized.replaceAll("s-c", "-c");
		}
		fullnames.put(normalized, str);
		return normalized;
	}
	
	
	private void createBeanElement(DboBean bean, boolean bEnable) throws Exception {
		String databaseObjectClassName = bean.getClassName();
		Class<?> databaseObjectClass = Class.forName(databaseObjectClassName);
		DatabaseObject databaseObject = (DatabaseObject) databaseObjectClass.getConstructor().newInstance();
		BeanInfo beanInfo = Introspector.getBeanInfo(databaseObjectClass);
		BeanDescriptor databaseObjectBeanDescriptor = beanInfo.getBeanDescriptor();
		
		String displayName = databaseObjectBeanDescriptor.getDisplayName();
		
		String normalizedGroupName = normalize(groupName);
		String normalizedCategoryName = normalize(categoryName);
		String normalizedBeansCategoryName = normalize(beansCategoryName);
		String normalizedName = normalize(displayName);
		
		String path = (normalizedGroupName + "/" + normalizedCategoryName + "/" + normalizedBeansCategoryName + "/" + normalizedName).replaceAll("/+", "/");
		
		String iconName = MySimpleBeanInfo.getIconName(beanInfo, BeanInfo.ICON_COLOR_32x32);
		String iconPath = iconName.replaceFirst(".*/beans/", "");
		try {
			try (InputStream is = getClass().getResourceAsStream(iconName)) {
				FileUtils.copyInputStreamToFile(is, new File(imageDirectory, iconPath));
			}
		} catch (Exception e) {
			iconName = "/com/twinsoft/convertigo/beans/core/images/default_color_16x16.png";
			iconPath = iconName.replaceFirst(".*/beans/", "");
			try (InputStream is = getClass().getResourceAsStream(iconName)) {
				FileUtils.copyInputStreamToFile(is, new File(imageDirectory, iconPath));
			}
		}
		StringBuilder sb = new StringBuilder();
		String permalink = "reference-manual/convertigo-objects/" + path + "/";
		
		String metadesc = databaseObjectBeanDescriptor.getShortDescription();
		metadesc = metadesc.replaceAll("<[a-zA-Z]*>|<\\/[a-zA-Z]*>|<br\\/>", " ");
		metadesc = metadesc.replaceAll(":", " ");
		metadesc = metadesc.replaceAll("\\|", " ");
		
		if(metadesc.length() >= 150)
			metadesc = metadesc.substring(0, 150);
		
		sb.append(
			"---\n" +
			"layout: page\n" +
			"title: " + displayName + "\n" +
			"sidebar: c8o_sidebar\n" +
			"permalink: " + permalink + "\n" +
			"metadesc: " + metadesc + "\n" +
			"ObjGroup: " + groupName + "\n" +
			"ObjCatName: " + normalizedBeansCategoryName + "\n" +
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
			
			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
			
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
				if (databaseObject instanceof ExtractionRule) {
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
				description = description.replace("|", "&#124;");
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
		FileUtils.write(new File(outputDirectory, path + ".md"), toWrite, "UTF-8");
	}
	
	private void makeListingPages(File dir) throws IOException {
		String path = dir.getAbsolutePath().substring(outputDirectory.getAbsolutePath().length()).replace("\\", "/");
		String permalink = ("reference-manual/convertigo-objects/" + path + "/").replace("//", "/");
		StringBuilder sb = new StringBuilder();
		sb.append(
			"---\n" +
			"layout: page\n" +
			"title: " + fullnames.get(dir.getName()) + "\n" +
			"sidebar: c8o_sidebar\n" +
			"permalink: " + permalink + "\n" +
//			"ObjGroup: " + groupName + "\n" +
//			"ObjCatName: " + normalizedBeansCategoryName + "\n" +
//			"ObjName: " + displayName + "\n" +
//			"ObjClass: " + databaseObjectClassName + "\n" +
//			"ObjIcon: /images/beans/" + iconPath + "\n" +
			"topnav: topnavobj" + "\n" +
			"---\n"
		);
		String[] files = dir.list();
		Arrays.sort(files);
		for (String name: files) {
			File f = new File(dir, name);
			name = name.replace(".md", "");
			sb.append("* [" + fullnames.get(name) + "](" + name + "/)\n");
			if (f.isDirectory()) {
				makeListingPages(f);
			}
		}
		String toWrite = sb.toString();
		if (!"\n".equals(System.lineSeparator())) {
			toWrite = toWrite.replace("\n", System.lineSeparator());
		}
		FileUtils.write(new File(outputDirectory, path + "/index.md"), toWrite, "UTF-8");
	}
	
	String normalize(String str) {
		String normalized = str.toLowerCase().replaceAll("\\W", "-");
		fullnames.put(normalized, str);
		return normalized;
	}
}