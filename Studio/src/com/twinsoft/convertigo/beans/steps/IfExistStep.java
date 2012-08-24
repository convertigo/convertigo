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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.EngineException;

public class IfExistStep extends TestStep {

	private static final long serialVersionUID = 1615428447261700976L;

	transient protected XMLVector<XMLVector<String>> testDefinition = new XMLVector<XMLVector<String>>();
	
	public IfExistStep() {
		super();
	}

	@Override
    public IfExistStep clone() throws CloneNotSupportedException {
    	IfExistStep clonedObject = (IfExistStep) super.clone();
        return clonedObject;
    }

	@Override
	public IfExistStep copy() throws CloneNotSupportedException {
		IfExistStep copiedObject = (IfExistStep)super.copy();
		return copiedObject;
	}
	
	protected boolean executeTest(Context javascriptContext, Scriptable scope) throws EngineException {
		NodeList list = getSource().getContextValues();
		if (list != null) {
			return list.getLength() > 0;
		}
		return false;
	}

}
