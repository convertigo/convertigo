/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScEntryDefaultHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScEntryHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScExitDefaultHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScExitHandlerStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class CreateHandlerDialogComposite extends MyAbstractDialogComposite {

	private Button jCheckBoxTransactionStarted = null;
	private Button jCheckBoxXmlGenerated = null;
	private Button jCheckBoxTransactionDefaultHandlerEntry = null;
	private Button jCheckBoxTransactionDefaultHandlerExit = null;
	private Button jCheckBoxTransactionScreenClassHandler = null;
	private Label jLabelChooseScreenClass = null;
	private Button jCheckBoxEntry = null;
	private Button jCheckBoxExit = null;
	
	private Tree tree;

	private Transaction transaction = null;
	private String handlers = null;
	private List<Object> result = null;
	private boolean isScreenClassAware = false;
	private boolean isDefaultHandlerAware = false;
	
	public CreateHandlerDialogComposite(Composite parent, int style, Object parentObject) {
		super(parent, style);
		this.transaction = (Transaction)parentObject;
		this.handlers = this.transaction.handlers;
		
		Connector connector = (Connector)transaction.getParent();
		
		isScreenClassAware = connector instanceof IScreenClassContainer<?>;
		isDefaultHandlerAware = ((transaction instanceof JavelinTransaction) || (transaction instanceof HtmlTransaction));
		initialize();		
	}

	protected void initialize() {
		jCheckBoxTransactionScreenClassHandler = new Button(this, SWT.CHECK);
		jCheckBoxTransactionScreenClassHandler.setText("Screenclass transaction handler");
		jCheckBoxTransactionScreenClassHandler.setEnabled(isScreenClassAware);
		jCheckBoxTransactionScreenClassHandler.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				boolean isChecked = jCheckBoxTransactionScreenClassHandler.getSelection();
				if (!isChecked) {
					jCheckBoxExit.setSelection(false);
				}
				tree.setEnabled(isChecked);
				jCheckBoxEntry.setEnabled(isChecked);
				jCheckBoxEntry.setSelection(isChecked);
				jCheckBoxExit.setEnabled(isChecked);
			}
		});
		
		jLabelChooseScreenClass = new Label(this, SWT.NONE);
		jLabelChooseScreenClass.setText("ScreenClass : ");
		jLabelChooseScreenClass.setEnabled(isScreenClassAware);
		
		createTreeScreenClasses();
		tree.setEnabled(false);
		
		jCheckBoxEntry = new Button(this, SWT.CHECK);
		jCheckBoxEntry.setText("Entry handler");
		jCheckBoxEntry.setEnabled(false);
		
		jCheckBoxExit = new Button(this, SWT.CHECK);
		jCheckBoxExit.setText("Exit handler");
		jCheckBoxExit.setEnabled(false);
				
		Label lab = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData labelGridData = new GridData(GridData.FILL_HORIZONTAL);
		lab.setLayoutData(labelGridData);
		
		jCheckBoxTransactionStarted = new Button(this, SWT.CHECK);
		jCheckBoxTransactionStarted.setText("Start of transaction");
		
		jCheckBoxXmlGenerated = new Button(this, SWT.CHECK);
		jCheckBoxXmlGenerated.setText("XML generation");
		
		jCheckBoxTransactionDefaultHandlerEntry = new Button(this, SWT.CHECK);
		jCheckBoxTransactionDefaultHandlerEntry.setText("Default transaction entry handler");
		jCheckBoxTransactionDefaultHandlerEntry.setEnabled(isDefaultHandlerAware);
		
		jCheckBoxTransactionDefaultHandlerExit = new Button(this, SWT.CHECK);
		jCheckBoxTransactionDefaultHandlerExit.setText("Default transaction exit handler");
		jCheckBoxTransactionDefaultHandlerExit.setEnabled(isDefaultHandlerAware);
		
		GridLayout gridLayout = new GridLayout();
		setLayout(gridLayout);		
}
	
	public Object getValue(String name) {
		return null;
	}

	/**
	 * This method initializes jComboBoxScreenClasses	
	 *
	 */
	private void createTreeScreenClasses() {

		tree = new Tree(this, SWT.MULTI | SWT.BORDER);
		tree.setHeaderVisible(false);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.verticalSpan = 20;
		tree.setLayoutData(gridData);		
	
		if (isScreenClassAware) {
			Connector connector = (Connector)transaction.getParent();
			if (connector instanceof HtmlConnector) {
				HtmlConnector htmlConnector = (HtmlConnector) connector;
				ScreenClass defaultScreenClass = htmlConnector.getDefaultScreenClass();
				TreeItem branch = new TreeItem(tree, SWT.NONE);
				branch.setText(defaultScreenClass.getName());
				
				List<ScreenClass> screenClasses = defaultScreenClass.getInheritedScreenClasses();
				
				for (ScreenClass screenClass : screenClasses) {
					getInHeritedScreenClass(screenClass, branch);				
				}
			} else if (connector instanceof JavelinConnector) {
				JavelinConnector javelinConnector = (JavelinConnector) connector;
				ScreenClass defaultScreenClass = javelinConnector.getDefaultScreenClass();
				TreeItem branch = new TreeItem(tree, SWT.NONE);
				branch.setText(defaultScreenClass.getName());
				
				List<ScreenClass> screenClasses = defaultScreenClass.getInheritedScreenClasses();
				
				for (ScreenClass screenClass : screenClasses) {
					getInHeritedScreenClass(screenClass, branch);				
				}	
			}
		}
	}
	
	public void getInHeritedScreenClass(ScreenClass screenClass, TreeItem branch) {
		TreeItem leaf = new TreeItem(branch, SWT.NONE);
		leaf.setText(screenClass.getName());
		List<ScreenClass> screenClasses = screenClass.getInheritedScreenClasses();
		for (ScreenClass sC : screenClasses) {
			getInHeritedScreenClass(sC, leaf);
		}
	}
	
	public List<Object> generateHandler() throws EngineException {
		result = null;
		if (transaction instanceof HtmlTransaction)
			generateStatementHandler();
		else
			generateStringHandler();
		return result;
	}
	
	public void generateStringHandler() {
		String handler, functionName = "";
		if (jCheckBoxTransactionStarted.getSelection()) {
			functionName = "function on" + Transaction.EVENT_TRANSACTION_STARTED + "()";
			if (handlers.indexOf(functionName) == -1) {
				handler = "";
				handler += "\n";
				handler += "// Handles the transaction start event.\n";
				handler += "function on" + Transaction.EVENT_TRANSACTION_STARTED + "() {\n";
				handler += "    // TODO: add your code here\n";
				handler += "\n";
				handler += "    // TODO: customize the returned value (if you omit returned value, the \n";
				handler += "    // algorithm will continue its process).\n";
				handler += "    // Possible values are:\n";
				handler += "    //    cancel - means the algorithm cancels the transaction core process.\n";
				handler += "\n";
				handler += "    // return \"cancel\";\n";
				handler += "}\n";
				addElement(handler, "");
			}
			else Beep();
		}

		if (jCheckBoxXmlGenerated.getSelection()) {
			functionName = "function on" + Transaction.EVENT_XML_GENERATED + "()";
			if (handlers.indexOf(functionName) == -1) {
				handler = "";
				handler += "\n";
				handler += "// Handles the XML generated event.\n";
				handler += "function on" + Transaction.EVENT_XML_GENERATED + "() {\n";
				handler += "    // TODO: add your code here\n";
				handler += "}\n";
				addElement(handler, "");
			}
			else Beep();
		}

		if (jCheckBoxTransactionDefaultHandlerEntry.getSelection()) {
			functionName = "function onTransactionDefaultHandlerEntry()";
			if (handlers.indexOf(functionName) == -1) {
				handler = "";
				handler += "\n";
				handler += "// Handles the default screenclass entry event.\n";
				handler += functionName + " {\n";
				handler += "    // TODO: add your code here\n";
				handler += "}\n";
				addElement(handler, "");
			}
			else Beep();
		}

		if (jCheckBoxTransactionDefaultHandlerExit.getSelection()) {
			functionName = "function onTransactionDefaultHandlerExit()";
			if (handlers.indexOf(functionName) == -1) {
				handler = "";
				handler += "\n";
				handler += "// Handles the default screenclass exit event.\n";
				handler += functionName + " {\n";
				handler += "    // TODO: add your code here\n";
				handler += "}\n";
				addElement(handler, "");
			}
			else Beep();
		}
		
		String handlerName, commentEntry, commentExit;

		if (jCheckBoxEntry.getSelection() || jCheckBoxExit.getSelection()) {
			TreeItem[] treeItems = tree.getSelection();
			for (int i=0; i<treeItems.length; i++) {
				String selectedScreenClass = treeItems[i].getText();
				handlerName = StringUtils.normalize(selectedScreenClass);
				commentEntry = "// Entry handler for screen class \"" + selectedScreenClass + "\"\n";
				commentExit  = "// Exit handler for screen class \"" + selectedScreenClass + "\"\n";
		            	
				if (jCheckBoxEntry.getSelection()) {
					functionName = "function on" + handlerName + "Entry()";
					if (handlers.indexOf(functionName) == -1) {
						handler = "";
						handler += "\n";
						handler += commentEntry;
						handler += "function on" + handlerName + "Entry() {\n";
						handler += "    // TODO: add your code here\n";
						handler += "\n";
						handler += "    // TODO: customize the returned value (if you omit returned value, the \n";
						handler += "    // algorithm will continue its process).\n";
						handler += "    // Possible values are:\n";
						handler += "    //    redetect - means the algorithm detects again the screen class.\n";
						handler += "    //    skip     - means the algorithm skips the xmlization process and \n";
						handler += "    //               directly goes to the exit handler for the current\n";
						handler += "    //               screen class.\n";
						handler += "    //    continue - equivalent to an empty string or no return or empty return,\n";
						handler += "    //               means the algorithm will continue its process\n";
						handler += "\n";
						handler += "    // return \"redetect\";\n";
						handler += "}\n";
						addElement(handler, "");
					}
					else Beep();
				}
				if (jCheckBoxExit.getSelection()) {
					functionName = "function on" + handlerName + "Exit()";
					if (handlers.indexOf(functionName) == -1) {
						handler = "";
						handler += "\n";
						handler += commentExit;
						handler += "function on" + handlerName + "Exit() {\n";
						handler += "    // TODO: add your code here\n";
						handler += "\n";
						handler += "    // TODO: customize the returned value (if you omit returned value, the \n";
						handler += "    // algorithm will continue its process).\n";
						handler += "    // Possible values are:\n";
						handler += "    //    accumulate - means the algorithm accumulates XML data and go to\n";
						handler += "    //                 the next detected screen class.\n";
						handler += "    //    continue - equivalent to an empty string or no return or empty return,\n";
						handler += "    //               means the algorithm will continue its process\n";
						handler += "\n";
						handler += "    // return \"accumulate\";\n";
						handler += "}\n";
						addElement(handler, "");
					}
					else Beep();
				}
			}
		}
	}
	
	public void generateStatementHandler() throws EngineException {
		String normalizedScreenClassName, handlerName = "";
		
		if (jCheckBoxTransactionStarted.getSelection()) {
			handlerName = "on" + Transaction.EVENT_TRANSACTION_STARTED;
			if (((HtmlTransaction)transaction).getHandlerStatement(handlerName) == null) {
				HandlerStatement statement = new HandlerStatement(HandlerStatement.EVENT_TRANSACTION_STARTED, "");
				addElement(statement, handlerName);
			}
			else Beep();
		}

		if (jCheckBoxXmlGenerated.getSelection()) {
			handlerName = "on" + Transaction.EVENT_XML_GENERATED;
			if (((HtmlTransaction)transaction).getHandlerStatement(handlerName) == null) {
				HandlerStatement statement = new HandlerStatement(HandlerStatement.EVENT_XML_GENERATED, "");
				addElement(statement, handlerName);
			}
			else Beep();
		}

		if (jCheckBoxTransactionDefaultHandlerEntry.getSelection()) {
			handlerName = "onTransactionDefaultHandlerEntry";
			if (((HtmlTransaction)transaction).getHandlerStatement(handlerName) == null) {
				ScEntryDefaultHandlerStatement statement = new ScEntryDefaultHandlerStatement();
				addElement(statement, handlerName);
			}
			else Beep();
		}
		
		if (jCheckBoxTransactionDefaultHandlerExit.getSelection()) {
			handlerName = "onTransactionDefaultHandlerExit";
			if (((HtmlTransaction)transaction).getHandlerStatement(handlerName) == null) {
				ScExitDefaultHandlerStatement statement = new ScExitDefaultHandlerStatement();
				addElement(statement, handlerName);
			}
			else Beep();
		}
		
		if (jCheckBoxEntry.getSelection() || jCheckBoxExit.getSelection()) {
			TreeItem[] treeItems = tree.getSelection();
			for (int i=0; i<treeItems.length; i++) {
				String selectedScreenClass = treeItems[i].getText();
				normalizedScreenClassName = StringUtils.normalize(selectedScreenClass);
	            	
				if (jCheckBoxEntry.getSelection()) {
					handlerName = "on" + normalizedScreenClassName + "Entry";
					if (((HtmlTransaction)transaction).getHandlerStatement(handlerName) == null) {
						ScEntryHandlerStatement statement = new ScEntryHandlerStatement(normalizedScreenClassName);
						addElement(statement, handlerName);
					}
					else Beep();
				}
				if (jCheckBoxExit.getSelection()) {
					handlerName = "on" + normalizedScreenClassName + "Exit";
					if (((HtmlTransaction)transaction).getHandlerStatement(handlerName) == null) {
						ScExitHandlerStatement statement = new ScExitHandlerStatement(normalizedScreenClassName);
						addElement(statement, handlerName);
					}
					else Beep();
				}
			}
		}
	}
	
	
	private void addElement(Object object, String name) {
		if (result == null) result = new ArrayList<Object>();
		
		if ((object != null) && (name != null)) {
			try {
				if (object instanceof Statement) {
					if (!StringUtils.isNormalized(name)) {
						Beep();
						throw new EngineException("Handler name is not normalized : \""+name+"\".");
					}
					
					((Statement)object).setName(name);
					((Statement)object).hasChanged = true;
					((Statement)object).bNew = true;
					((Statement)object).priority = 0;
					((Statement)object).newPriority = 0;
				}
				result.add(object);
			}
			catch (EngineException e) {;}
		}
	}
	
	private void Beep() {
		Toolkit.getDefaultToolkit().beep();
	}
}
