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

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import org.eclipse.jface.viewers.ISelection;
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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.eclipse.property_editors.EmulatorTechnologyEditor;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectExplorerWizardPage;

public class ServiceCodeWizardPage extends WizardPage {
	protected Text textConnectionParameter;
	protected Text textHostName;
	protected Text textHostPort;
	
	private String connectionType;
	private String serviceCode = "";
	
	private final String descriptionMessage = "Please enter a connection address for connector object.";
	
	public ServiceCodeWizardPage() {
		super("ServiceCodeWizardPage");
		setTitle("Connection address");
		setDescription(descriptionMessage);
	}
	
	public ServiceCodeWizardPage(ISelection selection) {
		super("ServiceCodeWizardPage");
		setTitle("Connection address");
		setDescription(descriptionMessage);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		
		GridData gridData3;
		Label label1;
		IWizardPage wp = getWizard().getPage("ObjectExplorerWizardPage");
		if (wp == null) {
			gridData3 = new org.eclipse.swt.layout.GridData();
	        gridData3.horizontalSpan = 2;
	        label1 = new Label(container, SWT.NONE);
	        label1.setText("The chosen project template includes a ''screen'' connector. \n\nThis connector needs:\n\t a destination address, as a hostname (or IP adress) and optionally a port,\n\t a connection parameter, optional.\n ");
	        label1.setLayoutData(gridData3);
		}
		GridData gridData2 = new org.eclipse.swt.layout.GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData2.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData1 = new org.eclipse.swt.layout.GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.grabExcessHorizontalSpace = false;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalSpan = 2;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.verticalSpacing = 5;
		Label labelHelp = new Label(container, SWT.NONE);
		labelHelp.setText("The connection parameter has different meanings according to the emulator technology:\n\t3270: TN3270 device name\n\t5250: TN5250 device name\n\tDKU: MAILBOX\n\tMinitel: service code (e.g. '3615SNCF')\n\n");
		labelHelp.setLayoutData(gridData);
		Label labelConnectionParameter = new Label(container, SWT.NULL);
		labelConnectionParameter.setText("Connection parameter");
		textConnectionParameter = new Text(container, SWT.BORDER);
		textConnectionParameter.setLayoutData(gridData1);
		textConnectionParameter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		Label labelHostName = new Label(container, SWT.NULL);
		labelHostName.setText("Host name");
		textHostName = new Text(container, SWT.BORDER);
		textHostName.setLayoutData(gridData2);
		textHostName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		Label labelHostPort = new Label(container, SWT.NULL);
		labelHostPort.setText("Host port");
		textHostPort = new Text(container, SWT.BORDER);
		textHostPort.setLayoutData(gridData2);
		textHostPort.addModifyListener(new ModifyListener() {
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
		IWizardPage wp = getWizard().getPage("ObjectExplorerWizardPage");
		if (wp != null) {
		// connector object add
			EmulatorTechnologyWizardPage emulTechWP = (EmulatorTechnologyWizardPage)getWizard().getPage("EmulatorTechnologyWizardPage");
			String emulTech = emulTechWP.getEmulatorTechnology();
			String[] tags = EmulatorTechnologyEditor.getTags(null);
			String[] classNames = EmulatorTechnologyEditor.getEmulatorClassNames(null);
			int i = 0;
			boolean found = false;
			while(i < classNames.length && !found) {
				if (classNames[i].equals(emulTech))
					found = true;
				else
					i ++;
			}
			String testedValue = "";
			if (found)
				testedValue = tags[i]; 
			
			if (testedValue.equals(EmulatorTechnologyEditor.BULLDKU)) {
				connectionType = "TCP";
				textConnectionParameter.setText("");
				textHostName.setText("localhost");
				textHostPort.setText("23");
			} else if (testedValue.equals(EmulatorTechnologyEditor.IBM3270) 
					|| testedValue.equals(EmulatorTechnologyEditor.IBM5250)
					|| testedValue.equals(EmulatorTechnologyEditor.VT)) {
				connectionType = "DIR";
				textConnectionParameter.setText("");
				textHostName.setText("localhost");
				textHostPort.setText("23");
			} else if (testedValue.equals(EmulatorTechnologyEditor.VDX)) {
				connectionType = "";
				textConnectionParameter.setText("3615SNCF");
				textHostName.setText("");
				textHostPort.setText("");
			} else {
				connectionType = "";
			}
		} else {
		// new project creation
			NewProjectWizard newProjW = (NewProjectWizard)getWizard();
			switch (newProjW.templateId) {
			case NewProjectWizard.TEMPLATE_WEB_HTML_BULL_DKU_7107:
			case NewProjectWizard.TEMPLATE_EAI_BULL_DKU_7107:
				connectionType = "TCP";
				break;
			case NewProjectWizard.TEMPLATE_WEB_HTML_IBM_3270:
			case NewProjectWizard.TEMPLATE_WEB_HTML_IBM_5250:
			case NewProjectWizard.TEMPLATE_EAI_IBM_3270:
			case NewProjectWizard.TEMPLATE_EAI_IBM_5250:
			case NewProjectWizard.TEMPLATE_EAI_UNIX_VT220:
				connectionType = "DIR";
				break;
			default:
				connectionType = "";
				break;
			}
		}
	}
	
	private void dialogChanged() {
		String servCode = buildServiceCode();
		if (servCode.length() == 0) {
			updateStatus("Connection address must be specified");
			return;
		}
		
		IWizardPage wp = getWizard().getPage("ObjectExplorerWizardPage");
		if (wp != null) {
			try {
				DatabaseObject dbo = ((ObjectExplorerWizardPage)wp).getCreatedBean();
				if (dbo != null) {
					if (dbo instanceof JavelinConnector)
						((JavelinConnector)dbo).setServiceCode(servCode);
				}
			} catch (NullPointerException e) {
				updateStatus("New bean has not been instantiated");
				return;
			}
		} else {
			setServiceCode(servCode); 
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	private String buildServiceCode() {
		String param = textConnectionParameter.getText();
		String hostName = textHostName.getText();
		String hostPort = textHostPort.getText();
		String host = "";
		if (!hostName.equals("")) {
			if (!hostPort.equals("")) {
				host = hostName + ":" + hostPort;
			} else {
				host = hostName;
			}
		}
		
		// for update status not to work when no data has been typed
		if ( (connectionType.length() == 0 && param.length() == 0) // empty connection type and no parameter filled
			|| connectionType.length() != 0 && host.length() == 0) // not empty connection type and no host filled
			return "";
		
		// real service code building
		if (connectionType.equals(""))
			return param;
		else
			return param + "," + connectionType + "|" + host;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}
	
	public void update() {
		initialize();
	}
}