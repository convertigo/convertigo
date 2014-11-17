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

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
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
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.StepUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SchemaObjectsDialog extends Dialog implements Runnable {

	private String dialogTitle;
	private Object parentObject = null;
	private XmlSchema xmlSchema = null;
	private QName qname = null;
	public Object result = null;
	
	public SchemaObjectsDialog(Shell parentShell, Object parentObject, XmlSchema xmlSchema) {
		this(parentShell, ObjectsExplorerComposite.class, "Schema elements", parentObject);
		this.xmlSchema = xmlSchema;
	}
	
	public SchemaObjectsDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, Object parentObject) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.parentObject = parentObject;
	}
	
	private SchemaObjectsDialogComposite schemaObjectsDialogComposite = null;
	private ProgressBar progressBar = null;
	private Label labelProgression = null;
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		try {
			GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
			
			schemaObjectsDialogComposite = new SchemaObjectsDialogComposite(composite,SWT.NONE,parentObject,xmlSchema);
			schemaObjectsDialogComposite.setLayoutData(gridData);
			schemaObjectsDialogComposite.list.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					enableOK(schemaObjectsDialogComposite.list.getSelectionCount() > 0);
				}
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
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
		getButton(IDialogConstants.OK_ID).setEnabled(false);
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
				qname = (QName)schemaObjectsDialogComposite.getValue(null);
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
			if (qname != null) {
				setTextLabel("Generating xml structure from schema object");
        		//Document document = xsd.generateElementXmlStructure(qname);
				XmlSchemaElement xso = SchemaMeta.getCollection(xmlSchema).getElementByQName(qname);
				SchemaMeta.setSchema(xso, xmlSchema);
				Document document = XmlSchemaUtils.getDomInstance(xso);
        		if (document != null) {
	        		//String s = XMLUtils.prettyPrintDOM(document);
	        		setTextLabel("Creating steps from xml");
	        		Element root = document.getDocumentElement();
	        		Element firstChild = (Element)root.getFirstChild();
	        		if (firstChild != null)
	        			result = StepUtils.createStepFromSchemaDomModel(parentObject, firstChild);
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
