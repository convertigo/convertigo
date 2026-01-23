/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;

public class SecurityFilter implements Filter, PropertyChangeEventListener {
	
	private int contextLength = 0;
	private boolean init = false;
	private Path config;
	private List<Rule> rules = Collections.emptyList();
	private WatchService ws;
	private boolean enabled = false;
	
    public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain chain) throws IOException, ServletException {
    	HttpServletRequest request = (HttpServletRequest) _request;
    	HttpServletResponse response = (HttpServletResponse) _response;
    	
    	Level level = Level.ERROR;
    	StringBuilder sb = new StringBuilder();
    	try {
    		level = Engine.logSecurityFilter.getLevel();
    	} catch (Exception e) {
		}
    	
    	checkInit();
    	
    	List<Rule> rules = this.rules;
    	boolean doFilter = true;
    	
    	boolean first = true;
    	for (Rule rule: rules) {
    		if (first && Level.INFO.isGreaterOrEqual(level)) {
    			String uri = request.getRequestURI().substring(contextLength);
    			if (uri.endsWith("/services/logs.Get")) {
    				level = Level.OFF;
    			} else {
	    			sb.append("ip[").append(request.getRemoteAddr()).append("] ");
	    			sb.append("port[").append(request.getLocalPort()).append("] ");
	    			sb.append("uri[").append(uri).append("]");
    			}
    		}
    		
    		if (rule.match(request)) {
    			if (Level.DEBUG.isGreaterOrEqual(level)) {
    				sb.append("\nmatch ").append(rule);
    			} else if (Level.INFO.isGreaterOrEqual(level)) {
    				sb.append(" match ").append(rule.accept ? "keep" : "drop");
    			}
    			
    			if (!rule.accept) {
    				doFilter = false;
    				response.setStatus(rule.status);
    			}
    			
    			break;
    		} else {
    			if (Level.TRACE.isGreaterOrEqual(level)) {
    				sb.append("\nno match ").append(rule);
    			}
    		}
    		first = false;
    	}
    	
		if (sb.length() > 0) {
			try {
				Engine.logSecurityFilter.log(level, sb.toString());
			} catch (Exception e) {
				System.out.println("[Convertigo Security Filter] " + sb.toString());
			}
		}
		
		if (doFilter) {
			chain.doFilter(_request, _response);
		}
    }
    
    @Override
	public void onEvent(PropertyChangeEvent event) {
		PropertyName name = event.getKey();
		if (name == PropertyName.SECURITY_FILTER) {
			Engine.logSecurityFilter.info("Property '" + name + "' changed to " + event.getValue());
			boolean enabled = "true".equals(event.getValue());
			parse(enabled);
		}
	}
    
    private void checkInit() {
    	if (!init && Engine.theApp != null && Files.exists(Paths.get(Engine.CONFIGURATION_PATH))) {
    		init = true;
    		config = Paths.get(Engine.CONFIGURATION_PATH, "security_filter.json");
    		
    		if (Files.exists(config)) {
    			parse();
    		}
    		
    		Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
    		
    		Engine.execute(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.currentThread().setName("SecurityFilter_watch_config");
			    		Path parent = config.getParent();
			    		ws = parent.getFileSystem().newWatchService();
			    		
			    		while (true) {
			    			WatchKey wk = ws.take();
			    			
							for (final WatchEvent<?> event: wk.pollEvents()) {
								Path ctx = (Path) event.context();
								
								if (config.endsWith(ctx)) {
									parse();
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
    
    private void parse() {
		try {
			enabled = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.SECURITY_FILTER);
		} catch (Exception e) {
		}
    		
    	parse(enabled);
    }
    
    private void parse(boolean enabled) {
    	Level level = Level.DEBUG;
    	StringBuilder sb = new StringBuilder();
    	try {
    		level = Engine.logSecurityFilter.getLevel();
    	} catch (Exception e) {
		}
    	
    	try {
    		if (enabled) {
    			String content = new String(Files.readAllBytes(config), "UTF-8");
				if (Level.WARN.isGreaterOrEqual(level)) {
					sb.append("parsing ").append(content.length()).append(" chars from ").append(config);
				}
				
        		JSONObject json = new JSONObject(content);
    			JSONArray jRules = json.getJSONArray("rules");
    			List<Rule> rules = new ArrayList<Rule>(jRules.length());
    			for (int i = 0; i < jRules.length(); i++) {
    				JSONObject rule = jRules.getJSONObject(i);
    				if (!rule.has("enabled") || rule.getBoolean("enabled")) {
    					rules.add(new Rule(rule));
    					
    					if (Level.DEBUG.isGreaterOrEqual(level)) {
    						sb.append('\n').append("add  ").append(rules.get(rules.size() - 1));
    					}
    				} else {
    					if (Level.DEBUG.isGreaterOrEqual(level)) {
    						sb.append('\n').append("skip ").append(rule.toString());
    					}
    				}
    			}
    			if (Level.INFO.isGreaterOrEqual(level)) {
					sb.append('\n').append(rules.size()).append(" rules added");
				}
    			this.rules = rules;
			} else {
				if (Level.WARN.isGreaterOrEqual(level)) {
					sb.append("parse skipped, security filter is disabled");
				}
				this.rules = Collections.emptyList();
			}
		} catch (Exception e) {
			level = Level.FATAL;
			sb.append("Failed to parse '").append(config).append("' due to ").append(e.getMessage());
		}
    	
		if (sb.length() > 0) {
			try {
				Engine.logSecurityFilter.log(level, sb.toString());
			} catch (Exception e) {
				System.out.println("[Convertigo Security Filter] " + sb.toString());
			}
		}
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
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (!accept) {
				sb.append("drop[").append(status).append("] ");
			} else {
				sb.append("keep ");
			}
			
			if (ip != null) {
				sb.append("ip[").append(ip.pattern().pattern()).append("]" );
			}
			
			if (uri != null) {
				sb.append("uri[").append(uri.pattern().pattern()).append("]" );
			}
			
			if (port != null) {
				sb.append("port[").append(port.pattern().pattern()).append("]" );
			}
			return sb.toString();
		}
	}

	public static boolean isAccept(ServletRequest _request) {
		final boolean accept[] = {false};
		try {
			SecurityFilter sf = new SecurityFilter();
			sf.contextLength = _request.getServletContext().getContextPath().length();
			sf.init = true;
			sf.config = Paths.get(Engine.CONFIGURATION_PATH, "security_filter.json");
			sf.parse(true);
			
			sf.doFilter(_request, (HttpServletResponse) Proxy.newProxyInstance(
					HttpServletResponse.class.getClassLoader(),
					new Class<?>[] { HttpServletResponse.class },
					(proxy, method, args) -> {
						String name = method.getName();
						if ("setStatus".equals(name)) {
							return null;
						}
						Class<?> returnType = method.getReturnType();
						if (returnType == boolean.class) {
							return false;
						}
						if (returnType == int.class) {
							return 0;
						}
						if (returnType == long.class) {
							return 0L;
						}
						return null;
					}),
					new FilterChain() {

				@Override
				public void doFilter(ServletRequest arg0, ServletResponse arg1) throws IOException, ServletException {
					accept[0] = true;
				}
			});
		} catch (Exception e) {
			// TODO: handle exception
		}

		return accept[0];
	}
}
