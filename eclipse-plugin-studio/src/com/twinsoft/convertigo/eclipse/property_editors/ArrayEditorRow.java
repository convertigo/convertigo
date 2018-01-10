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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.lang.reflect.Constructor;

public class ArrayEditorRow {

	private Object line = null;
	
	public ArrayEditorRow(Object line) {
		super();
		this.line = clone(line);
	}

	protected Object clone(Object line) {
		Object value = null;
		Class<?> objectClass = line.getClass();
		try {
			Constructor<?> constructor = objectClass.getConstructor(new Class[]{String.class});
			value = constructor.newInstance(new Object[]{line.toString()});
		} catch (Exception e) {
			value = new String(line.toString());
		}
		return value;
	}
	
	public Object cloneValue() {
		return clone(line);
	}
	
	public Object getValue(int columnIndex) {
		return (columnIndex < 1)?line:null;
	}

	public void setValue(Object value, int columnIndex) {
		if (columnIndex < 1)line = value;
	}

	public Object getLine() {
		return line;
	}
}
