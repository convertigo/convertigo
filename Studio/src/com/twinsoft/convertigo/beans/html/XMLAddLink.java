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

package com.twinsoft.convertigo.beans.html;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.RequestableObject.RequestableThread;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

/**
 * 
 * This class simply removes nodes from ouputDom.
 *
 */
public class XMLAddLink extends XMLAddText {

	private static final long serialVersionUID = 6333551567580185077L;

	protected String href = null;
	
	protected boolean targetBlank;
	
	public XMLAddLink() {
		super();
		text = "click here";
		href = "\"#\"";
		targetBlank = false;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
	
	public boolean isTargetBlank() {
		return targetBlank;
	}

	public void setTargetBlank(boolean targetBlank) {
		this.targetBlank = targetBlank;
	}

	protected Element generateContent(Document dom) {
		// A element generation
		Element aElem = null;
		aElem = dom.createElement("A");
		
		// add of href attribute
		try {
			org.mozilla.javascript.Context javascriptContext = ((RequestableThread) Thread.currentThread()).javascriptContext;
			evaluate(javascriptContext, context.requestedObject.scope, getHref(), "href", false);
		} catch (EngineException e) {
			Engine.logBeans.debug("XMLAddLink : Exception when evaluating href property '" + getHref() + "' in javascript scope.");
		}
		if (evaluated != null)
			aElem.setAttribute("href", evaluated.toString());
		
		// add of target="blank" attribute if wanted
		if (isTargetBlank())
			aElem.setAttribute("target", "blank");
				
		// set text/image link content
		setLinkContent(aElem, dom);
		
		return aElem;
	}
	
	protected void setLinkContent(Element parentElem, Document dom) {
		// add of link text content
		parentElem.setTextContent(text);
	}
	
}
