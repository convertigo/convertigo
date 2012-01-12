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

package com.twinsoft.convertigo.engine.dbo_explorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.engine.util.XMLUtils;

public class DboExplorerManager {

	Document documentBeansXmlDatabase;

	List<DboGroup> groups;

	public DboExplorerManager() throws SAXException, IOException,
			ParserConfigurationException {
		
		InputStream dbInputstream = getClass().getResourceAsStream(
				"/com/twinsoft/convertigo/beans/database_objects.xml");
		documentBeansXmlDatabase = XMLUtils.getDefaultDocumentBuilder().parse(dbInputstream);

		NodeList nodeListGroups = documentBeansXmlDatabase.getDocumentElement()
				.getElementsByTagName("group");
		groups = new ArrayList<DboGroup>();
		for (int i = 0; i < nodeListGroups.getLength(); i++) {
			Element elementGroup = ((Element) nodeListGroups.item(i));
			DboGroup group = buildGroup(elementGroup);
			groups.add(group);
		}
	}

	private DboGroup buildGroup(Element elementGroup) {
		String name = elementGroup.getAttribute("name");

		List<DboCategory> categories = new ArrayList<DboCategory>();
		NodeList nodeListCategories = elementGroup.getElementsByTagName("category");
		for (int i = 0; i < nodeListCategories.getLength(); i++) {
			Element elementCategory = ((Element) nodeListCategories.item(i));
			DboCategory category = buildCategory(elementCategory);
			categories.add(category);
		}

		DboGroup group = new DboGroup(name, categories);
		return group;
	}

	private DboCategory buildCategory(Element elementCategory) {
		String name = elementCategory.getAttribute("name");
		List<DboBeans> beansCategories = new ArrayList<DboBeans>();
		NodeList nodeListBeans = elementCategory.getElementsByTagName("beans");
		for (int i = 0; i < nodeListBeans.getLength(); i++) {
			Element elementBeans = ((Element) nodeListBeans.item(i));
			DboBeans beans = buildBeansCategory(elementBeans);
			beansCategories.add(beans);
		}

		DboCategory category = new DboCategory(name, beansCategories);
		return category;
	}
	
	private DboBeans buildBeansCategory(Element elementBeans) {
		String groupName = elementBeans.getAttribute("groupname");
		List<DboBean> beans = new ArrayList<DboBean>();
		NodeList nodeListBeans = elementBeans.getElementsByTagName("bean");
		for (int i = 0; i < nodeListBeans.getLength(); i++) {
			Element elementBean = ((Element) nodeListBeans.item(i));
			DboBean bean = buildBean(elementBean);
			beans.add(bean);
		}

		DboBeans beansCategory = new DboBeans(groupName, beans);
		return beansCategory;
	}

	private DboBean buildBean(Element elementBean) {
		String className = elementBean.getAttribute("classname");
		String sEnable = elementBean.getAttribute("enable");
		String sDocumented = elementBean.getAttribute("documented");
		String sDefault = elementBean.getAttribute("default");
		Boolean bEnable;
		if ("true".equals(sEnable)) {
			bEnable = true;
		}
		else {
			bEnable = false;
		}
		Boolean bDocumented;
		if ("true".equals(sDocumented)) {
			bDocumented = true;
		}
		else {
			bDocumented = false;
		}
		Boolean bDefault;
		if ("true".equals(sDefault)) {
			bDefault = true;
		} 
		else {
			bDefault = false;
		}
		List<DboParent> parents = new ArrayList<DboParent>();
		NodeList nodeListParents = elementBean.getElementsByTagName("parent");
		for (int i = 0; i < nodeListParents.getLength(); i++) {
			Element elementParent = ((Element) nodeListParents.item(i));
			String sParent = elementParent.getAttribute("classname");
			String sAllowInheritance = elementParent.getAttribute("allowInheritance");
			Boolean bAllowInheritance;
			if ("true".equals(sAllowInheritance)) {
				bAllowInheritance = true;
			}
			else {
				bAllowInheritance = false;
			}
			DboParent parent = new DboParent(sParent, bAllowInheritance);
			parents.add(parent);
		}		
	
		List<String> EmulatorTechnologies = new ArrayList<String>();
		NodeList nodeListEmulatorTechnologies = elementBean.getElementsByTagName("emulator-technology");
		for (int i = 0; i < nodeListEmulatorTechnologies.getLength(); i++) {
			Element elementEmulatorTechnology = ((Element) nodeListEmulatorTechnologies.item(i));
			String emulatorTechnology = elementEmulatorTechnology.getAttribute("classname");
			EmulatorTechnologies.add(emulatorTechnology);
		}

		DboBean bean = new DboBean(className, bEnable, bDocumented, bDefault, parents, EmulatorTechnologies);
		return bean;
	}

	public List<DboGroup> getGroups() {
		return groups;
	}
}
