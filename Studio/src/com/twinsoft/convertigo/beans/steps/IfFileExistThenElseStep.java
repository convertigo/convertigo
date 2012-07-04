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
 * $URL: http://sourceus/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/IfThenElseStep.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.beans.steps;

public class IfFileExistThenElseStep extends IfFileExistStep implements IThenElseContainer {

	private static final long serialVersionUID = 8261334001222123869L;

	public IfFileExistThenElseStep() {
		super();
	}

	public IfFileExistThenElseStep(String condition) {
		super(condition);
	}

    @Override
    public IfFileExistThenElseStep clone() throws CloneNotSupportedException {
    	IfFileExistThenElseStep clonedObject = (IfFileExistThenElseStep) super.clone();
        return clonedObject;
    }

    @Override
    public IfFileExistThenElseStep copy() throws CloneNotSupportedException {
    	IfFileExistThenElseStep copiedObject = (IfFileExistThenElseStep) super.copy();
        return copiedObject;
    }
	
    @Override
	public boolean hasThenElseSteps() {
		return true;
	}
}
