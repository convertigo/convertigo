package com.twinsoft.convertigo.engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ListUtils;

import com.twinsoft.convertigo.engine.AuthenticationException;

public class AuthenticatedSessionManager implements AbstractManager {

	public static enum Role { ANONYMOUS, AUTHENTICATED, WEB_ADMIN, TRIAL, MANAGER, MONITOR_AGENT, TEST_PLATFORM };

	private Map<String, Role[]> authenticatedSessions;
	
	public void init() throws EngineException {
		authenticatedSessions = new HashMap<String, Role[]>();
	}

	public void destroy() throws EngineException {
		authenticatedSessions = null;
	}

	public void addAuthenticatedSession(String sessionId, Role[] roles) {
		authenticatedSessions.put(sessionId,  roles);
		Engine.logAdmin.debug("Added roles " + roles + " to session " + sessionId);
	}
	
	public void removeAuthenticatedSession(String sessionId) {
		authenticatedSessions.remove(sessionId);
		Engine.logAdmin.debug("Removed authenticated sdession from session " + sessionId);
	}

	public boolean hasRole(String sessionId, Role role) {
		Role[] roles = authenticatedSessions.get(sessionId);
		boolean bResult = AuthenticatedSessionManager.hasRole(roles, role);
		Engine.logAdmin.debug("Session " + sessionId + " has " + (bResult ? "" : "not ") + "role " + role);
		return bResult;
	}
	
	public boolean isAuthenticated(String sessionId) {
		Role[] roles = authenticatedSessions.get(sessionId);
		if (roles == null) return false;
		
		return AuthenticatedSessionManager.hasRole(roles, Role.AUTHENTICATED);
	}
	
	public boolean isAnonymous(String sessionId) {
		Role[] roles = authenticatedSessions.get(sessionId);
		return (roles == null || !AuthenticatedSessionManager.hasRole(roles, Role.AUTHENTICATED));
	}
	
	public static boolean hasRole(Role[] roles, Role role) {
		return (roles != null && Arrays.asList(roles).contains(role));
	}

	public Role[] getRoles(String sessionId) {
		Role[] roles = authenticatedSessions.get(sessionId);
		roles = (roles == null ? null : roles.clone());
		Engine.logAdmin.debug("Getting roles for session " + sessionId + ": " + roles);
		return roles;
	}
	
	public void checkRoles(String sessionId, Role[] requiredRoles) throws AuthenticationException {
		List<Role> lRequiredRoles = Arrays.asList(requiredRoles);
		
		Role[] userRoles = authenticatedSessions.get(sessionId);
		if (userRoles == null)
			throw new AuthenticationException("Authentication failure: no role defined");
		
		List<Role> lUserRoles = Arrays.asList(userRoles);
		
		Engine.logAdmin.debug("User roles: " + lUserRoles);
		Engine.logAdmin.debug("Required roles: " + lRequiredRoles);

		// Check if the user has one (or more) role(s) in common with the required roles list
		if (ListUtils.intersection(lUserRoles, lRequiredRoles).size() > 0) return;
		
		throw new AuthenticationException("Authentication failure: user has not sufficient rights!");
	}

}
