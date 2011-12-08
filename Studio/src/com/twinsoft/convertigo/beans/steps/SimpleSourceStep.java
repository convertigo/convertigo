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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.EngineException;

public class SimpleSourceStep extends SourceStep {

	private static final long serialVersionUID = 3615732415195665643L;
	
	public SimpleSourceStep() {
		super();
		variableName = "myVariable";
	}
	
	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				NodeList list = (NodeList) scope.get(variableName, scope);
				
				Object string = null;
				if (list.getLength() > 0) {
					Node node = list.item(0);
					if (node instanceof Element) {
						Element element = (Element) node;
						string = element.getTextContent();
					} else {
						string = node.getNodeValue();
					}
				}
				scope.put(variableName, scope, string);
				return true;
			}
		}
		return false;
	}	
}
