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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.JdbcConnectionManager;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * The values sent by the generated user interface, run through the CRUD transactions generated today
 * ({@link SqlConnector#createSqlTransaction}), unchanged. HSQLDB in memory by default; PostgreSQL with
 * -Dconvertigo.test.crud.postgresql.url=jdbc:postgresql://host:port/db (and .user, .password).
 */
public class SqlCrudContractTest {
	private static final String PG_URL = System.getProperty("convertigo.test.crud.postgresql.url");
	private Engine previousEngine;
	private String previousProjects;
	@Rule public TemporaryFolder workspace = new TemporaryFolder();
	private SqlConnector connector;
	private Context context;
	private final Map<String, SqlTransaction> transactions = new LinkedHashMap<>();

	@Before
	public void setUp() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = Engine.logBeans = Engine.logContext = Engine.logDatabaseObjectManager = Engine.logStatistics =
				org.apache.log4j.Logger.getLogger("sql-crud-contract-test");
		previousEngine = Engine.theApp;
		previousProjects = Engine.PROJECTS_PATH;
		Engine.PROJECTS_PATH = workspace.newFolder("projects").getPath();
		Engine.theApp = new Engine();
		Engine.theApp.sqlConnectionManager = new JdbcConnectionManager();
		Engine.theApp.sqlConnectionManager.init();
		var project = new Project();
		project.setName("crudcontract");
		connector = new SqlConnector();
		connector.setName("db");
		if (PG_URL != null) {
			connector.setJdbcDriverClassName("org.postgresql.Driver");
			connector.setJdbcURL(PG_URL);
			connector.setJdbcUserName(System.getProperty("convertigo.test.crud.postgresql.user", "postgres"));
			connector.setJdbcUserPassword(System.getProperty("convertigo.test.crud.postgresql.password", ""));
		} else {
			connector.setJdbcDriverClassName("org.hsqldb.jdbc.JDBCDriver");
			connector.setJdbcURL(System.getProperty("convertigo.test.crud.hsqldb.url", "jdbc:hsqldb:mem:crudcontract" + System.nanoTime()));
			connector.setJdbcUserName("SA");
			connector.setSystemTablesQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
		}
		project.add(connector);
		context = new Context("sql-crud-contract-test");
		context.project = project;
		context.setConnector(connector);

		connector.open();
		try (var st = connector.connection.createStatement()) {
			st.execute("DROP TABLE IF EXISTS items");
			st.execute(PG_URL != null
					? "CREATE TABLE items (id serial PRIMARY KEY, label varchar(20) NOT NULL, note varchar(50), qty integer,"
						+ " price numeric(10,2), active boolean, born date, seen timestamp(6), at_time time, code uuid DEFAULT gen_random_uuid())"
					: "CREATE TABLE items (id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, label varchar(20) NOT NULL, note varchar(50), qty integer,"
						+ " price decimal(10,2), active boolean, born date, seen timestamp(6), at_time time, code UUID)");
		}
		generate(column("items"), List.of("LIST", "COUNT", "INSERT", "UPDATE", "DELETE"));
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override
			public Project getOriginalProjectByName(String projectName, boolean checkOpenable) {
				return project;
			}
		};
	}

	private void generate(String name, List<String> verbs) throws Exception {
		useTransactions(SqlConnector.createSqlTransaction(connector, name, verbs));
	}

	private void useTransactions(List<SqlTransaction> generated) throws Exception {
		SqlCrudGeneration.installTransactions(connector, generated, false, null, false, false);
		for (var transaction : generated) {
			transactions.put(transaction.getName().substring(transaction.getName().lastIndexOf('_') + 1), transaction);
		}
	}

	private void execute(String... statements) throws Exception {
		connector.open();
		try (var statement = connector.connection.createStatement()) {
			for (var sql : statements) {
				statement.execute(sql);
			}
		} finally {
			connector.close();
		}
	}

	@After
	public void tearDown() throws Exception {
		connector.close();
		Engine.theApp.sqlConnectionManager.destroy();
		Engine.theApp = previousEngine;
		Engine.PROJECTS_PATH = previousProjects;
	}

	/** Runs a generated transaction with the variables the user interface sends, as the engine does. */
	private Document run(String verb, Map<String, String> variables) throws Exception {
		var transaction = transactions.get(verb);
		transaction.variables.clear();
		transaction.variables.putAll(variables);
		var output = XMLUtils.getDefaultDocumentBuilder().newDocument();
		output.appendChild(output.createElement("document"));
		context.outputDocument = output;
		context.requestedObject = context.transaction = transaction;
		transaction.context = context;
		transaction.createRequestableThread();
		transaction.runCore();
		return output;
	}

	private static String error(Document output) {
		NodeList errors = output.getElementsByTagName("error");
		if (errors.getLength() == 0) {
			return null;
		}
		var message = ((Element) errors.item(0)).getElementsByTagName("message");
		return message.getLength() > 0 ? message.item(0).getTextContent() : errors.item(0).getTextContent();
	}

	private List<String> labels(Document output) {
		var labels = new ArrayList<String>();
		var rows = output.getElementsByTagName("row");
		for (int i = 0; i < rows.getLength(); i++) {
			for (var child = rows.item(i).getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child instanceof Element e && e.getTagName().equalsIgnoreCase("label")) {
					labels.add(e.getTextContent());
				}
			}
		}
		return labels;
	}

	private String select(String sql) throws Exception {
		connector.open();
		try (var st = connector.connection.createStatement(); var rs = st.executeQuery(sql)) {
			var sb = new StringBuilder();
			var columns = rs.getMetaData().getColumnCount();
			while (rs.next()) {
				for (int i = 1; i <= columns; i++) {
					var value = rs.getString(i);
					// booleans are TRUE for HSQLDB and t for PostgreSQL
					value = "t".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) ? "true"
							: "f".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) ? "false" : value;
					sb.append(i > 1 ? "|" : "").append(value);
				}
				sb.append(';');
			}
			return sb.toString();
		}
	}

	@Test
	public void ordinarySqlExpandsFragmentsBeforeBinding() throws Exception {
		var transaction = connector.newTransaction();
		transaction.setName("ordinary");
		connector.add(transaction);
		transaction.setSqlQuery("SELECT label FROM items WHERE {{predicate}}");
		transaction.variables.put("predicate", "{{column}} = '{label}' OR {{column}} = {other}");
		transaction.variables.put("column", "label");
		transaction.variables.put("label", "first");
		transaction.variables.put("other", "second");
		var query = transaction.initializeQueries(true).get(0);
		assertEquals("SELECT label FROM items WHERE label = ? OR label = ?", query.getQuery());
		assertEquals(List.of("label", "other"), query.getOrderedParametersList());
		transaction.variables.put("predicate", "{{column}}");
		transaction.variables.put("column", "{{predicate}}");
		assertThrows(EngineException.class, () -> transaction.initializeQueries(false));
	}

	@Test
	public void overridePreservesCustomizedTransactionWithOrWithoutWrapperGeneration() throws Exception {
		var update = transactions.get("UPDATE");
		var wrapper = SqlCrudGeneration.createSequence(update, Accessibility.Public, false);
		var query = update.getSqlQuery() + " AND 1 = 0";
		update.setSqlQuery(query);

		for (var accessibility : new Accessibility[] {Accessibility.Public, null}) {
			var replacement = SqlConnector.createSqlTransaction(connector, column("items"), List.of("UPDATE")).get(0);
			assertThrows(EngineException.class, () -> SqlCrudGeneration.installTransactions(connector,
					List.of(replacement), true, accessibility, false, accessibility != null));
			assertSame(update, connector.getTransactionByName(update.getName()));
			assertSame(wrapper, update.getProject().getSequenceByName(update.getName()));
			assertEquals(query, update.getSqlQuery());
		}
	}

	@Test
	public void overridePreservesChangedTransactionVariablesAndSettings() throws Exception {
		var update = transactions.get("UPDATE");
		var replacement = SqlConnector.createSqlTransaction(connector, column("items"), List.of("UPDATE")).get(0);
		var variable = update.getVariable("crud_fields");
		variable.setValueOrNull("label");
		assertThrows(EngineException.class, () -> SqlCrudGeneration.installTransactions(connector,
				List.of(replacement), true, null, false, false));
		variable.setValueOrNull(replacement.getVariable("crud_fields").getValueOrNull());
		update.setMaxResult("1");
		assertThrows(EngineException.class, () -> SqlCrudGeneration.installTransactions(connector,
				List.of(replacement), true, null, false, false));
		assertSame(update, connector.getTransactionByName(update.getName()));
	}

	@Test
	public void regeneratedSequenceTakesTheChosenSettings() throws Exception {
		var transaction = transactions.get("LIST");
		var hidden = SqlCrudGeneration.createSequence(transaction, Accessibility.Hidden, true);
		assertEquals(Accessibility.Hidden, hidden.getAccessibility());
		assertTrue(hidden.getAuthenticatedContextRequired());
		assertSame(hidden, SqlCrudGeneration.createSequence(transaction, Accessibility.Public, false));
		assertEquals(Accessibility.Hidden, hidden.getAccessibility());

		var replacement = SqlConnector.createSqlTransaction(connector, column("items"), List.of("LIST")).get(0);
		assertTrue(SqlCrudGeneration.installTransactions(connector, List.of(replacement), true, Accessibility.Public, false, true));
		var regenerated = replacement.getProject().getSequenceByName(replacement.getName());
		assertEquals(Accessibility.Public, regenerated.getAccessibility());
		assertFalse(regenerated.getAuthenticatedContextRequired());
		assertEquals(2, regenerated.getAllSteps().size());
		assertEquals(1, replacement.getProject().getSequencesList().size());
	}

	@Test
	public void metadataOfTheDatabase() throws Exception {
		connector.open();
		try (var st = connector.connection.createStatement()) {
			st.execute("DROP TABLE IF EXISTS kinds");
			st.execute(PG_URL != null
					? "CREATE TABLE kinds (id serial PRIMARY KEY, code uuid DEFAULT gen_random_uuid(), name varchar(40) NOT NULL,"
						+ " body text, amount numeric(12,3) NOT NULL DEFAULT 0, ok boolean NOT NULL DEFAULT false, data jsonb, pic bytea,"
						+ " at timestamptz NOT NULL DEFAULT now(), status varchar(10) DEFAULT 'draft', big bigint, flags bit(4))"
					: "CREATE TABLE kinds (id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name varchar(40) NOT NULL)");
		}
		var table = SqlTableInfo.read(connector.connection, connector.getJdbcDriverClassName(), PG_URL != null ? "kinds" : "KINDS");
		for (var column : table.getColumns()) {
			System.out.println("column " + column + " required=" + column.isRequired() + " hiddenOnCreate=" + column.isHiddenOnCreate()
					+ " createDefault=" + column.getCreateDefault());
		}
		table.getWarnings().forEach(w -> System.out.println("warning " + w));
		assertEquals(PG_URL != null ? 12 : 2, table.getColumns().size());
	}

	private String column(String name) {
		return PG_URL != null ? name : name.toUpperCase();
	}

	private Map<String, String> fullRow(String label) {
		var full = new LinkedHashMap<String, String>();
		full.put(column("label"), label);
		full.put(column("note"), "a note");
		full.put(column("qty"), "3");
		full.put(column("price"), "12.50");
		full.put(column("active"), "true");
		full.put(column("born"), "2026-11-24");
		full.put(column("seen"), "2026-11-24 10:11:12");
		full.put(column("at_time"), "10:11:00");
		return full;
	}

	@Test
	public void generatedQueriesTurnEmptyValuesIntoNull() {
		var insert = transactions.get("INSERT").getSqlQuery();
		var update = transactions.get("UPDATE").getSqlQuery();
		System.out.println(insert + "\n" + update);
		var qty = PG_URL != null ? "CAST(NULLIF({qty}, '') AS int4)" : "NULLIF(CAST({QTY} AS LONGVARCHAR), '')";
		var code = PG_URL != null ? "CAST(NULLIF({code}, '') AS uuid)" : "NULLIF(CAST({CODE} AS LONGVARCHAR), '')";
		var note = PG_URL != null ? "NULLIF({note}, '')" : "NULLIF(CAST({NOTE} AS LONGVARCHAR), '')";
		var label = "{" + column("label") + "}";
		assertTrue(insert, insert.contains(qty));
		assertTrue(insert, insert.contains(code));
		assertTrue(insert, insert.contains(note));
		assertTrue(insert, insert.contains(label) && !insert.contains("NULLIF(" + label));
		assertTrue(update, update.contains("SET {{crud_update}}"));
		assertTrue(update, update.contains("WHERE \"" + column("id") + "\" = {" + column("id") + "}"));
	}

	@Test
	public void emptyFieldsAreSavedAsNull() throws Exception {
		// only the required column: the others are not sent, as emptied fields of the form
		assertNull(error(run("INSERT", Map.of(column("label"), "empty"))));
		assertEquals("empty|true|true|true|true|true;",
				select("SELECT label, qty IS NULL, born IS NULL, note IS NULL, price IS NULL, code IS NULL FROM items"));

		// a full row, every value as a string (crudNormalize), then emptied by an update
		assertNull(error(run("INSERT", fullRow("full"))));
		var id = select("SELECT max(id) FROM items").replace(";", "");
		assertEquals("full|3|12.50|2026-11-24|a note;", select("SELECT label, qty, price, born, note FROM items WHERE id = " + id));
		var emptied = new LinkedHashMap<String, String>();
		emptied.put(column("id"), id);
		emptied.put(column("label"), "emptied");
		emptied.put(column("qty"), "");
		emptied.put(column("born"), "");
		emptied.put(column("note"), "");
		emptied.put("crud_fields", String.join(",", List.of(column("label"), column("qty"), column("born"), column("note"))));
		assertNull(error(run("UPDATE", emptied)));
		assertEquals("emptied|true|true|true|true;", select("SELECT label, qty IS NULL, born IS NULL, note IS NULL, seen IS NOT NULL FROM items WHERE id = " + id));
	}

	@Test
	public void listUpdateAndDelete() throws Exception {
		for (var label : List.of("first", "second", "third")) {
			assertNull(error(run("INSERT", fullRow(label))));
		}
		// the list, sorted by the quoted sort key of the grid, a block at a time
		var sortKey = column("label");
		var list = run("LIST", Map.of("sort_key", sortKey, "sort_direction", "desc", "limit", "2", "offset", "0"));
		assertNull(error(list));
		assertEquals(List.of("third", "second"), labels(list));
		var next = run("LIST", Map.of("sort_key", sortKey, "sort_direction", "desc", "limit", "2", "offset", "2"));
		assertEquals(List.of("first"), labels(next));

		// the count of the rows of the list, read as the grid reads it: the first value of the row
		var count = transactions.get("COUNT");
		assertTrue(count.getSqlQuery(), count.getSqlQuery().startsWith("SELECT COUNT(*) AS total\nFROM "));
		var total = run("COUNT", Map.of());
		assertNull(error(total));
		var row = (Element) total.getElementsByTagName("row").item(0);
		assertEquals("3", ((Element) row.getElementsByTagName("*").item(0)).getTextContent());

		var id = select("SELECT min(id) FROM items").replace(";", "");
		var update = new LinkedHashMap<>(fullRow("first updated"));
		update.put(column("id"), id);
		update.put("crud_fields", column("label"));
		assertNull(error(run("UPDATE", update)));
		assertEquals("first updated;", select("SELECT label FROM items WHERE id = " + id));
		assertNull(error(run("DELETE", Map.of(column("id"), id))));
		assertEquals("2;", select("SELECT count(*) FROM items"));
	}
	@Test
	public void businessSequenceSurvivesRegeneration() throws Exception {
		var transaction = transactions.get("UPDATE");
		var project = transaction.getProject();
		var business = new GenericSequence();
		business.setName(transaction.getName());
		business.setAccessibility(Accessibility.Private);
		business.setAuthenticatedContextRequired(true);
		project.add(business);
		var step = new XMLCopyStep();
		step.setName("BusinessRule");
		business.add(step);
		var variable = new RequestableVariable();
		variable.setName("rule");
		business.add(variable);

		assertThrows(EngineException.class, () -> SqlCrudGeneration.createSequence(transaction, Accessibility.Public, false));
		assertSame(business, project.getSequenceByName(transaction.getName()));
		assertSame(step, business.getAllSteps().get(0));
		assertSame(variable, business.getVariable("rule"));
		assertEquals(Accessibility.Private, business.getAccessibility());
		assertTrue(business.getAuthenticatedContextRequired());
		assertSame(transaction, connector.getTransactionByName(transaction.getName()));
	}

	@Test
	public void failedSequencePreparationLeavesPreviousObjectsInPlace() throws Exception {
		var update = transactions.get("UPDATE");
		var project = update.getProject();
		var original = SqlCrudGeneration.createSequence(update, Accessibility.Hidden, true);
		var list = transactions.get("LIST");
		var candidates = SqlConnector.createSqlTransaction(connector, column("items"), List.of("LIST", "UPDATE"));
		var invalidName = new AtomicBoolean();
		var brokenVariable = new RequestableVariable() {
			@Override
			public String getName() {
				return invalidName.get() ? "invalid name" : super.getName();
			}
		};
		brokenVariable.setName("testVariable");
		candidates.get(1).addVariable(brokenVariable);
		invalidName.set(true);
		assertThrows(EngineException.class, () ->
				SqlCrudGeneration.installTransactions(connector, candidates, true, Accessibility.Public, false, true));
		assertSame(list, connector.getTransactionByName(list.getName()));
		assertSame(update, connector.getTransactionByName(update.getName()));
		assertSame(original, project.getSequenceByName(update.getName()));
		assertEquals(1, project.getSequencesList().size());
		assertEquals(Accessibility.Hidden, original.getAccessibility());
		assertTrue(original.getAuthenticatedContextRequired());
	}

	@Test
	public void paginatedListRejectsOutsideItsInputContract() throws Exception {
		for (var input : List.of(Map.of("sort_key", "UnknownColumn"), Map.of("sort_direction", "up"),
				Map.of("limit", "0"), Map.of("limit", "1001"), Map.of("limit", "1.5"), Map.of("offset", "-1"))) {
			assertThrows(input.toString(), EngineException.class, () -> run("LIST", input));
		}
	}

	@Test
	public void generatedQueryRejectsAnUnresolvedMarker() throws Exception {
		var list = transactions.get("LIST");
		list.setSqlQuery(list.getSqlQuery() + "\n{{missing_marker}}");
		var failure = assertThrows(EngineException.class, () -> list.initializeQueries(false));
		assertTrue(failure.getMessage().contains("Unresolved marker"));
	}

	@Test
	public void importedLegacyListKeepsItsOrderByInput() throws Exception {
		execute("CREATE TABLE legacy_notes (label VARCHAR(20))",
				"INSERT INTO legacy_notes VALUES ('first'), ('second')");
		var original = connector.newTransaction();
		original.setName(column("legacy_notes") + "_LIST");
		original.setSqlQuery("SELECT label FROM legacy_notes ORDER BY {{order_by}}");
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var definition = original.toXml(document);
		document.appendChild(definition);
		definition.setAttribute("version", "8.4.7");
		for (var child = definition.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element property && property.getAttribute("name").equals("crudQuery")) {
				definition.removeChild(child);
				break;
			}
		}
		var legacy = (SqlTransaction) com.twinsoft.convertigo.beans.core.DatabaseObject.read(definition);
		connector.add(legacy);
		transactions.put("LEGACY", legacy);
		assertEquals(List.of("second", "first"), labels(run("LEGACY", Map.of("order_by", "label DESC"))));
		var table = SqlTableInfo.read(connector, column("legacy_notes"));
		var replacement = SqlConnector.createSqlTransactions(connector, table, List.of("LIST"));
		assertThrows(EngineException.class, () -> SqlCrudGeneration.installTransactions(connector,
				replacement, true, Accessibility.Public, false, true));
		assertSame(legacy, connector.getTransactionByName(legacy.getName()));
	}

	@Test
	public void legacyWritesAreReportedWhenTheTableNoLongerAllowsThem() throws Exception {
		execute("CREATE TABLE legacy_notes (label VARCHAR(20))");
		var table = SqlTableInfo.read(connector, column("legacy_notes"));
		var legacy = connector.newTransaction();
		legacy.setName(column("legacy_notes") + "_UPDATE");
		legacy.setSqlQuery("UPDATE legacy_notes SET label = {label}");
		connector.add(legacy);
		var wrapper = SqlCrudGeneration.createSequence(legacy, Accessibility.Public, false);
		var warnings = SqlCrudGeneration.legacyMigrationWarnings(connector, table);
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains(legacy.getQName()));
		assertTrue(warnings.get(0).contains(wrapper.getQName()));
		assertTrue(warnings.get(0).contains("generator omits it"));
		assertSame(legacy, connector.getTransactionByName(legacy.getName()));
	}

	@Test
	public void noWritesAreGeneratedForAKeylessTable() throws Exception {
		execute("DROP TABLE IF EXISTS logs_review", "CREATE TABLE logs_review (msg VARCHAR(20))",
				"INSERT INTO logs_review VALUES ('same'), ('same')");
		var generated = SqlConnector.createSqlTransaction(connector, column("logs_review"),
				List.of("LIST", "COUNT", "INSERT", "UPDATE", "DELETE"));
		for (var transaction : generated) {
			connector.add(transaction);
			SqlCrudGeneration.createSequence(transaction, Accessibility.Hidden, false);
		}
		var name = column("logs_review");
		assertEquals(List.of(name + "_LIST", name + "_COUNT"), generated.stream().map(SqlTransaction::getName).toList());
		assertEquals(2, connector.getProject().getSequencesList().size());
		assertFalse(generated.get(0).getSqlQuery().contains("LIMIT"));
	}

	@Test
	public void selectedTableIdentitySurvivesDiscoveryAndSqlGeneration() throws Exception {
		var tableName = "Order_Table" + System.nanoTime();
		var wildcardNeighbor = tableName.replace("_", "X");
		var firstSchema = "Review One " + System.nanoTime();
		var secondSchema = "Review Two " + System.nanoTime();
		if (PG_URL != null) {
			execute("CREATE SCHEMA IF NOT EXISTS \"" + firstSchema + "\"",
					"CREATE SCHEMA IF NOT EXISTS \"" + secondSchema + "\"");
		} else {
			execute("CREATE SCHEMA \"" + firstSchema + "\"", "CREATE SCHEMA \"" + secondSchema + "\"");
		}
		var first = "\"" + firstSchema + "\".\"" + tableName + "\"";
		var second = "\"" + secondSchema + "\".\"" + tableName + "\"";
		execute("CREATE TABLE " + first + " (id INT PRIMARY KEY, label VARCHAR(20))",
				"CREATE TABLE " + second + " (id INT PRIMARY KEY, other INT)",
				"CREATE TABLE \"" + firstSchema + "\".\"" + wildcardNeighbor + "\" (id INT PRIMARY KEY, surprise INT)",
				"INSERT INTO " + first + " VALUES (1, 'first')", "INSERT INTO " + second + " VALUES (1, 99)");
		var discovered = SqlConnector.executeSearch(connector, tableName, List.of("TABLE"));
		var names = discovered.getElementsByTagName("NAME");
		var found = new LinkedHashMap<String, Element>();
		for (int i = 0; i < names.getLength(); i++) {
			var name = (Element) names.item(i);
			if (tableName.equals(name.getTextContent())) {
				found.put(name.getAttribute("schema"), name);
			}
		}
		assertEquals(2, found.size());
		assertThrows(SQLException.class, () -> SqlConnector.createSqlTransaction(connector, tableName, List.of("LIST")));
		var node = found.get(firstSchema);
		var identity = new SqlTableIdentity(node.hasAttribute("catalog") ? node.getAttribute("catalog") : null, firstSchema, tableName);
		var metadata = SqlTableInfo.read(connector, identity);
		assertEquals(List.of(column("id"), column("label")), metadata.getColumns().stream().map(SqlColumnInfo::getName).toList());
		var generated = SqlConnector.createSqlTransactions(connector, metadata, List.of("LIST"));
		SqlCrudGeneration.installTransactions(connector, generated, false, null, false, false);
		transactions.put("LIST", generated.get(0));
		assertEquals(List.of("first"), labels(run("LIST", Map.of())));
		var otherNode = found.get(secondSchema);
		var other = new SqlTableIdentity(otherNode.hasAttribute("catalog") ? otherNode.getAttribute("catalog") : null, secondSchema, tableName);
		var otherTransactions = SqlConnector.createSqlTransactions(connector, SqlTableInfo.read(connector, other), List.of("LIST"));
		assertThrows(EngineException.class, () -> SqlCrudGeneration.installTransactions(connector, otherTransactions, true, null, false, false));
		assertSame(generated.get(0), connector.getTransactionByName(generated.get(0).getName()));
	}

	@Test
	public void unrelatedEditKeepsNullAndTimestampPrecision() throws Exception {
		execute("INSERT INTO items (label, active, seen) VALUES ('before', NULL, TIMESTAMP '2026-11-24 10:11:12.123456')");
		var id = select("SELECT id FROM items").replace(";", "");
		var original = select("SELECT active, seen FROM items");
		var input = Map.of(column("id"), id, column("label"), "after", "crud_fields", column("label"));
		assertNull(error(run("UPDATE", input)));
		assertEquals("after;", select("SELECT label FROM items"));
		assertEquals(original, select("SELECT active, seen FROM items"));
	}

	@Test
	public void compositeKeyBreaksSortTiesOnEveryPage() throws Exception {
		execute("DROP TABLE IF EXISTS paging_review",
				"CREATE TABLE paging_review (tenant INT, id INT, status VARCHAR(20), label VARCHAR(20), PRIMARY KEY(tenant, id))",
				"INSERT INTO paging_review VALUES (2,2,'same','2/2'), (1,2,'same','1/2'), (2,1,'same','2/1'), (1,1,'same','1/1')");
		generate(column("paging_review"), List.of("LIST"));
		var first = run("LIST", Map.of("sort_key", column("status"), "sort_direction", "desc", "limit", "3", "offset", "0"));
		var next = run("LIST", Map.of("sort_key", column("status"), "sort_direction", "desc", "limit", "3", "offset", "3"));
		assertEquals(List.of("1/1", "1/2", "2/1"), labels(first));
		assertEquals(List.of("2/2"), labels(next));
		var statement = transactions.get("LIST").getPreparedSqlQueries().get(0).getQuery();
		assertTrue(statement, statement.contains("ORDER BY \"" + column("status") + "\" DESC, \""
				+ column("tenant") + "\" ASC, \"" + column("id") + "\" ASC"));
		var defaultPage = run("LIST", Map.of("limit", "3"));
		assertEquals(List.of("1/1", "1/2", "2/1"), labels(defaultPage));
		assertTrue(transactions.get("LIST").getPreparedSqlQueries().get(0).getQuery().contains(
				"ORDER BY \"" + column("tenant") + "\" ASC, \"" + column("id") + "\" ASC"));
	}

	@Test
	public void discoveryClosesColumnsBeforeReturningConnectionOnKeyError() throws Exception {
		var columnsClosed = new AtomicBoolean();
		var closed = new AtomicBoolean();
		var failing = discoveryConnector(columnsClosed, closed, false);
		assertThrows(SQLException.class, () -> SqlConnector.createSqlTransaction(failing, column("items"), List.of("LIST")));
		assertTrue("getColumns result was closed", columnsClosed.get());
		assertTrue("discovery connection was closed", closed.get());
	}

	@Test
	public void generationStopsWhenColumnMetadataIsEmpty() throws Exception {
		var failing = discoveryConnector(new AtomicBoolean(), new AtomicBoolean(), true);
		var failure = assertThrows(SQLException.class, () -> SqlConnector.createSqlTransaction(failing, column("items"), List.of("LIST", "UPDATE")));
		assertTrue(failure.getMessage(), failure.getMessage().contains("No columns found"));
		assertTrue(failing.getTransactionsList().isEmpty());
	}

	private SqlConnector discoveryConnector(AtomicBoolean columnsClosed, AtomicBoolean closed, boolean empty) throws Exception {
		connector.open();
		var delegate = connector.connection;
		var metadata = delegate.getMetaData();
		var wrappedMetadata = proxy(DatabaseMetaData.class, (method, args) -> switch (method.getName()) {
			case "getColumns" -> {
				var result = (ResultSet) invoke(metadata, method, args);
				yield proxy(ResultSet.class, (m, a) -> {
					if (m.getName().equals("close")) {
						columnsClosed.set(true);
					}
					return empty && m.getName().equals("next") ? false : invoke(result, m, a);
				});
			}
			case "getPrimaryKeys" -> {
				if (!empty) {
					throw new SQLException("Primary key metadata unavailable");
				}
				yield invoke(metadata, method, args);
			}
			default -> invoke(metadata, method, args);
		});
		var wrappedConnection = proxy(Connection.class, (method, args) -> method.getName().equals("getMetaData")
				? wrappedMetadata : invoke(delegate, method, args));
		var failing = new SqlConnector() {
			@Override
			public synchronized void open() {
				connection = wrappedConnection;
			}

			@Override
			public synchronized void close() {
				connection = null;
				closed.set(true);
				connector.close();
			}
		};
		failing.setJdbcDriverClassName(connector.getJdbcDriverClassName());
		return failing;
	}

	@FunctionalInterface
	private interface JdbcCall {
		Object invoke(Method method, Object[] args) throws Throwable;
	}

	private static <T> T proxy(Class<T> type, JdbcCall call) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
				(object, method, args) -> call.invoke(method, args)));
	}

	private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
		try {
			return method.invoke(target, args);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
