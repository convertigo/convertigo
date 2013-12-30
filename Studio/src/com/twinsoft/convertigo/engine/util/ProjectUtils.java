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

package com.twinsoft.convertigo.engine.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class ProjectUtils {

	public static void copyIndexFile(String projectName) throws Exception {
    	String projectRoot = Engine.PROJECTS_PATH+'/'+ projectName;
    	String templateBase = Engine.TEMPLATES_PATH+"/base";
    	File indexPage = new File(projectRoot+"/index.html");
    	if(!indexPage.exists()){
    		if(new File(projectRoot+"/sna.xsl").exists()){ /** webization javelin */
        		if(new File(projectRoot+"/templates/status.xsl").exists()) /** not DKU / DKU */
        			FileUtils.copyFile(new File(templateBase+"/index_javelin.html"), indexPage);
        		else FileUtils.copyFile(new File(templateBase+"/index_javelinDKU.html"), indexPage);
        	}else{
        		FileFilter fileFilterNoSVN = new FileFilter() {
    				public boolean accept(File pathname) {
    					return !pathname.getName().startsWith(".svn");
    				}
    			};
        		FileUtils.copyFile(new File(templateBase+"/index.html"), indexPage);
        		FileUtils.copyDirectory(new File(templateBase+"/js"), new File(projectRoot+"/js"), fileFilterNoSVN);
        		FileUtils.copyDirectory(new File(templateBase+"/css"), new File(projectRoot+"/css"), fileFilterNoSVN);
        	}
    	}
	}

	public static void renameProjectFile(String projectsDir, String sourceProjectName, String targetProjectName) throws Exception {
		String oldPath = projectsDir + "/" + targetProjectName + "/" + sourceProjectName + ".xml";
		File oldFile = new File(oldPath);
		if (oldFile.exists()) {
			String newPath = projectsDir + "/" + targetProjectName + "/" + targetProjectName + ".xml";
			File newFile = new File(newPath);
			if (!newFile.exists()) {
				if (oldFile.renameTo(newFile)) {
					List<Replacement> replacements = new ArrayList<Replacement>();
					replacements.add(new Replacement("value=\""+sourceProjectName+"\"", "value=\""+targetProjectName+"\""));
					makeReplacementsInFile(replacements, newPath, "<!--<Project");
				}
				else {
					throw new Exception("Unable to rename \""+oldPath+"\" to \""+newPath+"\"");
				}
			}
			else {
				throw new Exception("File \""+newPath+"\" already exists");
			}
		}
		else {
			throw new Exception("File \""+oldPath+"\" does not exist");
		}
	}

	public static void renameXmlProject(String projectsDir, String sourceProjectName, String targetProjectName) throws Exception {
		String oldPath = projectsDir + "/" + targetProjectName + "/" + sourceProjectName + ".xml";
		File oldFile = new File(oldPath);
		if (oldFile.exists()) {
			String newPath = projectsDir + "/" + targetProjectName + "/" + targetProjectName + ".xml";
			File newFile = new File(newPath);
			if (!newFile.exists()) {
				if (oldFile.renameTo(newFile)) {
					List<Replacement> replacements = new ArrayList<Replacement>();
					replacements.add(new Replacement("value=\""+sourceProjectName+"\"", "value=\""+targetProjectName+"\""));
					replacements.add(new Replacement("value=\""+sourceProjectName+"\\.", "value=\""+targetProjectName+"\\.")); // for call steps
					makeReplacementsInFile(replacements, newPath);
				}
				else {
					throw new Exception("Unable to rename \""+oldPath+"\" to \""+newPath+"\"");
				}
			}
			else {
				throw new Exception("File \""+newPath+"\" already exists");
			}
		}
		else {
			throw new Exception("File \""+oldPath+"\" does not exist");
		}
	}
	
	
	
	public static void renameXsdFile(String projectsDir, String sourceProjectName, String targetProjectName) throws Exception {
		String oldPath = projectsDir + "/" + targetProjectName + "/" + sourceProjectName + ".xsd";
		File oldFile = new File(oldPath);
		if (oldFile.exists()) {
			String newPath = projectsDir + "/" + targetProjectName + "/" + targetProjectName + ".xsd";
			File newFile = new File(newPath);
			if (!newFile.exists()) {
				if (oldFile.renameTo(newFile)) {
					List<Replacement> replacements = new ArrayList<Replacement>();
					replacements.add(new Replacement("/"+sourceProjectName, "/"+targetProjectName));
					replacements.add(new Replacement(sourceProjectName+"_ns", targetProjectName+"_ns"));
					makeReplacementsInFile(replacements, newPath);
				}
				else {
					throw new Exception("Unable to rename \""+oldPath+"\" to \""+newPath+"\"");
				}
			}
			else {
				throw new Exception("File \""+newPath+"\" already exists");
			}
		}
	}
	
	public static void renameWsdlFile(String projectsDir, String sourceProjectName, String targetProjectName) throws Exception {
		String oldPath = projectsDir + "/" + targetProjectName + "/" + sourceProjectName + ".wsdl";
		File oldFile = new File(oldPath);
		if (oldFile.exists()) {
			String newPath = projectsDir + "/" + targetProjectName + "/" + targetProjectName + ".wsdl";
			File newFile = new File(newPath);
			if (!newFile.exists()) {
				if (oldFile.renameTo(newFile)) {
					List<Replacement> replacements = new ArrayList<Replacement>();
					replacements.add(new Replacement("/"+sourceProjectName, "/"+targetProjectName));
					replacements.add(new Replacement(sourceProjectName+"_ns", targetProjectName+"_ns"));
					replacements.add(new Replacement(sourceProjectName+".xsd", targetProjectName+".xsd"));
					replacements.add(new Replacement(sourceProjectName+"Port", targetProjectName+"Port"));
					replacements.add(new Replacement(sourceProjectName+"SOAP", targetProjectName+"SOAP"));
					replacements.add(new Replacement("soapAction=\""+sourceProjectName+"\\?", "soapAction=\""+targetProjectName+"\\?"));
					replacements.add(new Replacement("definitions name=\""+sourceProjectName+"\"", "definitions name=\""+targetProjectName+"\""));
					replacements.add(new Replacement("service name=\""+sourceProjectName+"\"", "service name=\""+targetProjectName+"\""));
					makeReplacementsInFile(replacements, newPath);
				}
				else {
					throw new Exception("Unable to rename \""+oldPath+"\" to \""+newPath+"\"");
				}
			}
			else {
				throw new Exception("File \""+newPath+"\" already exists");
			}
		}
	}
	
	public static void renameConnector(String filePath, String oldName, String newName) throws Exception {
		if (filePath.endsWith(".wsdl") || filePath.endsWith(".xsd")) {
			List<Replacement> replacements = new ArrayList<Replacement>();
			replacements.add(new Replacement(oldName+"__", newName+"__"));
			makeReplacementsInFile(replacements, filePath);
		}
	}
	
	public static void makeReplacementsInFile(List<Replacement> replacements, String filePath) throws Exception {
		makeReplacementsInFile(replacements, filePath, null);
	}
	
	public static void makeReplacementsInFile(List<Replacement> replacements, String filePath, String lineBegin) throws Exception {
		File file = new File(filePath);
		if (file.exists()) {
			String line;
			StringBuffer sb = new StringBuffer();
			
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			while((line = br.readLine()) != null) {
				for (Replacement replacement: replacements) {
					if ((lineBegin == null) || (line.startsWith(lineBegin))) {
						line = line.replaceAll(replacement.getSource(), replacement.getTarget());
					}
				}
				sb.append(line+"\n");
			}
			br.close();
			
			BufferedWriter out= new BufferedWriter(new FileWriter(filePath));
			out.write(sb.toString());
			out.close();
		}
		else {
			throw new Exception("File \""+filePath+"\" does not exist");
		}
	}

	public static void getFullProjectDOM(Document document,String projectName, StreamSource xslFilter) throws TransformerFactoryConfigurationError, EngineException, TransformerException{
		Element root = document.getDocumentElement();
		getFullProjectDOM(document,projectName);
		
		// transformation du dom
		Transformer xslt = TransformerFactory.newInstance().newTransformer(xslFilter);				
		Element xsl = document.createElement("xsl");
		xslt.transform(new DOMSource(document), new DOMResult(xsl));
		root.replaceChild(xsl.getFirstChild(), root.getFirstChild());
	}
	
	public static void getFullProjectDOM(Document document, String projectName, ExportOption... exportOptions) throws TransformerFactoryConfigurationError, EngineException, TransformerException{
		Element root = document.getDocumentElement();
				
		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		Element projectTag = project.toXml(document, exportOptions);
		projectTag.setAttribute("qname", project.getQName());
		root.appendChild(projectTag);

		constructDom(document, projectTag, project);
	}
	
	private static void constructDom(Document document, Element root, DatabaseObject father, ExportOption... exportOptions) throws EngineException {		
		if (father instanceof HtmlTransaction) {
			Collection<Statement> statements = ((HtmlTransaction) father).getStatements();
			addElement(statements, document, root, exportOptions);			
		} else {
			if (father instanceof StatementWithExpressions) {
				Collection<Statement> statements = ((StatementWithExpressions) father).getStatements();
				addElement(statements, document, root, exportOptions);
			} 
		}
		List<DatabaseObject> dbos = father.getAllChildren();
		addElement(dbos, document, root, exportOptions);
	}

	private static <E extends DatabaseObject> void addElement(Collection<E> collection, Document document, Element root, ExportOption... exportOptions) throws EngineException {
		for (E dbo : collection) {
			Element tag = dbo.toXml(document, exportOptions);
			tag.setAttribute("qname", dbo.getQName());
			root.appendChild(tag);
			constructDom(document, tag, dbo, exportOptions);
		}
	}
	
	public static boolean existProjectSchemaReference(Project project, String projectName) {
		if (projectName.equals(project.getName()))
			return true;
		for (Reference reference : project.getReferenceList()) {
			if (reference instanceof ProjectSchemaReference) {
				if (((ProjectSchemaReference)reference).getProjectName().equals(projectName))
					return true;
			}
		}
		return false;
	}
	
	public static void addUndefinedGlobalSymbols(Project currentProject) throws Exception {
		Map<String,String> globalSymbols = new HashMap<String,String>();
		addUndefinedGlobalSymbols(currentProject, globalSymbols);
		
		//Update the global symbols file
		Properties prop = new Properties();
        prop.load(new FileInputStream(Engine.theApp.databaseObjectsManager.getGlobalSymbolsFilePath()));
        
        if (globalSymbols != null) {
	        for (String symbol : globalSymbols.keySet()) {
	        	prop.setProperty(symbol, globalSymbols.get(symbol) == null ? "0" : globalSymbols.get(symbol));
	        } 
        }
		prop.store(new FileOutputStream(Engine.theApp.databaseObjectsManager.getGlobalSymbolsFilePath()), "global symbols");
   
		Engine.theApp.databaseObjectsManager.updateSymbols(prop);
	}
	
	private static void addUndefinedGlobalSymbols(DatabaseObject currentDBO, Map<String,String> globalSymbols){
		List<DatabaseObject> dboChildrens = null;
		
		if ((dboChildrens = currentDBO.getAllChildren()) != null) {
			for (DatabaseObject dboChild : dboChildrens) {
				addUndefinedGlobalSymbols(dboChild,globalSymbols);
			}
		}
		
		Set<String> symbols = null;
		if ((symbols = currentDBO.getSymbolsErrors()) != null) {
			for (String symb : symbols) {
				globalSymbols.put(symb,null);
			}
		}
		
		//We add the symbol with default value
		Map<String,String> symbolsDefaultsValues = null;
		if ((symbolsDefaultsValues = currentDBO.getSymbolsDefaulsValues()) != null) {
			for (String symb : symbolsDefaultsValues.keySet()) {
				globalSymbols.put(symb, symbolsDefaultsValues.get(symb));
			}
		}		
	}
}
