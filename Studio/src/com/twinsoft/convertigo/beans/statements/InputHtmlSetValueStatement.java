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

package com.twinsoft.convertigo.beans.statements;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.InputValueEvent;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class InputHtmlSetValueStatement extends AbstractComplexeEventStatement {
	private static final long serialVersionUID = 5711783044269121254L;
	
	private String expression = "//todo";
	
	public InputHtmlSetValueStatement(){
		super();
	}
	
	public InputHtmlSetValueStatement(String xpath, String expression){
		super(xpath);
		this.expression = expression;
	}

	public String toString(){
		return "set value of " + expression + super.toString();
	}
	
	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) {
		try {
			evaluate(javascriptContext, scope, expression, "expression", true);
		} catch (EngineException e) {
			//TODO:
		}
		
		String value = "";
		if (evaluated != null) {
			if (evaluated instanceof NativeJavaArray) {
				Object object = ((NativeJavaArray)evaluated).unwrap();
				List<Object> list = Arrays.asList((Object[])object);
				for (int j=0; j<list.size(); j++) {
					Object item = list.get(j);
					value += item.toString() + ",";
				}
			} else if (evaluated instanceof NativeJavaObject) {
				NativeJavaObject nativeJavaObject = (NativeJavaObject)evaluated;
				Object javaObject = nativeJavaObject.unwrap();
				if (javaObject instanceof Vector) {
					Vector<String> v = GenericUtils.cast(javaObject);
					for (int j=0; j<v.size(); j++) {
						value += v.get(j) + ",";
					}
				} else {
					value = (String)nativeJavaObject.getDefaultValue(String.class);
				}
				
			} else if (evaluated instanceof NativeArray) {
				NativeArray array = (NativeArray)evaluated;
				for (int j=0; j<array.getLength(); j++) {
					Object item = array.get(j,array);
					value += item.toString() + ",";
				}
			} else if (evaluated instanceof Vector) {
				Vector<String> v = GenericUtils.cast(evaluated);
				for (int j=0; j<v.size(); j++) {
					value += v.get(j) + ",";
				}
			} else {
				value = evaluated.toString();
			}
		}
		
		return new InputValueEvent(xpath, uiEvent, value);
	}
	
	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	public String toJsString() {
		return expression;
	}
}
