/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

public enum JsonFieldType {
	string,
	number("number"),
	bool("boolean"),
	nul("null");
	
	String toString;
	
	JsonFieldType () {
		toString = name();
	}
	
	JsonFieldType (String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}
	
	public static JsonFieldType parse(String type) {
		if (type != null) {
			type = type.toLowerCase();
			if (type.matches("number|int|integer|float|double")) {
				return number;
			} else if (type.matches("bool|boolean")) {
				return bool;
			} else if (type.equals("null")) {
				return nul;
			}
		}
		return string;
	}
}
