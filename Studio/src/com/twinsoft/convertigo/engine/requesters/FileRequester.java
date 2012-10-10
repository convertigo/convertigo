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

package com.twinsoft.convertigo.engine.requesters;

import java.io.UnsupportedEncodingException;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.translators.NullTranslator;
import com.twinsoft.convertigo.engine.translators.Translator;

public class FileRequester extends GenericRequester {
	
    public FileRequester() {
    }
    
    public String getName() {
        return "FileRequester";
    }
    
	public Context getContext() throws Exception {
		// TODO: implémenter
		throw new IllegalArgumentException();
//		return Engine.theApp.contextManager.get(...);
	}

	public void initContext(Context context) throws Exception {
		super.initContext(context);
	}

	public Translator getTranslator() {
		return new NullTranslator();
	}

	public void preGetDocument() {
		context.isXsltRequest = true;
	}

    public void setStyleSheet(Document document) {
        // Nothing to do
    }

	protected void initInternalVariables() throws EngineException {
		// Do nothing
	}
	
	protected Object addStatisticsAsData(Object result) {
		return result;
	}
	
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{
		return result;
	}
}
