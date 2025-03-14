/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.property_editors.validators;

import org.eclipse.jface.viewers.ICellEditorValidator;

public class NumberValidator implements ICellEditorValidator {

	private Class<?> c;
	
	public NumberValidator(Class<?> c) {
		this.c = c;
	}
	
	public String isValid(Object value) {
		try {
			if (c == Short.class) {
				Short.valueOf(String.valueOf(value));
			}
			else if (c == Byte.class) {
				Byte.valueOf(String.valueOf(value));
			}
			else if (c == Integer.class) {
				Integer.valueOf(String.valueOf(value));
			}
			else if (c == Long.class) {
				Long.valueOf(String.valueOf(value));
			}
			else if (c == Float.class) {
				Float.valueOf(String.valueOf(value));
			}
			else if (c == Double.class) {
				Double.valueOf(String.valueOf(value));
			}
		}
		catch(NumberFormatException e) {
    		return "The value \"" + value + "\" is not a valid "+ c.getSimpleName()+" number!";
		}
		return null;
	}

}
