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

package com.twinsoft.convertigo.eclipse.wizards;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.common.XMLTable;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.statements.FunctionStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ObjectInfoWizardPage extends WizardPage {
	private Text beanName;
	
	public ObjectInfoWizardPage() {
		super("ObjectInfoWizardPage");
		setTitle("Informations");
		setDescription("Please enter a name for object.");
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.verticalSpacing = 9;

		Label label = new Label(container, SWT.NULL);
		label.setText("&Name:");

		beanName = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		beanName.setLayoutData(gd);
		beanName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		initialize();
		dialogChanged();
		setControl(container);
	}
	
	@Override
	public void performHelp() {
		getPreviousPage().performHelp();
	}

	private void initialize() {
		beanName.setText("");
	}
	
	private void dialogChanged() {
		DatabaseObject dbo = ((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean();
		if (dbo instanceof FunctionStatement) {
			beanName.setEnabled(true);
			if (dbo instanceof HandlerStatement)
				beanName.setEnabled(false);
		}
		
		String name = getBeanName();
		if (name.length() == 0) {
			updateStatus("Name must be specified");
			return;
		}
		
		if (!StringUtils.isNormalized(name)) {
			updateStatus("Name must be normalized.\nDon't start with number and don't use non ASCII caracters.");
			return;
		}
		
		try {
			dbo.setName(name);
		} catch (EngineException e) {
			updateStatus("Name could not be set on bean");
			return;
		} catch (NullPointerException e) {
			updateStatus("New Bean has not been instanciated");
			return;
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	public IWizardPage getNextPage() {
		try {
			if (((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean() instanceof XMLTable)
				return getWizard().getPage("XMLTableWizardPage");
			
			if (((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean() instanceof JavelinConnector)
				return getWizard().getPage("EmulatorTechnologyWizardPage");
		}
		catch (NullPointerException e) {
			return null;
		}
		return null;
	}
	
	public String getBeanName() {
		return beanName.getText();
	}
	
	public void setBeanName(String name) {
		beanName.setText(name);
	}
}