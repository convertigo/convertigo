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

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.translators.Translator;

/**
 * This is the interface that must satisfy a requester in order to be
 * used with the Convertigo engine.
 */
public abstract class Requester {
    public Context context;
    
    public Object inputData;
    
    public abstract String getName();
    
    public abstract Context getContext() throws Exception;
    
    public abstract void initContext(Context context) throws Exception;
    
    public abstract Document createDOM(String encodingCharSet) throws EngineException;
    
    public abstract Document parseDOM(String xml) throws EngineException;
        
    public abstract Object processRequest(Object inputData) throws Exception;

    public abstract Translator getTranslator();
    
    public abstract void makeInputDocument() throws Exception;
    
    public abstract Document getDocument() throws Exception;
    
    public abstract void checkAccessibility(RequestableObject requestable) throws EngineException;
}
