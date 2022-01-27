/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;
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
		FULLSYNC_VIEW("Consult the fullsync part"),
		FULLSYNC_CONFIG("Configure the fullsync part"),
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
		TEST_PLATFORM("Unlock the testplatform"),
		TEST_PLATFORM_HIDDEN("Unlock the testplatform and see hidden requestables", TEST_PLATFORM),
		TEST_PLATFORM_PRIVATE("Unlock the testplatform and see hidden/private requestables", TEST_PLATFORM_HIDDEN),
		WEB_ADMIN(TEST_PLATFORM_PRIVATE);
		
		String description = null;
		Role[] subroles = null;
		
		Role(Role... subroles) {
			this.subroles = subroles;			
		}
		
		Role(String description, Role... subroles) {
			this.description = description;
			this.subroles = subroles;
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
		if (httpSession == null) {
			return;
		}
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
	
	public static boolean hasRole(Role[] userRoles, Role[] requiredRoles) {
		if (userRoles == null || requiredRoles == null) {
			return false;
		}
		return !ListUtils.intersection(Arrays.asList(userRoles), Arrays.asList(requiredRoles)).isEmpty();
	}
	
	public static void addRoles(Set<Role> roles, Role role) {
		roles.add(role);
		if (role.subroles != null) {
			for (Role r: role.subroles) {
				addRoles(roles, r);
			}
		}
	}
	
	public static Role[] toRoles(Role... roles) {
		Set<Role> rs = new HashSet<AuthenticatedSessionManager.Role>(roles.length * 2);
		for (Role role: roles) {
			addRoles(rs, role);
		}
		return rs.toArray(new Role[rs.size()]);
	}

	public Role[] getRoles(HttpSession httpSession) {
		if (httpSession == null) {
			return null;
		}
		Role[] roles = roles(httpSession);
		roles = (roles == null ? null : roles.clone());
		Engine.logAdmin.debug("Getting roles for session " + httpSession.getId() + ": " + roles);
		return roles;
	}
	
	public void checkRoles(HttpSession httpSession, Role... requiredRoles) throws AuthenticationException {
		Role[] userRoles = roles(httpSession);
		if (userRoles == null) {
			throw new AuthenticationException("Authentication failure: no role defined");
		}
		List<Role> lRequiredRoles = Arrays.asList(requiredRoles);
		
		List<Role> lUserRoles = Arrays.asList(userRoles);
		
		Engine.logAdmin.debug("User roles: " + lUserRoles);
		Engine.logAdmin.debug("Required roles: " + lRequiredRoles);

		// Check if the user has one (or more) role(s) in common with the required roles list
		if (!ListUtils.intersection(lUserRoles, lRequiredRoles).isEmpty()) {
			return;
		}
		
		throw new AuthenticationException("Authentication failure: user has not sufficient rights!");
	}
	
	private Role[] roles(HttpSession httpSession) {
		if (httpSession == null) {
			return null;
		}
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
	
	private synchronized JSONObject loadSync() throws IOException, JSONException {
		return load();
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
			JSONObject db = loadSync();
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
			return getRoles(loadSync(), username);
		} catch (Exception e) {
			throw new EngineException("Failed to get roles", e);
		}
	}
	
	private Set<Role> getRoles(JSONObject db, String username) throws JSONException {
		JSONArray array = db.getJSONObject(username).getJSONArray("roles");
		Set<Role> roles = new TreeSet<Role>();
		for (int i = 0; i < array.length(); i++) {
			try {
				addRoles(roles, Role.valueOf(array.getString(i)));
			} catch (IllegalArgumentException e) {
				Engine.logEngine.warn("Fail to load the role '" + array.getString(i) + "', ignored");
			}
		}
		return roles;
	}
	
	public String getPassword(String username) throws EngineException {
		try {
			return getPassword(loadSync(), username);
		} catch (Exception e) {
			throw new EngineException("Failed to get the password", e);
		}
	}
	
	private String getPassword(JSONObject db, String username) throws JSONException {
		return db.getJSONObject(username).getString("password");
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
			return loadSync().has(username);
		} catch (Exception e) {
			return false;
		}
	}
	
	public Set<Role> checkUser(String username, String password) throws EngineException {
		try {
			JSONObject db = loadSync();
			String pwd = getPassword(db, username);
			if (DigestUtils.sha512Hex(password).equals(pwd) || DigestUtils.md5Hex(password).equals(pwd)) {
				return getRoles(db, username);
			}
		} catch (Exception e) {
		}
		return null;
	}
	
	public void updateUsers(JSONObject users, String importAction) throws EngineException {
		try {
			if (!(importAction.equals("clear-import") || importAction.equals("priority-server") || importAction.equals("priority-import"))) {
				throw new IllegalArgumentException("importAction must be 'clear-import', 'priority-server' or 'priority-import'");
			}
			
			File f = new File(Engine.CONFIGURATION_PATH + "/user_roles.db");
			
			File oldFile = null;
			if (f.exists()) {
				Date date = new Date();
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				
				File parentFile = f.getParentFile();
				oldFile = new File(parentFile, f.getName().replaceAll(".db", "_" + dateFormat.format(date) + ".db"));
				
				int i = 1;
				while (oldFile.exists()) {
					oldFile = new File(parentFile, f.getName().replaceAll(".db", "_" + dateFormat.format(date) + "_" + i + ".db"));
					i++;
				}
				f.renameTo(oldFile);
			}
			synchronized (this) {
				if (!importAction.equals("clear-import")) {
					boolean priorityServer = importAction.equals("priority-server");
					JSONObject oldUsers = load();
					for (Iterator<String> i = GenericUtils.cast(oldUsers.keys()); i.hasNext();) {
						String name = i.next();
						if (priorityServer || !users.has(name)) {
							users.put(name, oldUsers.get(name));
						}
					}
					
				}
				save(users);
			}
		} catch (Exception exception) {
			throw new EngineException("Failed to update Users", exception);
		}
	}
	
	public JSONObject exportUsers(JSONArray usernames) throws EngineException {
		try {
			JSONObject users;
			JSONObject export = new JSONObject();
			
			synchronized (this) {
				users = load();
			}
	
			for (int i = 0; i < usernames.length(); i++) {
				String name = usernames.getJSONObject(i).getString("name");
				if (users.has(name)) {
					export.put(name, users.get(name));
				}
			}
			return export;
		} catch (Exception exception) {
			throw new EngineException("Failed to update Users", exception);
		}
	}
	
	public static void validatePassword(String password) throws EngineException {
		String message = "Invalid password: ";
		try {
			String regex = EnginePropertiesManager.getProperty(PropertyName.USER_PASSWORD_REGEX);
			if (StringUtils.isBlank(regex)) {
				return;
			}
			if (Pattern.compile(regex).matcher(password).matches()) {
				return;
			}
			message += EnginePropertiesManager.getProperty(PropertyName.USER_PASSWORD_INSTRUCTION);
			if (StringUtils.isBlank(message)) {
				message += "doesn't respect policy.";
			}
		} catch (Exception e) {
			message = e.getMessage();
		}
		throw new EngineException(message);
	}
}
