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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;

public class ProjectChooseTestCasesDialogComposite extends MyAbstractDialogComposite {

	private org.eclipse.swt.widgets.List listTCSequence = null;
	private org.eclipse.swt.widgets.List listTCTransaction = null;
	private Map<DatabaseObject, List<TestCase>> mapListTestCases = new HashMap<DatabaseObject, List<TestCase>>();
	private Project project = null;
	
	public ProjectChooseTestCasesDialogComposite(Composite parent, int style, Project project) {
		super(parent, style);	
		this.project = project;
		initialize();
	}

	protected void initialize() {
		GridData labelData = new GridData(GridData.FILL_HORIZONTAL);
		labelData.horizontalSpan = 2;
		
		Label label = new Label (this, SWT.NONE);
		label.setText("You can choose test cases to export.\n" +
				"Select one or more Sequences/Transactions test cases below :");
		label.setLayoutData(labelData);
		
		label = new Label(this, SWT.NONE);
		label.setText("\nSequences:");
		
		label = new Label(this, SWT.NONE);
		label.setText("\nTransactions:");
		
		// We add Test Cases into SWT List of Sequence
		listTCSequence = new org.eclipse.swt.widgets.List(this, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		List<Sequence> sequences = project.getSequencesList();
		for (Sequence sequence : sequences) {
			addToList(sequence.getTestCasesList(), sequence.getName(), listTCSequence);
		}	
		listTCSequence.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// We add Test Cases  into SWT List of Transaction
		listTCTransaction = new org.eclipse.swt.widgets.List(this, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		List<Connector> connectors = project.getConnectorsList();
		for (Connector connector : connectors) {
			List<Transaction> transactions = connector.getTransactionsList();
			for (Transaction transaction : transactions ) {
				if (transaction instanceof TransactionWithVariables) {
					List<TestCase> testcases = ((TransactionWithVariables)transaction).getTestCasesList();
					addToList(testcases, transaction.getName(), listTCTransaction);
				}
			}
		}
		listTCTransaction.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		GridLayout gridLayout = new GridLayout(2, true);
		setLayout(gridLayout);
		
		GridData gridData = new GridData(GridData.FILL_BOTH);
		setLayoutData(gridData);
	}

	@Override
	public Object getValue(String name) {
		return mapListTestCases;
	}
	
	public org.eclipse.swt.widgets.List getListTCTransaction(){
		return listTCTransaction;
	}
	
	public org.eclipse.swt.widgets.List getListTCSequence(){
		return listTCSequence;
	}
	
	private void addToList(List<TestCase> testcases, String objectName, 
			org.eclipse.swt.widgets.List listTestCase) {
		for (TestCase testcase : testcases) {
			String name = "(" + objectName + ") " + testcase.getName();
			listTestCase.add(name);
			listTestCase.setData(name, testcase);
		} 
	}
}
