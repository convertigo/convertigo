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

package com.twinsoft.convertigo.engine.admin.services.logs;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.logmanager.LogServiceHelper;
import com.twinsoft.convertigo.engine.admin.logmanager.LogServiceHelper.LogManagerParameter;
import com.twinsoft.convertigo.engine.admin.logmanager.LogManager;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.LOGS_CONFIG, Role.LOGS_VIEW },
		parameters = {},
		returnValue = ""
	)
public class Get extends JSonService {
	static private final String attr_start = Get.class.getCanonicalName()+".start";
	static private final String attr_appender = Get.class.getCanonicalName()+".appender.";
	
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
        HttpSession session = request.getSession();
        LogManager logmanager = LogServiceHelper.getLogManager(request);
        Long start = System.currentTimeMillis();
        session.setAttribute(attr_start, start);
        
        synchronized (logmanager) {
	        boolean realtime = false;
	        try {
	        	realtime = Boolean.parseBoolean(request.getParameter("realtime"));
	        } catch (Exception e) {}
	        
	        if (realtime) {
	        	LogServiceHelper.prepareLogManager(request, logmanager, LogManagerParameter.filter, LogManagerParameter.timeout, LogManagerParameter.nbLines);
		        logmanager.setContinue(true);
		        
	        	if (session.getAttribute("isRealtime") == null) {
	        		// fix #2959 - Removed 10 last minutes added to real time mode
	        		//logmanager.setDateStart(new Date(System.currentTimeMillis() - 600000));
	        		logmanager.setDateStart(new Date(System.currentTimeMillis()));
	        		logmanager.setDateEnd(LogManager.date_last);
	        		session.setAttribute("isRealtime", true);
	        	}
	            Appender appender = (Appender) session.getAttribute(attr_appender + ServiceUtils.getAdminInstance(request));
	            if (appender == null) {
	            	appender = new AppenderSkeleton() {
	
	                	@Override
	                	protected void append(LoggingEvent arg0) {
	                		synchronized (this) {
	    						this.notifyAll();
	    					}
	                	}
	
	        			public void close() {
	        			}
	
	        			public boolean requiresLayout() {
	        				return false;
	        			}
	                };
	                session.setAttribute(attr_appender + ServiceUtils.getAdminInstance(request), appender);
	            } else {
	            	synchronized (appender) {
	            		appender.notifyAll();
	            	}
	            }
	            try {
	    	        Engine.logConvertigo.addAppender(appender);
	    	        
	    	        boolean interrupted = false;
	    	        JSONArray lines = logmanager.getLines();
	    	        while (lines.length() == 0 && !interrupted && session.getAttribute(attr_start) == start) {
	    	        	synchronized (appender) {
							try {
								appender.wait(2000);
							} catch (InterruptedException e) {
								interrupted = true;
							}
						}
	    	        	lines = logmanager.getLines();
	    	        }
	    	        response.put("lines", lines);
	            } finally {
	            	if (appender != null) {
	            		Engine.logConvertigo.removeAppender(appender);
	            	}
	            }
	        } else {
	        	LogServiceHelper.prepareLogManager(request, logmanager);
	        	session.removeAttribute("isRealtime");
		        
	        	JSONArray lines = logmanager.getLines();
	        	logmanager.setContinue(true);
	        	while (lines.length() == 0 && logmanager.hasMoreResults() && session.getAttribute(attr_start) == start) {
	        		lines = logmanager.getLines();
	        	}
	        	
				response.put("lines", lines);
				response.put("hasMoreResults", logmanager.hasMoreResults());
	        }
		}
	}
}
