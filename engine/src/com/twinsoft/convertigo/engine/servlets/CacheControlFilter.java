/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;

/** Cache policy for static files bundled with the Admin webapp, outside project workspaces. */
public class CacheControlFilter implements Filter {

	private static final String IMMUTABLE = "public, max-age=31536000, immutable";
	private static final String REVALIDATE = "no-cache, must-revalidate";
	private static final String SHORT = "public, max-age=60, must-revalidate";
	private static final Pattern ASSET_PATH = Pattern.compile("^/(?:[^/]+|(?:_app|fonts|icons|bezels|monaco/vs)/.+)$");
	private static final String ASSET_EXTENSION = "(?:css|m?js|map|json|webmanifest|txt|png|apng|jpe?g|gif|svg|webp|avif|ico|woff2?|ttf|otf|eot|wasm)";
	private static final Pattern STATIC_FILE = Pattern.compile(".+\\." + ASSET_EXTENSION + "$", Pattern.CASE_INSENSITIVE);
	private static final Pattern HEX_HASH_FILE = Pattern.compile(".+\\.[0-9a-fA-F]{8,64}\\." + ASSET_EXTENSION + "(?:\\.map)?$");
	// The Svelte/Vite output uses eight-character hashes, including '-' and '_'.
	// Restrict this format to generated output and keep ambiguous lowercase words on short TTLs.
	private static final Pattern SVELTE_HASH_PATH = Pattern.compile(
			"^/_app/immutable/(?:assets|chunks|entry|nodes)/[^/]+\\."
			+ "(?=[A-Za-z0-9_-]{0,7}[A-Z0-9_-])[A-Za-z0-9_-]{8}\\."
			+ ASSET_EXTENSION + "(?:\\.map)?$");
	private static final Pattern MONACO_HASH_PATH = Pattern.compile(
			"^/monaco/vs/(?:[^/]+/)*[^/]+-(?=[A-Za-z0-9_-]{0,7}[A-Z0-9_-])[A-Za-z0-9_-]{8}\\."
			+ ASSET_EXTENSION + "(?:\\.map)?$");
	private static final Pattern SCRIPT_FILE = Pattern.compile(".+\\.m?js$", Pattern.CASE_INSENSITIVE);
	private static final Set<String> REVALIDATED_FILES = Set.of(
			"env.json", "version.json", "manifest.json", "manifest.webmanifest", "asset-manifest.json",
			"ngsw.json", "ngsw-worker.js", "service-worker.js", "safety-worker.js", "sw.js", "chunk-recovery.js");
	private static final Set<String> EXCLUDED_ROOTS = Set.of("projects", "system", "services", "api", "fullsync");
	private static final Set<String> EXCLUDED_SEGMENTS = Set.of(".", "..", "WEB-INF", "META-INF", "services");

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse
				&& ("GET".equalsIgnoreCase(httpRequest.getMethod()) || "HEAD".equalsIgnoreCase(httpRequest.getMethod()))
				&& !HeaderName.CacheControl.has(httpResponse)) {
			var uri = httpRequest.getRequestURI();
			var contextPath = httpRequest.getContextPath();
			var path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
			if (path.isEmpty()) {
				path = "/";
			}
			var policy = getCacheControl(path);
			if (policy != null) {
				HeaderName.CacheControl.setHeader(httpResponse, policy);
				// A file can disappear, or a Range request can fail, after the existence check.
				// Set headers before body delivery/304, while preventing the policy from caching errors.
				response = new HttpServletResponseWrapper(httpResponse) {
					private void preventErrorCaching(int status) {
						if (status >= 300 && status != SC_NOT_MODIFIED) {
							HeaderName.CacheControl.setHeader(this, "no-store");
						}
					}
					@Override public void setStatus(int status) { preventErrorCaching(status); super.setStatus(status); }
					@Override public void sendError(int status) throws IOException { preventErrorCaching(status); super.sendError(status); }
					@Override public void sendError(int status, String message) throws IOException { preventErrorCaching(status); super.sendError(status, message); }
					@Override public void sendRedirect(String location) throws IOException { preventErrorCaching(SC_FOUND); super.sendRedirect(location); }
				};
			}
		}
		chain.doFilter(request, response);
	}

	private static String getCacheControl(String path) throws IOException {
		if (!isWebappPath(path)) {
			return null;
		}
		var file = webappFile(path.endsWith("/") ? path + "index.html" : path);
		if (file != null && file.isFile()) {
			var name = file.getName().toLowerCase(java.util.Locale.ROOT);
			if (name.endsWith(".html") || name.endsWith(".htm")) {
				return REVALIDATE;
			}
			if (ASSET_PATH.matcher(path).matches() && STATIC_FILE.matcher(name).matches()) {
				if (REVALIDATED_FILES.contains(name)) {
					return REVALIDATE;
				}
				if (SVELTE_HASH_PATH.matcher(path).matches() || MONACO_HASH_PATH.matcher(path).matches()
						|| HEX_HASH_FILE.matcher(file.getName()).matches()) {
					return IMMUTABLE;
				}
				// Stable AMD entry points reference version-specific chunks, even after HTML is refreshed.
				if (path.startsWith("/monaco/vs/") && SCRIPT_FILE.matcher(name).matches()) {
					return REVALIDATE;
				}
				return SHORT;
			}
		} else if (resolvesToIndexHtml(path)) {
			return REVALIDATE;
		}
		return null;
	}

	private static boolean isWebappPath(String path) {
		if (!path.startsWith("/") || path.contains("\\") || path.contains("%")) {
			return false;
		}
		var segments = path.substring(1).split("/");
		if (segments.length > 0 && EXCLUDED_ROOTS.contains(segments[0])) {
			return false;
		}
		for (var segment : segments) {
			if (EXCLUDED_SEGMENTS.contains(segment)) {
				return false;
			}
		}
		return true;
	}

	private static boolean resolvesToIndexHtml(String path) throws IOException {
		var servletPath = path.endsWith("/") ? path + "index.html" : path;
		if (!servletPath.endsWith("/index.html")) {
			return false;
		}
		var resolved = new StringBuilder();
		for (var segment : servletPath.substring(1).split("/")) {
			resolved.append("/");
			var direct = webappFile(resolved + segment);
			var fallback = webappFile(resolved + "_");
			if (direct != null && direct.exists()) {
				resolved.append(segment);
			} else if (fallback != null && fallback.exists()) {
				resolved.append("_");
			} else {
				return false;
			}
		}
		var file = webappFile(resolved.toString());
		return file != null && file.isFile() && "index.html".equals(file.getName());
	}

	private static File webappFile(String path) throws IOException {
		var root = new File(Engine.WEBAPP_PATH).getCanonicalFile();
		var file = new File(root, path.substring(1)).getCanonicalFile();
		// Do not add Admin policies to symlinks pointing into a workspace or outside the webapp.
		return file.toPath().startsWith(root.toPath()) ? file : null;
	}
}
