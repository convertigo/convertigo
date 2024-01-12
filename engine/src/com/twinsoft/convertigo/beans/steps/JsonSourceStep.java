/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.beans.steps;

import org.codehaus.jettison.json.JSONException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class JsonSourceStep extends SourceStep {

	private static final long serialVersionUID = 3619512415195665643L;
	
	public JsonSourceStep() {
		super();
		setVariableName("myVariable");
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (super.stepExecute(javascriptContext, scope)) {
				String variableName = getVariableName();
				NodeList list = (NodeList) scope.get(variableName, scope);
				if (list.getLength() == 0) {
					scope.delete(variableName);
					return true;
				}
				boolean isArray = list.getLength() > 1;
				String string = isArray ? "[" : "";
				for (int i = 0; i < list.getLength();) {
					Node node = list.item(i);
					if (node instanceof Element) {
						try {
							string += XMLUtils.XmlToJson((Element) node, true, true, null);
						} catch (JSONException e) {
							string += node.getNodeValue();
						}
					} else {
						string += node.getNodeValue();
					}
					if (++i < list.getLength()) {
						string += ", ";
					}
				}
				if (isArray) {
					string += "]";
				}
				Object obj = RhinoUtils.jsonParse(string);

				scope.put(variableName, scope, obj);
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected String defaultBeanName(String displayName) {
		return "myVariable";
	}
}
