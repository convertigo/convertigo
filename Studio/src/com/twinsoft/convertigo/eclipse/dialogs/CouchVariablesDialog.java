/*
 * Copyright (c) 2001-2015 Convertigo SA.
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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.CouchVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;

public class CouchVariablesDialog extends Dialog {

	private CouchVariablesComposite couchVariablesComposite;
	private PropertyDescriptor[] props = null;
	
	private AbstractCouchDbTransaction couchDbTransaction = null; 
	
	private int nWidth = 650;
	private int nHeight = 670;
	
	public CouchVariablesDialog(Shell parentShell, AbstractCouchDbTransaction couchDbTransaction, PropertyDescriptor[] props) {
		super(parentShell);		
		this.couchDbTransaction = couchDbTransaction;
		this.props = props;
	}
	
	public CouchVariablesDialog(Shell parentShell, AbstractCouchDbTransaction couchDbTransaction) {
		super(parentShell);		
		this.couchDbTransaction = couchDbTransaction;
	}
	
	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Parameters availables");
		newShell.setSize(nWidth, nHeight);

		int nLeft = 0;
		int nTop = 0;

		Display display = newShell.getDisplay();

		Point pt = display.getCursorLocation();
		Monitor[] monitors = display.getMonitors();

		for (int i = 0; i < monitors.length; i++) {
			if (monitors[i].getBounds().contains(pt)) {
				Rectangle rect = monitors[i].getClientArea();

				if (rect.x < 0)
					nLeft = ((rect.width - nWidth) / 2) + rect.x;
				else
					nLeft = (rect.width - nWidth) / 2;

				if (rect.y < 0)
					nTop = ((rect.height - nHeight) / 2) + rect.y;
				else
					nTop = (rect.height - nHeight) / 2;

				break;
			}
		}

		newShell.setBounds(nLeft, nTop, nWidth, nHeight);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {	
		try {
			PropertyDescriptor[] availablesParameters = null;
			List<RequestableVariable> dboVariables = null;
			if (props != null) {
				availablesParameters = props;
			} else {
				availablesParameters = CachedIntrospector.getBeanInfo(couchDbTransaction).getPropertyDescriptors();
			}
			
			if (couchDbTransaction != null) {
				dboVariables = couchDbTransaction.getVariablesList();
			}
			
			couchVariablesComposite = new CouchVariablesComposite(parent, SWT.V_SCROLL, dboVariables);
			couchVariablesComposite.setPropertyDescriptor(couchDbTransaction, availablesParameters, couchDbTransaction.getParent());

			GridData couchVarData = new GridData(GridData.FILL_BOTH);
			couchVarData.horizontalSpan = 2;

			couchVariablesComposite.setLayoutData(couchVarData);
			
			if (couchVariablesComposite.getSelectedParameters().size()==0) {
				setReturnCode(-1);
			}
			
		} catch (IntrospectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return couchVariablesComposite;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {	
		/* APPLY ACTION */
		Button buttonApply = createButton(parent, IDialogConstants.PROCEED_ID, "Apply", true);
		buttonApply.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				applyProceed();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		buttonApply.setEnabled(true);
	}
	
	private void applyProceed() {
		List<CouchVariable> selectedVariables = couchVariablesComposite.getSelectedParameters();
		
		if (selectedVariables != null) {
			couchDbTransaction.createVariables(selectedVariables);
		}

		close();
	}
	
}
