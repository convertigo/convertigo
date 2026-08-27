/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.source;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;

@ServiceDefinition(name = "Get", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Get extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		if (id == null || id.isBlank()) {
			throw new ServiceException("missing id parameter");
		}
		var source = sourceDocument(Utils.getDbo(id));
		response.put("id", id);
		response.put("content", source.content());
		response.put("revision", source.revision());
		response.put("fileName", source.file().getName());
		response.put("relativePath", source.relativePath());
		response.put("language", language(source.file()));
		response.put("readOnly", true);
	}

	static SourceDocument sourceDocument(DatabaseObject dbo) throws Exception {
		if (!(dbo instanceof FlowVirtualObject flowObject)) {
			throw new ServiceException("The selected object has no Flow source document.");
		}
		var sourcePath = flowObject.getSourcePath();
		if (sourcePath == null || sourcePath.isBlank()) {
			throw new ServiceException("The selected Flow object has no source document.");
		}
		var file = new File(sourcePath).getCanonicalFile();
		if (!file.isFile() || !isSupportedSource(file)) {
			throw new ServiceException("The selected Flow source is not an editable code document.");
		}
		var content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		var info = flowObject.getVirtualInfoObject();
		var relativePath = info == null ? "" : info.optString("sourceRelativePath", "");
		if (relativePath.isBlank()) {
			relativePath = file.getName();
		}
		return new SourceDocument(file, relativePath, content, sha256(content));
	}

	private static boolean isSupportedSource(File file) {
		var name = file.getName().toLowerCase();
		return name.endsWith(".flow.svelte") || name.endsWith(".flow.css")
				|| name.endsWith(".svelte") || name.endsWith(".svelte.js")
				|| name.endsWith(".svelte.ts") || name.endsWith(".block.js");
	}

	private static String language(File file) {
		var name = file.getName().toLowerCase();
		if (name.endsWith(".css")) {
			return "css";
		}
		if (name.endsWith(".ts")) {
			return "typescript";
		}
		if (name.endsWith(".js")) {
			return "javascript";
		}
		return "html";
	}

	private static String sha256(String content) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(content.getBytes(StandardCharsets.UTF_8)));
	}

	static record SourceDocument(File file, String relativePath, String content, String revision) {
	}
}
