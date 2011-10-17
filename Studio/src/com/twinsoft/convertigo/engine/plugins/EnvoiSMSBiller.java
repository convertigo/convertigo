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

package com.twinsoft.convertigo.engine.plugins;

import java.io.IOException;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.plugins.LCABiller;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * 
 * @author GUMO
 *
 */
public class EnvoiSMSBiller extends LCABiller {

	/**
	 * 
	 * @throws IOException
	 */
	public EnvoiSMSBiller() throws IOException {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @Override
	 */
	protected boolean isBillable(Context context) {
		// TODO Auto-generated method stub
		Document doc = context.outputDocument;
		if (Engine.logEngine.isTraceEnabled())
			Engine.logEngine.trace(XMLUtils.prettyPrintDOM(doc));
		
		return true;
	}

	
	
}
