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

public class IsInThenElseStep extends IsInStep implements IThenElseContainer {

	private static final long serialVersionUID = -1043042913971960610L;

	public IsInThenElseStep() {
		super();
	}

	@Override
    public IsInThenElseStep clone() throws CloneNotSupportedException {
    	IsInThenElseStep clonedObject = (IsInThenElseStep) super.clone();
        return clonedObject;
    }

	@Override
    public IsInThenElseStep copy() throws CloneNotSupportedException {
    	IsInThenElseStep copiedObject = (IsInThenElseStep) super.copy();
        return copiedObject;
    }

	@Override
	public boolean hasThenElseSteps() {
		return true;
	}
}
