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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.wizards.enums.MobileSencha;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class NewProjectWizardComposite14 extends Composite {

	private Label label1 = null;
	private ModifyListener modifyListener;
	private SelectionListener selectionListener;
	private Combo comboSencha;
	private Boolean frameworkSelected = false;
	private String senchaUrl = "http://www.sencha.com/products/touch/download/1.1.0/";
	private List<String> senchaPaths;
	
	public NewProjectWizardComposite14(Composite parent, int style, ModifyListener ml, SelectionListener sl, NewProjectWizard wizard) {
		super(parent, style);
		modifyListener = ml;
		selectionListener = sl;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);

		Composite compositeHeader = new Composite(this, SWT.NONE);
		compositeHeader.setLayout(gridLayout);
		GridData gridData2 = new GridData();
		gridData2.horizontalSpan = 2;
		gridData2.horizontalAlignment = GridData.FILL;
		compositeHeader.setLayoutData(gridData2);
		
        label1 = new Label(compositeHeader, SWT.NONE);
        label1.setText("Your Mobile project requires the Sencha Touch framework. \n\nPlease configure your Mobile Project here:\n\n");

        Label labelImage = new Label(compositeHeader, SWT.NONE);
        Image image = new Image(getDisplay(), NewProjectWizardComposite14.class.getResourceAsStream("/com/twinsoft/convertigo/eclipse/wizards/images/logo_sencha.png"));
        labelImage.setImage(image);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.END;
        gridData.grabExcessHorizontalSpace = true;
        labelImage.setLayoutData(gridData);
        
        Label labelCombo = new Label(this, SWT.NONE);
      	labelCombo.setText("Selected Sencha Touch framework: ");
      	
      	Composite compositeButton = new Composite(this, SWT.NONE);
      	compositeButton.setLayout(gridLayout);
      	
		comboSencha = new Combo(compositeButton, SWT.READ_ONLY);
		findFiles(Engine.SENCHA_PATH);
		comboSencha.select(comboSencha.getItemCount()-1);
		comboSencha.addSelectionListener(selectionListener);
	
    	Label labelButton = new Label(this, SWT.NONE);
		labelButton.setText("If you do not already have the Sencha Touch framework, you can follow these instructions in order to get it: ");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.verticalIndent = 10;
		labelButton.setLayoutData(gridData);

		Group group = new Group(this, SWT.NONE);
		group.setText("Instructions");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = GridData.CENTER;
		gridData.verticalIndent = 15;
		group.setLayoutData(gridData);
		group.setLayout(gridLayout);
		
		Label labelLink = new Label(group, SWT.NONE);
		labelLink.setText("1 - Download the Sencha Touch framework: ");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.verticalIndent = 10;
		labelLink.setLayoutData(gridData);
			
		Link link = new Link(group, SWT.NONE);
		String linkText = "<a>" + senchaUrl + "</a>";
		link.setText(linkText);
		Rectangle clientArea = group.getClientArea();
		link.setBounds(clientArea.x, clientArea.y, 140, 40);
		link.addListener(SWT.Selection, new Listener () {
			public void handleEvent(Event event) {
				org.eclipse.swt.program.Program.launch(senchaUrl);
			}
		});
		gridData = new GridData();
		gridData.horizontalIndent = 50;
		gridData.verticalIndent = 5;
		gridData.horizontalSpan = 2;
		link.setLayoutData(gridData);

		Label label2 = new Label(group, SWT.NONE);
		label2.setText("2 - Save the \".zip\" downloaded file in the following directory: ");
		gridData = new GridData();
		gridData.verticalIndent = 10;
		gridData.horizontalSpan = 2;
		label2.setLayoutData(gridData);
		
		Text text = new Text(group, SWT.READ_ONLY);
		text.setText(Engine.SENCHA_PATH);
		gridData = new GridData();
		gridData.horizontalIndent = 50;
		gridData.verticalIndent = 5;
		gridData.horizontalSpan = 2;
		text.setLayoutData(gridData);
		
		Label label4 = new Label(group, SWT.NONE);
		label4.setText("4 - Click \"Update\" button below to refresh available frameworks: ");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.verticalIndent = 10;
		label4.setLayoutData(gridData);
		
		Button updateButton = new Button(group, SWT.NONE);
		updateButton.setText("Update frameworks list");
		updateButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				findFiles(Engine.SENCHA_PATH);
				comboSencha.select(comboSencha.getItemCount()-1);
				comboSencha.notifyListeners(SWT.Selection, new Event());
			}
			
		});
		
		
		gridData = new GridData();
		gridData.horizontalIndent = 50;
		gridData.verticalIndent = 5;
		gridData.horizontalSpan = 2;
		updateButton.setLayoutData(gridData);
		
		Label label5 = new Label(group, SWT.NONE);
		label5.setText("Finally you can choose manually the wanted framework or just click \"Finish\". ");
		gridData = new GridData();
		gridData.verticalIndent = 10;
		gridData.horizontalSpan = 2;
		label5.setLayoutData(gridData);
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	public void findFiles(String directoryPath) {
		boolean frameworkFound = false;
		boolean frameworkValid = false;
		File sencha = new File(directoryPath);
		if (!sencha.exists()) {
			sencha.mkdir();
		}
		Pattern p = Pattern.compile("(.*).zip$");
		Matcher m; 
		File[] subfiles = sencha.listFiles();
		comboSencha.removeAll();
		senchaPaths = new ArrayList<String>();		
		for(int i=0 ; i < subfiles.length; i++){
			m = p.matcher(subfiles[i].getName());
			if (m.matches()) {
				try {
					frameworkValid = ZipUtils.checkFilesInZip(Engine.SENCHA_PATH+"/"+subfiles[i].getName(), MobileSencha.getJSFiles(), "-");
				} catch (IOException e) {
					frameworkValid = false;
				}
				if(frameworkValid){
					comboSencha.add(subfiles[i].getName());
					senchaPaths.add(subfiles[i].getPath());
					frameworkFound = true;
				}
			}
		}		
		if (senchaPaths.size() == 0 || !frameworkFound) {
			comboSencha.add("Framework not found!");
			comboSencha.setEnabled(false);
			frameworkSelected = false;
		}
		else {
			comboSencha.setEnabled(true);
			frameworkSelected = true;
		}
	}
	
	public Combo getSenchaCombo() {
		return comboSencha;
	}
	
	public List<String> getSenchaPath() {
		return senchaPaths;
	}
	
	public Boolean getFrameworkSelected() {
		return frameworkSelected;
	}
}
