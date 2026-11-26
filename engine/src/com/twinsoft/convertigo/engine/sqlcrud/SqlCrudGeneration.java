/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.sqlcrud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * What the Design tab of the SQL connector editor does with the selected tables, without its user interface: the
 * generated transactions are added, their sequences created and the NGX user interface generated. The editor and
 * the tests call the same code.
 */
public final class SqlCrudGeneration {

	private SqlCrudGeneration() {
	}

	private static final String GENERATED_SEQUENCE = "Generated SQL CRUD wrapper. Definition SHA-256: ";

	private record Change(Transaction previous, SqlTransaction replacement, Sequence previousSequence, GenericSequence replacementSequence) {}

	@FunctionalInterface
	private interface Undo {
		void run() throws EngineException;
	}

	/** Stages all replacements for a table before changing its project. */
	public static boolean installTransactions(SqlConnector connector, List<SqlTransaction> candidates, boolean override,
			Accessibility accessibility, boolean authenticated, boolean generateSequences) throws EngineException {
		var project = connector.getProject();
		synchronized (project) {
			var changes = new ArrayList<Change>();
			for (var candidate : candidates) {
				var name = candidate.getName();
				var previous = connector.getTransactionByName(name);
				var previousSequence = findSequence(project, name);
				var target = connector.getQName() + "." + name;
				if (previous != null && !sameTable(previous, candidate)) {
					throw new EngineException("Transaction " + target + " already exists for another table or has no generated table identity.");
				}
				GenericSequence sequence = null;
				if (accessibility != null) {
					if (previousSequence != null && !isManagedSequence(previousSequence, target)) {
						throw new EngineException("Sequence " + previousSequence.getQName() + " already exists and has a different or customized definition.");
					}
					if (previousSequence == null || override && generateSequences) {
						var transaction = previous == null || override ? candidate : (SqlTransaction) previous;
						sequence = buildSequence(transaction, target, accessibility, authenticated);
					}
				}
				var transaction = previous == null || override ? candidate : null;
				// NGX alone reuses existing wrappers and their transaction contract.
				if (accessibility != null && !generateSequences && previousSequence != null && previous != null) {
					transaction = null;
				}
				if (sequence != null || transaction != null) {
					changes.add(new Change(previous, transaction, previousSequence, sequence));
				}
			}

			var undo = new ArrayDeque<Undo>();
			var schemas = new ArrayList<Runnable>();
			try {
				for (var change : changes) {
					if (change.replacement() != null) {
						var replacement = change.replacement();
						var schema = replacement.takePendingSchemaTypes();
						if (schema != null) {
							schemas.add(() -> replacement.writeSchemaToFile(schema));
						}
						if (change.previous() != null && change.previous().isDefault) {
							replacement.isDefault = true;
							undo.push(() -> connector.setDefaultTransaction(change.previous()));
						}
						replace(connector, change.previous(), replacement, undo);
					}
					if (change.replacementSequence() != null) {
						replace(project, change.previousSequence(), change.replacementSequence(), undo);
					}
				}
			} catch (EngineException | RuntimeException e) {
				while (!undo.isEmpty()) {
					try {
						undo.pop().run();
					} catch (EngineException | RuntimeException undoFailure) {
						e.addSuppressed(undoFailure);
					}
				}
				throw e;
			}
			schemas.forEach(Runnable::run);
			return !changes.isEmpty();
		}
	}

	private static void replace(DatabaseObject parent, DatabaseObject previous, DatabaseObject replacement, ArrayDeque<Undo> undo)
			throws EngineException {
		if (previous != null) {
			parent.remove(previous);
			undo.push(() -> parent.add(previous));
		}
		parent.add(replacement);
		undo.push(() -> parent.remove(replacement));
	}

	private static boolean sameTable(Transaction previous, SqlTransaction candidate) throws EngineException {
		if (!(previous instanceof SqlTransaction sql)) {
			return false;
		}
		if (sql.getCrudQuery().isEmpty() || candidate.getCrudQuery().isEmpty()) {
			return sql.getCrudQuery().equals(candidate.getCrudQuery());
		}
		return SqlCrudQuery.parse(sql.getCrudQuery()).table().equals(SqlCrudQuery.parse(candidate.getCrudQuery()).table());
	}

	static Sequence findSequence(Project project, String name) {
		return project.getSequencesList().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}

	static boolean isManagedSequence(Sequence sequence, String target) throws EngineException {
		return sequence instanceof GenericSequence && sequence.getComment().startsWith(GENERATED_SEQUENCE)
				&& sequence.getAllSteps().size() == 2
				&& sequence.getAllSteps().get(0) instanceof TransactionStep step
				&& target.equals(step.getSourceTransaction())
				&& sequence.getComment().equals(GENERATED_SEQUENCE + definitionDigest(sequence));
	}

	/** Creates a missing wrapper. Existing wrappers can be replaced by an explicit generation with Override. */
	public static GenericSequence createSequence(SqlTransaction transaction, Accessibility accessibility, boolean authenticated)
			throws EngineException {
		var connector = (SqlConnector) transaction.getParent();
		installTransactions(connector, List.of(transaction), false, accessibility, authenticated, true);
		return (GenericSequence) findSequence(connector.getProject(), transaction.getName());
	}

	private static GenericSequence buildSequence(SqlTransaction transaction, String target, Accessibility accessibility, boolean authenticated)
			throws EngineException {
		var sequence = new GenericSequence();
		sequence.setName(transaction.getName());
		sequence.setAccessibility(accessibility);
		sequence.setAuthenticatedContextRequired(authenticated);
		sequence.bNew = true;
		sequence.hasChanged = true;

		var transactionStep = new TransactionStep();
		transactionStep.setSourceTransaction(target);
		sequence.add(transactionStep);
		transactionStep.importVariableDefinition(transaction);
		transactionStep.exportVariableDefinition(transaction);

		var xmlCopyStep = new XMLCopyStep();
		var source = new XMLVector<String>();
		source.add(Long.toString(transactionStep.priority));
		source.add("./document/sql_output[row]/*|./document/sql_output[not(row)]|./document/error");
		xmlCopyStep.setSourceDefinition(source);
		sequence.add(xmlCopyStep);
		sequence.setComment(GENERATED_SEQUENCE + definitionDigest(sequence));
		return sequence;
	}

	private static String definitionDigest(Sequence sequence) throws EngineException {
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = sequence.toXml(document);
		for (var child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element property && property.getTagName().equals("property") && property.getAttribute("name").equals("comment")) {
				root.removeChild(child);
				break;
			}
		}
		document.appendChild(root);
		appendDefinition(document, root, sequence);
		return DigestUtils.sha256Hex(XMLUtils.prettyPrintDOM(document));
	}

	private static void appendDefinition(Document document, Element parent, DatabaseObject object) throws EngineException {
		for (var child : object.getAllChildren()) {
			var definition = child.toXml(document);
			parent.appendChild(definition);
			appendDefinition(document, definition, child);
		}
	}

	/**
	 * Generates the NGX user interface of the tables of a connector, once the library is in the workspace
	 * ({@link NgxCrudLibrary#importIfMissing} and {@link NgxCrudLibrary#check}), and adds the project reference to it if
	 * needed. Returns the report shown to the developer: what was created or skipped, and why.
	 */
	public static String generateUserInterface(Project project, SqlConnector connector, List<SqlTableInfo> tables, boolean withPage) throws EngineException {
		var referenceAdded = NgxCrudLibrary.ensureReference(project);
		var report = new StringBuilder();
		for (var info : tables) {
			var tableName = info.getIdentity().displayName();
			try {
				var result = new NgxCrudGenerator(project, connector, info).generate(withPage);
				report.append(tableName).append(": ");
				if (result.isSkipped()) {
					report.append("skipped, ").append(result.getSkipReason());
				} else {
					report.append("shared component ").append(result.getComponent().getName());
					if (result.getForm() != null) {
						report.append(", form ").append(result.getForm().getName());
					}
					if (result.getPage() != null) {
						report.append(" and page ").append(result.getPage().getName());
					}
					report.append(" created");
				}
				report.append('\n');
				if (!result.isSkipped()) {
					for (var warning : result.getWarnings()) {
						report.append("  - ").append(warning).append('\n');
					}
				}
			} catch (Exception e) {
				report.append(tableName).append(": failed, ").append(e.getMessage()).append('\n');
			}
		}
		if (referenceAdded) {
			report.append("\nReference to ").append(CrudUiDefaults.LIB_PROJECT_NAME).append(" added (").append(CrudUiDefaults.LIB_VERSION).append(").");
		}
		return report.toString();
	}
}
