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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.InputSelectEvent;

public class InputHtmlSetSelectedStatement extends AbstractComplexeEventStatement implements ITagsProperty{
	private static final long serialVersionUID = 1774237547889998277L;
	
	private String mode = InputSelectEvent.MOD_INDEX;
	private String expression = "//todo";
	
	public InputHtmlSetSelectedStatement() {
		super();
	}
	
	public InputHtmlSetSelectedStatement(String xpath) {
		super(xpath);
	}

	public InputHtmlSetSelectedStatement(String xpath, String expression, String mode) {
		super(xpath);
		this.expression = expression;
		this.mode = mode;
	}
	
	@Override
	public String toString() {
		return "select " + expression + super.toString();
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	@Override
	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) throws EngineException {
		evaluate(javascriptContext, scope, expression, "expression", true);
		String [] values = null;		
		if (evaluated != null){
			if (evaluated instanceof NativeJavaArray) {
				Object object = ((NativeJavaArray)evaluated).unwrap();
				List<Object> list = Arrays.asList((Object[])object);
				values = new String[(int) list.size()];
				for (int i = 0; i < values.length; i++) {
					Object item = list.get(i);
					values[i] = convertValue(item);
				}
			}
			else if (evaluated instanceof NativeArray) {
				NativeArray nativeArray = (NativeArray) evaluated;
				values = new String[(int) nativeArray.getLength()];
				for (int i = 0; i < values.length; i++) {
					Object item = nativeArray.get(i, scope);
					values[i] = convertValue(item);
				}
			} else {
				values = new String[] {
					convertValue(evaluated)
				};
			}
		}
		return new InputSelectEvent(xpath, uiEvent, mode, values);
	}
	
	protected String convertValue(Object obj) {
		if (mode.equals(InputSelectEvent.MOD_INDEX)) {
			if (obj instanceof Double) {
				return "" + ((Double) obj).intValue();
			} else if(obj instanceof Float) {
				return "" + ((Float) obj).intValue();
			}
		}
		return obj.toString();
	}
	
	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	@Override
	public String toJsString() {
		return expression;
	}

	public String[] getTagsForProperty(String propertyName){
		if(propertyName.equals("mode")) {
			return new String[] {
					InputSelectEvent.MOD_INDEX,
					InputSelectEvent.MOD_CONTENT,
					InputSelectEvent.MOD_VALUE
			};
		}
		return new String[0];
	}
}