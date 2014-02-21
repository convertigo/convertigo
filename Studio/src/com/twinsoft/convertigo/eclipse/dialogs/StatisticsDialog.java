/*
* Copyright (c) 2009-2014 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.engine.util.ProjectUtils;

public class StatisticsDialog extends Dialog {
	private Display display;
	private Label topLabel;
	private Label labelImage;
	private Image imageLeft;
	private Composite descriptifRight;
	private String projectName, comment, version;
	private int nWidth = 800;
	private int nHeight = 500;
 
	/**
	 * Create the dialog.
	 * @param parentShell, errorMessage
	 */
	public StatisticsDialog(Shell parentShell, String projectName, String comment, String version) {
		super(parentShell);
		this.projectName = projectName;
		this.comment = comment;
		this.version = version;
		
		this.setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
	}

	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Statistics");
		newShell.setSize(nWidth, nHeight); 
		display = newShell.getDisplay();
		
		int nLeft = 0;
		int nTop = 0;
		 
		Display display = newShell.getDisplay();

		Point pt = display.getCursorLocation();
	    Monitor [] monitors = display.getMonitors();

	    for (int i= 0; i<monitors.length; i++) {
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
	
	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing = 5;
		gridLayout.numColumns = 2;
		Color white = new Color(display, 255,255,255);
		container.setLayout(gridLayout);
		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		
		topLabel = new Label(container, SWT.NONE);
		topLabel.setText(projectName+(version.equals("") ? "" : " v"+version)+"\n"+comment);
		topLabel.setLayoutData(gridData);		
		
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessHorizontalSpace = false;
		
		imageLeft = new Image(display, getClass().getResourceAsStream("images/dialog_statistic.jpg"));
		labelImage = new Label(container, SWT.NONE);
		labelImage.setImage(imageLeft);
		labelImage.setBackground(white);
		labelImage.setLayoutData(gridData);
		
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessHorizontalSpace = true;
		descriptifRight = new Composite(container, SWT.NONE);		
		
		descriptifRight.setBackground(white);
		descriptifRight.setLayoutData(gridData);
		gridLayout = new GridLayout();
		descriptifRight.setLayout(gridLayout);
		
		addStats();
		
		return container;
	}
	
	private void addStats() {
		ProjectTreeObject projectTreeObject = (ProjectTreeObject)ConvertigoPlugin.getDefault().getProjectExplorerView().getFirstSelectedTreeObject();
		Project project = (Project) projectTreeObject.getObject();
		
		Map<String, String> statsProject = null;
		try {
			statsProject = ProjectUtils.getStatByProject(project);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Exception while computing statistics");
		}
		CLabel projectInfo = new CLabel(descriptifRight, SWT.NONE);
		projectInfo.setImage(new Image(display, getClass().getResourceAsStream("images/project_16x16.png")));	                                                                
		projectInfo.setText(statsProject.get(project.getName()).replaceAll("<br/>", "\r\n").replaceAll("&nbsp;", " "));
		projectInfo.setBackground(new Color(display, 255,255,255));
		
		for (String key: statsProject.keySet()) {
			if (key != project.getName()) {
				CLabel t = new CLabel(descriptifRight,SWT.NONE);
				t.setText(key+"\n"+statsProject.get(key).replaceAll("<br/>", "\r\n").replaceAll("&nbsp;", " "));
				t.setImage(new Image(display, getClass().getResourceAsStream("images/"+key.replaceAll(" ", "_").toLowerCase()+"_16x16.png")));
				t.setBackground(new Color(display, 255,255,255));
			}
		}
	}
	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button buttonOk = createButton(parent, IDialogConstants.OK_ID, "OK", true);
		buttonOk.setEnabled(true);
	}
}