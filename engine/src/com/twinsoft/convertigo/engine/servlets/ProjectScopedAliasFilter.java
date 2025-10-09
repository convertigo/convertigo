/*
 * Copyright (c) 2001-2025 Convertigo SA.
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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;

public class ProjectScopedAliasFilter implements Filter {

 private static final String ATTRIBUTE_FORWARDED = ProjectScopedAliasFilter.class.getName() + ".forwarded";

private static final Alias[] ALIASES = {
	new Alias("/.services", "/services", true),
	new Alias("/.fullsync", "/fullsync", true)
};

private static final Set<String> REQUESTER_EXTENSIONS = Set.of(
	".json",
	".jsonp",
	".xml",
	".pxml",
	".cxml",
	".cpdf",
	".bin",
	".proxy",
	".ws",
	".wsr",
	".wsl"
);

private static final Pattern PROJECT_NAME = Pattern.compile("[A-Za-z0-9_.\\-]+");
private static final Pattern PROJECT_IN_PATH = Pattern.compile("/projects/([^/]+)/");

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			chain.doFilter(request, response);
			return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		if (httpRequest.getAttribute(ATTRIBUTE_FORWARDED) != null) {
			chain.doFilter(request, response);
			return;
		}

		String contextPath = httpRequest.getContextPath();
		String requestUri = httpRequest.getRequestURI();
		String path = requestUri.substring(contextPath.length());

		if (handleRequesterAlias(httpRequest, (HttpServletResponse) response, path)) {
			return;
		}

		AliasMatch match = findAlias(path);
		if (match != null) {
			RequestDispatcher dispatcher = httpRequest.getRequestDispatcher(match.targetPath);
			if (dispatcher != null) {
				if (Engine.logEngine.isDebugEnabled()) {
					Engine.logEngine.debug("(ProjectScopedAliasFilter) Forward " + path + " -> " + match.targetPath);
				}
				httpRequest.setAttribute(ATTRIBUTE_FORWARDED, Boolean.TRUE);
				dispatcher.forward(request, response);
				return;
			}
		}

		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	private AliasMatch findAlias(String path) {
		for (Alias alias : ALIASES) {
			AliasMatch match = alias.match(path);
			if (match != null) {
				return match;
			}
		}
		return null;
	}

	private boolean handleRequesterAlias(HttpServletRequest request, HttpServletResponse response, String path) throws ServletException, IOException {
		int aliasIndex = path.lastIndexOf("/.");
		if (aliasIndex == -1) {
			return false;
		}
		var extension = path.substring(aliasIndex + 1);
		int queryIndex = extension.indexOf('?');
		if (queryIndex != -1) {
			extension = extension.substring(0, queryIndex);
		}
		extension = extension.toLowerCase(Locale.ROOT);
		if (!REQUESTER_EXTENSIONS.contains(extension)) {
			return false;
		}
		var project = request.getParameter("__project");
		if (project == null || project.isEmpty()) {
			project = detectProjectFromAlias(path, request.getRequestURI());
		}
		if (project == null || project.isEmpty()) {
			return false;
		}
		if (!PROJECT_NAME.matcher(project).matches()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid project name");
			return true;
		}
		var target = "/projects/" + project + "/" + extension;
		var dispatcher = request.getRequestDispatcher(target);
		if (dispatcher == null) {
			return false;
		}
		if (Engine.logEngine.isDebugEnabled()) {
			Engine.logEngine.debug("(ProjectScopedAliasFilter) Forward " + path + " -> " + target);
		}
		request.setAttribute(ATTRIBUTE_FORWARDED, Boolean.TRUE);
		dispatcher.forward(request, response);
		return true;
	}

	private String detectProjectFromAlias(String path, String uri) {
		var matcher = PROJECT_IN_PATH.matcher(uri);
		if (matcher.find()) {
			return matcher.group(1);
		}
		var matcherPath = PROJECT_IN_PATH.matcher(path);
		if (matcherPath.find()) {
			return matcherPath.group(1);
		}
		return null;
	}

	private static class Alias {
		private final String alias;
		private final String target;
		private final boolean allowSubPath;

		private Alias(String alias, String target, boolean allowSubPath) {
			this.alias = alias;
			this.target = target;
			this.allowSubPath = allowSubPath;
		}

		private AliasMatch match(String path) {
			int index = path.indexOf(alias);
			if (index == -1) {
				return null;
			}

			int aliasLength = alias.length();
			int endIndex = index + aliasLength;

			if (path.length() == endIndex) {
				return new AliasMatch(target);
			}

			if (!allowSubPath) {
				return null;
			}

			char separator = path.charAt(endIndex);
			if (separator != '/') {
				return null;
			}

			String suffix = path.substring(endIndex);
			return new AliasMatch(target + suffix);
		}
	}

	private static class AliasMatch {
		private final String targetPath;

		private AliasMatch(String targetPath) {
			this.targetPath = targetPath;
		}
	}
}
