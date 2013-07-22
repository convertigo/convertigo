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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.EngineException;

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
		NodeList list = getSource().getContextValues();
		if (list != null) {
			int len = list.getLength();
			if (len != 0) {
				boolean match = true;
				for (int i=0; i<len; i++) {
					String nodeValue = getNodeValue(list.item(i));
					if (nodeValue == null)
						nodeValue = "";
					for (int j=0; j<testDefinition.size(); j++) {
						XMLVector<String> xmlv = testDefinition.elementAt(j);
						String operator = (String)xmlv.elementAt(0);
						String regexp = (String)xmlv.elementAt(1);
						
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
