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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.DeploymentConfiguration;
import com.twinsoft.convertigo.eclipse.DeploymentInformation;
import com.twinsoft.convertigo.engine.Engine;

public class ProjectDeployDialogComposite extends MyAbstractDialogComposite {

	//private TWSKey twsKey;
	public DeploymentInformation deploymentInformation;

	public Tree tree = null;
	public Label label = null;
	public Combo combo = null;
	public Button checkBox = null;
	public Button assembleXsl = null;
	public Button checkTrustAllCertificates = null;
	public Group webGroup = null;
	public Group convertigoGroup = null;
	public Label webAdminLabel = null;
	public Text webAdmin = null;
	public Label webAdminPassword = null;
	public Text webPassword = null;
	public Label convertigoAdminLabel = null;
	public Text convertigoAdmin = null;
	public Label convertigoAdminPassword = null;
	public Text convertigoPassword = null;
	public ProgressBar progressBar = null;
	public Label labelProgress = null;
	public ObjectOutputStream objectOutputStream = null;
	
	public ProjectDeployDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

    private void getDeploymentInformation() {
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + "/_private/deploy.ser"));
            deploymentInformation = (DeploymentInformation) objectInputStream.readObject();
        }
        catch(Exception e) {
            deploymentInformation = new DeploymentInformation();
        }
        
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(Engine.USER_WORKSPACE_PATH + "/studio/trial_deploy.ser"));
            DeploymentConfiguration deploymentConfiguration = (DeploymentConfiguration) objectInputStream.readObject();
            deploymentInformation.deploymentConfigurations.put(deploymentConfiguration.getServer(), deploymentConfiguration);
        }
        catch(Exception e) {
            // Ignore
        }
    }
    
    private void fillComboBox() {
        combo.removeAll();
        
        for (DeploymentConfiguration deploymentConfiguration : deploymentInformation.deploymentConfigurations.values()) {
        	combo.add(deploymentConfiguration.toString());
        }
        
        // Select the first entry if it exists
        if (combo.getItemCount() > 0) {
            combo.select(0);
            DeploymentConfiguration dc = getDeployementConfiguration(combo.getText());
            convertigoAdmin.setText(dc.getUsername());
            convertigoPassword.setText(dc.getUserpassword());
            checkBox.setSelection(dc.isBHttps());
            checkTrustAllCertificates.setSelection(dc.isBTrustAllCertificates());
        }
    }
    
	/**
	 * This method initializes this
	 * 
	 */
	protected void initialize() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		this.setLayout(gridLayout);
		
        label = new Label(this, SWT.NONE);
        label.setText("Convertigo server");
        
        createCombo();
        
        GridData gridData1 = new GridData();
        gridData1.horizontalSpan = 2;
        gridData1.horizontalAlignment = GridData.FILL;
        gridData1.grabExcessHorizontalSpace = true;
        gridData1.verticalIndent = 15;
        checkBox = new Button(this, SWT.CHECK);
        checkBox.setText("HTTPS connection");
        checkBox.setLayoutData(gridData1);
        
        gridData1 = new GridData();
        gridData1.horizontalSpan = 2;
        gridData1.horizontalAlignment = GridData.FILL;
        gridData1.grabExcessHorizontalSpace = true;
        gridData1.verticalIndent = 5;
        
        checkTrustAllCertificates = new Button(this, SWT.CHECK);
        checkTrustAllCertificates.setText("Trust all certificates");
        checkTrustAllCertificates.setLayoutData(gridData1);
        
        createConvertigoGroup();
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
        
        getDeploymentInformation();
        fillComboBox();
	}

	public Object getValue(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * This method initializes combo	
	 *
	 */
	private void createCombo() {
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = GridData.FILL;
		gridData1.verticalAlignment = GridData.CENTER;
		gridData1.grabExcessHorizontalSpace = true;
		combo = new Combo(this, SWT.NONE);
		combo.setLayoutData(gridData1);
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				//String serverName = combo.getText();
	            DeploymentConfiguration dc = getDeployementConfiguration(combo.getText());
	            convertigoAdmin.setText(dc.getUsername());
	            convertigoPassword.setText(dc.getUserpassword());
	            checkBox.setSelection(dc.isBHttps());
			}
		});
		combo.addKeyListener(new KeyListener() {
			
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				if (e.character == SWT.DEL) {
					int index = combo.getSelectionIndex();
					String item = combo.getItem(index);
					removeDeploymentConfiguration(item);
					combo.remove(combo.getSelectionIndex());
				}
			}
		});
	}

	private DeploymentConfiguration getDeployementConfiguration(String name) {
		DeploymentConfiguration dc = null;

		for (DeploymentConfiguration deploymentConfiguration : deploymentInformation.deploymentConfigurations.values()) {
            if  (deploymentConfiguration.getServer().equals(name))
            	return dc = deploymentConfiguration;
        }

        return dc;
	}
	
	private void removeDeploymentConfiguration(String name) {
		Set<String> keys = deploymentInformation.deploymentConfigurations.keySet();
		Iterator<String> it = keys.iterator();
		
		while (it.hasNext()){
		   Object key = it.next();
		    if (key.equals(name)) {
		    	deploymentInformation.deploymentConfigurations.remove(key);
		    	
				try {
		            objectOutputStream = new ObjectOutputStream(new FileOutputStream(Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + "/_private/deploy.ser"));
		            objectOutputStream.writeObject(deploymentInformation);
		            objectOutputStream.flush();
		            objectOutputStream.close();
		        }
		        catch(Exception e) {
		        	ConvertigoPlugin.logException(e, "Unable to save the deployment information.");
		        }
		    }
		}
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
		gridData2.verticalAlignment = GridData.CENTER;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.verticalIndent = 15;
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
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
