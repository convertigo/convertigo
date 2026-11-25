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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.ws.commons.schema.constants.Constants;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Accessibility;

/**
 * What the Design tab of the SQL connector editor does with the selected tables, without its user interface: the
 * generated transactions are added, their sequences created and the NGX user interface generated. The editor and
 * the tests call the same code.
 */
public final class SqlCrudGeneration {

	private SqlCrudGeneration() {
	}

	/**
	 * Adds a generated transaction to its connector. An existing transaction of the same name is replaced only with
	 * override. Returns the transaction of the connector: the added one, or the existing one.
	 */
	public static SqlTransaction addTransaction(SqlConnector connector, SqlTransaction transaction, boolean override) throws EngineException {
		var existing = connector.getTransactionByName(transaction.getName());
		if (existing != null && !override) {
			return existing instanceof SqlTransaction sqlTransaction ? sqlTransaction : null;
		}
		if (existing != null) {
			try {
				var xsdFile = new File(existing.getSchemaFilePath());
				if (xsdFile.exists()) {
					xsdFile.delete();
				}
			} catch (Exception e) {
				// the schema is generated again
			}
			connector.remove(existing);
		}
		connector.add(transaction);
		if (transaction.getName().endsWith("_LIST")) {
			var order = "id";
			var mOrder = Pattern.compile("SELECT (.*?),").matcher(transaction.getSqlQuery());
			if (mOrder.find()) {
				order = mOrder.group(1);
			}
			if (transaction.getVariable("order_by") instanceof RequestableVariable v) {
				v.setValueOrNull(order);
			}
			if (transaction.getVariable("limit") instanceof RequestableVariable v) {
				v.setXmlTypeAffectation(new XmlQName(Constants.XSD_LONG));
				v.setValueOrNull("100");
			}
			if (transaction.getVariable("offset") instanceof RequestableVariable v) {
				v.setXmlTypeAffectation(new XmlQName(Constants.XSD_LONG));
				v.setValueOrNull("0");
			}
		}
		return transaction;
	}

	public static boolean hasSequence(SqlTransaction transaction) {
		try {
			return transaction.getProject().getSequenceByName(transaction.getName()) != null;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Creates the sequence of the same name that calls the transaction and copies its rows. An existing sequence is
	 * emptied and filled again, it keeps its accessibility and authentication.
	 */
	public static GenericSequence createSequence(SqlTransaction transaction, Accessibility accessibility, boolean authenticatedContextRequired) throws EngineException {
		GenericSequence sequence;
		var project = transaction.getProject();
		var sequenceName = transaction.getName();
		try {
			sequence = (GenericSequence) project.getSequenceByName(sequenceName);
			List<Step> steps = sequence.getAllSteps();
			List<RequestableVariable> vars = sequence.getAllVariables();
			List<DatabaseObject> children = new ArrayList<>(steps.size() + vars.size());
			children.addAll(steps);
			children.addAll(vars);
			for (DatabaseObject dbo : children) {
				sequence.remove(dbo);
			}
		} catch (Exception e) {
			sequence = new GenericSequence();
			sequence.setName(sequenceName);
			sequence.setAccessibility(accessibility);
			sequence.setAuthenticatedContextRequired(authenticatedContextRequired);
			project.add(sequence);
		}
		var transactionStep = new TransactionStep();
		transactionStep.setSourceTransaction(transaction.getQName());
		sequence.add(transactionStep);
		transactionStep.importVariableDefinition();
		transactionStep.exportVariableDefinition();

		var xmlCopyStep = new XMLCopyStep();
		var source = new XMLVector<String>();
		source.add(Long.toString(transactionStep.priority));
		source.add("./document/sql_output[row]/*|./document/sql_output[not(row)]|./document/error");
		xmlCopyStep.setSourceDefinition(source);
		sequence.add(xmlCopyStep);
		return sequence;
	}

	/**
	 * Generates the NGX user interface of the tables of a connector, once the library is in the workspace
	 * ({@link NgxCrudLibrary#importIfMissing} and {@link NgxCrudLibrary#check}), and adds the project reference to it if
	 * needed. Returns the report shown to the developer: what was created or skipped, and why.
	 */
	public static String generateUserInterface(Project project, SqlConnector connector, List<String> tables, boolean withPage) throws EngineException {
		var referenceAdded = NgxCrudLibrary.ensureReference(project);
		var report = new StringBuilder();
		for (var tableName : tables) {
			try {
				var info = SqlTableInfo.read(connector, tableName);
				var result = new NgxCrudGenerator(project, info).generate(withPage);
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
