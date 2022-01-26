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

package com.twinsoft.convertigo.engine;

import java.util.Collections;
import java.util.Set;


public class UndefinedSymbolsException extends Exception {
	private static final long serialVersionUID = -4202689033508498831L;
	
	private Set<String> undefinedSymbols;
	private Object incompletValue;
	
	UndefinedSymbolsException(Set<String> undefinedSymbols, Object incompletValue) {
		this.undefinedSymbols = Collections.unmodifiableSet(undefinedSymbols);
		this.incompletValue = incompletValue;
	}
	
	public Set<String> undefinedSymbols() {
		return undefinedSymbols;
	}
	
	public Object incompletValue() {
		return incompletValue;
	}
}
