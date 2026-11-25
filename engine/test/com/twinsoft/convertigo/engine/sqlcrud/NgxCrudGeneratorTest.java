/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sqlcrud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.UIUseVariable;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.Engine;

/** NGX objects generated for the tables of {@link MatrixTables}, on a project built in memory. */
public class NgxCrudGeneratorTest {
	private static final List<String> ALL = List.of("LIST", "COUNT", "INSERT", "SELECT", "UPDATE", "DELETE");
	private static Connection connection;

	@BeforeClass
	public static void open() throws Exception {
		if (Engine.logEngine == null) {
			Engine.logEngine = org.apache.log4j.Logger.getLogger("sql-crud-ui-test");
			Engine.logBeans = Engine.logEngine;
		}
		connection = MatrixTables.open("ngxcrud");
	}

	@AfterClass
	public static void close() throws Exception {
		if (connection != null) {
			connection.close();
		}
	}

	private static SqlTableInfo table(String name) throws Exception {
		return SqlTableInfo.read(connection, SqlTableInfoTest.HSQLDB, name);
	}

	static ApplicationComponent project(String table, List<String> verbs) throws Exception {
		var project = new Project();
		project.setName("crudtest");
		var mobile = new MobileApplication();
		project.add(mobile);
		var app = new ApplicationComponent();
		mobile.add(app);
		for (var verb : verbs) {
			var sequence = new GenericSequence();
			sequence.setName(table + "_" + verb);
			project.add(sequence);
		}
		return app;
	}

	static List<DatabaseObject> tree(DatabaseObject root) {
		var all = new ArrayList<DatabaseObject>();
		all.add(root);
		for (var child : root.getAllChildren()) {
			all.addAll(tree(child));
		}
		return all;
	}

	static <T extends DatabaseObject> List<T> find(DatabaseObject root, Class<T> type) {
		return tree(root).stream().filter(type::isInstance).map(type::cast).toList();
	}

	static DatabaseObject named(DatabaseObject root, String name) {
		return tree(root).stream().filter(d -> d.getName().equals(name)).findFirst().orElse(null);
	}

	static Map<String, String> useVariables(UIUseShared use) {
		return find(use, UIUseVariable.class).stream().filter(v -> v.getParent() == use)
				.collect(Collectors.toMap(UIUseVariable::getName, v -> v.getVarSmartType().getSmartValue()));
	}

	@Test
	public void sharedComponentOfAnEditableTable() throws Exception {
		var app = project("PAIRS", ALL);
		var result = new NgxCrudGenerator(app.getProject(), table("PAIRS")).generate(false);
		assertFalse(result.isSkipped());
		var comp = result.getComponent();
		assertEquals("pairTable", comp.getName());
		assertTrue(app.getSharedComponentList().contains(comp));
		assertNull(result.getPage());

		var variables = find(comp, UICompVariable.class).stream().map(UICompVariable::getName).toList();
		assertEquals(List.of("enableAdd", "enableEdit", "enableView", "enableDelete", "multiSelect", "showId", "hiddenColumns",
				"pageSize", "pagination", "height", "showTitle", "reloadKey", "labels"), variables);
		assertEquals("50", ((UICompVariable) named(comp, "pageSize")).getVariableValue());
		assertEquals("false", ((UICompVariable) named(comp, "pagination")).getVariableValue());
		assertEquals("'600px'", ((UICompVariable) named(comp, "height")).getVariableValue());
		var labels = new JSONObject(((UICompVariable) named(comp, "labels")).getVariableValue());
		assertEquals("Pairs", labels.getString("title"));
		assertEquals("Clear selection", labels.getString("clearSelection"));
		assertEquals("V", labels.getJSONObject("fields").getString("V"));
		assertEquals(List.of("SelectionChanged", "RowSaved", "RowsDeleted", "FormCancelled"),
				find(comp, UICompEvent.class).stream().map(UICompEvent::getName).toList());

		// toolbar: actions shown by the selection
		for (var button : List.of("ClearSelection", "Add", "View", "Edit", "Delete")) {
			assertNotNull(button, named(comp, button + "If"));
		}
		assertEquals("this.crudCanUpdate() && this.crudSelectedCount == 1",
				((com.twinsoft.convertigo.beans.ngx.components.UIControlDirective) named(comp, "EditIf")).getSourceSmartType().getSmartValue());
		var add = find(comp, UICustomAction.class).stream().filter(a -> a.getName().equals("Add")).findFirst().get();
		assertTrue(add.getActionValue().getString().contains("page.crudOpenForm('create', null)"));

		// the grid of the library, in continuous scrolling over the paginated list
		var grid = (UIUseShared) named(comp, "Grid");
		assertEquals(CrudUiDefaults.LIB_GRID, grid.getSharedComponentQName());
		var grids = useVariables(grid);
		assertEquals("'infinite'", grids.get("rowModelType"));
		assertEquals("this.pageSize", grids.get("cacheBlockSize"));
		assertEquals("this.pagination", grids.get("pagination"));
		assertEquals("this.pageSize", grids.get("paginationPageSize"));
		assertEquals("this.crudColumnDefs", grids.get("columnDefs"));
		assertEquals("this.crudMultiSelect() ? 'multiple' : 'single'", grids.get("rowSelection"));
		assertEquals("false", grids.get("suppressRowClickSelection"));
		assertEquals("'ag-theme-quartz'", grids.get("class"));
		var events = find(grid, UIControlEvent.class).stream().map(UIControlEvent::getEventName).toList();
		assertEquals(List.of("GridReady", "GetRows", "SelectionChanged"), events);
		var update = find(grid, UIDynamicInvoke.class).get(0);
		assertEquals(CrudUiDefaults.LIB_GRID_UPDATE_ROWS, update.getSharedActionQName());
		var updates = find(update, UIUseVariable.class).stream()
				.collect(Collectors.toMap(UIUseVariable::getName, v -> v.getVarSmartType().getSmartValue()));
		assertEquals(Map.of("data", "parent.out", "agGridEvent", "event"), updates);

		// the script: sequences, keys, sort keys and rights
		var script = comp.getScriptContent().getString();
		assertTrue(script.contains("list: \"crudtest.PAIRS_LIST\", count: \"crudtest.PAIRS_COUNT\", insert: \"crudtest.PAIRS_INSERT\", update: \"crudtest.PAIRS_UPDATE\", delete: \"crudtest.PAIRS_DELETE\""));
		assertTrue(script.contains("crudCount()"));
		assertFalse(result.getWarnings().stream().anyMatch(w -> w.contains("no row count")));
		assertTrue(script.contains("public crudKeys: string[] = [\"A\", \"B\"];"));
		assertTrue(script.contains("public crudDefaultSortKey: string = \"\\\"A\\\"\";"));
		assertTrue(script.contains("public crudCan: any = {create: true, update: true, delete: true};"));
		assertTrue(script.contains("public crudPaginated: boolean = true;"));
		assertTrue(script.contains("filter: false"));
		assertScriptIsBalanced(script);

		// the form, in a modal of the component, for the creation and the edition
		var form = result.getForm();
		assertEquals("pairForm", form.getName());
		assertTrue(app.getSharedComponentList().contains(form));
		assertEquals(List.of("row", "mode", "showTitle", "labels"), find(form, UICompVariable.class).stream().map(UICompVariable::getName).toList());
		assertEquals(List.of("RowSaved", "FormCancelled"), find(form, UICompEvent.class).stream().map(UICompEvent::getName).toList());
		var modal = (UIDynamicElement) named(comp, "FormModal");
		var modalData = new JSONObject(modal.getBeanData());
		assertEquals("InlineModal", modalData.getString("name"));
		assertEquals("this.crudFormOpen", modalData.getJSONObject("properties").getJSONObject("IsOpen").getString("value"));
		var use = (UIUseShared) named(comp, "Form");
		assertEquals(form.getQName(), use.getSharedComponentQName());
		assertEquals(Map.of("row", "this.crudFormRow", "mode", "this.crudFormMode", "labels", "this.labels"), useVariables(use));
		assertEquals(List.of("RowSaved", "FormCancelled"), find(use, UIControlEvent.class).stream().map(UIControlEvent::getEventName).toList());
		var formScript = form.getScriptContent().getString();
		assertTrue(formScript.contains("public crudSequences: any = {insert: \"crudtest.PAIRS_INSERT\", update: \"crudtest.PAIRS_UPDATE\"};"));
		assertTrue(formScript.contains("crudNormalize()"));
		assertScriptIsBalanced(formScript);
		var input = new JSONObject(((UIDynamicElement) named(form, "Field_A").getAllChildren().get(0)).getBeanData()).getJSONObject("properties");
		assertEquals("crudDraft['A']", input.getJSONObject("DoubleBinding").getString("value"));
		assertEquals("this.mode != 'create'", input.getJSONObject("Readonly").getString("value"));
		var v = new JSONObject(((UIDynamicElement) named(form, "Field_V").getAllChildren().get(0)).getBeanData()).getJSONObject("properties");
		assertEquals("this.mode == 'view'", v.getJSONObject("Readonly").getString("value"));
		assertEquals("number", input.getJSONObject("Type").getString("value"));
		assertEquals("true", input.getJSONObject("Required").getString("value"));
		assertEquals("10", new JSONObject(((UIDynamicElement) named(form, "Field_B").getAllChildren().get(0)).getBeanData())
				.getJSONObject("properties").getJSONObject("MaxLength").getString("value"));
	}

	@Test
	public void binaryColumnsAndComputedDefaults() throws Exception {
		var app = project("MATRIX", ALL);
		var result = new NgxCrudGenerator(app.getProject(), table("MATRIX")).generate(false);
		var comp = result.getComponent();
		assertEquals("matrixTable", comp.getName());
		var script = comp.getScriptContent().getString();
		assertFalse(script.contains("PHOTO"));
		assertTrue(script.contains("public crudCan: any = {create: true, update: false, delete: true};"));
		assertTrue(script.contains("{name: \"FirstName\", kind: \"TEXT\", input: \"text\", key: false, required: true"));
		assertTrue(script.contains("{name: \"PRICE\", kind: \"DECIMAL\", input: \"number\", key: false, required: false, hiddenOnCreate: false, readOnlyOnEdit: false, readOnly: false, maxLength: 0, step: \"0.01\", scale: 2, sortKey: \"\\\"PRICE\\\"\", createDefault: \"0.00\""));
		assertTrue(script.contains("{name: \"ID\", kind: \"INTEGER\", input: \"number\", key: true, required: false, hiddenOnCreate: true, readOnlyOnEdit: true"));
		assertTrue(script.contains("help: \"Free notes\""));
		assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("PHOTO")));
		assertNotNull(named(comp, "Add"));
		assertNull("no edition with a binary column", named(comp, "Edit"));
		assertNotNull(named(comp, "View"));
		assertScriptIsBalanced(script);
		var form = result.getForm();
		assertTrue(form.getScriptContent().getString().contains("{insert: \"crudtest.MATRIX_INSERT\", update: null}"));
		assertNull(named(form, "Field_PHOTO"));
		assertNotNull(named(form, "If_ID"));
		assertNotNull(named(form, "If_UPDATED_AT"));
		assertNull(named(form, "If_CREATED_AT"));
		assertEquals("TextArea", named(form, "Field_NOTES").getAllChildren().get(0).getName());
		assertEquals("Toggle", named(form, "Field_ACTIVE").getAllChildren().get(0).getName());
		var born = new JSONObject(((UIDynamicElement) named(form, "Field_BORN").getAllChildren().get(0)).getBeanData()).getJSONObject("properties");
		assertEquals("date", born.getJSONObject("Type").getString("value"));
		var created = new JSONObject(((UIDynamicElement) named(form, "Field_CREATED_AT").getAllChildren().get(0)).getBeanData()).getJSONObject("properties");
		assertEquals("datetime-local", created.getJSONObject("Type").getString("value"));
		var notes = new JSONObject(((UIDynamicElement) named(form, "Field_NOTES").getAllChildren().get(0)).getBeanData()).getJSONObject("properties");
		assertEquals("Free notes", notes.getJSONObject("HelperText").getString("value"));
	}

	@Test
	public void missingVerbsAdaptTheUserInterface() throws Exception {
		var app = project("PAIRS", List.of("LIST", "DELETE"));
		var result = new NgxCrudGenerator(app.getProject(), table("PAIRS")).generate(false);
		var comp = result.getComponent();
		assertNull(named(comp, "Add"));
		assertNull(named(comp, "Edit"));
		assertNotNull(named(comp, "Delete"));
		var script = comp.getScriptContent().getString();
		assertTrue(script.contains("insert: null, update: null, delete: \"crudtest.PAIRS_DELETE\""));
		assertTrue(script.contains("public crudCan: any = {create: false, update: false, delete: true};"));
		assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("PAIRS_INSERT")));
		assertTrue(script.contains("count: null"));
		assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("no row count: the sequence PAIRS_COUNT is missing")));
	}

	@Test
	public void tableWithoutKeyGivesAReadOnlyList() throws Exception {
		var app = project("LOGS", ALL);
		var result = new NgxCrudGenerator(app.getProject(), table("LOGS")).generate(false);
		var comp = result.getComponent();
		assertEquals("logTable", comp.getName());
		assertNull(named(comp, "Add"));
		assertNull(named(comp, "Edit"));
		assertNull(named(comp, "Delete"));
		assertNotNull("a read-only table can still be viewed", named(comp, "View"));
		assertTrue(comp.getScriptContent().getString().contains("public crudCan: any = {create: false, update: false, delete: false};"));
		assertTrue(result.getWarnings().get(0).startsWith("read-only list"));
		assertNotNull("the form shows a row", result.getForm());
		assertNotNull(named(comp, "FormModal"));
	}

	@Test
	public void noListSequenceNoComponent() throws Exception {
		var app = project("PAIRS", List.of("INSERT", "UPDATE", "DELETE"));
		var result = new NgxCrudGenerator(app.getProject(), table("PAIRS")).generate(true);
		assertTrue(result.isSkipped());
		assertTrue(result.getSkipReason().contains("PAIRS_LIST"));
		assertTrue(app.getSharedComponentList().isEmpty());
		assertTrue(app.getPageComponentList().isEmpty());
	}

	@Test
	public void existingObjectsAreNeverOverwritten() throws Exception {
		var app = project("PAIRS", ALL);
		var existing = new UISharedRegularComponent();
		existing.setName("pairTable");
		app.add(existing);
		var result = new NgxCrudGenerator(app.getProject(), table("PAIRS")).generate(true);
		assertTrue(result.isSkipped());
		assertEquals(List.of(existing), app.getSharedComponentList());
		assertTrue(app.getPageComponentList().isEmpty());

		app = project("PAIRS", ALL);
		var existingForm = new UISharedRegularComponent();
		existingForm.setName("pairForm");
		app.add(existingForm);
		result = new NgxCrudGenerator(app.getProject(), table("PAIRS")).generate(true);
		assertTrue(result.isSkipped());
		assertTrue(result.getSkipReason().contains("pairForm"));
		assertEquals(List.of(existingForm), app.getSharedComponentList());

		app = project("PAIRS", ALL);
		var page = new PageComponent();
		page.setName("Pairs");
		page.setSegment("other");
		app.add(page);
		result = new NgxCrudGenerator(app.getProject(), table("PAIRS")).generate(true);
		assertFalse(result.isSkipped());
		assertNull(result.getPage());
		assertEquals(List.of(page), app.getPageComponentList());
		assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("page Pairs already exists")));
	}

	@Test
	public void samplePage() throws Exception {
		var app = project("MATRIX", ALL);
		var result = new NgxCrudGenerator(app.getProject(), table("MATRIX")).generate(true);
		var page = result.getPage();
		assertNotNull(page);
		assertEquals("Matrix", page.getName());
		assertEquals("matrix", page.getSegment());
		assertEquals("Matrix", page.getTitle());
		assertTrue(page.isInAutoMenu());
		assertTrue(app.getPageComponentList().contains(page));
		var use = find(page, UIUseShared.class).get(0);
		assertEquals(result.getComponent().getQName(), use.getSharedComponentQName());
		assertEquals(Map.of("showTitle", "false"), useVariables(use));
		assertEquals(List.of("SelectionChanged", "RowSaved", "RowsDeleted"),
				find(use, UIControlEvent.class).stream().filter(e -> e.getParent() == use).map(UIControlEvent::getEventName).toList());
		// the sample: the form under the grid shows the selected row kept by the page
		assertEquals("this.local.selectedRow",
				((com.twinsoft.convertigo.beans.ngx.components.UIControlDirective) named(page, "SelectedRowIf")).getSourceSmartType().getSmartValue());
		var below = (UIUseShared) named(page, "matrixForm");
		assertEquals(result.getForm().getQName(), below.getSharedComponentQName());
		assertEquals(Map.of("row", "this.local.selectedRow", "mode", "'view'"), useVariables(below));
		assertEquals(List.of("FormCancelled"), find(below, UIControlEvent.class).stream().map(UIControlEvent::getEventName).toList());
		var keep = find(use, UICustomAction.class).stream().filter(a -> a.getName().equals("KeepSelectedRow")).findFirst().get();
		assertTrue(keep.getActionValue().getString().contains("page.local.selectedRow = event.rows?.length == 1 ? event.rows[0] : null;"));
		assertNotNull(named(page, "MenuButton"));
	}

	@Test
	public void databaseWithoutPaginatedListLoadsEverything() throws Exception {
		var column = new SqlColumnInfo("ID", Types.INTEGER, "INTEGER", 10, 0, true, null, false, false, null);
		var table = new SqlTableInfo("ITEMS", "com.ibm.db2.jcc.DB2Driver", List.of(column), List.of("ID"));
		var app = project("ITEMS", ALL);
		var comp = new NgxCrudGenerator(app.getProject(), table).generate(false).getComponent();
		var grid = (UIUseShared) named(comp, "Grid");
		var grids = useVariables(grid);
		assertEquals("'clientSide'", grids.get("rowModelType"));
		assertEquals("this.crudRows", grids.get("rowData"));
		assertFalse(grids.containsKey("cacheBlockSize"));
		assertFalse(find(grid, UIControlEvent.class).stream().anyMatch(e -> e.getEventName().equals("GetRows")));
		assertTrue(comp.getScriptContent().getString().contains("public crudPaginated: boolean = false;"));
	}

	/** The template writes each expression between double quotes ([attr]="expression"): none may contain one. */
	@Test
	public void templateExpressionsHaveNoDoubleQuote() throws Exception {
		for (var name : List.of("PAIRS", "MATRIX", "LOGS")) {
			var result = new NgxCrudGenerator(project(name, ALL).getProject(), table(name)).generate(true);
			var objects = new ArrayList<DatabaseObject>(tree(result.getComponent()));
			objects.addAll(tree(result.getPage()));
			if (result.getForm() != null) {
				objects.addAll(tree(result.getForm()));
			}
			for (var dbo : objects) {
				var expressions = new ArrayList<String>();
				if (dbo instanceof UICompVariable c) {
					// the default values of shared component variables: no double quote either
					expressions.add(c.getVariableValue());
				} else if (dbo instanceof UIUseVariable v) {
					expressions.add(v.getVarSmartType().getSmartValue());
				} else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIText t) {
					expressions.add(t.getTextSmartType().getSmartValue());
				} else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlDirective d) {
					expressions.add(d.getSourceSmartType().getSmartValue());
				} else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIAttribute a && !(dbo instanceof UIControlEvent)) {
					expressions.add(a.getAttrSmartType().getSmartValue());
				} else if (dbo instanceof UIDynamicElement e && !(dbo instanceof UIDynamicInvoke)) {
					var properties = new JSONObject(e.getBeanData()).getJSONObject("properties");
					for (var it = properties.keys(); it.hasNext();) {
						expressions.add(properties.getJSONObject((String) it.next()).getString("value"));
					}
				}
				for (var expression : expressions) {
					assertFalse(name + " " + dbo.getName() + ": " + expression, expression.contains("\""));
				}
			}
		}
	}

	@Test
	public void names() {
		assertEquals("clientTable", NgxCrudGenerator.componentName("CLIENTS"));
		assertEquals("clientForm", NgxCrudGenerator.formName("clients"));
		assertEquals("client", NgxCrudGenerator.entityName("clients"));
		assertEquals("orderLine", NgxCrudGenerator.entityName("OrderLines"));
		assertEquals("address", NgxCrudGenerator.entityName("address"));
		assertEquals("bus", NgxCrudGenerator.entityName("bus"));
		assertEquals("Clients", NgxCrudGenerator.pageName("CLIENTS"));
		assertEquals("OrderLines", NgxCrudGenerator.pageName("OrderLines"));
		assertEquals("country", NgxCrudGenerator.entityName("countries"));
		assertEquals("auditLogTable", NgxCrudGenerator.componentName("audit_log"));
		assertEquals("AuditLog", NgxCrudGenerator.pageName("audit_log"));
		assertEquals("box", NgxCrudGenerator.entityName("BOXES"));
		assertEquals("status", NgxCrudGenerator.entityName("status"));
		assertEquals("address", NgxCrudGenerator.entityName("addresses"));
	}

	/**
	 * Every template component of the generated objects exists in the NGX template, with the properties it sets. Runs
	 * when the template project is given by -Dconvertigo.test.ngx.template=/path/to/mobilebuilder_tpl_8_4_0_ngx.
	 */
	@Test
	public void templateComponentsExist() throws Exception {
		var template = System.getProperty("convertigo.test.ngx.template");
		if (template == null || !new File(template).isDirectory()) {
			System.out.println("templateComponentsExist: skipped, no convertigo.test.ngx.template");
			return;
		}
		var text = Files.readString(Path.of(template, "ionicTpl", "ion", "ion_objects.json"));
		var beans = new JSONObject(text.replaceAll("(?s)/\\*.*?\\*/", "")).getJSONObject("Beans");
		var app = project("PAIRS", ALL);
		var result = new NgxCrudGenerator(app.getProject(), table("PAIRS")).generate(true);
		var elements = new ArrayList<UIDynamicElement>();
		elements.addAll(find(result.getComponent(), UIDynamicElement.class));
		elements.addAll(find(result.getPage(), UIDynamicElement.class));
		elements.addAll(find(result.getForm(), UIDynamicElement.class));
		var matrix = new NgxCrudGenerator(project("MATRIX", ALL).getProject(), table("MATRIX")).generate(false);
		elements.addAll(find(matrix.getForm(), UIDynamicElement.class));
		assertTrue(elements.size() > 10);
		for (var element : elements) {
			var data = new JSONObject(element.getBeanData());
			var name = data.getString("name");
			assertTrue("unknown template component " + name, beans.has(name));
			var model = beans.getJSONObject(name);
			if (model.has("tag")) {
				assertEquals(name, model.getString("tag"), element.getTagName());
			}
			var properties = data.getJSONObject("properties");
			for (var it = properties.keys(); it.hasNext();) {
				var property = (String) it.next();
				assertTrue(name + " has no property " + property, model.getJSONObject("properties").has(property));
			}
		}
	}

	/**
	 * Writes, for each table, a TypeScript file with the generated component script and actions in a class stub, for a
	 * compilation by tsc. Runs when -Dconvertigo.test.crud.out=/path/to/folder is given.
	 */
	@Test
	public void typeScriptHarness() throws Exception {
		var out = System.getProperty("convertigo.test.crud.out");
		if (out == null) {
			System.out.println("typeScriptHarness: skipped, no convertigo.test.crud.out");
			return;
		}
		Files.createDirectories(Path.of(out));
		for (var name : List.of("MATRIX", "PAIRS", "LOGS", "PAIRS-form", "MATRIX-form")) {
			var tableName = name.replace("-form", "");
			var app = project(tableName, ALL);
			var result = new NgxCrudGenerator(app.getProject(), table(tableName)).generate(false);
			var comp = name.endsWith("-form") ? result.getForm() : result.getComponent();
			var script = comp.getScriptContent().getString();
			var sb = new StringBuilder();
			sb.append("import { AlertController, ToastController, C8oPageBase, ActionBeans } from './stubs';\n\n");
			sb.append("export class Comp extends C8oPageBase {\n");
			for (var variable : find(comp, UICompVariable.class)) {
				sb.append("\tpublic ").append(variable.getName()).append(": any = ").append(variable.getVariableValue()).append(";\n");
			}
			sb.append("\tpublic actionBeans: ActionBeans;\n");
			sb.append(marker(script, "CompDeclaration")).append('\n');
			sb.append("\tngOnInit() {\n").append(marker(script, "CompInitialization")).append("\n\t}\n");
			sb.append("\tngOnChanges(changes: any) {\n").append(marker(script, "CompChanges")).append("\n\t}\n");
			sb.append(marker(script, "CompFunction")).append('\n');
			for (var action : find(comp, UICustomAction.class)) {
				var async = action instanceof com.twinsoft.convertigo.beans.ngx.components.UICustomAsyncAction;
				sb.append('\t').append(async ? "async " : "").append(action.getName()).append("(page: Comp, props: any, vars: any, event: any): Promise<any> {\n");
				if (async) {
					sb.append(action.getActionValue().getString());
				} else {
					sb.append("\t\treturn new Promise((resolve, reject) => {\n").append(action.getActionValue().getString()).append("\t\t});\n");
				}
				sb.append("\t}\n");
			}
			sb.append("}\n");
			Files.writeString(Path.of(out, name.toLowerCase() + ".ts"), sb.toString());
		}
	}

	/**
	 * Writes the HTML templates the NGX builder computes for the generated objects, with the components of the NGX
	 * template project. Runs with -Dconvertigo.test.ngx.template and -Dconvertigo.test.crud.out.
	 */
	@Test
	public void htmlTemplates() throws Exception {
		var template = System.getProperty("convertigo.test.ngx.template");
		var out = System.getProperty("convertigo.test.crud.out");
		if (template == null || out == null) {
			System.out.println("htmlTemplates: skipped");
			return;
		}
		com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.addIonicTemplateProject("crudtpl", new File(template));
		for (var name : List.of("PAIRS", "MATRIX")) {
			var app = project(name, ALL);
			app.setTplProjectName("crudtpl");
			var result = new NgxCrudGenerator(app.getProject(), table(name)).generate(true);
			for (var comp : List.of(result.getComponent(), result.getForm())) {
				Files.writeString(Path.of(out, comp.getName() + ".html"), comp.computeTemplate());
			}
			Files.writeString(Path.of(out, result.getPage().getName() + ".page.html"), result.getPage().computeTemplate());
		}
	}

	private static String marker(String script, String marker) {
		var begin = script.indexOf("/*Begin_c8o_" + marker + "*/") + ("/*Begin_c8o_" + marker + "*/").length();
		return script.substring(begin, script.indexOf("/*End_c8o_" + marker + "*/"));
	}

	/** Same number of opening and closing braces, brackets and parentheses, and every marker closed. */
	static void assertScriptIsBalanced(String script) {
		var code = script.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"");
		for (var pair : new String[] { "{}", "[]", "()" }) {
			var open = code.chars().filter(c -> c == pair.charAt(0)).count();
			var close = code.chars().filter(c -> c == pair.charAt(1)).count();
			assertEquals("balanced " + pair, open, close);
		}
		for (var marker : List.of("CompImport", "CompDeclaration", "CompConstructor", "CompInitialization", "CompFinalization",
				"CompChanges", "CompDoCheck", "CompAfterContentInit", "CompAfterContentChecked", "CompAfterViewInit",
				"CompAfterViewChecked", "CompFunction")) {
			var begin = script.indexOf("/*Begin_c8o_" + marker + "*/");
			var end = script.indexOf("/*End_c8o_" + marker + "*/");
			assertTrue(marker, begin >= 0 && end > begin);
		}
	}
}
