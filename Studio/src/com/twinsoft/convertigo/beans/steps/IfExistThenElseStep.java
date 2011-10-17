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

package com.twinsoft.convertigo.beans.steps;

public class IfExistThenElseStep extends IfExistStep implements IThenElseContainer {

	private static final long serialVersionUID = 5124025868949574305L;

	public IfExistThenElseStep() {
		super();
	}

    public Object clone() throws CloneNotSupportedException {
    	IfExistThenElseStep clonedObject = (IfExistThenElseStep) super.clone();
        return clonedObject;
    }
	
    public Object copy() throws CloneNotSupportedException {
    	IfExistThenElseStep copiedObject = (IfExistThenElseStep) super.copy();
        return copiedObject;
    }
	
	public boolean hasThenElseSteps() {
		return true;
	}
	
}
