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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;

import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class ElseStep extends StepWithExpressions {

	private static final long serialVersionUID = 1041122987773796568L;

	public ElseStep() {
		super();
	}

    public ElseStep clone() throws CloneNotSupportedException {
    	ElseStep clonedObject = (ElseStep) super.clone();
        return clonedObject;
    }

	@Override
	public ElseStep copy() throws CloneNotSupportedException {
		ElseStep copiedObject = (ElseStep)super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		return "Else";
	}
	
	@Override
	public XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		return getXmlSchemaParticle(collection, schema, XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence()));
	}
	
	@Override
	public boolean isGenerateSchema() {
		return true;
	}
}
