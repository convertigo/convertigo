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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

/**
 * 
 * This class simply extract text nodes from single node.
 *
 */
public abstract class AbstractXMLReferer extends HtmlExtractionRule {
	private static final long serialVersionUID = -6289517723754312205L;

	protected boolean displayReferer = false;
	
	private transient String referer = null;
	
	
	public AbstractXMLReferer() {
		super();
	}

	@Override
	public boolean apply(Document xmlDom, Context context) {
		// handle referer value retrieve
		referer = null;
		if(displayReferer){
			try {
				try{
					if(xpathApi!=null || (xpathApi=context.getXpathApi())!=null){
						NodeList nl = xpathApi.selectNodeList(xmlDom, "(("+xpath+")/ancestor::IFRAME|("+xpath+")/ancestor::FRAME)[last()]");
						if(nl.getLength()>0) referer = ((Element)nl.item(0)).getAttribute("src");
					}
				}catch (Exception e) {
					Engine.logBeans.trace("(AbstractXMLReferer) Failed to retrieve referer in DOM : "+e.getMessage());
				}
				if(referer == null) referer = getReferer(context);
			} catch (Exception e) {
				Engine.logBeans.error("Exception while getting referer value.", e);
			}
		}
		return super.apply(xmlDom, context);
	}
	
	public boolean isDisplayReferer() {
		return displayReferer;
	}

	public void setDisplayReferer(boolean displayReferer) {
		this.displayReferer = displayReferer;
	}
	
	public void addReferer(Element parentNode) {
		if (displayReferer) parentNode.setAttribute("referer", referer==null?"":referer);
	}
}
