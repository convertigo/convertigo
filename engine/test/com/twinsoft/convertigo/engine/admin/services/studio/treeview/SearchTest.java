/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.PdfFormStep;
import com.twinsoft.convertigo.beans.steps.IfStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.steps.XMLGenerateDatesStep;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.util.DatabaseObjectSearchFilters;

public class SearchTest {
	private Engine previous;
	private Project project;
	private GenericSequence sequence;
	private XMLCopyStep active;
	private XMLCopyStep inactive;

	@Before
	public void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("search-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		project = new Project();
		project.setName("SearchProject");
		project.isSubLoaded = true;
		sequence = new GenericSequence();
		sequence.setName("SearchSequence");
		sequence.isSubLoaded = true;
		project.add(sequence);
		active = step("Active");
		inactive = step("Inactive");
		inactive.setEnabled(false);
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			{ symbolsProperties = new Properties(); }
			@Override
			public DatabaseObject getDatabaseObjectByQName(String name) {
				return project.getName().equals(name) ? project : sequence.getQName().equals(name) ? sequence : null;
			}
			@Override
			public List<String> getAllProjectNamesList(boolean checkOpenable) {
				return List.of("Unavailable", project.getName());
			}
			@Override
			public Project getOriginalProjectByName(String name, boolean checkOpenable) {
				return project.getName().equals(name) ? project : null;
			}
		};
	}

	@After
	public void restoreEngine() {
		Engine.theApp = previous;
	}

	@Test
	public void typeOnlySearchIncludesActiveAndInactiveObjects() throws Exception {
		assertEquals(2, search(Map.of("type", "Step", "scope", sequence.getQName(), "regExp", "true"))
				.getJSONArray("results").length());
	}

	@Test
	public void filterOnlySearchSkipsUnavailableWorkspaceProjects() throws Exception {
		var results = search(Map.of("inactive", "true")).getJSONArray("results");
		assertEquals(1, results.length());
		assertEquals(inactive.getQName(true), results.getJSONObject(0).getString("id"));
		assertEquals(project.getName(), results.getJSONObject(0).getString("project"));
	}

	@Test
	public void filtersApplyToOwnPropertiesAndStillVisitChildren() throws Exception {
		inactive.setEnabled(true);
		var parent = new IfStep();
		parent.isSubLoaded = true;
		parent.setEnabled(false);
		sequence.addStep(parent);
		var child = new XMLCopyStep();
		parent.addStep(child);
		assertEquals(1, search(Map.of("inactive", "true", "type", "Step")).getJSONArray("results").length());
		child.setEnabled(false);
		assertEquals(2, search(Map.of("inactive", "true", "type", "Step")).getJSONArray("results").length());
	}

	@Test
	public void filtersAndTextAreCombinedWithAnd() throws Exception {
		active.setCompilablePropertySourceValue("comment", "${known=default}");
		inactive.setCompilablePropertySourceValue("comment", "${missing}");
		inactive.addSymbolError("comment", Set.of("missing"));
		assertEquals(2, search(Map.of("symbols", "true")).getJSONArray("results").length());
		assertEquals(1, search(Map.of("symbols", "true", "unknownSymbols", "true", "inactive", "true"))
				.getJSONArray("results").length());
		assertEquals(0, search(Map.of("symbols", "true", "inactive", "true", "text", "Active", "matchCase", "true"))
				.getJSONArray("results").length());
	}

	@Test
	public void unknownSymbolsCountAsSymbolUsageWithoutRetainedSource() throws Exception {
		inactive.addSymbolError("comment", Set.of("missing"));
		assertEquals(1, search(Map.of("symbols", "true", "unknownSymbols", "true")).getJSONArray("results").length());
	}

	@Test
	public void brokenSourceFilterCoversClassicPdfAndStepVariables() throws Exception {
		inactive.setSourceDefinition(definition("invalid"));
		var pdf = new PdfFormStep();
		pdf.setName("Pdf");
		sequence.addStep(pdf);
		var fields = new SmartType();
		fields.setMode(SmartType.Mode.SOURCE);
		fields.setSourceDefinition(definition("-1"));
		pdf.setFields(fields);
		assertEquals(2, search(Map.of("brokenSources", "true")).getJSONArray("results").length());
		var variable = new StepVariable();
		variable.setParent(inactive);
		variable.setSourceDefinition(definition("-2"));
		assertTrue(new DatabaseObjectSearchFilters(true, false, false, false).matches(variable));
		variable.setSourceDefinition(definition(Long.toString(active.priority)));
		assertFalse(new DatabaseObjectSearchFilters(true, false, false, false).matches(variable));
		fields.setMode(SmartType.Mode.PLAIN);
		inactive.setSourceDefinition(new XMLVector<>());
		assertEquals(0, search(Map.of("brokenSources", "true")).getJSONArray("results").length());
	}

	@Test
	public void additionalDateSourcesWorkWithoutEclipseEditorClasses() throws Exception {
		var dates = new XMLGenerateDatesStep();
		dates.setName("Dates");
		dates.setStartDefinition(definition("-1"));
		sequence.addStep(dates);
		assertEquals(1, search(Map.of("brokenSources", "true")).getJSONArray("results").length());
		dates.setStartDefinition(definition(Long.toString(active.priority)));
		assertEquals(0, search(Map.of("brokenSources", "true")).getJSONArray("results").length());
	}

	@Test
	public void legacyMultiSourceTextAndMalformedRowsDoNotAbortSearch() throws Exception {
		var concat = new XMLConcatStep();
		var rows = new XMLVector<XMLVector<Object>>();
		for (Object source : new Object[] { "literal", new XMLVector<String>(), definition("-1") }) {
			var row = new XMLVector<Object>();
			row.add("description");
			row.add(source);
			row.add("");
			rows.add(row);
		}
		rows.add(new XMLVector<>());
		concat.setSourcesDefinition(rows);
		sequence.addStep(concat);
		assertEquals(1, search(Map.of("brokenSources", "true")).getJSONArray("results").length());
	}

	@Test
	public void textCaseAndRegularExpressionKeepTheirExistingBehavior() throws Exception {
		assertEquals(1, search(Map.of("type", "Step", "text", "\\bActive\\b", "regExp", "true", "matchCase", "true"))
				.getJSONArray("results").length());
		assertEquals(2, search(Map.of("type", "Step", "text", "active")).getJSONArray("results").length());
		assertEquals(0, search(Map.of("type", "Step", "text", "ACTIVE", "matchCase", "true"))
				.getJSONArray("results").length());
	}

	@Test
	public void resultLimitOnlyTruncatesWhenAnotherMatchExists() throws Exception {
		assertTrue(search(Map.of("type", "Step", "limit", "1")).getBoolean("truncated"));
		assertFalse(search(Map.of("type", "Step", "limit", "2")).getBoolean("truncated"));
	}

	@Test
	public void invalidRequestsReportCriteriaRegexAndScopeErrors() {
		assertThrows(ServiceException.class, () -> search(Map.of()));
		assertThrows(ServiceException.class, () -> search(Map.of("text", "[", "regExp", "true")));
		assertThrows(ServiceException.class, () -> search(Map.of("type", "Step", "scope", "Missing")));
		assertThrows(IllegalArgumentException.class, () -> search(Map.of("type", "Missing")));
	}

	private XMLCopyStep step(String name) throws Exception {
		var step = new XMLCopyStep();
		step.setName(name);
		sequence.addStep(step);
		return step;
	}

	private static XMLVector<String> definition(String priority) {
		var definition = new XMLVector<String>();
		definition.add(priority);
		definition.add(".");
		return definition;
	}

	private JSONObject search(Map<String, String> parameters) throws Exception {
		var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {
					if ("getParameter".equals(method.getName())) return parameters.get(args[0]);
					throw new UnsupportedOperationException(method.getName());
				});
		var response = new JSONObject();
		new Search().getServiceResult(request, response);
		return response;
	}
}
