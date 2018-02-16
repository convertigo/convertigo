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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/engine/servlets/ProjectsDataFilter.java $
 * $Author: victorn $
 * $Revision: 39260 $
 * $Date: 2015-02-26 18:06:28 +0100 (jeu., 26 fÃ©vr. 2015) $
 */

package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;

public class RequestFilter implements Filter {
	
	private int contextLength = 0;
	private boolean init = false;
	private List<Rule> rules = Collections.emptyList();
	private WatchService ws;
	private WatchKey wk;
	
    public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain chain) throws IOException, ServletException {
    	HttpServletRequest request = (HttpServletRequest) _request;
    	HttpServletResponse response = (HttpServletResponse) _response;
    	
    	checkInit();
    	
    	List<Rule> rules = this.rules;
    	
    	for (Rule rule: rules) {
    		if (rule.match(request)) {
    			if (rule.accept) {
    				chain.doFilter(_request, _response);
    			} else {
    				response.setStatus(rule.status);
    			}
    			return;
    		}
    	}
    	
    	chain.doFilter(_request, _response);
    }
    
    private void checkInit() {
    	if (!init && Engine.theApp != null && Files.exists(Paths.get(Engine.CONFIGURATION_PATH))) {
    		init = true;
    		final Path config = Paths.get(Engine.CONFIGURATION_PATH, "requestfilter.json");
    		if (Files.exists(config)) {
    			parse(config);
    		}
    		
    		Engine.execute(new Runnable() {

				@Override
				public void run() {
					try {
			    		Path parent = config.getParent();
			    		ws = parent.getFileSystem().newWatchService();
			    		wk = parent.register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
			    		
			    		while (true) {
			    			WatchKey wk = ws.take();
			    			
							for (final WatchEvent<?> event: wk.pollEvents()) {
								Path ctx = (Path) event.context();
								
								if (config.endsWith(ctx)) {
									parse(config);
								}
							}
							
			                if (!wk.reset() || !init) {
			                	wk.cancel();
			                	ws.close();
			                    break;
			                }
			    		}
					} catch (Exception e) {
						
					}
				}
    			
    		});
    		
    	}
    }
    
    private void parse(Path config) {
    	try {
    		String content = new String(Files.readAllBytes(config), "UTF-8");
    		System.out.println(content);
    		JSONObject json = new JSONObject(content);
    		if (json.getBoolean("enabled")) {
    			JSONArray jRules = json.getJSONArray("rules");
    			List<Rule> rules = new ArrayList<Rule>(jRules.length());
    			for (int i = 0; i < jRules.length(); i++) {
    				JSONObject rule = jRules.getJSONObject(i);
    				if (!rule.has("enabled") || rule.getBoolean("enabled")) {
    					rules.add(new Rule(rule));
    				}
    			}
    			this.rules = rules;
			} else {
				this.rules = Collections.emptyList();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void destroy() {
    	try {
    		ws.close();
    	} catch (Exception e) {
    	}
    	try {
    		wk.cancel();
    	} catch (Exception e) {
    	}
    	rules = Collections.emptyList();
    	init = false;
    }

	public void init(FilterConfig filterConfig) throws ServletException {
		contextLength = filterConfig.getServletContext().getContextPath().length();
		System.out.println("Request data filter has been initialized");
	}
	
	private class Rule {
		
		Matcher ip = null;
		Matcher uri = null;
		Matcher port = null;
		
		boolean accept = false;
		int status = 404;

		public Rule(JSONObject rule) throws Exception {
			if (rule.has("ip")) {
				ip = Pattern.compile(rule.getString("ip")).matcher("");
			}
			
			if (rule.has("uri")) {
				uri = Pattern.compile(rule.getString("uri")).matcher("");
			}
			
			if (rule.has("port")) {
				port = Pattern.compile(rule.getString("port")).matcher("");
			}
			
			if (rule.has("accept")) {
				accept = rule.getBoolean("accept");
			}
			
			if (rule.has("status")) {
				status = rule.getInt("status");
			}
		}
		
		public boolean match(HttpServletRequest request) {
			boolean ok = true;
			
			if (ok && ip != null) {
				ip.reset(request.getRemoteAddr());
				ok = ip.find();
			}
			
			if (ok && uri != null) {
				uri.reset(request.getRequestURI().substring(contextLength));
				ok = uri.find();
			}
			
			if (ok && port != null) {
				port.reset(Integer.toString(request.getLocalPort()));
				ok = port.find();
			}
			
			return ok;
		}
		
	}
}
