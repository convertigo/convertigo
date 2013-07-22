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

package com.twinsoft.convertigo.eclipse.property_editors.validators;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ICellEditorValidator;

import com.twinsoft.convertigo.engine.Engine;

public class CompilableValueValidator implements ICellEditorValidator {

	private ICellEditorValidator cev = null;
	
	public CompilableValueValidator(ICellEditorValidator cev) {
		this.cev = cev;
	}
	
	public String isValid(Object value) {
		if (isCompilable(value)) {
			try {
				value = getCompiledValue((String)value);
			} catch (Exception e) {
				return escape(e.getMessage());
			}
		}
		
		if (cev == null)
			return null;
		
		return escape(cev.isValid(value));
	}

	private static String escape(String message) {
		if (message == null) return null;
		message = message.replaceAll("'", "''");
		message = message.replaceAll("\\{", "\'\\{");
		return message;
	}
	
	private static boolean isCompilable(Object value) {
		if (value instanceof String) {
			return ((String)value).indexOf("${") != -1;
		}
		return false;
	}
	
	private static Object getCompiledValue(Object value) throws Exception {
		if (value instanceof String) {
			String val = (String)value;
			StringBuffer sb = new StringBuffer(val.length());
			Pattern p = Pattern.compile("(\\$\\{)([^\\{\\}]*)(\\})");
			Matcher m = p.matcher(val);
			while (m.find()) {
				String symbol = m.group(2);
				int idxEqual = symbol.indexOf("=");
				String symbolName = (idxEqual == -1) ? symbol:symbol.substring(0,idxEqual);
				String symbolDefaultValue = (idxEqual == -1) ? null:symbol.substring(idxEqual+1);
				String symbolValue = getSymbolValue(symbolName);
				if (symbolValue == null) {
					if (symbolDefaultValue == null) {
						String cgs = Engine.theApp.databaseObjectsManager.getGlobalSymbolsFilePath();
						if (new File(cgs).exists())
							throw new Exception("Symbol \"" + symbolName + "\" has not been defined in \"" + cgs + "\"!");
						else
							throw new Exception("Symbol \"" + symbolName + "\" has not been found : \"" + cgs + "\" file is missing!");
					}
					else
						symbolValue = symbolDefaultValue;
				}
				m.appendReplacement(sb, symbolValue);
			}
			m.appendTail(sb);
			String s = sb.toString();
			if (isCompilable(s))
				throw new Exception("Invalid symbol syntax in value : \""+ s +"\" !");
			return s;
		}
		return value;
	}
	
	private static String getSymbolValue(String symbolName) {
		return Engine.theApp.databaseObjectsManager.getSymbolValue(symbolName);
	}
}
