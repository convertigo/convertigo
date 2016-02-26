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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.io.FileUtils;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.tas.KeyManager;
import com.twinsoft.tas.TASException;

/**
 * This class is a workaround class, allowing to detect HTTP session
 * start and end, because of lack of specification from the Servlet
 * 2.2 API.
 */
public class HttpSessionListener implements HttpSessionBindingListener {
	private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss.SSS");
    private static final Map<String, HttpSession> httpSessions = new HashMap<String, HttpSession>();
    
    public void valueBound(HttpSessionBindingEvent event) {
        try {
            Engine.logContext.debug("HTTP session starting...");
            HttpSession httpSession = event.getSession();
            String httpSessionID = httpSession.getId();
            synchronized (httpSessions) {
                httpSessions.put(httpSessionID, httpSession);				
			}
            Engine.logContext.debug("HTTP session started [" + httpSessionID + "]");
            
            if (Engine.isEngineMode()) {
            	KeyManager.start(com.twinsoft.api.Session.EmulIDSE);
            }
        } catch(TASException e) {
        	if (e.isOverflow()) {
        		String line = dateFormat.format(new Date()) + "\t" + e.getCvMax() + "\t" + e.getCvCurrent() + "\n";
        		try {
					FileUtils.write(new File(Engine.LOG_PATH + "/Session License exceeded.log"), line, true);
				} catch (IOException e1) {
					Engine.logContext.error("Failed to write the 'Session License exceeded.log' file", e1);
				}
        	} else {
	        	event.getSession().setAttribute("__exception", e);
	        	HttpUtils.terminateSession(event.getSession());
        	}
        } catch(Exception e) {
            Engine.logContext.error("Exception during binding HTTP session listener", e);
        }
    }
    
    public void valueUnbound(HttpSessionBindingEvent event) {
        try {
            Engine.logContext.debug("HTTP session stopping...");
            HttpSession httpSession = event.getSession();
            String httpSessionID = httpSession.getId();

            if (Engine.theApp != null) Engine.theApp.contextManager.removeAll(httpSessionID);
            removeSession(httpSessionID);
            
            Engine.logContext.debug("HTTP session stopped [" + httpSessionID + "]");
        } catch(Exception e) {
            Engine.logContext.error("Exception during unbinding HTTP session listener", e);
        }
    }
    
    static public void removeSession(String httpSessionID) {
        synchronized (httpSessions) {
            if (Engine.isEngineMode() && httpSessions.remove(httpSessionID) != null) {
            	KeyManager.stop(com.twinsoft.api.Session.EmulIDSE);
            }
        }    	
    }
    
    static public HttpSession getHttpSession(String sessionID) {
        synchronized (httpSessions) {
        	return httpSessions.get(sessionID);
        }
    }
    
    static public void removeAllSession() {
        synchronized (httpSessions) {
        	for (Iterator<Entry<String, HttpSession>> iEntry = httpSessions.entrySet().iterator(); iEntry.hasNext();) {
        		Entry<String, HttpSession> entry = iEntry.next();
        		HttpUtils.terminateSession(entry.getValue());
        		removeSession(entry.getKey());
        	}
        }
    }
}
