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

package com.twinsoft.convertigo.eclipse.wizards.new_statement;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;

import com.twinsoft.convertigo.beans.common.XMLTable;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class StatementInfoWizardPage extends WizardPage {
	private Text beanName;
	
	public StatementInfoWizardPage() {
		super("StatementInfoWizardPage");
		setTitle("Informations");
		setDescription("Please enter a name for statement group.");
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
	
	private void initialize() {
		beanName.setText("");
		((StatementGeneratorWizardPage)getWizard().getPage("StatementGeneratorWizardPage")).setPageComplete(true);
	}
	
	private void dialogChanged() {
		
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
			((StatementGeneratorWizardPage)getWizard().getPage("StatementGeneratorWizardPage")).getCreatedBean().setName(name);
		} catch (EngineException e) {
			updateStatus("Name could not be set on bean");
			return;
		}
		catch (NullPointerException e) {
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
			if (((StatementGeneratorWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean() instanceof XMLTable)
				return getWizard().getPage("XMLTableWizardPage");
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