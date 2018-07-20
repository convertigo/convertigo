package com.twinsoft.convertigo.beans;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class BeansDefaultValues {
	
	private static final String XMLPATH = "/com/twinsoft/convertigo/beans/dabase_objects_default.xml";
	
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
			if (pElt.getFirstChild() instanceof CDATASection) {
				String dData = ((CDATASection) dElt.getFirstChild()).getData();
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
	
	private static void shrinkChildren(Element beans, Element element) {
		TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
		for (Node pBeanNode: xpath.selectList(element, "*[@classname]")) {
			Element pBean = (Element) pBeanNode;
			String classname = pBean.getAttribute("classname");
			Element dBean = (Element) xpath.selectNode(beans, "*[@classname='" + classname + "']");
			
			for (Node dAttr: xpath.selectList(dBean, "@*")) {
				String name = dAttr.getNodeName(); 
				if (!name.equals("classname")) {
					if (dAttr.getNodeValue().equals(pBean.getAttribute(name))) {
						pBean.removeAttribute(name);
					}
				}
			}
			
			for (Node dPropNode: xpath.selectList(dBean, "property[@name]")) {
				Element dProp = (Element) dPropNode;
				String name = dProp.getAttribute("name");
				Element pProp = (Element) xpath.selectNode(pBean, "property[@name='" + name + "']");
				if (checkIsSame(dProp, pProp)) {
					pBean.removeChild(pProp);
				}
			}
			
			shrinkChildren(beans, pBean);
		}
	}
	
	public static void shrinkProject(Document project) throws SAXException, IOException {
		TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
		Document beans;
		try (InputStream is = BeansDefaultValues.class.getResourceAsStream(XMLPATH)) {
			beans = XMLUtils.getDefaultDocumentBuilder().parse(is);
		}
		shrinkChildren(beans.getDocumentElement(), project.getDocumentElement());
		for (Node n: xpath.selectList(project, "//@classname")) {
			n.setNodeValue(n.getNodeValue().replace("com.twinsoft.convertigo.beans.", "c8o."));
		}
	}
	
	public static void unshrinkProject(Document project) throws SAXException, IOException {
		TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
		Document beans;
		try (InputStream is = BeansDefaultValues.class.getResourceAsStream(XMLPATH)) {
			beans = XMLUtils.getDefaultDocumentBuilder().parse(is);
		}
		for (Node n: xpath.selectList(project, "//@classname")) {
			n.setNodeValue(n.getNodeValue().replace("c8o.", "com.twinsoft.convertigo.beans."));
		}
		unshrinkChildren(beans.getDocumentElement(), project.getDocumentElement());
	}
	
	private static void unshrinkChildren(Element beans, Element element) {
		TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
		for (Node pBeanNode: xpath.selectList(element, "*[@classname]")) {
			Element pBean = (Element) pBeanNode;
			String classname = pBean.getAttribute("classname");
			Element dBean = (Element) xpath.selectNode(beans, "*[@classname='" + classname + "']");
			
			for (Node dAttr: xpath.selectList(dBean, "@*")) {
				String name = dAttr.getNodeName(); 
				if (!name.equals("classname")) {
					if (!pBean.hasAttribute(name)) {
						pBean.setAttribute(name, dAttr.getNodeValue());
					}
				}
			}
			
			for (Node dPropNode: xpath.selectList(dBean, "property[@name]")) {
				Element dProp = (Element) dPropNode;
				String name = dProp.getAttribute("name");
				if (null == xpath.selectNode(pBean, "property[@name='" + name + "']")) {
					pBean.appendChild(element.getOwnerDocument().importNode(dProp, true));
				}
			}
			
			unshrinkChildren(beans, pBean);
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
			nodes.addAll(xpath.selectList(documentBeansXmlDatabase, "//bean[@enable='true']/@classname"));
			
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
				def.removeAttribute("newPriority");
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
