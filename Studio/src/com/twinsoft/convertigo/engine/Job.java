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

package com.twinsoft.convertigo.engine;           

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.cache.CacheManager;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.XMLUtils;

class Job extends Thread {
	protected CacheManager cacheManager;
	protected DatabaseObject requestedObject;
    protected Requester requester;
    protected Context context;
    protected Document document;
    protected boolean isFinished;

    public Job(CacheManager cacheManager, DatabaseObject requestedObject, Requester requester, Context context) {
    	this.cacheManager = cacheManager;
    	this.requestedObject = requestedObject;
        this.requester = requester;
        this.context = context;

        setName("Job #" + context.contextID);
    }

    public void run() {
        setPriority(7);
        
        isFinished = false;
        try {
        	document = cacheManager.getDocument(requester, context);
        	if (Engine.logEngine.isTraceEnabled())
        		Engine.logEngine.trace("Job run: getDocument returned : \n" + XMLUtils.prettyPrintDOM(document));
        }
        catch(EngineException e) {
            document = null;
            String message = "Unable to finish the job for the object \"" + requestedObject.getName() + "\".";
            if (System.getProperty("java.specification.version").compareTo("1.4") >= 0) {
                Throwable eCause = e;
                while ((eCause = eCause.getCause()) != null)  {
                    message += "\n" + eCause.getMessage();
                }
            }
            Engine.logEngine.error(message);
        }
        catch(Exception e) {
            document = null;
            Engine.logEngine.error("Unable to finish the job for the object \"" + requestedObject.getName() + "\".", e);
        }
        finally {
            isFinished = true;
            Engine.logEngine.trace("Job run: isFinished set to true");
        }
    }
}
