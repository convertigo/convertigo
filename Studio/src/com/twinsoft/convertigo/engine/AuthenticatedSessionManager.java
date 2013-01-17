package com.twinsoft.convertigo.engine;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.collections.ListUtils;

public class AuthenticatedSessionManager implements AbstractManager {
	public static enum SessionKey {ADMIN_ROLES, ADMIN_USER};
	public static enum Role { ANONYMOUS, AUTHENTICATED, WEB_ADMIN, TRIAL, MANAGER, MONITOR_AGENT, TEST_PLATFORM };

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

}
