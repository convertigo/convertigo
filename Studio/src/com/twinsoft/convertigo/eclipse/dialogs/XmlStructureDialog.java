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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectsExplorerComposite;
import com.twinsoft.convertigo.engine.util.StepUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class XmlStructureDialog extends Dialog implements Runnable {

	private String xml = null;
	private String dialogTitle;
	private Object parentObject = null;
	public Object result = null;
	
	public XmlStructureDialog(Shell parentShell, Object parentObject) {
		this(parentShell, ObjectsExplorerComposite.class, "Xml structure", parentObject);
	}
	
	public XmlStructureDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, Object parentObject) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.parentObject = parentObject;
	}
	
	public XmlStructureDialog(Shell parentShell, Object parentObject, String xmlContent) {
		this(parentShell, ObjectsExplorerComposite.class, "Xml structure", parentObject);
		xml = xmlContent;
	}
	
	private XmlStructureDialogComposite schemaObjectsDialogComposite = null;
	private ProgressBar progressBar = null;
	private Label labelProgression = null;

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		try {
			GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
			
			if (xml == null)
				schemaObjectsDialogComposite = new XmlStructureDialogComposite(composite, SWT.NONE, parentObject);
			else
				schemaObjectsDialogComposite = new XmlStructureDialogComposite(composite, SWT.NONE, parentObject, xml);
			
			schemaObjectsDialogComposite.setLayoutData(gridData);
			labelProgression = schemaObjectsDialogComposite.labelProgression;
			progressBar = schemaObjectsDialogComposite.progressBar;
		}
		catch(Exception e) {
			
		}
		
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar =  super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText("Import");
		getButton(IDialogConstants.OK_ID).setEnabled(true);
		return buttonBar;
	}
	
	public void enableOK(boolean enabled) {
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(dialogTitle);
	}
	
	@Override
	protected int getShellStyle() {
		return SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL;
	}
	
	@Override
	protected void okPressed() {
		try {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
			
			if (schemaObjectsDialogComposite != null) {
				xml = (String)schemaObjectsDialogComposite.getValue(null);
			}
			
			Thread thread = new Thread(this);
			thread.start();
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to import objects");
		}
		finally {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
		}
	}
	
	public void run() {
		final Display display = getParentShell().getDisplay();
		Thread progressBarThread = new Thread("Progress Bar thread") {
			@Override
			public void run() {
				int i = 0;
				while (true) {
					try {
						i += 5;
						if (i >= 100) i = 0;
						final int j = i;
						display.asyncExec(new Runnable() {
							public void run() {
								if (!progressBar.isDisposed())
									progressBar.setSelection(j);
							}
						});
						
						sleep(500);
					}
					catch(InterruptedException e) {
						break;
					}
				}
			}
		};
		
		try {
			progressBarThread.start();
			if (xml != null) {
				setTextLabel("Generating structure from xml");
				Document document = XMLUtils.parseDOMFromString(xml);
        		if (document != null) {
	        		System.out.println(XMLUtils.prettyPrintDOM(document));
	        		setTextLabel("Creating steps from xml");
	        		Element root = document.getDocumentElement();
	        		if (root != null)
	        			result = StepUtils.createStepFromXmlDomModel(parentObject, root);
        		}
			}
		}
		catch (Throwable e) {
			result = e;
		}
		finally {
			try {
				progressBarThread.interrupt();
				
				display.asyncExec(new Runnable() {
					public void run() {
						setReturnCode(OK);
						close();
					}
				});
				
			}
			catch (Throwable e) {}
		}
	}
	
	public void setTextLabel(String text) {
		final Display display = getParentShell().getDisplay();
		final String labelText = text;
		display.asyncExec(new Runnable() {
			public void run() {
				if (!labelProgression.isDisposed())
					labelProgression.setText(labelText);
			}
		});
	}
	
}
