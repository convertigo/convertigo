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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.DeploymentConfiguration;
import com.twinsoft.convertigo.eclipse.DeploymentConfigurationReadOnly;

public class ProjectDeployDialogComposite extends MyAbstractDialogComposite {

	//private TWSKey twsKey;
	
	Tree tree = null;
	Label label = null;
	Button delButton = null;
	Button checkBox = null;
	Button assembleXsl = null;
	Button checkTrustAllCertificates = null;
	Group webGroup = null;
	Group convertigoGroup = null;
	Group SSLGroup = null;
	Label webAdminLabel = null;
	Text webAdmin = null;
	Label webAdminPassword = null;
	Text webPassword = null;
	Label convertigoAdminLabel = null;
	Text convertigoAdmin = null;
	Label convertigoAdminPassword = null;
	Text convertigoPassword = null;
	Text convertigoServer = null;
	ProgressBar progressBar = null;
	Label labelProgress = null;
	MessageBox messageBox = null;
	List list = null;
	ObjectOutputStream objectOutputStream = null;	
	Button okButton = null;

	static String messageList = "-- No deployment configuration saved --";
	
	public ProjectDeployDialogComposite(Composite parent, int style) {
		super(parent, style);
	}
	
    void fillList() {
    	if (list.isDisposed()) {
    		return;
    	}
    	
    	list.removeAll();
        
        SortedSet<String> deploymentConfigurationNames = new TreeSet<String>(ConvertigoPlugin.deploymentConfigurationManager.getAllDeploymentConfigurationNames());
        
        for (String deploymentConfigurationName: deploymentConfigurationNames) {
        	list.add(ConvertigoPlugin.deploymentConfigurationManager.get(deploymentConfigurationName).toString());
        }
        
        String currentProjectName = ConvertigoPlugin.projectManager.currentProjectName;
        
        DeploymentConfiguration defaultDeploymentConfiguration = null;

        try {
        	defaultDeploymentConfiguration = ConvertigoPlugin.deploymentConfigurationManager.getDefault(currentProjectName);
        }
        catch (NullPointerException e) {
        	// No default configuration
        }
        
        if (list.getItemCount() > 0) {
        	String [] items = list.getItems();
        	boolean found = false;
        	for (int i=0; i < list.getItemCount(); i++) {
        		if (defaultDeploymentConfiguration != null) {
            		if (items[i].equals(defaultDeploymentConfiguration.getServer())) {
            			list.select(i);
            			DeploymentConfiguration dc = ConvertigoPlugin.deploymentConfigurationManager.get(list.getSelection()[0]);
            			fillDialog(dc);
            			found = true;
            		}
        		}
        	}
        	if (!found) {
        		list.select(0);
        		DeploymentConfiguration dc = ConvertigoPlugin.deploymentConfigurationManager.get(list.getSelection()[0]); 
    			fillDialog(dc);
        	}
        } else {
        	list.add(messageList);
        	convertigoAdmin.setText("");
            convertigoPassword.setText("");
            checkBox.setSelection(false);
            checkTrustAllCertificates.setSelection(false);
            convertigoServer.setText("");
            assembleXsl.setSelection(false);
            convertigoServer.setText("");
        }
        
        if (list.getSelectionIndex() == -1) {
        	delButton.setEnabled(false);
        }
    }
    
    
    private void fillDialog(DeploymentConfiguration dc) {
    	convertigoAdmin.setText(dc.getUsername());
        convertigoPassword.setText(dc.getUserpassword());
        checkBox.setSelection(dc.isBHttps());
        checkTrustAllCertificates.setSelection(dc.isBTrustAllCertificates());
        convertigoServer.setText(dc.getServer());
        assembleXsl.setSelection(dc.isBAssembleXsl());
        convertigoServer.setText(dc.getServer());
        delButton.setEnabled(!(dc instanceof DeploymentConfigurationReadOnly));
    }
    
	/**
	 * This method initializes this
	 * 
	 */
    @Override
	protected void initialize() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		
        label = new Label(this, SWT.NONE);
        label.setText("Convertigo server");
        
        GridData gridData1 = new GridData();
        gridData1.horizontalSpan = 2;
        gridData1.horizontalAlignment = GridData.FILL;
        gridData1.grabExcessHorizontalSpace = true;
        convertigoServer = new Text(this, SWT.BORDER);
        convertigoServer.setLayoutData(gridData1);
        
        convertigoServer.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
            	changeOkButtonState();
            }
          });

        createList(); 
        createConvertigoGroup();
        createSSLGroup();
        
        GridData gridData3 = new GridData();
        gridData3.horizontalSpan = 2;
        gridData3.horizontalAlignment = GridData.FILL;
        gridData3.grabExcessHorizontalSpace = true;
        gridData3.verticalIndent = 15;
        assembleXsl = new Button(this, SWT.CHECK);
        assembleXsl.setText("Assemble xsl files included in stylesheets");
        assembleXsl.setLayoutData(gridData3);
        
        GridData gridData2 = new GridData();
		gridData2.horizontalSpan = 2;
		gridData2.verticalAlignment = GridData.CENTER;
		gridData2.horizontalAlignment = GridData.FILL;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.verticalIndent = 20;
        labelProgress = new Label(this, SWT.NONE);
        labelProgress.setText("Progression");
        labelProgress.setLayoutData(gridData2);
        
        GridData gridData4 = new GridData();
		gridData4.horizontalSpan = 2;
		gridData4.verticalAlignment = GridData.CENTER;
		gridData4.horizontalAlignment = GridData.FILL;
		gridData4.grabExcessHorizontalSpace = true;
        progressBar = new ProgressBar(this, SWT.NONE);
        progressBar.setLayoutData(gridData4);
        
        fillList();
	}

	public Object getValue(String name) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * This method initializes list	
	 *
	 */
	private void createList() {
		
		Composite composite = new Composite(this, SWT.NONE);
		
		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		
		composite.setLayoutData(gridData);
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		composite.setLayout(gridLayout);

		list = new List(composite, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);

		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		list.setLayoutData(gridData);
		
		delButton = new Button(composite, SWT.PUSH);
		delButton.setText("Remove");
		
		list.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent evt) {
				if (list.getSelectionIndex() != -1 && !list.getItem(list.getSelectionIndex()).equals(messageList)) {
		            DeploymentConfiguration dc = ConvertigoPlugin.deploymentConfigurationManager.get(list.getSelection()[0]);
		            convertigoAdmin.setText(dc.getUsername());
		            convertigoPassword.setText(dc.getUserpassword());
		            checkBox.setSelection(dc.isBHttps());
		            checkTrustAllCertificates.setSelection(dc.isBTrustAllCertificates());
		            convertigoServer.setText(dc.getServer());
		            assembleXsl.setSelection(dc.isBAssembleXsl());
		            delButton.setEnabled(!(dc instanceof DeploymentConfigurationReadOnly));
		            changeOkButtonState();
				}
			}
			
		});
		
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.END;
		gridData.verticalAlignment = GridData.BEGINNING;
		delButton.setLayoutData(gridData);
		
		delButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = list.getSelectionIndex();
				String item = list.getItem(index);
				int response = 0;
				if (!item.equals(messageList)) {
					messageBox = new MessageBox(parentDialog.getShell(), SWT.YES | SWT.NO);
				    messageBox.setMessage("Do you really want to delete this configuration? \n\n\t" + item);
				    messageBox.setText("Deleting configuration");
				    response = messageBox.open();
				}				    
			    if (response == SWT.YES) {			    	
			    	try {
			    		ConvertigoPlugin.deploymentConfigurationManager.remove(ConvertigoPlugin.deploymentConfigurationManager.get(item));
					} catch (IOException e1) {
						ConvertigoPlugin.logException(e1, "Unable to remove the deployment configurations");
					}
					fillList();
			    }				  
			}
			
		});
	}
	
	private void createSSLGroup() {
		SSLGroup = new Group(this, SWT.FILL);
		SSLGroup.setLayout(new GridLayout());
		SSLGroup.setText("SSL options");
		
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalIndent = 10;
		SSLGroup.setLayoutData(gridData);
		
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.CENTER;
		gridData.verticalIndent = 20;
        checkBox = new Button(SSLGroup, SWT.CHECK);
        checkBox.setText("HTTPS connection");
        checkBox.setLayoutData(gridData);
        
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.verticalAlignment = GridData.FILL;
        gridData.verticalIndent = 25;
        
        checkTrustAllCertificates = new Button(SSLGroup, SWT.CHECK);
        checkTrustAllCertificates.setText("Trust all certificates");
        checkTrustAllCertificates.setLayoutData(gridData);
	}

	/**
	 * This method initializes convertigoGroup	
	 *
	 */
	private void createConvertigoGroup() {
		convertigoGroup = new Group(this, SWT.FILL);
		convertigoGroup.setLayout(new GridLayout());
		convertigoGroup.setText("Convertigo server login");
		
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = GridData.FILL;
		gridData2.verticalAlignment = GridData.FILL;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.verticalIndent = 10;
		convertigoGroup.setLayoutData(gridData2);
		
		GridData gridData3 = new GridData();
		gridData3.horizontalAlignment = GridData.FILL;
		gridData3.verticalAlignment = GridData.CENTER;
		gridData3.grabExcessHorizontalSpace = true;
		convertigoAdminLabel = new Label(convertigoGroup, SWT.NONE);
		convertigoAdminLabel.setText("Server administrator");
		convertigoAdmin = new Text(convertigoGroup, SWT.BORDER);
		convertigoAdmin.setLayoutData(gridData3);
		
		GridData gridData5 = new GridData();
		gridData5.horizontalAlignment = GridData.FILL;
		gridData5.verticalAlignment = GridData.CENTER;
		gridData5.grabExcessHorizontalSpace = true;
		convertigoAdminPassword = new Label(convertigoGroup, SWT.NONE);
		convertigoAdminPassword.setText("Password");
		convertigoPassword = new Text(convertigoGroup, SWT.BORDER | SWT.PASSWORD);
		convertigoPassword.setLayoutData(gridData5);
	}
	
	private void changeOkButtonState() {
		if (okButton != null) {
			okButton.setEnabled(false);
			if (list.getSelectionIndex() != -1) {
				okButton.setEnabled(true);
			}
			if (!("").equals(convertigoServer.getText())) {
				okButton.setEnabled(true);
			}
		}
	}

	public void setOkButton(Button okButton) {
		this.okButton = okButton;
		changeOkButtonState();
	}
	
}
