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

import java.io.File;
import java.io.InputStream;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class BeansDefaultValues {
	
	private static final String XMLPATH = "/com/twinsoft/convertigo/beans/dabase_objects_default.xml";
	private static final Pattern patternBeanName = Pattern.compile("(.*) \\[(.*?)(?:-(.*))?\\]");
	
	private static Element nextElement(Node node, boolean checkParameter) {
		if (node == null || (checkParameter && node instanceof Element)) {
			return (Element) node;
		}
		do {
			node = node.getNextSibling();
		} while (node != null && node.getNodeType() != Node.ELEMENT_NODE);
		return (Element) node;
	}
	
	private static boolean checkIsSame(Element dElt, Element pElt) {
		if (dElt == null || pElt == null) {
			return false;
		}
		
		if (!dElt.getTagName().equals(pElt.getTagName())) {
			return false;
		}
		
		NamedNodeMap dAttr = dElt.getAttributes();
		NamedNodeMap pAttr = pElt.getAttributes();
		
		if (dAttr.getLength() != pAttr.getLength()) {
			return false;
		}
		
		for (int i = 0; i < dAttr.getLength(); i++) {
			Node dAt = dAttr.item(i);
			Node pAt = pAttr.getNamedItem(dAt.getNodeName());
			if (pAt == null || !dAt.getNodeValue().equals(pAt.getNodeValue())) {
				return false;
			}
		}
		
		if (dElt.getFirstChild() instanceof CDATASection) {
			String dData = ((CDATASection) dElt.getFirstChild()).getData();
			if (pElt.getFirstChild() instanceof CDATASection) {
				String pData = ((CDATASection) pElt.getFirstChild()).getData();
				if (!dData.equals(pData)) {
					return false;
				}
			} else {
				return false;
			}
		} else {
			Element dNextElt = nextElement(dElt.getFirstChild(), true);
			Element pNextElt = nextElement(pElt.getFirstChild(), true);
			
			if (dNextElt == null && pNextElt == null) {
				if (!dElt.getTextContent().equals(pElt.getTextContent())) {
					return false;
				}
			}
			
			while (dNextElt != null) {
				if (!checkIsSame(dNextElt, pNextElt)) {
					return false;
				}
				
				dNextElt = nextElement(dNextElt, false);
				pNextElt = nextElement(pNextElt, false);
			}
			if (pNextElt != null) {
				return false;
			}
		}
		
		return true;
	}
	
	private static void shrinkChildren(Element beans, Element element, Element copy) {
		TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
		for (Node pBeanNode: xpath.selectList(element, "*[@classname]")) {
			Element pBean = (Element) pBeanNode;
			String classname = pBean.getAttribute("classname");
			String cls = classname.substring(30);
			String pName = xpath.selectNode(pBean, "property[@name='name']/*/@value").getNodeValue();
			String pPriority = pBean.getAttribute("priority");
			
			pPriority = "0".equals(pPriority) ? "" : '-' + pPriority;
			
			Element nCopy = copy.getOwnerDocument().createElement("bean");
			copy.appendChild(nCopy);
			nCopy.setAttribute("yaml_key", pName + " [" + cls + pPriority + ']');
			
			if (cls.startsWith("connectors.")) {
				nCopy.setAttribute("yaml_file", "connectors/" + pName + ".yaml");
			} else if (cls.startsWith("sequences.")) {
				nCopy.setAttribute("yaml_file", "sequences/" + pName + ".yaml");
			} else if (cls.equals("mobile.components.PageComponent")) {
				nCopy.setAttribute("yaml_file", "mobilePages/" + pName + ".yaml");
			}
			
			Element dBean = (Element) xpath.selectNode(beans, "*[@classname='" + classname + "']");
			
			for (Node pAttr: xpath.selectList(pBean, "@*")) {
				String name = pAttr.getNodeName();
				if (!name.equals("classname") &&
						!name.equals("priority") && (
						!dBean.hasAttribute(name) ||
						!pAttr.getNodeValue().equals(dBean.getAttribute(name))
				)) {
					nCopy.setAttribute(name, pAttr.getNodeValue());
				}
			}
			
			for (Node pPropNode: xpath.selectList(pBean, "property[@name]")) {
				Element pProp = (Element) pPropNode;
				String name = pProp.getAttribute("name");
				Element dProp = (Element) xpath.selectNode(dBean, "property[@name='" + name + "']");
				if (!"name".equals(name) &&
						(dProp == null || !checkIsSame(pProp, dProp)
				)) {
					Element nProp = (Element) nCopy.appendChild(nCopy.getOwnerDocument().createElement(name));
					
					for (Node pAttr: xpath.selectList(pProp, "@*")) {
						String aName = pAttr.getNodeName();
						if (!aName.equals("name") && 
								!aName.equals("isNull") && (
								!dProp.hasAttribute(aName) ||
								!pAttr.getNodeValue().equals(dProp.getAttribute(aName))
						)) {
							nProp.setAttribute(aName, pAttr.getNodeValue());
						}
					}
					
					Element content = nextElement(pProp.getFirstChild(), true);
					
					if (nextElement(content, false) == null && content.getTagName().startsWith("java.lang.")) {
						nProp.setTextContent(content.getAttribute("value"));
					} else {
						nProp.appendChild(nProp.getOwnerDocument().importNode(content, true));
					}
				}
			}
			
			for (Node pOther: xpath.selectList(pBean, "*[local-name()!='property' and not(@classname)]")) {
				String name = pOther.getNodeName();
				Element dOther = (Element) xpath.selectNode(dBean, name);
				if (!checkIsSame(dOther, (Element) pOther)) {
					Element nImport = (Element) nCopy.getOwnerDocument().importNode(pOther, true);
					nCopy.appendChild(nImport);
				}
			}
			
			shrinkChildren(beans, pBean, nCopy);
		}
	}
	
	public static Document shrinkProject(Document project) throws Exception {
		Document beans;
		try (InputStream is = BeansDefaultValues.class.getResourceAsStream(XMLPATH)) {
			beans = XMLUtils.getDefaultDocumentBuilder().parse(is);
		}
		
		Element eProject = project.getDocumentElement();
		
		Document nProjectDoc = XMLUtils.createDom();
		Element nProject = (Element) nProjectDoc.appendChild(nProjectDoc.createElement("root"));
		
		Element eAttr = (Element) nProject.appendChild(nProjectDoc.createElement("bean"));
		eAttr.setAttribute("yaml_attr", "convertigo");
		eAttr.setTextContent(eProject.getAttribute("version"));
		
		eAttr = (Element) nProject.appendChild(nProjectDoc.createElement("bean"));
		eAttr.setAttribute("yaml_attr", "beans");
		eAttr.setTextContent(eProject.getAttribute("beans"));
		
		shrinkChildren(beans.getDocumentElement(), project.getDocumentElement(), nProject);
		
		return nProjectDoc;
	}
	
	public static Document unshrinkProject(Document project) throws Exception {
		Document beans;
		try (InputStream is = BeansDefaultValues.class.getResourceAsStream(XMLPATH)) {
			beans = XMLUtils.getDefaultDocumentBuilder().parse(is);
		}
		Document nProjectDoc = XMLUtils.createDom();
		
		Element nProject = (Element) nProjectDoc.appendChild(nProjectDoc.importNode(project.getDocumentElement(), false));
		
		unshrinkChildren(beans.getDocumentElement(), project.getDocumentElement(), nProject);
		
		String shortVersion = nProject.getAttribute("beans");
		shortVersion = shortVersion.substring(0, shortVersion.lastIndexOf("."));
		nProject.setAttribute("engine", shortVersion);
		nProject.setAttribute("studio", shortVersion);
		nProject.setAttribute("version", nProject.getAttribute("convertigo"));
		nProject.removeAttribute("convertigo");
		
		return nProjectDoc;
	}
	
	private static void unshrinkChildren(Element beans, Element element, Element nParent) {
		TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
		for (Node pBeanNode: xpath.selectList(element, "bean[@yaml_key]")) {
			Element pBean = (Element) pBeanNode;
			
			Matcher matcherBeanName = patternBeanName.matcher(pBean.getAttribute("yaml_key"));
			
			matcherBeanName.matches();

			String pName = matcherBeanName.group(1);
			String classname = "com.twinsoft.convertigo.beans." + matcherBeanName.group(2);
			String pPriority = matcherBeanName.group(3);
			Element dBean = (Element) xpath.selectNode(beans, "*[@classname='" + classname + "']");
			
			Element nBean = null;
			try {
				nBean = (Element) nParent.getOwnerDocument().importNode(dBean, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			nParent.appendChild(nBean);
			
			for (Node pAttr: xpath.selectList(pBean, "@*")) {
				String name = pAttr.getNodeName();
				if (!name.startsWith("yaml_")) {
					nBean.setAttribute(name, pAttr.getNodeValue());
				}
			}
			
			((Element) xpath.selectNode(nBean, "property[@name='name']/*")).setAttribute("value", pName);
			if (pPriority != null) {
				nBean.setAttribute("priority", pPriority);
			}
			
			for (Node pPropNode: xpath.selectList(pBean, "*[not(@yaml_key)]")) {
				Element nProp = (Element) xpath.selectNode(nBean, "property[@name='" + pPropNode.getNodeName() + "']");
				if (nProp == null) {
					Element nOther = (Element) xpath.selectNode(nBean, pPropNode.getNodeName());
					if (nOther != null) {
						nBean.replaceChild(nBean.getOwnerDocument().importNode(pPropNode, true), nOther);
						continue;	
					} else {
						nProp = (Element) nBean.appendChild(nBean.getOwnerDocument().createElement("property"));
					}
				}
				nProp.setAttribute("name", pPropNode.getNodeName());
				
				for (Node pAttr: xpath.selectList(pPropNode, "@*")) {
					String name = pAttr.getNodeName();
					if (!name.equals("name")) {
						nProp.setAttribute(name, pAttr.getNodeValue());
					}
				}
				
				if (xpath.selectNode(pPropNode, "*") == null) {
					Element nValue = (Element) xpath.selectNode(nProp, "*");
					if (nValue == null) {
						nValue = (Element) nProp.appendChild(nBean.getOwnerDocument().createElement("java.lang.String"));
					}
					String value = ((Element) pPropNode).getTextContent();
					nValue.setAttribute("value", value);

					if (nProp.hasAttribute("isNull")) {
						nProp.setAttribute("isNull", "false");
					}
				} else {
					while (nProp.getFirstChild() != null) {
						nProp.removeChild(nProp.getFirstChild());
					}
					Node pNode = pPropNode.getFirstChild();
					while (pNode != null) {
						nProp.appendChild(nProp.getOwnerDocument().importNode(pNode, true));
						pNode = pNode.getNextSibling();
					}
				}
			}
			
			unshrinkChildren(beans, pBean, nBean);
		}
	}
	
	private static Document updateBeansDefaultValues() throws Exception {
		try (InputStream dbInputstream = BeansDefaultValues.class.getResourceAsStream(
				"/com/twinsoft/convertigo/beans/database_objects.xml")) {
			Document documentBeansXmlDatabase = XMLUtils.getDefaultDocumentBuilder().parse(dbInputstream);
			TwsCachedXPathAPI xpath = new TwsCachedXPathAPI();
			EnginePropertiesManager.initProperties();
			
			SortedSet<Node> nodes = new TreeSet<Node>((n1, n2) -> {
				return n1.getNodeValue().compareTo(n2.getNodeValue());
			});
			nodes.addAll(xpath.selectList(documentBeansXmlDatabase, "//bean/@classname"));
			
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element beans = document.createElement("beans");
			document.appendChild(beans);
			
			for (Node node: nodes) {
				String className = node.getNodeValue();
				DatabaseObject dbo = (DatabaseObject) Class.forName(className).newInstance();
				Element def = dbo.toXml(document);
				if (def.hasAttribute("priority")) {
					def.setAttribute("priority", "0");
				}
				beans.appendChild(def);
			};
			
			return document;
		}
	}
	
	private static void updateBeansDefaultValues(File output) throws Exception {
		FileUtils.write(output, XMLUtils.prettyPrintDOM(updateBeansDefaultValues()), "UTF-8");
	}

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			File output = new File(args[0]);
			if (!output.exists() || !output.isDirectory()) {
				System.err.println("Base dir " + output.getCanonicalPath() + " doesn't exists nor a directory.");
			}
			
			output = new File(output, XMLPATH);
			
			Engine.logBeans = Logger.getRootLogger();
			
			updateBeansDefaultValues(output);
			System.out.println("Beans default values updated in: " + output.getCanonicalPath());
			for (int i = 1; i < args.length; i++) {
				File copy = new File(args[1]);
				if (copy.exists() && copy.isDirectory()) {
					copy = new File(copy, XMLPATH);
					FileUtils.copyFile(output, copy);
					System.out.println("Beans default values updated in: " + copy.getCanonicalPath());
				}
			}
		}
//		File output = new File("c:/TMP/beans.xml");
////		updateBeansDefaultValues(output);
//		File projectOri = new File("C:/TMP/sample_HelloWorld5/sample_HelloWorld5.xml");
//		File projectShrink = new File("C:/TMP/sample_HelloWorld5/sample_HelloWorld5.shrink.xml");
//		File projectUnshrink = new File("C:/TMP/sample_HelloWorld5/sample_HelloWorld5.unshrink.xml");
//		Document project = XMLUtils.loadXml(projectOri);
//		FileUtils.write(new File("C:/TMP/sample_HelloWorld5/sample_HelloWorld5.pretty.xml"), XMLUtils.prettyPrintDOM(project), "UTF-8");
//		Document beans = XMLUtils.loadXml(output);
//		shrinkProject(beans, project);
//		FileUtils.write(projectShrink, XMLUtils.prettyPrintDOM(project), "UTF-8");
//		unshrinkProject(beans, project);
//		FileUtils.write(projectUnshrink, XMLUtils.prettyPrintDOM(project), "UTF-8");
	}

}
