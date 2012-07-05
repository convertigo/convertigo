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

package com.twinsoft.convertigo.beans.statements;

import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GetTextStatement extends GetNodesStatement {

	private static final long serialVersionUID = 6413317003878053141L;

	public GetTextStatement() {
		super();
		variableName = "nodeText";
	}
	
	@Override
	protected void addToScope(Scriptable scope, NodeList nodeList) {
		if (nodeList != null) {
			String nodeValue = null;
			if (nodeList.getLength() > 0) {
				Node node = nodeList.item(0);
				nodeValue = node.getNodeValue();
				if (node instanceof Element)
					nodeValue = ((Element) node).getTextContent();
			}
			scope.put(variableName, scope, nodeValue);
		}
	}
}