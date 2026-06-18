/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.sourcepicker;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.CachedIntrospector.Property;

@ServiceDefinition(name = "Apply", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Apply extends JSonService {
	private static class Candidate {
		private final PropertyDescriptor descriptor;
		private final String kind;

		private Candidate(PropertyDescriptor descriptor, String kind) {
			this.descriptor = descriptor;
			this.kind = kind;
		}
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		String id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}
		String sourcePriority = request.getParameter("sourcePriority");
		if (sourcePriority == null || sourcePriority.isBlank()) {
			throw new ServiceException("missing sourcePriority parameter");
		}
		String xpath = request.getParameter("xpath");
		String propertyName = request.getParameter("propertyName");

		DatabaseObject dbo = Utils.getDbo(id);
		if (dbo == null) {
			response.put("done", false);
			response.put("state", "not_found");
			response.put("message", "Target object not found");
			return;
		}

		List<Candidate> candidates = sourceCandidates(dbo);
		Candidate candidate = selectCandidate(candidates, propertyName);
		if (candidate == null) {
			if (propertyName != null && !propertyName.isBlank()) {
				response.put("done", false);
				response.put("state", "not_found");
				response.put("message", "No source property named " + propertyName);
			} else if (candidates.size() > 1) {
				response.put("done", false);
				response.put("state", "choice");
				response.put("candidates", candidatesToJson(candidates));
			} else {
				response.put("done", false);
				response.put("state", "not_available");
				response.put("message", "No source property available on this object");
			}
			return;
		}

		XMLVector<String> sourceDefinition = sourceDefinition(sourcePriority, xpath);
		apply(dbo, candidate, sourceDefinition);

		response.put("done", true);
		response.put("id", dbo.getFullQName());
		response.put("state", "success");
		response.put("propertyName", candidate.descriptor.getName());
		response.put("displayName", candidate.descriptor.getDisplayName());
		response.put("sourceDefinition", sourceDefinitionToJson(sourceDefinition));
		response.put("message", "Source has been successfully applied!");
	}

	private static List<Candidate> sourceCandidates(DatabaseObject dbo) {
		List<Candidate> candidates = new ArrayList<>();
		for (PropertyDescriptor descriptor : CachedIntrospector.getPropertyDescriptors(dbo, Property.smartType)) {
			addCandidate(candidates, descriptor, "smartType");
		}
		for (PropertyDescriptor descriptor : CachedIntrospector.getPropertyDescriptors(dbo, Property.sourceDefinition)) {
			addCandidate(candidates, descriptor, "sourceDefinition");
		}
		for (PropertyDescriptor descriptor : CachedIntrospector.getPropertyDescriptors(dbo, Property.sourcesDefinition)) {
			addCandidate(candidates, descriptor, "sourcesDefinition");
		}
		candidates.sort(Comparator.comparing(candidate -> candidate.descriptor.getDisplayName()));
		return candidates;
	}

	private static void addCandidate(List<Candidate> candidates, PropertyDescriptor descriptor, String kind) {
		if (descriptor.getReadMethod() != null && descriptor.getWriteMethod() != null && !descriptor.isHidden()) {
			candidates.add(new Candidate(descriptor, kind));
		}
	}

	private static Candidate selectCandidate(List<Candidate> candidates, String propertyName) {
		if (propertyName != null && !propertyName.isBlank()) {
			for (Candidate candidate : candidates) {
				if (propertyName.equals(candidate.descriptor.getName())) {
					return candidate;
				}
			}
			return null;
		}
		return candidates.size() == 1 ? candidates.get(0) : null;
	}

	private static JSONArray candidatesToJson(List<Candidate> candidates) throws Exception {
		JSONArray array = new JSONArray();
		for (Candidate candidate : candidates) {
			JSONObject json = new JSONObject();
			json.put("name", candidate.descriptor.getName());
			json.put("displayName", candidate.descriptor.getDisplayName());
			json.put("kind", candidate.kind);
			array.put(json);
		}
		return array;
	}

	private static XMLVector<String> sourceDefinition(String sourcePriority, String xpath) {
		XMLVector<String> sourceDefinition = new XMLVector<>(2);
		sourceDefinition.add(sourcePriority);
		sourceDefinition.add(xpath == null || xpath.isBlank() ? "." : xpath);
		return sourceDefinition;
	}

	private static JSONArray sourceDefinitionToJson(XMLVector<String> sourceDefinition) {
		JSONArray array = new JSONArray();
		for (String item : sourceDefinition) {
			array.put(item);
		}
		return array;
	}

	private static void apply(DatabaseObject dbo, Candidate candidate, XMLVector<String> sourceDefinition)
			throws Exception {
		Method getter = candidate.descriptor.getReadMethod();
		Method setter = candidate.descriptor.getWriteMethod();
		Object oldValue = getter.invoke(dbo);
		Object newValue;

		switch (candidate.kind) {
		case "smartType":
			SmartType smartType = new SmartType();
			smartType.setMode(SmartType.Mode.SOURCE);
			smartType.setSourceDefinition(sourceDefinition);
			smartType.pack();
			setter.invoke(dbo, smartType);
			break;
		case "sourceDefinition":
			setter.invoke(dbo, sourceDefinition);
			break;
		case "sourcesDefinition":
			oldValue = copySourcesDefinition(oldValue);
			setter.invoke(dbo, appendSourceDefinition(oldValue, sourceDefinition));
			break;
		default:
			throw new ServiceException("Unsupported source property " + candidate.descriptor.getName());
		}

		newValue = getter.invoke(dbo);
		dbo.hasChanged = true;
		BuilderUtils.dboChanged(dbo, candidate.descriptor.getName(), oldValue, newValue);
	}

	private static XMLVector<XMLVector<Object>> copySourcesDefinition(Object value) {
		XMLVector<XMLVector<Object>> copy = new XMLVector<>();
		if (value instanceof XMLVector<?> rows) {
			for (Object row : rows) {
				if (row instanceof XMLVector<?> sourceRow) {
					XMLVector<Object> rowCopy = new XMLVector<>();
					rowCopy.addAll(sourceRow);
					copy.add(rowCopy);
				}
			}
		}
		return copy;
	}

	private static XMLVector<XMLVector<Object>> appendSourceDefinition(Object value, XMLVector<String> sourceDefinition) {
		XMLVector<XMLVector<Object>> sourcesDefinition = copySourcesDefinition(value);
		XMLVector<Object> row = new XMLVector<>();
		row.add("");
		row.add(sourceDefinition);
		row.add("");
		sourcesDefinition.add(row);
		return sourcesDefinition;
	}
}
