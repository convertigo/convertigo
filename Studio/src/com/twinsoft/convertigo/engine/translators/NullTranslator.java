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

package com.twinsoft.convertigo.engine.translators;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;

public class NullTranslator implements Translator {

	public void buildInputDocument(Context context, Object inputData) throws Exception {
    	// Do nothing
    }

	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
		return convertigoResponse;
	}

	public String getContextName(byte[] data) throws Exception {
		throw new EngineException("The NullTranslator translator does not support the getContextName() method");
	}

	public String getProjectName(byte[] data) throws Exception {
		throw new EngineException("The NullTranslator translator does not support the getProjectName() method");
	}

}
