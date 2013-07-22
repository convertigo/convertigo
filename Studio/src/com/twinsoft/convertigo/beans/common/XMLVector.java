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

package com.twinsoft.convertigo.beans.common;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class XMLVector<E> extends Vector<E> implements XMLizable, List<E> {

	private static final long serialVersionUID = 8052622891480541738L;

	private static final Class<?>[] classList = new Class<?>[]{
		Byte.class, Short.class, Integer.class, Long.class, Float.class,
		Double.class, Character.class, Boolean.class, String.class
	};
	final static Comparator<Class<?>> comparator = new Comparator<Class<?>>(){
		public int compare(Class<?> o1, Class<?> o2) {
			return o1.toString().compareTo(o2.toString());
		}
	};
	static{ Arrays.sort(classList, comparator); }
	
	public XMLVector() {
		super();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public XMLVector(XMLVector<E> v) {
    	for(E e : v){
    		if(Arrays.binarySearch(classList, e.getClass(), comparator)!=-1) add(e);
    		else if(e instanceof XMLVector)
    			add((E) new XMLVector((XMLVector) e));
    		else try {
					Method method = e.getClass().getMethod("clone");
					add((E) method.invoke(e));
			} catch(Exception ex) {
					Engine.logBeans.error("Unable to clone object; objectClass='" + e.getClass().getName() + "'", ex);
			}
    	}
    }
    
    public Node writeXml(Document document) throws Exception {
        Element element = document.createElement(getClass().getName());
        
        for(E object : this){
        	Node objectNode = XMLUtils.writeObjectToXml(document, object);
        	element.appendChild(objectNode);
        }
        
        return element;
    }

    @SuppressWarnings("unchecked")
	public void readXml(Node node) throws Exception {
        NodeList nl = node.getChildNodes();
        int len = nl.getLength();
        for (int i = 0 ; i < len ; i++) {
            Node objectNode = nl.item(i);
            if (objectNode.getNodeType() == Node.ELEMENT_NODE) {
                Object object = XMLUtils.readObjectFromXml((Element) objectNode);
                add((E) object);
            }
        }
    }
}
