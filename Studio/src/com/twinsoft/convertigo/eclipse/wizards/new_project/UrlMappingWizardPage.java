/*
 * Copyright (c) 2001-2014 Convertigo SA.
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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/wizards/SQLQueriesWizardPage.java $
 * $Author: julienda $
 */

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectExplorerWizardPage;

public class UrlMappingWizardPage extends WizardPage {
	private Object parentObject = null;
	
	private Text mappingPath = null;
	private Composite container;

	public UrlMappingWizardPage(Object parentObject) {
		super("UrlMappingWizardPage");
		this.parentObject = parentObject;
		setTitle("Define mapping path");
		setDescription("This step configures the mapping path");
	}
	
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 10;
		container.setLayout(gridLayout);
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Please enter the Convertigo api path of the mapping\n");
		
		mappingPath = new Text(container, SWT.BORDER | SWT.SINGLE);
		mappingPath.setFont(new Font(container.getDisplay(), "Tahoma", 10, 0));
		mappingPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mappingPath.setText("/api/");
		mappingPath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		setControl(container);
	}
	
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			dialogChanged();
		}
	}

	private void dialogChanged() {
		DatabaseObject dbo = ((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean();
		if (dbo != null && dbo instanceof UrlMapping) {
			String path = getPath();
			if (path.length() == 0) {
				updateStatus("Path must be specified");
				return;
			}
			if (!path.startsWith("/api/")) {
				updateStatus("Path must start with \"/api/\"");
				return;
			}
			
			if (parentObject != null && parentObject instanceof UrlMapper) {
				UrlMapper urlMapper = (UrlMapper)parentObject;
				if (urlMapper.getMappingByPath(path) != null) {
					updateStatus("This mapping path already exists");
					return;
				}
				
				UrlMapping urlMapping = (UrlMapping)dbo;
				urlMapping.setPath(path);
			}
		}
		
		updateStatus(null);
	}
	
	public String getPath() {
		return mappingPath.getText();
	}
	
	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	@Override
	public IWizardPage getNextPage() {
		return null;
	}
}
