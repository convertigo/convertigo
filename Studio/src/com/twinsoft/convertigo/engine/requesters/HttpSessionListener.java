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

import javax.servlet.http.*;

import com.twinsoft.convertigo.engine.Engine;

import java.util.*;

/**
 * This class is a workaround class, allowing to detect HTTP session
 * start and end, because of lack of specification from the Servlet
 * 2.2 API.
 */
public class HttpSessionListener implements HttpSessionBindingListener {
    public static Map<String, HttpSession> httpSessions = Collections.synchronizedMap(new HashMap<String, HttpSession>(256));
    
    public void valueBound(HttpSessionBindingEvent event) {
        try {
            Engine.logContext.debug("HTTP session starting...");
            HttpSession httpSession = event.getSession();
            String httpSessionID = httpSession.getId();
            httpSessions.put(httpSessionID, httpSession);
            Engine.logContext.debug("HTTP session started [" + httpSessionID + "]");
        }
        catch(Exception e) {
            Engine.logContext.error("Exception during binding HTTP session listener", e);
        }
    }
    
    public void valueUnbound(HttpSessionBindingEvent event) {
        try {
            Engine.logContext.debug("HTTP session stopping...");
            HttpSession httpSession = event.getSession();
            String httpSessionID = httpSession.getId();
            if (Engine.theApp != null) Engine.theApp.contextManager.removeAll(httpSessionID);
            httpSessions.remove(httpSessionID);
            Engine.logContext.debug("HTTP session stopped [" + httpSessionID + "]");
        }
        catch(Exception e) {
            Engine.logContext.error("Exception during unbinding HTTP session listener", e);
        }
    }
}
