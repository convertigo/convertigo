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

package com.twinsoft.convertigo.beans.steps;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.EngineException;

public class XMLCountStep extends XMLElementStep {

	private static final long serialVersionUID = 3930191126183020616L;

	public XMLCountStep() {
		super();
	}

	@Override
    public XMLCountStep clone() throws CloneNotSupportedException {
    	XMLCountStep clonedObject = (XMLCountStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLCountStep copy() throws CloneNotSupportedException {
    	XMLCountStep copiedObject = (XMLCountStep) super.copy();
        return copiedObject;
    }

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		NodeList list = getContextValues();
		if (list != null) {
			int count = list.getLength();
			stepNode.appendChild(doc.createTextNode(String.valueOf(count)));
		}
		else {
			stepNode.appendChild(doc.createTextNode("0"));
		}
	}
}
