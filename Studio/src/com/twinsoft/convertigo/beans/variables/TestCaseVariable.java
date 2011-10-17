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

package com.twinsoft.convertigo.beans.variables;


import com.twinsoft.convertigo.beans.core.Variable;

public class TestCaseVariable extends Variable {

	private static final long serialVersionUID = -2603119914647543481L;

	public TestCaseVariable() {
		super();
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		TestCaseVariable clonedObject = (TestCaseVariable)super.clone();
		return clonedObject;
	}
	
  

}
