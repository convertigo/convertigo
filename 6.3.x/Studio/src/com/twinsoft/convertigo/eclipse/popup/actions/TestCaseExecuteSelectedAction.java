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

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TestCaseTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;

public class TestCaseExecuteSelectedAction extends MyAbstractAction {

	public TestCaseExecuteSelectedAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
        	boolean withXslt = false;
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			if ((treeObject != null) && (treeObject instanceof TestCaseTreeObject)) {
    				TestCase testCase = (TestCase)treeObject.getObject();
    				ProjectTreeObject projectTreeObject = treeObject.getProjectTreeObject();

    				RequestableObject requestable = (RequestableObject)testCase.getParent();
    				if (requestable instanceof Transaction) {
        				TransactionTreeObject transactionTreeObject = (TransactionTreeObject)treeObject.getParent().getParent();
        				transactionTreeObject.getConnectorTreeObject().openConnectorEditor();
    					
        				Transaction transaction = (Transaction)testCase.getParent();
        				Connector connector = (Connector)transaction.getParent();
        				ConnectorEditor connectorEditor = projectTreeObject.getConnectorEditor(connector);
        				if (connectorEditor != null) {
        					getActivePage().activate(connectorEditor);
        					connectorEditor.getDocument(transaction.getName(), testCase.getName(), false, withXslt);
        				}
    				}
    				if (requestable instanceof Sequence) {
    					SequenceTreeObject sequenceTreeObject = (SequenceTreeObject)treeObject.getParent().getParent();
    					new SequenceExecuteSelectedAction().openEditors(explorerView, sequenceTreeObject);
    					
    					Sequence sequence = (Sequence)testCase.getParent();
    					SequenceEditor sequenceEditor = projectTreeObject.getSequenceEditor(sequence);
    					if (sequenceEditor != null) {
    						getActivePage().activate(sequenceEditor);
    						sequenceEditor.getDocument(sequence.getName(), testCase.getName(), false, withXslt);
    					}
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to execute the selected test case!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}
