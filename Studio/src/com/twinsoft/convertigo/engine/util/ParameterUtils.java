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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.engine.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ParameterUtils {

	public static String toString(Object ob) {
		String parameterToString;
		if (ob != null) {
			if (ob instanceof NativeJavaObject) {
				NativeJavaObject nativeJavaObject = (NativeJavaObject) ob;
				parameterToString = toString(nativeJavaObject.unwrap());
			} else if (ob instanceof NativeJavaArray || ob instanceof NativeArray || ob.getClass().isArray() || ob instanceof Collection<?>) {
				parameterToString = toStringList(ob).toString();
			} else if (ob instanceof NodeList) {
				parameterToString = "";
				NodeList nl = (NodeList) ob;
				for (int i = 0; i < nl.getLength(); i++) {
					parameterToString += nodeToString(nl.item(i));
				}
			} else if (ob instanceof Node) {
				parameterToString = nodeToString((Node)ob);
			} else {
				parameterToString = ob.toString();
			}
		} else {
			parameterToString = null;
		}
		return parameterToString;
	}
	
	public static List<String> toStringList(Object ob) {
		List<String> list;
		if (ob != null) {
			if (ob instanceof NativeJavaObject) {
				NativeJavaObject nativeJavaObject = (NativeJavaObject) ob;
				list = toStringList(nativeJavaObject.unwrap());
			} else if (ob instanceof NativeJavaArray) {
				Object object = ((NativeJavaArray) ob).unwrap();
				list = toStringList(object);
			} else if (ob.getClass().isArray()) {
				list = toStringList(Arrays.asList((Object[]) ob));
			} else if (ob instanceof NativeArray) {
				NativeArray array = (NativeArray) ob;
				list = new ArrayList<String>((int) array.getLength());
				for (java.util.Iterator<?> i = array.iterator(); i.hasNext();) {
					list.add(toString(i.next()));
				}
			} else if (ob instanceof Collection<?>) {
				Collection<?> collection = GenericUtils.cast(ob);
				list = new ArrayList<String>(collection.size());
				for (Object o : collection) {
					list.add(toString(o));
				}
			} else {
				list = Arrays.asList(toString(ob));
			}
		} else {
			list = Collections.emptyList();
		}
		return list;
	}
	
	private static String nodeToString(Node node) {
		int nodeType = node.getNodeType();
		switch (nodeType) {
			case Node.DOCUMENT_NODE:
				return XMLUtils.prettyPrintElement(((Document) node).getDocumentElement(), true, false);
			case Node.ELEMENT_NODE:
				return XMLUtils.prettyPrintElement((Element) node, true, false);
			case Node.CDATA_SECTION_NODE:
			case Node.TEXT_NODE:
				int len = node.getChildNodes().getLength();
				return ((len<2) ? node.getNodeValue():XMLUtils.getNormalizedText(node));
			case Node.ATTRIBUTE_NODE:
				return node.getNodeValue();
			default:
				return null;
		}
	}
	
/*
	private static void testCollections() {
		List<String> l1 = new ArrayList<String>();
		l1.add("1");l1.add("2");l1.add("3");l1.add("4");
		System.out.println("\nlist of String : "+ toString(l1));
		
		java.util.Vector<String> v1 = new java.util.Vector<String>();
		v1.add("i1");v1.add("i2");v1.add("i3");
		System.out.println("\nvector of String : "+ toString(v1));
		
		List<Object> l2 = new ArrayList<Object>();
		l2.add(l1);l2.add(v1);
		System.out.println("\nlist of Object : "+ toString(l2));
	}
	
	private static void testArrays() {
		String[] ar1 = new String[]{"a","b","c"};
		System.out.println("\nvector of String : "+ toString(ar1));

		Object[] ar2 = new Object[]{ar1,ar1};
		System.out.println("\nvector of Object : "+ toString(ar2));
	}
	
	private static void testDom() {
		try {
			javax.xml.parsers.DocumentBuilderFactory documentBuilderFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			javax.xml.parsers.DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			
			org.w3c.dom.Document doc = documentBuilder.newDocument();
			org.w3c.dom.Element root = doc.createElement("root");
			org.w3c.dom.Element child = doc.createElement("child");
			child.setAttribute("attr", "attr");
			child.appendChild(doc.createTextNode("text"));
			root.appendChild(child);
			doc.appendChild(root);
			System.out.println("\ndom Document : "+ toString(doc));
			System.out.println("\ndom NodeList : "+ toString(doc.getElementsByTagName("child")));
			System.out.println("\ndom Text : "+ toString(child.getTextContent()));
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		testCollections();
		testArrays();
		testDom();
	}
*/
}
