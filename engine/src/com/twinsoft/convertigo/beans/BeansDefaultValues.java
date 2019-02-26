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

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class BeansDefaultValues {
	
	private static final String XMLPATH = "/com/twinsoft/convertigo/beans/database_objects_default.xml";
	private static final String JSONPATH = "/com/twinsoft/convertigo/beans/mobile/components/dynamic/ion_objects_default.json";
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
			String dData = ((CDATASection) dElt.getFirstChild()).getData().replace('\r', '\n');
			if (pElt.getFirstChild() instanceof CDATASection) {
				String pData = ((CDATASection) pElt.getFirstChild()).getData();
				if (!dData.equals(pData)) {
					return false;
				}
			} else if (pElt.getFirstChild() == null && !dData.isEmpty()) {
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
	
	static private class ShrinkProject {
		TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
		Element beans;
		JSONObject ionObjects;
		String nVersion = VersionUtils.normalizeVersionString(ProductVersion.productVersion);
		
		ShrinkProject() throws Exception {
			Document beansDoc;
			try (InputStream is = BeansDefaultValues.class.getResourceAsStream(XMLPATH)) {
				beansDoc = XMLUtils.getDefaultDocumentBuilder().parse(is);
			}
			try (InputStream is = BeansDefaultValues.class.getResourceAsStream(JSONPATH)) {
				ionObjects = new JSONObject(IOUtils.toString(is, "UTF-8"));
			}
			beans = beansDoc.getDocumentElement();
		}
		
		private void shrinkChildren(Element element, Element copy) {
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
				
				Element dBean = getBeanForVersion(xpath, beans, classname, nVersion);
				
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
							
							if (name.equals("beanData")) {
								try {
									String beanData = nProp.getTextContent();
									JSONObject ion = new JSONObject(beanData);
									String ionName = (String) ion.remove("name");
									ion.put("ionBean", ionName);
									JSONObject dIonProps = ionObjects.getJSONObject(ionName);
									String lVersion = (String) dIonProps.keys().next();
									dIonProps = dIonProps.getJSONObject(lVersion).getJSONObject("properties");
									JSONObject ionProps = (JSONObject) ion.remove("properties");
									for (Iterator<?> i = ionProps.keys(); i.hasNext();) {
										String keyProp = (String) i.next();
										if (dIonProps.has(keyProp)) {
											JSONObject dIonProp = dIonProps.getJSONObject(keyProp);
											JSONObject ionProp = ionProps.getJSONObject(keyProp);
											if (!dIonProp.equals(ionProp)) {
												ion.put(keyProp, ionProp.getString("mode") + ":" + ionProp.getString("value"));
											}
										}
									}
									if (ion.length() > 2) {
										nProp.setTextContent(ion.toString(1));
									} else {
										nProp.setTextContent(ion.toString());
									}
								} catch (Exception e) {
								}
							}
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
				
				shrinkChildren(pBean, nCopy);
			}
		}
		
		public Document shrinkProject(Document project) throws Exception {
			Element eProject = project.getDocumentElement();
			
			Document nProjectDoc = XMLUtils.createDom();
			Element nProject = (Element) nProjectDoc.appendChild(nProjectDoc.createElement("root"));
			
			Element eAttr = (Element) nProject.appendChild(nProjectDoc.createElement("bean"));
			eAttr.setAttribute("yaml_attr", "convertigo");
			eAttr.setTextContent(eProject.getAttribute("beans"));
			
			shrinkChildren(project.getDocumentElement(), nProject);
			
			return nProjectDoc;
		}
	}
	
	public static Document shrinkProject(Document project) throws Exception {
		return new ShrinkProject().shrinkProject(project);
	}
	
	static private class UnshrinkProject {
		TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
		Element beans;
		JSONObject ionObjects;
		String version;
		String nVersion;
		
		
		UnshrinkProject() throws Exception {
			Document beansDoc;
			try (InputStream is = BeansDefaultValues.class.getResourceAsStream(XMLPATH)) {
				beansDoc = XMLUtils.getDefaultDocumentBuilder().parse(is);
			}
			try (InputStream is = BeansDefaultValues.class.getResourceAsStream(JSONPATH)) {
				ionObjects = new JSONObject(IOUtils.toString(is, "UTF-8"));
			}
			beans = beansDoc.getDocumentElement();
		}
		
		Document unshrinkProject(Document project) throws Exception {
			Document nProjectDoc = XMLUtils.createDom();
			Element eProject = project.getDocumentElement();
			
			String beansVersion = eProject.getAttribute(eProject.hasAttribute("beans") ? "beans" : "convertigo");
			version = beansVersion.substring(0, beansVersion.lastIndexOf("."));
			nVersion = VersionUtils.normalizeVersionString(version);
			
			Element nProject = (Element) nProjectDoc.appendChild(nProjectDoc.importNode(eProject, false));
			
			unshrinkChildren(eProject, nProject);
			
			nProject.setAttribute("beans", beansVersion);
			nProject.setAttribute("engine", version);
			nProject.setAttribute("studio", version);
			nProject.setAttribute("version", version);
			nProject.removeAttribute("convertigo");
			
			return nProjectDoc;
		}
		
		void unshrinkChildren(Element element, Element nParent) {
			for (Node pBeanNode: xpath.selectList(element, "bean[@yaml_key]")) {
				Element pBean = (Element) pBeanNode;
				
				Matcher matcherBeanName = patternBeanName.matcher(pBean.getAttribute("yaml_key"));
				
				matcherBeanName.matches();

				String pName = matcherBeanName.group(1);
				String classname = "com.twinsoft.convertigo.beans." + matcherBeanName.group(2);
				String pPriority = matcherBeanName.group(3);
				
				Element dBean = getBeanForVersion(xpath, beans, classname, nVersion);
				
				Element nBean = null;
				try {
					nBean = (Element) nParent.getOwnerDocument().importNode(dBean, true);
					nBean.removeAttribute("version");
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
					String propName = pPropNode.getNodeName();
					Element nProp = (Element) xpath.selectNode(nBean, "property[@name='" + propName + "']");
					if (nProp == null) {
						Element nOther = (Element) xpath.selectNode(nBean, propName);
						if (nOther != null) {
							nBean.replaceChild(nBean.getOwnerDocument().importNode(pPropNode, true), nOther);
							continue;	
						} else {
							nProp = (Element) nBean.appendChild(nBean.getOwnerDocument().createElement("property"));
						}
					}
					nProp.setAttribute("name", propName);
					
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
						
						if (propName.equals("beanData")) {
							try {
								JSONObject ion = new JSONObject(value);
								String ionName = (String) ion.remove("ionBean");
								JSONObject dIonProps = ionObjects.getJSONObject(ionName);
								
								String dVersion;
								Iterator<?> iProp = dIonProps.keys();
								while ((dVersion = iProp.next().toString()).compareTo(nVersion) > 0);
								dIonProps = dIonProps.getJSONObject(dVersion).getJSONObject("properties");
								
								JSONObject ionProps = new JSONObject();
								ion.put("properties", ionProps);
								
								for (iProp = dIonProps.keys(); iProp.hasNext();) {
									String keyProp = (String) iProp.next();
									if (!ion.has(keyProp)) {
										ionProps.put(keyProp, dIonProps.getJSONObject(keyProp));
									} else {
										String[] smart = ((String) ion.remove(keyProp)).split(":", 2);
										JSONObject v = new JSONObject();
										v.put("mode", smart[0]);
										v.put("value", smart[1]);
										ionProps.put(keyProp, v);
									}
									ionProps.getJSONObject(keyProp).put("name", keyProp);
								}
								ion.put("name", ionName);
								value = ion.toString();
							} catch (Exception e) {
							}
						}
						
						nValue.setAttribute("value", value);
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
					
					if (nProp.hasAttribute("isNull")) {
						nProp.setAttribute("isNull", "false");
					}
				}
				
				unshrinkChildren(pBean, nBean);
			}
		}
	}
	
	public static Document unshrinkProject(Document project) throws Exception {
		return new UnshrinkProject().unshrinkProject(project);
	}
	
	private static Element getBeanForVersion(TwsCachedXPathAPI xpath, Element beans, String classname, String version) {
		for (Node n : xpath.selectList(beans, "*[@classname='" + classname + "']")) {
			Element e = (Element) n;
			String eVersion = e.getAttribute("version");
			if (eVersion.compareTo(version) <= 0) {
				return e;
			}
		}
		return null;
	}
	
	private static Document updateBeansDefaultValues(Document document) throws Exception {
		try (InputStream dbInputstream = BeansDefaultValues.class.getResourceAsStream(
				"/com/twinsoft/convertigo/beans/database_objects.xml")) {
			Document documentBeansXmlDatabase = XMLUtils.getDefaultDocumentBuilder().parse(dbInputstream);
			TwsCachedXPathAPI xpath = new TwsCachedXPathAPI();
			EnginePropertiesManager.initProperties();
			
			SortedSet<Node> nodes = new TreeSet<Node>((n1, n2) -> {
				return n1.getNodeValue().compareTo(n2.getNodeValue());
			});
			nodes.addAll(xpath.selectList(documentBeansXmlDatabase, "//bean/@classname"));
			
			Element beans = document.getDocumentElement();
			if (beans == null) {
				beans = document.createElement("beans");
				document.appendChild(beans);
			}
			
			String nVersion = VersionUtils.normalizeVersionString(ProductVersion.productVersion);
			
			for (Node node: nodes) {
				String classname = node.getNodeValue();
				DatabaseObject dbo = (DatabaseObject) Class.forName(classname).newInstance();
				Element def = dbo.toXml(document);
				if (def.hasAttribute("priority")) {
					def.setAttribute("priority", "0");
				}
				Element eBean = getBeanForVersion(xpath, beans, classname, nVersion);
				if (eBean != null) {
					String eVersion = eBean.getAttribute("version");
					eBean.removeAttribute("version");
					if (!checkIsSame(def, eBean)) {
						if (eVersion.equals(nVersion)) {
							beans.removeChild(eBean);
						}
						def.setAttribute("version", nVersion);
						beans.insertBefore(def, beans.getFirstChild());
					}
					eBean.setAttribute("version", eVersion);
				} else {
					def.setAttribute("version", nVersion);
					beans.insertBefore(def, beans.getFirstChild());
				}
			};
			
			return document;
		}
	}
	
	private static void updateBeansDefaultValues(File output) throws Exception {
		Document doc;
		try {
			doc = XMLUtils.getDefaultDocumentBuilder().parse(output);
		} catch (Exception e) {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		}
		FileUtils.write(output, XMLUtils.prettyPrintDOMWithEncoding(updateBeansDefaultValues(doc), "UTF-8"), "UTF-8");
	}
	
	private static void updateIonBeansDefaultValues(File output) throws Exception {
		String nVersion = VersionUtils.normalizeVersionString(ProductVersion.productVersion);
		JSONObject jObject;
		try {
			jObject = new JSONObject(FileUtils.readFileToString(output, "UTF-8"));
		} catch (Exception e) {
			jObject = new JSONObject();
		}
		
		for (Entry<String, IonBean> entry : ComponentManager.getIonBeans().entrySet()) {
			String key = entry.getKey();
			JSONObject beans;
			if (jObject.has(key)) {
				beans = jObject.getJSONObject(key);
			} else {
				jObject.put(key, beans = new JSONObject());
			}
			Iterator<?> i = beans.keys();
			String lastKey = null;
			while (i.hasNext()) {
				lastKey = (String) i.next();
			}
			JSONObject dBean = new JSONObject(entry.getValue().toBeanData());
			JSONObject lBean = lastKey != null ? beans.getJSONObject(lastKey) : null;
			if (!dBean.equals(lBean)) {
				beans.put(nVersion, dBean);
			}
		}
		String content = jObject.toString(1);
		if (System.lineSeparator().equals("\r\n")) {
			content = content.replace("\n", "\r\n");
		}
		FileUtils.write(output, content, "UTF-8");
	}

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			File output = new File(args[0]);
			if (!output.exists() || !output.isDirectory()) {
				System.err.println("Base dir " + output.getCanonicalPath() + " doesn't exists nor a directory.");
			}

			File xOutput = new File(output, XMLPATH);
			File jOutput = new File(output, JSONPATH);	
			
			Engine.logBeans = Logger.getRootLogger();
			
			updateBeansDefaultValues(xOutput);
			updateIonBeansDefaultValues(jOutput);
			
			System.out.println("Beans default values updated in: " + output.getCanonicalPath());
			for (int i = 1; i < args.length; i++) {
				File copy = new File(args[1]);
				if (copy.exists() && copy.isDirectory()) {
					File fCopy = new File(copy, XMLPATH);
					FileUtils.copyFile(xOutput, fCopy);
					System.out.println("Beans default values updated in: " + fCopy.getCanonicalPath());
					fCopy = new File(copy, JSONPATH);
					FileUtils.copyFile(jOutput, fCopy);
					System.out.println("Ion Beans default values updated in: " + fCopy.getCanonicalPath());
				}
			}
		}
	}

}
