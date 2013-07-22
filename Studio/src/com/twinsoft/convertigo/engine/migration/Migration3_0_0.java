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

package com.twinsoft.convertigo.engine.migration;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public class Migration3_0_0 {

    public static Element migrate(Document document, Element projectNode) throws Exception {
    	// Creation of the connector
		Engine.logDatabaseObjectManager.info("Connector creation");
    	JavelinConnector connector = new JavelinConnector();
    	connector.setName("javelin");
    	connector.isDefault = true;
		
		Element element;

		// Set the connector properties and remove the associated project properties
		NodeList nodeList = projectNode.getElementsByTagName("property");

		element = (Element) XMLUtils.findNodeByAttributeValue(nodeList, "name", "emulatorTechnology");
		String emulatorTechnology = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(element, Node.ELEMENT_NODE));
		connector.setEmulatorTechnology(emulatorTechnology);
		projectNode.removeChild(element);
    	
		element = (Element) XMLUtils.findNodeByAttributeValue(nodeList, "name", "tasServiceCode");
		String tasServiceCode = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(element, Node.ELEMENT_NODE));
		connector.setServiceCode(tasServiceCode);
		projectNode.removeChild(element);
    	
		element = (Element) XMLUtils.findNodeByAttributeValue(nodeList, "name", "cariocaVirtualServer");
		String cariocaVirtualServer = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(element, Node.ELEMENT_NODE));
		connector.setVirtualServer(cariocaVirtualServer);
		projectNode.removeChild(element);
    	
    	Element connectorNode = connector.toXml(document);
    	
		// Move the pools to the children of the connector
		Engine.logDatabaseObjectManager.info("Moving pools");
		nodeList = projectNode.getElementsByTagName("pool");
		int len = nodeList.getLength();
		Element[] pools = new Element[len];
		for (int i = 0 ; i < len ; i++) {
			pools[i] = (Element) nodeList.item(i);
		}
		Element pool;
		for (int i = 0 ; i < len ; i++) {
			pool = pools[i];
			connectorNode.appendChild(pool);
		}
    	
		// Move all transactions to the children of the connector
		Engine.logDatabaseObjectManager.info("Moving transactions");
		nodeList = projectNode.getElementsByTagName("transaction");
		len = nodeList.getLength();
		Element[] transactions = new Element[len];
		for (int i = 0 ; i < len ; i++) {
			transactions[i] = (Element) nodeList.item(i);
		}
		Element transaction;
		for (int i = 0 ; i < len ; i++) {
			transaction = transactions[i];
			if (transaction.getAttribute("classname").equals("com.twinsoft.convertigo.beans.core.JavelinTransaction")) {
				transaction.setAttribute("classname", "com.twinsoft.convertigo.beans.transactions.JavelinTransaction");
			}
			connectorNode.appendChild(transactions[i]);
		}

		// Move the root screen class to the children of the connector
		Engine.logDatabaseObjectManager.info("Moving screen classes");
		nodeList = projectNode.getElementsByTagName("screenclass");
		Node rootScreenClass = nodeList.item(0);
		connectorNode.appendChild(rootScreenClass);
    	
    	projectNode.appendChild(connectorNode);
    	
    	return projectNode;
    }

	public static void perform() throws Exception {
		String migration3_0_0 = EnginePropertiesManager.getProperty(PropertyName.MIGRATION_3_0_0);
		
		if (migration3_0_0.equals("")) {
			Engine.logDatabaseObjectManager.info("Beginning migration 3.0.0");
			renameAllFilesWithDollarSign(new File(Engine.PROJECTS_PATH));
			EnginePropertiesManager.setProperty(PropertyName.MIGRATION_3_0_0, "done");
			EnginePropertiesManager.saveProperties();
			Engine.logDatabaseObjectManager.info("Migration 3.0.0 successfully done");
		}
		else {
			Engine.logDatabaseObjectManager.info("Migration 3.0.0 already done");
		}
	}
    
	private static void renameAllFilesWithDollarSign(File file) {
		File[] projectDirs = file.listFiles(new FileFilter() {
			public boolean accept(File dir) {
				return dir.isDirectory();
			}
		});
		
		int len = projectDirs.length;
		for (int i = 0 ; i < len ; i++) {
			renameFilesWithDollarSign(new File(projectDirs[i].getAbsolutePath() + "/_data"));
		}
	}
    
	public static void done() throws Exception {
		String migration3_0_0 = EnginePropertiesManager.getProperty(PropertyName.MIGRATION_3_0_0);
		if (migration3_0_0.equals("")) {
			EnginePropertiesManager.setProperty(PropertyName.MIGRATION_3_0_0, "done");
			EnginePropertiesManager.saveProperties();
			Engine.logDatabaseObjectManager.debug("Migration 3.0.0 successfully done");
		}
		else {
			Engine.logDatabaseObjectManager.debug("Migration 3.0.0 already done");
		}
	}

	public static void projectRenameFilesWithDollarSign(File projectDir) {
		String migration3_0_0 = EnginePropertiesManager.getProperty(PropertyName.MIGRATION_3_0_0);
		if (migration3_0_0.equals("")) {
			if ((projectDir != null) && (projectDir.exists()))
				renameFilesWithDollarSign(new File(projectDir.getAbsolutePath() + "/_data"));
		}
	}
	
	private static void renameFilesWithDollarSign(File file) {
		Engine.logDatabaseObjectManager.debug("File: " + file.toString());
		if (!file.isDirectory()) return;
		
		File[] files = file.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.indexOf('$') != -1);
			}
		});
		
		int len = files.length;
		for (int i = 0 ; i < len ; i++) {
			renameFileWithDollarSign(files[i]);
		}
		
		File[] subDirs = file.listFiles(new FileFilter() {
			public boolean accept(File dir) {
				return dir.isDirectory();
			}
		});

		len = subDirs.length;
		for (int i = 0 ; i < len ; i++) {
			renameFilesWithDollarSign(subDirs[i]);
		}
	}

	private static void renameFileWithDollarSign(File file) {
		StringEx sx = new StringEx(file.getName());
		sx.replaceAll("$", "-");
		String newName = file.getParentFile().getAbsolutePath() + "/" + sx.toString();
		if (!file.renameTo(new File(newName))) {
			Engine.logDatabaseObjectManager.warn("Unable to rename '" + file.getAbsolutePath() + "' to '" + newName + "'");
		}
	}
}
