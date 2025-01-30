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

package com.twinsoft.convertigo.beans.steps;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;

import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class IfThenElseStep extends IfStep implements IThenElseContainer {

	private static final long serialVersionUID = 8261334001222123869L;

	public IfThenElseStep() {
		super();
	}

	public IfThenElseStep(String condition) {
		super(condition);
	}

    @Override
    public IfThenElseStep clone() throws CloneNotSupportedException {
    	IfThenElseStep clonedObject = (IfThenElseStep) super.clone();
        return clonedObject;
    }

    @Override
    public IfThenElseStep copy() throws CloneNotSupportedException {
    	IfThenElseStep copiedObject = (IfThenElseStep) super.copy();
        return copiedObject;
    }
	
    @Override
	public boolean hasThenElseSteps() {
		return true;
	}

    @Override
    public XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
    	XmlSchemaChoice choice = XmlSchemaUtils.makeDynamic(this, new XmlSchemaChoice());
		return getXmlSchemaParticle(collection, schema, choice);
	}
}
