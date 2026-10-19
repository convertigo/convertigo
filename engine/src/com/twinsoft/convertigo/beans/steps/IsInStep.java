/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.json.JsonPath;
import com.twinsoft.convertigo.engine.json.JsonPathSource;

public class IsInStep extends TestStep {

	private static final long serialVersionUID = 2062311729789746229L;

	private XMLVector<XMLVector<String>> testDefinition = new XMLVector<XMLVector<String>>();
	
	public IsInStep() {
		super();
	}

	@Override
    public IsInStep clone() throws CloneNotSupportedException {
    	IsInStep clonedObject = (IsInStep) super.clone();
        return clonedObject;
    }

	@Override
	public IsInStep copy() throws CloneNotSupportedException {
		IsInStep copiedObject = (IsInStep)super.copy();
		return copiedObject;
	}
	
	public XMLVector<XMLVector<String>> getTestDefinition() {
		return testDefinition;
	}

	public void setTestDefinition(XMLVector<XMLVector<String>> testDefinition) {
		this.testDefinition = testDefinition;
	}

	protected boolean executeTest(Context javascriptContext, Scriptable scope) throws EngineException {
		StepSource source = getSource();
		List<String> values = null;
		if (source.isJsonPath()) {
			// a JSON path source: the texts of the values, without XML
			Object value = source.getJsonValue();
			if (value != JsonPath.MISSING) {
				values = new ArrayList<String>();
				for (Object v : JsonPath.parse(source.getXpath()).hasWildcard() ? (List<?>) value : Collections.singletonList(value)) {
					values.add(JsonPathSource.text(v));
				}
			}
		} else {
			NodeList list = source.getContextValues();
			if (list != null) {
				values = new ArrayList<String>(list.getLength());
				for (int i = 0; i < list.getLength(); i++) {
					values.add(getNodeValue(list.item(i)));
				}
			}
		}
		if (values != null) {
			int len = values.size();
			if (len != 0) {
				boolean match = true;
				for (int i=0; i<len; i++) {
					String nodeValue = values.get(i);
					if (nodeValue == null)
						nodeValue = "";
					for (int j=0; j<testDefinition.size(); j++) {
						XMLVector<String> xmlv = testDefinition.get(j);
						String operator = (String)xmlv.get(0);
						String regexp = (String)xmlv.get(1);
						
						Pattern myPattern = Pattern.compile(regexp);
						Matcher myMatcher = myPattern.matcher(nodeValue);
						boolean hasMatched = myMatcher.find();
						
						if ((operator.equals("AND") && hasMatched) || (operator.equals("NOT") && !hasMatched))
							match &= true;
						else if ((operator.equals("AND") && !hasMatched) || (operator.equals("NOT") && hasMatched))
							match &= false;
					}
				}
				return match;
			}
		}
		return false;
	}

}
