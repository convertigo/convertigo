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

package com.twinsoft.convertigo.eclipse.editors.connector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.EventListenerList;

import org.apache.ws.commons.schema.constants.Constants;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils.SelectionListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Accessibility;

class SqlConnectorDesignComposite extends Composite {

	private Text text;
	private Table table;
	private TableColumn tblclmnCallableName;
	private TableColumn tblclmnDescription;
	private TableColumn tblclmnGroupName;

	private Button btnImportAsTransactions;

	private SqlConnector sqlConnector;

	private ProjectExplorerView projectExplorerView = null;

	public SqlConnectorDesignComposite(Connector connector, Composite parent, int style) {
		super(parent, style);

		sqlConnector = (SqlConnector)connector;

		// add ProjectExplorerView to the listeners of the associated sql connector
		projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			addCompositeListener(projectExplorerView);
		}

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		this.setLayout(gridLayout);

		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		composite.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setToolTipText("Type Here a Stored Procedure/Function Pattern such as SUB%");
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("Search Pattern");

		text = new Text(composite, SWT.BORDER);
		text.setText("%");
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button btnNewButton = new Button(composite, SWT.NONE);
		btnNewButton.setText("Search");

		lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("Search in…");

		var types = new Composite(composite, SWT.NONE);
		types.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		types.setLayout(new RowLayout());

		var checkbox = new Button(types, SWT.CHECK);
		checkbox.setText("TABLE");
		checkbox.setSelection(true);
		checkbox = new Button(types, SWT.CHECK);
		checkbox.setText("PROCEDURE");
		checkbox.setSelection(true);
		checkbox = new Button(types, SWT.CHECK);
		checkbox.setText("FUNCTION");
		checkbox.setSelection(true);


		text.addKeyListener(new org.eclipse.swt.events.KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.CR) {
					btnNewButton.notifyListeners(SWT.MouseDown, null);
				}
			}
		});
		btnNewButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				var enabled = new LinkedList<String>();
				for (var c: types.getChildren()) {
					if (c instanceof Button b && b.getSelection()) {
						enabled.add(b.getText());
					}
				}
				search(text.getText(), enabled);
			}
		});

		table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		tblclmnCallableName = new TableColumn(table, SWT.NONE);
		tblclmnCallableName.setWidth(200);
		tblclmnCallableName.setText("Name");

		tblclmnDescription = new TableColumn(table, SWT.NONE);
		tblclmnDescription.setWidth(500);
		tblclmnDescription.setText("Description");

		tblclmnGroupName = new TableColumn(table, SWT.NONE);
		tblclmnGroupName.setWidth(200);
		tblclmnGroupName.setText("Type");

		var footer = new Composite(composite, SWT.NONE);
		footer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		footer.setLayout(new GridLayout(3, false));

		lblNewLabel = new Label(footer, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("For TABLE…");

		var cruds = new Composite(footer, SWT.NONE);
		cruds.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		cruds.setLayout(new RowLayout());

		checkbox = new Button(cruds, SWT.CHECK);
		checkbox.setText("LIST");
		checkbox.setSelection(true);
		checkbox = new Button(cruds, SWT.CHECK);
		checkbox.setText("INSERT");
		checkbox.setSelection(true);
		checkbox = new Button(cruds, SWT.CHECK);
		checkbox.setText("SELECT");
		checkbox.setSelection(true);
		checkbox = new Button(cruds, SWT.CHECK);
		checkbox.setText("UPDATE");
		checkbox.setSelection(true);
		checkbox = new Button(cruds, SWT.CHECK);
		checkbox.setText("DELETE");
		checkbox.setSelection(true);

		var right = new Composite(footer, SWT.NONE);
		right.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		right.setLayout(RowLayoutFactory.fillDefaults().center(true).create());
		var override = new Button(right, SWT.CHECK);
		override.setText("Override");
		var wrap = new Button(right, SWT.CHECK);
		wrap.setText("Sequence wrap");

		btnImportAsTransactions = new Button(right, SWT.NONE);
		btnImportAsTransactions.setText("Import as transaction(s) in project");

		var wrapPart = new Composite(footer, SWT.NONE);
		var wrapPartGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		wrapPart.setLayoutData(wrapPartGridData);
		wrapPart.setLayout(RowLayoutFactory.fillDefaults().center(true).create());
		wrapPart.setVisible(false);
		wrapPartGridData.exclude = true;

		lblNewLabel = new Label(wrapPart, SWT.NONE);
		lblNewLabel.setText("For Sequences…");

		var combo = new Combo(wrapPart, SWT.READ_ONLY);
		for (Accessibility a : Accessibility.values()) {
			combo.add("Accessibility " + a.name());
		}
		combo.setText(combo.getItem(1));

		var auth = new Button(wrapPart, SWT.CHECK);
		auth.setText("Authenticated session MANDATORY");
		auth.setSelection(true);

		btnImportAsTransactions.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				var enabled = new LinkedList<String>();
				for (var c: cruds.getChildren()) {
					if (c instanceof Button b && b.getSelection()) {
						enabled.add(b.getText());
					}
				}
				Accessibility accessibility = null;
				if (wrap.getSelection()) {
					accessibility = Accessibility.valueOf(combo.getSelectionIndex());
				}
				createSqlTransactions(table.getSelection(), override.getSelection(), enabled, accessibility, auth.getSelection());
			}
		});

		wrap.addSelectionListener((SelectionListener)(e) -> {
			var visible = wrap.getSelection();
			wrapPart.setVisible(visible);
			wrapPartGridData.exclude = !visible;
			parent.layout(true, true);
		});
	}

	private EventListenerList compositeListeners = new EventListenerList();

	private void addCompositeListener(CompositeListener compositeListener) {
		compositeListeners.add(CompositeListener.class, compositeListener);
	}

	private void fireObjectChanged(CompositeEvent compositeEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = compositeListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2 ; i >= 0 ; i -= 2) {
			if (listeners[i] == CompositeListener.class) {
				((CompositeListener) listeners[i+1]).objectChanged(compositeEvent);
			}
		}
	}

	private void search(String pattern, List<String> types) {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
		Shell shell = display.getActiveShell();
		if (shell != null) {
			try {
				shell.setCursor(waitCursor);

				ConvertigoPlugin.logDebug("Searching repository...");
				Document doc = SqlConnector.executeSearch(sqlConnector, pattern, types);
				ConvertigoPlugin.logDebug("Search done.");

				if (doc != null) {
					table.removeAll();

					NodeList items = doc.getDocumentElement().getElementsByTagName("item");
					for (int i=0; i<items.getLength(); i++) {
						Element item = (Element) items.item(i);

						Element func = (Element) item.getElementsByTagName("NAME").item(0);
						String funcName = func.getTextContent();
						String specific_name = func.getAttribute("specific_name");

						String groupName ="";
						try {
							groupName = item.getElementsByTagName("TYPE").item(0).getTextContent();
						} catch (Exception e) {}
						String sText="";
						try {
							sText = item.getElementsByTagName("REMARKS").item(0).getTextContent();
						} catch (Exception e) {}

						TableItem tableItem = new TableItem(this.table, SWT.NONE);
						tableItem.setText(new String[] {funcName,sText,groupName});
						tableItem.setData("specific_name", specific_name);
					}
				}

			} catch (Exception ee) {
				ConvertigoPlugin.logException(ee, "Error while searching repository");
			} finally {
				shell.setCursor(null);
				waitCursor.dispose();
			}
		}
	}

	private void createSqlTransactions(final TableItem[] items, boolean override, List<String> cruds, Accessibility accessibility, boolean authenticated) {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
		Shell shell = display.getActiveShell();
		if (shell != null) {
			try {
				shell.setCursor(waitCursor);
				for (int i=0; i < items.length; i++) {
					TableItem item = items[i];
					String callableName = item.getText(0);
					String type = item.getText(2);
					List<SqlTransaction> sqlTransactions = null;
					if ("TABLE".equals(type)) {
						sqlTransactions = SqlConnector.createSqlTransaction(sqlConnector, callableName, cruds);
					} else {
						String specific_name = (String) item.getData("specific_name");
						ConvertigoPlugin.logDebug("Creating transaction for CALL '"+callableName+"' ...");

						if (specific_name.isEmpty()) {
							specific_name = callableName;
						}
						sqlTransactions = Arrays.asList(SqlConnector.createSqlTransaction(sqlConnector, callableName, specific_name));
					}
					for (var sqlTransaction: sqlTransactions) {
						if (sqlTransaction != null) {
							Transaction transaction = sqlConnector.getTransactionByName(sqlTransaction.getName());
							if (override || transaction == null) {
								if (transaction != null) {
									try {
										File xsdFile = new File(transaction.getSchemaFilePath());
										if (xsdFile.exists()) {
											xsdFile.delete();
										}
									}
									catch (Exception e) {}
									sqlConnector.remove(transaction);
								}
								sqlConnector.add(sqlTransaction);
								fireObjectChanged(new CompositeEvent(sqlConnector));
								var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
								if (pev != null) {
									var dbot = pev.findTreeObjectByUserObject(sqlTransaction);
									pev.fireTreeObjectPropertyChanged(new TreeObjectEvent(dbot, "sqlQuery", "", sqlTransaction.getSqlQuery(), TreeObjectEvent.UPDATE_NONE));
									pev.setSelectedTreeObject(dbot);
									if (sqlTransaction.getName().endsWith("_LIST")) {
										if (sqlTransaction.getVariable("limit") instanceof RequestableVariable v && v != null) {
											v.setXmlTypeAffectation(new XmlQName(Constants.XSD_LONG));
											v.setValueOrNull("100");
										}
										if (sqlTransaction.getVariable("offset") instanceof RequestableVariable v && v != null) {
											v.setXmlTypeAffectation(new XmlQName(Constants.XSD_LONG));
											v.setValueOrNull("0");
										}
									}
								}
								ConvertigoPlugin.logDebug("Transaction added.");
							}
							if (accessibility != null) {
								createSequenceWrapper(sqlTransaction, accessibility, authenticated);
							}
						}
					}
				}
			} catch (Exception ee) {
				ConvertigoPlugin.logException(ee, "Error while creating transaction(s)");
			} finally {
				shell.setCursor(null);
				waitCursor.dispose();
			}
		}
	}

	private void createSequenceWrapper(SqlTransaction transaction, Accessibility accessibility, boolean authenticatedContextRequired) throws EngineException {
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
			for (DatabaseObject dbo: children) {
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
		source.add("./document/error|./document/sql_output[row]/*|./document/sql_output[not(row)]");
		xmlCopyStep.setSourceDefinition(source);
		sequence.add(xmlCopyStep);

		fireObjectChanged(new CompositeEvent(project));
		var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (pev != null) {
			var dbot = pev.findTreeObjectByUserObject(sequence);
			pev.setSelectedTreeObject(dbot);
		}
	}
}
