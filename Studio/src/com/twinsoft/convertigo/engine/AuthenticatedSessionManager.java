package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpSession;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class AuthenticatedSessionManager implements AbstractManager {
	public static enum SessionKey {ADMIN_ROLES, ADMIN_USER};
	public static enum Role {
		ANONYMOUS,
		AUTHENTICATED,
		WEB_ADMIN,
		TRIAL,
		MANAGER,
		MONITOR_AGENT,
		WEB_ACCESS,
		HOME_VIEW("Consult the home part"),
		HOME_CONFIG("Configure the home part"),
		CACHE_VIEW("Consult the cache part"),
		CACHE_CONFIG("Configure the cache part"),
		CERTIFICATE_VIEW("Consult the certificate part"),
		CERTIFICATE_CONFIG("Configure the certificate part"),
		CONNECTIONS_VIEW("Consult the connections part"),
		CONNECTIONS_CONFIG("Configure the connections part"),
		KEYS_VIEW("Consult the keys part"),
		KEYS_CONFIG("Configure the keys part"),
		LOGS_VIEW("Consult the logs part"),
		LOGS_CONFIG("Configure the logs part"),
		PROJECT_DBO_VIEW("Consult a project content"),
		PROJECT_DBO_CONFIG("Configure a project content"),
		PROJECTS_VIEW("Consult the projects part"),
		PROJECTS_CONFIG("Configure the projects part"),
		SCHEDULER_VIEW("Consult the scheduler part"),
		SCHEDULER_CONFIG("Configure the scheduler part"),
		STORE_VIEW("Consult the store part"),
		STORE_CONFIG("Configure the store part"),
		SYMBOLS_VIEW("Consult the symbols part"),
		SYMBOLS_CONFIG("Configure the symbols part"),
		TRACE_VIEW("Consult the trace player part"),
		TRACE_CONFIG("Configure the trace player part"),
		TEST_PLATFORM("Unlock the testplatform");
		
		String description = null;
		
		Role(){}
		Role(String description) {
			this.description = description;
		}
		
		public String description() {
			return description;
		}
	};
	
	private JSONObject cache = null;

	public void init() throws EngineException {
	}

	public void destroy() throws EngineException {
	}

	public void addAuthenticatedSession(HttpSession httpSession, Role[] roles) {
		httpSession.setAttribute(SessionKey.ADMIN_ROLES.toString(), roles);
		Engine.logAdmin.debug("Added roles " + roles + " to session " + httpSession.getId());
	}
	
	public void removeAuthenticatedSession(HttpSession httpSession) {
		for (SessionKey key : SessionKey.values()) {
			httpSession.removeAttribute(key.toString());
		}
		Engine.logAdmin.debug("Removed authenticated sdession from session " + httpSession.getId());
	}

	public boolean hasRole(HttpSession httpSession, Role role) {
		Role[] roles = (Role[]) httpSession.getAttribute(SessionKey.ADMIN_ROLES.toString());
		boolean bResult = AuthenticatedSessionManager.hasRole(roles, role);
		Engine.logAdmin.debug("Session " + httpSession.getId() + " has " + (bResult ? "" : "not ") + "role " + role);
		return bResult;
	}
	
	public boolean isAuthenticated(HttpSession httpSession) {
		Role[] roles = roles(httpSession);
		if (roles == null) {
			return false;
		}
		
		return AuthenticatedSessionManager.hasRole(roles, Role.AUTHENTICATED);
	}
	
	public boolean isAnonymous(HttpSession httpSession) {
		Role[] roles = roles(httpSession);
		return (roles == null || !AuthenticatedSessionManager.hasRole(roles, Role.AUTHENTICATED));
	}
	
	public static boolean hasRole(Role[] roles, Role role) {
		return (roles != null && Arrays.asList(roles).contains(role));
	}

	public Role[] getRoles(HttpSession httpSession) {
		Role[] roles = roles(httpSession);
		roles = (roles == null ? null : roles.clone());
		Engine.logAdmin.debug("Getting roles for session " + httpSession.getId() + ": " + roles);
		return roles;
	}
	
	public void checkRoles(HttpSession httpSession, Role[] requiredRoles) throws AuthenticationException {
		List<Role> lRequiredRoles = Arrays.asList(requiredRoles);
		
		Role[] userRoles = roles(httpSession);
		if (userRoles == null) {
			throw new AuthenticationException("Authentication failure: no role defined");
		}
		
		List<Role> lUserRoles = Arrays.asList(userRoles);
		
		Engine.logAdmin.debug("User roles: " + lUserRoles);
		Engine.logAdmin.debug("Required roles: " + lRequiredRoles);

		// Check if the user has one (or more) role(s) in common with the required roles list
		if (ListUtils.intersection(lUserRoles, lRequiredRoles).size() > 0) {
			return;
		}
		
		throw new AuthenticationException("Authentication failure: user has not sufficient rights!");
	}
	
	private Role[] roles(HttpSession httpSession) {
		return (Role[]) httpSession.getAttribute(SessionKey.ADMIN_ROLES.toString());
	}
	
	private JSONObject load() throws IOException, JSONException {
		if (cache == null) {
			try {
				byte[] data = FileUtils.readFileToByteArray(new File(Engine.CONFIGURATION_PATH + "/user_roles.db"));
				data = Crypto2.decodeFromByteArray(EnginePropertiesManager.getProperty(PropertyName.CRYPTO_PASSPHRASE), data);
				String json = new String(data, "UTF-8");
				cache = new JSONObject(json);
			} catch (FileNotFoundException e) {
				cache = new JSONObject();
			} catch (Exception e) {
				File copy = new File(Engine.CONFIGURATION_PATH + "/user_roles." + System.currentTimeMillis() + ".db");
				FileUtils.copyFile(new File(Engine.CONFIGURATION_PATH + "/user_roles.db"), copy);
				Engine.logEngine.error("Broken user_roles.db file, create a new one. The old db is saved here: " + copy, e);
				cache = new JSONObject();
				save(cache);
			}
		}
		return cache;
	}
	
	private void save(JSONObject db) throws IOException {
		String json = db.toString();
		cache = db;
		byte[] data = json.getBytes("UTF-8");
		data = Crypto2.encodeToByteArray(EnginePropertiesManager.getProperty(PropertyName.CRYPTO_PASSPHRASE), data);
		FileUtils.writeByteArrayToFile(new File(Engine.CONFIGURATION_PATH + "/user_roles.db"), data);
	}
	
	public void setUser(String username, String password, Set<Role> roles) throws EngineException {
		try {
			if (StringUtils.isBlank(username)) {
				throw new IllegalArgumentException("Blank username not allowed");
			}
			if (StringUtils.isBlank(password)) {
				throw new IllegalArgumentException("Blank password not allowed");
			}
			if ("admin".equals(username)) {
				throw new IllegalArgumentException("Cannot defined another 'admin' user");				
			}
			
			JSONArray array = new JSONArray();
			for (Role role : roles) {
				array.put(role.name());
			}
			
			JSONObject user = new JSONObject();
			user.put("password", password);
			user.put("roles", array);
			
			synchronized (this) {
				JSONObject db = load();
				db.put(username, user);
				save(db);
			}
		} catch (Exception e) {
			throw new EngineException("Failed to set the role", e);
		}
	}
	
	public void deleteUser(String username) throws EngineException {
		try {			
			synchronized (this) {
				JSONObject db = load();
				db.remove(username);
				save(db);
			}
		} catch (Exception e) {
			throw new EngineException("Failed to delete the user '" + username + "'", e);
		}
	}
	
	public Set<String> getUsers() throws EngineException {
		try {
			JSONObject db;
			synchronized (this) {
				db = load();
			}
			Set<String> users = new TreeSet<String>();
			for (Iterator<String> i = GenericUtils.cast(db.keys()); i.hasNext();) {
				users.add(i.next());
			}
			return users;
		} catch (Exception e) {
			throw new EngineException("Failed to get users", e);
		}
	}
	
	public Set<Role> getRoles(String username) throws EngineException {
		try {
			JSONObject db;
			synchronized (this) {
				db = load();
			}
			JSONArray array = db.getJSONObject(username).getJSONArray("roles");
			Set<Role> roles = new TreeSet<Role>();
			for (int i = 0; i < array.length(); i++) {
				try {
					roles.add(Role.valueOf(array.getString(i)));
				} catch (IllegalArgumentException e) {
					Engine.logEngine.warn("Fail to load the role '" + array.getString(i) + "', ignored");
				}
			}
			return roles;
		} catch (Exception e) {
			throw new EngineException("Failed to get roles", e);
		}
	}
	
	public String getPassword(String username) throws EngineException {
		try {
			JSONObject db;
			synchronized (this) {
				db = load();
			}
			return db.getJSONObject(username).getString("password");
		} catch (Exception e) {
			throw new EngineException("Failed to get the password", e);
		}
	}

	public void deleteUsers() throws EngineException {
		try {
			synchronized (this) {
				save(new JSONObject());
			}
		} catch (Exception e) {
			throw new EngineException("Failed to get remove users", e);
		}
	}
	
	public boolean hasUser(String username) throws EngineException {
		try {
			JSONObject db;
			synchronized (this) {
				db = load();
			}
			return db.has(username);
		} catch (Exception e) {
			throw new EngineException("Failed to get the password", e);
		}
	}
}
