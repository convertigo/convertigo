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

package com.twinsoft.convertigo.engine.enums;

public enum Accessibility {
	Public(0),
	Hidden(1),
	Private(2);
	
	int code;
	
	Accessibility(int code) {
		this.code = code;
	}
	
	public int code() {
		return code;
	}
	
	public static Accessibility valueOf(int code) {
		switch (code) {
		case 0 : return Public;
		case 1 : return Hidden;
		default : return Private;
		}
	}
}
