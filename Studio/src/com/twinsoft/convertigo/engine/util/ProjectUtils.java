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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.WSDLUtils.WSDL;
import com.twinsoft.convertigo.engine.util.XSDUtils.XSD;
import com.twinsoft.convertigo.engine.util.XSDUtils.XSDException;

public class ProjectUtils {

	public static void RemoveUselessObjects(XSD xsd, Project project) {
		String projectName = project.getName();
		String project_ns = projectName+ "_ns";
		
		List<QName> qnames = new ArrayList<QName>();
		
		for (Connector c : project.getConnectorsList()) {
			String connectorName = c.getName();
			for (Transaction t: c.getTransactionsList()) {
				if (t.isPublicMethod()) {
					try {
						for (QName qname: xsd.getElementQNameList(project_ns, connectorName + "__" + t.getName()))
							addQNameInList(qnames, qname);
						for (QName qname: xsd.getElementQNameList(project_ns, connectorName + "__" + t.getName()+ "Response"))
							addQNameInList(qnames, qname);
					}
					catch (Exception e) {}
				}
			}
		}
		
		for (Sequence s : project.getSequencesList()) {
			if (s.isPublicMethod()) {
				try {
					for (QName qname: xsd.getElementQNameList(project_ns, s.getName()))
						addQNameInList(qnames, qname);
					for (QName qname: xsd.getElementQNameList(project_ns, s.getName()+ "Response"))
						addQNameInList(qnames, qname);
				}
				catch (Exception e) {}
			}
		}
		
		xsd.removeSchemaObjectsNotIn(qnames);
	}

	private static void addQNameInList(List<QName> qnames, QName qname) {
		if (qname!=null) {
			if (!qnames.contains(qname)) {
				qnames.add(qname);
			}
		}
	}
	/*********************************************************************************************************/
	

	public static boolean createXsdFile(String projectsDir, String projectName) throws Exception {
		String xsdURI = projectsDir + "/" + projectName + "/" + projectName + ".xsd";
		File file = new File(xsdURI);
		if (!file.exists() || file.length() == 0) {
			XSDUtils.createXSD(projectName, xsdURI);
			return true;
		}
		return false;
	}
	
	public static boolean createWsdlFile(String projectsDir, String projectName) throws Exception {
		return createWsdlFile(projectsDir, projectName, WSDLUtils.WSDL_STYLE_DOC, false, false);
	}
	
	public static boolean createWsdlFile(String projectsDir, String projectName, String wsdlStyle, boolean overrites, boolean bTempFile) throws Exception {
		String wsdlURI = projectsDir + "/" + projectName + "/" + projectName + (bTempFile ? ".temp":"") + ".wsdl";
		File file = new File(wsdlURI);
		if (overrites || !file.exists() || file.length() == 0) {
			WSDLUtils.createWSDL(projectName, wsdlURI, wsdlStyle);
			return true;
		}
		// Add SoapEnc namespace for Rpc array encoding (multivalued variable) : Fixed #1197
		else if (!overrites && file.exists()) {
			WSDL wsdl = getWSDL(projectName, bTempFile);
			addWsdlNamespaceDeclaration(wsdl, "soapenc", "http://schemas.xmlsoap.org/soap/encoding/");
			return true;
		}
		return false;
	}
	
	public static void migrateWsdlTypes(String projectName, DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean bTempFile) throws Exception {
		XSD xsd = getXSD(projectName, bTempFile);
		WSDL wsdl = getWSDL(projectName, bTempFile);
		migrateWsdlTypes(xsd, wsdl, projectName, parentOfRequestable, requestable, xsdTypes);
	}
	
	public static void migrateWsdlTypes(XSD xsd, WSDL wsdl, String projectName, DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes) throws Exception {
		updateWebService(xsd, wsdl, projectName, parentOfRequestable, requestable, xsdTypes, xsdTypes==null);
	}

	public static void updateWebService(String projectName, Project project, DatabaseObject dbo) throws Exception {
		if (project == null) return;
		
		String xsdSchemaTypes = "", wsdlSchemaTypes = "";

		// Update XSD file
		XSD xsd = getXSD(projectName, true);
		
		if (dbo instanceof Project) { // case of ws reference import into a project
			for (Connector connector: project.getConnectorsList()) {
				if (connector.bNew) {
					for (Transaction transaction: connector.getTransactionsList()) {
						if (transaction.bNew) {
							String xsdTypes = transaction.generateXsdTypes(null, false);
							xsdSchemaTypes += generateRequestableSchema(xsd, projectName, connector, transaction, xsdTypes, false);
							wsdlSchemaTypes += generateXsdArrayOfData(connector, transaction);
						}
					}
				}
			}
		}
		else if (dbo instanceof Sequence) { // case of xsd import into a sequence
			for (Sequence sequence: project.getSequencesList()) {
				if (sequence.equals(dbo)) {
					String xsdTypes = sequence.generateXsdTypes(null, false);
					xsdSchemaTypes += generateRequestableSchema(xsd, projectName, project, sequence, xsdTypes, false);
					wsdlSchemaTypes += generateXsdArrayOfData(project, sequence);
				}
			}
		}
		
		if (!xsdSchemaTypes.equals("")) {
			xsd.addSchemaObjects(projectName, xsdSchemaTypes);
			xsd.save();
		}
		
		// Update WSDL file
		if (!wsdlSchemaTypes.equals("")) {
			String s = "<document>"+wsdlSchemaTypes+"</document>";
			Document xsdTypesDoc = XMLUtils.parseDOM(new ByteArrayInputStream(s.getBytes("UTF-8")));
			WSDL wsdl = getWSDL(projectName, true);
			wsdl.addSchemaTypes(xsdTypesDoc);
			wsdl.save();
		}
	}
	
	public static void updateWebService(String projectName,  DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add, boolean bTempFile) throws Exception {
		// update XSD file
		XSD xsd = getXSD(projectName, bTempFile);
		updateXSDFile(xsd, projectName, parentOfRequestable, requestable, xsdTypes, add);
		
		// Update WSDL file
		WSDL wsdl = getWSDL(projectName, bTempFile);
		updateWSDLFile(wsdl, projectName, parentOfRequestable, requestable);
	}
	
	public static void updateWebService(XSD xsd, WSDL wsdl, String projectName,  DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add) throws Exception {
		// update XSD file
		updateXSDFile(xsd, projectName, parentOfRequestable, requestable, xsdTypes, add);
		
		// Update WSDL file
		updateWSDLFile(wsdl, projectName, parentOfRequestable, requestable);
	}

	public static XSD getXSD(String projectName, boolean bTempFile) throws XSDException {
		String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + (bTempFile ? ".temp":"") + ".xsd";
		XSD xsd = XSDUtils.getXSD(filePath);
		return xsd;
	}
	
	public static WSDL getWSDL(String projectName, boolean bTempFile) throws WSDLException {
		String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + (bTempFile ? ".temp":"") + ".wsdl";
		WSDL wsdl = WSDLUtils.getWSDL(filePath);
		return wsdl;
	}

	public static void addWsdlNamespaceDeclaration(WSDL wsdl, String prefix, String ns) throws WSDLException, IOException {
		if (wsdl.getDefinition().getNamespace(prefix) == null) {
			wsdl.addNamespaceDeclaration(prefix, ns);
			wsdl.save();
		}
	}
	
	public static void updateXSDFile(String projectName, DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add, boolean bTempFile) throws Exception {
		if (requestable == null) {
        	throw new Exception("Invalid parameteter for requestable");
        }
		
		XSD xsd = getXSD(projectName, bTempFile);
		updateXSDFile(xsd, projectName, parentOfRequestable, requestable, xsdTypes, add);
	}

	public static void updateXSDFileWithNS(String projectName, DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add, boolean bTempFile) throws Exception {
		if (requestable == null) {
        	throw new Exception("Invalid parameteter for requestable");
        }
		
		// Update schema
		updateXSDFile(projectName, parentOfRequestable, requestable, xsdTypes, add, bTempFile);
		
		// Remove unused namespaces
		String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + (bTempFile ? ".temp":"") + ".xsd";
		XSD xsd = getXSD(projectName, bTempFile);
		Map<String, String> namespaceMap = xsd.getNamespaceMap();
		removeUnusedNamespaces(namespaceMap, filePath);
	}
	
	protected static String generateRequestableSchema(XSD xsd, String projectName, DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add) throws Exception {
		if (xsd == null) {
        	throw new Exception("Invalid parameteter for xsd");
        }
		
		if (requestable == null) {
        	throw new Exception("Invalid parameteter for requestable");
        }
		
        String ns = projectName + "_ns";
        String requestableName = requestable.getName();
        String connectorPrefix = requestable.getXsdTypePrefix(parentOfRequestable);
		
		String requestdataSearchString = connectorPrefix + requestableName + "RequestData";
		String responsedataSearchString = connectorPrefix + requestableName + "ResponseData";

		String xsdComplexRequest = "";
		if (xsdTypes == null) {
			if (add)
				xsdComplexRequest = requestable.generateXsdRequestData();
			else
				xsdComplexRequest = "<xsd:complexType name=\""+ requestdataSearchString +"\" />\n";
		}
		
		String xsdComplexResponse = "";
		if (add && (requestable instanceof XmlHttpTransaction) && (!((XmlHttpTransaction)requestable).getResponseElementQName().equals(""))) {
			String reqn = ((XmlHttpTransaction)requestable).getResponseElementQName();
    		String opName = requestable.getName()+"Response", eltName = "response", eltType = "xsd:string";
    		boolean useRef = true;
    		int index, index2;
    		if ((index = reqn.indexOf(";")) != -1) {
    			useRef = false;
    			opName = reqn.substring(0, index);
    			if ((index2 = reqn.indexOf(";", index+1)) != -1) {
        			eltName = reqn.substring(index+1,index2);
        			eltType = reqn.substring(index2+1);
    			}
    		}
			
			xsdComplexResponse += "<xsd:complexType name=\""+ responsedataSearchString +"\" >\n";
			xsdComplexResponse += "  <xsd:sequence>\n";
			if (useRef)
				xsdComplexResponse += "    <xsd:element ref=\""+ reqn +"\"/>\n";
    		else {
    			xsdComplexResponse += "    <xsd:element name=\""+ opName +"\">\n";
    			xsdComplexResponse += "      <xsd:complexType>\n";
    			xsdComplexResponse += "        <xsd:sequence>\n";
    			xsdComplexResponse += "          <xsd:element name=\""+ eltName +"\" type=\""+ eltType +"\"/>\n";
    			xsdComplexResponse += "        </xsd:sequence>\n";
    			xsdComplexResponse += "      </xsd:complexType>\n";
    			xsdComplexResponse += "    </xsd:element>\n";
    		}
			xsdComplexResponse += "  </xsd:sequence>\n";
			xsdComplexResponse += "</xsd:complexType>\n";
		}
		else
			xsdComplexResponse = "<xsd:complexType name=\""+ responsedataSearchString +"\" />\n";
		
		if (add || xsdTypes == null) {
			xsdTypes += xsdComplexRequest + xsdComplexResponse;
		}
		
		if (xsdTypes.indexOf(requestdataSearchString) == -1) {
			if (!xsd.hasSchemaObject(projectName, "<xsd:complexType name=\""+ requestdataSearchString +"\" />\n"))
				xsdTypes += "<xsd:complexType name=\""+ requestdataSearchString +"\" />\n";
		}
		if (xsdTypes.indexOf(responsedataSearchString) == -1) {
			if (!xsd.hasSchemaObject(projectName, "<xsd:complexType name=\""+ responsedataSearchString +"\" />\n"))
				xsdTypes += "<xsd:complexType name=\""+ responsedataSearchString +"\" />\n";
		}
			
        String xsdElements = "";
		xsdElements += "  <xsd:element name=\"" + connectorPrefix + requestableName + "\" type=\""+ ns + ":" + requestdataSearchString +"\">\n";
		xsdElements += "    <xsd:annotation>\n";
		xsdElements += "      <xsd:documentation>"+ XMLUtils.getCDataXml(requestable.getComment()) +"</xsd:documentation>\n";
		xsdElements += "    </xsd:annotation>\n";
		xsdElements += "  </xsd:element>\n";
		xsdElements += "  <xsd:element name=\"" + connectorPrefix + requestableName + "Response\">\n";
		xsdElements += "    <xsd:complexType>\n";
		xsdElements += "      <xsd:sequence>\n";
		xsdElements += "        <xsd:element name=\"response\" type=\""+ ns + ":" + responsedataSearchString +"\"/>\n";
		xsdElements += "      </xsd:sequence>\n";
		xsdElements += "    </xsd:complexType>\n";
		xsdElements += "  </xsd:element>\n";
		
		return xsdElements + xsdTypes;
	}
	
	public static void updateXSDFile(XSD xsd, String projectName, DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add) throws Exception {
		if (xsd == null) {
        	throw new Exception("Invalid parameteter for xsd");
        }
		
		if (requestable == null) {
        	throw new Exception("Invalid parameteter for requestable");
        }
		
        boolean delete = (requestable.getParent() == null);
        String xsdSchema = generateRequestableSchema(xsd, projectName, parentOfRequestable, requestable, xsdTypes, add);
		
		if (!delete)
			xsd.addSchemaObjects(projectName, xsdSchema);
		else
			xsd.removeSchemaObjects(projectName, xsdSchema);
		xsd.save();
		
	}
	
	public static void updateWSDLFile(String projectName, DatabaseObject parentOfRequestable, RequestableObject requestable, boolean bTempFile) throws Exception {
        if (requestable == null) {
        	throw new Exception("Invalid parameteter for requestable");
        }
        
        WSDL wsdl = getWSDL(projectName, bTempFile);
        updateWSDLFile(wsdl, projectName, parentOfRequestable, requestable);
	}

	protected static String generateXsdArrayOfData(DatabaseObject parentOfRequestable, RequestableObject requestable) throws Exception {
		String xsdArrayTypes = "";
		if (((parentOfRequestable.getParent() != null) || (parentOfRequestable instanceof Project)) && 
			(!parentOfRequestable.getProject().getWsdlStyle().equals(Project.WSDL_STYLE_DOC))) {
				xsdArrayTypes = requestable.generateXsdArrayOfData();
		}
		return xsdArrayTypes;
	}
	
	public static void updateWSDLFile(WSDL wsdl, String projectName, DatabaseObject parentOfRequestable, RequestableObject requestable) throws Exception {
        if (wsdl == null) {
        	throw new Exception("Invalid wsdl for requestable");
        }
        if (requestable == null) {
        	throw new Exception("Invalid parameteter for requestable");
        }
        
		boolean delete = (requestable.getParent() == null);
        String connectorPrefix = requestable.getXsdTypePrefix(parentOfRequestable);
		String operationName = connectorPrefix + requestable.getName();
		
		if (((parentOfRequestable.getParent() != null) || (parentOfRequestable instanceof Project)) && 
			(!parentOfRequestable.getProject().getWsdlStyle().equals(Project.WSDL_STYLE_DOC))) {
			String xsdArrayTypes = requestable.generateXsdArrayOfData();
			if (!xsdArrayTypes.equals("")) {
				String s = "<document>"+xsdArrayTypes+"</document>";
				Document xsdTypesDoc = XMLUtils.parseDOM(new ByteArrayInputStream(s.getBytes("UTF-8")));
				wsdl.addSchemaTypes(xsdTypesDoc);
			}
		}
		
		if  (!delete && requestable.isPublicMethod())
			wsdl.addOperation(projectName, operationName, XMLUtils.getCDataText(requestable.getComment()));
		else
			wsdl.removeOperation(projectName, operationName);
		wsdl.save();
	}

	private static void removeUnusedNamespaces(Map<String, String> namespaceMap, String filePath) throws Exception {
		// Remove namespace declarations and imports if not referenced by elements
		Document doc = XMLUtils.parseDOM(filePath);
		
		String xsdDom = XMLUtils.prettyPrintDOMWithEncoding(doc, "UTF-8");
		for (String prefix : namespaceMap.keySet()) {
			String namespaceUri = namespaceMap.get(prefix);
			if (xsdDom.indexOf("=\""+prefix+":") == -1) { // not used
				removeUnusedNamespace(doc, prefix, namespaceUri);
			}
		}
		
		// Save file
		FileOutputStream fos = new FileOutputStream(filePath);
        String s = XMLUtils.prettyPrintDOMWithEncoding(doc, "UTF-8");
        fos.write(s.getBytes("UTF-8"));
        fos.close();
	}
	
	private static void removeUnusedNamespace(Document doc, String prefix, String namespaceUri) {
		// Remove namespace declaration
		String attrName = "xmlns:"+prefix;
		Element xsdRoot = doc.getDocumentElement();
		xsdRoot.removeAttribute(attrName);
		
		// Remove namespace import
		Node node, found = null;
		NodeList children = xsdRoot.getElementsByTagName("xsd:import");
		for (int i=0; i<children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (((Element)node).getAttribute("namespace").equals(namespaceUri)) {
					found = node;
					break;
				}
			}
		}
		if (found != null)
			xsdRoot.removeChild(found);		
	}

	public static void cleanSchema(List<String> names, String projectName, boolean bTempFile) throws Exception {
		String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + (bTempFile ? ".temp":"") + ".xsd";
		
		// Retrieve current xsd namespaces map
		XSD xsd = getXSD(projectName, bTempFile);
		Map<String, String> namespaceMap = xsd.getNamespaceMap();

		// Remove unused namespaces
		removeUnusedNamespaces(namespaceMap, filePath);
		
		// Clean sequence and step schemas
		cleanSequences(names, filePath);
	}

	private static void cleanSequences(List<String> names, String filePath) throws Exception {
		Document xsdDom = XMLUtils.parseDOM(filePath);
		Document cleanDom = XMLUtils.createDom("java");
		Element xsdRoot = xsdDom.getDocumentElement();
		Element cleanRoot = (Element)cleanDom.importNode(xsdRoot, false);
		cleanDom.appendChild(cleanRoot);
		
		// Replace sequence response schemas by an empty ones
		// Remove all elements which are step's schema elements
		ArrayList<String> eltnames = new ArrayList<String>();
		NodeList children = xsdRoot.getChildNodes();
		Node node, child, cleanNode;
		Element element;
		String eltname;
		for (int i=0; i<children.getLength(); i++) {
			node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				eltname = ((Element)node).getAttribute("name");
				if ((eltname != null) && !eltname.equals("")) {
					if (!eltnames.contains(eltname)) {
						// Ignore step's schema
						if ((eltname.endsWith("StepType")) ||
							(eltname.startsWith("step") && (eltname.endsWith("Type"))) ||
							(eltname.startsWith("group") && (eltname.endsWith("Type")))) {
							;
						}
						// Empty sequence
						else if (names.contains(eltname)) {
							element = ((Element)node);
							while ((child = element.getFirstChild())!=null)
								element.removeChild(child);
						}
						// keep others
						else {
							cleanNode = cleanDom.importNode(node, true);
							cleanRoot.appendChild(cleanNode);
							eltnames.add(eltname);
						}
					}
				}
				// keep others (xsd:import ...)
				else {
					cleanNode = cleanDom.importNode(node, true);
					cleanRoot.appendChild(cleanNode);
					eltnames.add(eltname);
				}
			}
		}
		
		// Save file
		FileOutputStream fos = new FileOutputStream(filePath);
        String s = XMLUtils.prettyPrintDOMWithEncoding(cleanDom, "UTF-8");
        fos.write(s.getBytes("UTF-8"));
        fos.close();
	}
	
	public static void addXSDFileImport(String projectName, String targetProjectName, boolean bTempFile) throws Exception {
		String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + (bTempFile ? ".temp":"") + ".xsd";
		XSD xsd = XSDUtils.getXSD(filePath);
		addXSDFileImport(xsd, projectName, targetProjectName);
	}
	
	public static void addXSDFileImport(XSD xsd, String projectName, String targetProjectName) throws Exception {
		String projectNamespace = targetProjectName+"_ns";
		String projectTargetNamespace = Project.getProjectTargetNamespace(targetProjectName);
		String projectLocation = "../"+targetProjectName+"/"+targetProjectName+".xsd";
		
		HashMap<String, String> nsmap = new HashMap<String, String>();
		nsmap.put(projectNamespace, projectTargetNamespace);
		xsd.addNamespaces(nsmap);
		
		HashMap<String, String> immap = new HashMap<String, String>();
		immap.put(projectTargetNamespace, projectLocation);
		xsd.addImportObjects(immap);
		
		xsd.save();
	}
	
	public static void restoreTempXSD(String projectName) throws Exception {
		String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xsd";
		XSD xsd = XSDUtils.getXSD(filePath);
		String tempfilePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.xsd";
		xsd.writeTo(tempfilePath);
	}
	
	public static boolean copyXsdFile(String projectsDir, String sourceProjectName, String targetProjectName) throws IOException {
		String oldPath = projectsDir + "/" + sourceProjectName + "/" + sourceProjectName + ".xsd";
		File oldFile = new File(oldPath);
		if (oldFile.exists()) {
			String newPath = projectsDir + "/" + targetProjectName + "/" + targetProjectName + ".xsd";
			File newFile = new File(newPath);
			if (!newFile.exists()) {
				if (newFile.createNewFile()) {
					String line;
					BufferedReader br = new BufferedReader(new FileReader(oldPath));
					BufferedWriter bw = new BufferedWriter(new FileWriter(newPath));
					while((line = br.readLine()) != null) {
						line = makeXsdProjectReplacements(line, sourceProjectName, targetProjectName);
					    bw.write(line);
					    bw.newLine();
					}
					bw.close();
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean copyWsdlFile(String projectsDir, String sourceProjectName, String targetProjectName) throws IOException {
		String oldPath = projectsDir + "/" + sourceProjectName + "/" + sourceProjectName + ".wsdl";
		File oldFile = new File(oldPath);
		if (oldFile.exists()) {
			String newPath = projectsDir + "/" + targetProjectName + "/" + targetProjectName + ".wsdl";
			File newFile = new File(newPath);
			if (!newFile.exists()) {
				if (newFile.createNewFile()) {
					String line;
					BufferedReader br = new BufferedReader(new FileReader(oldPath));
					BufferedWriter bw = new BufferedWriter(new FileWriter(newPath));
					while((line = br.readLine()) != null) {
						line = makeWsdlProjectReplacements(line, sourceProjectName, targetProjectName);
					    bw.write(line);
					    bw.newLine();
					}
					bw.close();
					return true;
				}
			}
		}
		return false;
	}
	
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
	
	public static void renameStepsInXsd(String projectsDir, String projectName, Hashtable<String, Step> pastedSteps) throws Exception {
		String xsdURI = projectsDir + "/" + projectName + "/" + projectName + ".xsd";
		File file = new File(xsdURI);
		if (file.exists()) {
			String oldPriority, newPriority;
			Enumeration<String> e = pastedSteps.keys();
			if (!e.hasMoreElements())
				return;
			
			String line;
			StringBuffer sb = new StringBuffer();
			BufferedReader br = new BufferedReader(new FileReader(xsdURI));
			while((line = br.readLine()) != null) {
				e = pastedSteps.keys();
				while (e.hasMoreElements()) {
					try {
						oldPriority = e.nextElement();
						newPriority = String.valueOf(pastedSteps.get(oldPriority).priority);
						if (line.indexOf("step"+oldPriority+"Type") != -1) {
							line = line.replaceAll("step"+oldPriority+"Type", "step"+newPriority+"Type");
						}
					}
					catch (Exception e2){
						Engine.logEngine.error("Unexpected exception", e2);
					}
				}
				sb.append(line+"\n");					
			}
			br.close();
			
			BufferedWriter out = new BufferedWriter(new FileWriter(xsdURI));
			out.write(sb.toString());
			out.close();
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
					String line;
					StringBuffer sb = new StringBuffer();
					
					BufferedReader br = new BufferedReader(new FileReader(newPath));
					while((line = br.readLine()) != null) {
						line = line.replaceAll("value=\""+sourceProjectName+"\"", "value=\""+targetProjectName+"\"");
						sb.append(line+"\n");
					}
					br.close();
					
					BufferedWriter out= new BufferedWriter(new FileWriter(newPath));
					out.write(sb.toString());
					out.close();
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
					String line;
					StringBuffer sb = new StringBuffer();
					
					BufferedReader br = new BufferedReader(new FileReader(newPath));
					while((line = br.readLine()) != null) {
						line = makeXsdProjectReplacements(line, sourceProjectName, targetProjectName);
						sb.append(line+"\n");
					}
					br.close();
					
					BufferedWriter out= new BufferedWriter(new FileWriter(newPath));
					out.write(sb.toString());
					out.close();
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
	
	public static void renameWsdlFile(String projectsDir, String sourceProjectName, String targetProjectName) throws Exception {
		String oldPath = projectsDir + "/" + targetProjectName + "/" + sourceProjectName + ".wsdl";
		File oldFile = new File(oldPath);
		if (oldFile.exists()) {
			String newPath = projectsDir + "/" + targetProjectName + "/" + targetProjectName + ".wsdl";
			File newFile = new File(newPath);
			if (!newFile.exists()) {
				if (oldFile.renameTo(newFile)) {
					String line;
					StringBuffer sb = new StringBuffer();
					
					BufferedReader br = new BufferedReader(new FileReader(newPath));
					while((line = br.readLine()) != null) {
						line = makeWsdlProjectReplacements(line, sourceProjectName, targetProjectName);
						sb.append(line+"\n");
					}
					br.close();
					
					BufferedWriter out= new BufferedWriter(new FileWriter(newPath));
					out.write(sb.toString());
					out.close();
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
	
	public static void renameConnector(String filePath, String oldName, String newName) throws Exception {
		if (filePath.endsWith(".wsdl") || filePath.endsWith(".xsd")) {
			File wsdlFile = new File(filePath);
			if (wsdlFile.exists()) {
				String line;
				StringBuffer sb = new StringBuffer();
				
				BufferedReader br = new BufferedReader(new FileReader(filePath));
				while((line = br.readLine()) != null) {
					line = makeConnectorReplacements(line, oldName, newName);
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
	}
	
	private static String makeConnectorReplacements(String line, String oldName, String newName) {
		line = line.replaceAll(oldName+"__", newName+"__");
		return line;
	}

	private static String makeXsdProjectReplacements(String line, String sourceProjectName, String targetProjectName) {
		line = line.replaceAll("/"+sourceProjectName, "/"+targetProjectName);
		line = line.replaceAll(sourceProjectName+"_ns", targetProjectName+"_ns");
		return line;
	}
	
	private static String makeWsdlProjectReplacements(String line, String sourceProjectName, String targetProjectName) {
		line = line.replaceAll("/"+sourceProjectName, "/"+targetProjectName);
		line = line.replaceAll(sourceProjectName+"_ns", targetProjectName+"_ns");
		line = line.replaceAll(sourceProjectName+".xsd", targetProjectName+".xsd");
		line = line.replaceAll(sourceProjectName+"Port", targetProjectName+"Port");
		line = line.replaceAll(sourceProjectName+"SOAP", targetProjectName+"SOAP");
		line = line.replaceAll("soapAction=\""+sourceProjectName+"\\?", "soapAction=\""+targetProjectName+"\\?");
		line = line.replaceAll("definitions name=\""+sourceProjectName+"\"", "definitions name=\""+targetProjectName+"\"");
		line = line.replaceAll("service name=\""+sourceProjectName+"\"", "service name=\""+targetProjectName+"\"");
		return line;
	}
	
	
	public static void getFullProjectDOM(Document document,String projectName) throws TransformerFactoryConfigurationError, EngineException, TransformerException{
		getFullProjectDOM(document,projectName,false, false, false,false,false) ;			
	}
	
	public static void getFullProjectDOM(Document document,String projectName,StreamSource xslFilter) throws TransformerFactoryConfigurationError, EngineException, TransformerException{
		getFullProjectDOM(document,projectName,false, false, false,false,false,xslFilter) ;			
	}
		
	
	public static void getFullProjectDOM(Document document,String projectName,boolean bIncludeDisplayName, boolean bIncludeCompiledValue, boolean bIncludeShortDescription,boolean bIncludeEditorClass,boolean bIncludeBlackListedElements,StreamSource xslFilter) throws TransformerFactoryConfigurationError, EngineException, TransformerException{
		Element root = document.getDocumentElement();
		getFullProjectDOM(document,projectName,bIncludeDisplayName, bIncludeCompiledValue, bIncludeShortDescription,bIncludeEditorClass,bIncludeBlackListedElements);
		
		// transformation du dom
		Transformer xslt = TransformerFactory.newInstance().newTransformer(xslFilter);				
		Element xsl = document.createElement("xsl");
		xslt.transform(new DOMSource(document), new DOMResult(xsl));
		root.replaceChild(xsl.getFirstChild(), root.getFirstChild());
		
	}
	
	public static void getFullProjectDOM(Document document,String projectName,boolean bIncludeDisplayName, boolean bIncludeCompiledValue, boolean bIncludeShortDescription,boolean bIncludeEditorClass,boolean bIncludeBlackListedElements) throws TransformerFactoryConfigurationError, EngineException, TransformerException{
		Element root = document.getDocumentElement();
				
		Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
		Element projectTag = project.toXml(document);
		projectTag.setAttribute("qname", project.getQName());
		root.appendChild(projectTag);

		constructDom(document, projectTag, project, bIncludeDisplayName,  bIncludeCompiledValue, bIncludeShortDescription,bIncludeEditorClass,bIncludeBlackListedElements);		
		
	}
	
	
	
	private static void constructDom(Document document, Element root, DatabaseObject father,boolean bIncludeDisplayName, boolean bIncludeCompiledValue, boolean bIncludeShortDescription, boolean bIncludeEditorClass, boolean bIncludeBlackListedElements) throws EngineException {
		
		if (father instanceof HtmlTransaction) {
			Collection<Statement> statements = ((HtmlTransaction) father).getStatements();
			addElement(statements, document, root, bIncludeDisplayName, bIncludeCompiledValue,  bIncludeShortDescription, bIncludeEditorClass, bIncludeBlackListedElements);			
		} else {
			if (father instanceof StatementWithExpressions) {
				Collection<Statement> statements = ((StatementWithExpressions) father).getStatements();
				addElement(statements, document, root,bIncludeDisplayName, bIncludeCompiledValue, bIncludeShortDescription, bIncludeEditorClass, bIncludeBlackListedElements);
			} 
		}
		List<DatabaseObject> dbos = father.getAllChildren();
		addElement(dbos, document, root,bIncludeDisplayName,bIncludeCompiledValue,bIncludeShortDescription,bIncludeEditorClass, bIncludeBlackListedElements);
		
		
	}

	private static <E extends DatabaseObject> void addElement(Collection<E> collection,
			Document document, Element root,boolean bIncludeDisplayName, boolean bIncludeCompiledValue, boolean bIncludeShortDescription, boolean bIncludeEditorClass, boolean bIncludeBlackListedElements) throws EngineException {
		for (E dbo : collection) {
			Element tag = dbo.toXml(document,bIncludeDisplayName,bIncludeCompiledValue,bIncludeShortDescription,bIncludeEditorClass,bIncludeBlackListedElements);
			tag.setAttribute("qname", dbo.getQName());
			root.appendChild(tag);
			constructDom(document, tag, dbo,bIncludeDisplayName, bIncludeCompiledValue, bIncludeShortDescription,bIncludeEditorClass,bIncludeBlackListedElements);
		}

	}
	
}
