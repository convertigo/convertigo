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

package com.twinsoft.convertigo.beans.transactions;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.enums.Parameter;

public class ComparableFieldElement implements Comparable<ComparableFieldElement> {

	private Element fieldElement;
	
	public ComparableFieldElement(Element fieldElement) {
		this.fieldElement = fieldElement;
	}

	public int compareTo(ComparableFieldElement otherField) {
		int lineField = getLine(fieldElement);
		int lineOther = getLine(otherField.fieldElement); 
		
		// compare lines
		if (lineField < lineOther)
			return -1;
		
		if (lineField > lineOther)
			return 1;
		
		// lineField == lineOther
		
		// compare columns
		int colField = getColumn(fieldElement);
		int colOther = getColumn(otherField.fieldElement); 
		
		if (colField < colOther)
			return -1;
		
		if (colField > colOther)
			return 1;
		
		return 0;
	}

	public static int getLine(Element fieldElement) throws ClassCastException {
		int line = -1;
		String fieldName = fieldElement.getAttribute("name");
		if (fieldName.indexOf(Parameter.JavelinField.getName()) != 0)
			throw new ClassCastException("[ComparableFieldElement] The object trying to be compared with is not a ComparableFieldElement.");
		String ln = fieldName.substring(fieldName.indexOf("l", 8)+1);
		line = Integer.parseInt(ln);
		return line;
	}
	
	public static int getColumn(Element fieldElement) throws ClassCastException {
		int column = -1;
		String fieldName = fieldElement.getAttribute("name");
		if (fieldName.indexOf(Parameter.JavelinField.getName()) != 0)
			throw new ClassCastException("[ComparableFieldElement] The object trying to be compared with is not a ComparableFieldElement.");
		String col = fieldName.substring(fieldName.indexOf("c")+1, fieldName.indexOf("_", fieldName.indexOf("c")));
		column = Integer.parseInt(col);
		return column;
	}

	public Element getFieldElement() {
		return fieldElement;
	}
}
