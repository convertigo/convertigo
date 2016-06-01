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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.util.ImportWsReference;

public class WsReferenceImportDialog extends MyAbstractDialog implements Runnable {

	private ProgressBar progressBar = null;
	private RemoteFileReference wsReference;
	private HttpConnector httpConnector = null;
	private Project project = null;;
	
	/**
	 * @param parentShell
	 * @param dialogAreaClass
	 * @param dialogTitle
	 */
	public WsReferenceImportDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle) {
		super(parentShell, dialogAreaClass, dialogTitle);
	}

	/**
	 * @param parentShell
	 * @param dialogAreaClass
	 * @param dialogTitle
	 * @param width
	 * @param height
	 */
	public WsReferenceImportDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, int width, int height) {
		super(parentShell, dialogAreaClass, dialogTitle, width, height);
	}
	
	protected void okPressed() {		
		try {
			progressBar = ( (WsReferenceImportDialogComposite)dialogComposite ).progressBar;
			
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
	
			Thread thread = new Thread(this);
			thread.start();
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to import WS reference!");
		}
	}

	public void run() {		
		
		final Display display = getParentShell().getDisplay();
		Thread progressBarThread = new Thread("Progress Bar thread") {
			public void run() {
				int i = 0;
				while (true) {
					try {
						i += 5;
						if (i >= 100) i = 0;
						final int j = i;
						display.asyncExec(new Runnable() {
							public void run() {
								if (progressBar != null) {
									if (!progressBar.isDisposed()) {
										progressBar.setSelection(j);
									}
								}
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

		Throwable ex = null;
		try {		
			progressBarThread.start();
			
			ImportWsReference wsr = null;
			if (wsReference instanceof WebServiceReference)
				wsr = new ImportWsReference((WebServiceReference)wsReference);
			if (wsReference instanceof RestServiceReference)
				wsr = new ImportWsReference((RestServiceReference)wsReference);
			
			httpConnector = wsr.importInto(project);
		}
		catch (Throwable e) {
			ex = e;
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
			
			if (ex != null) {
				ConvertigoPlugin.logException(ex, "Unable to import from WSDL");
			}
		}
	}
	
	public void setReference(RemoteFileReference webServiceReference) {
		this.wsReference = webServiceReference;
	}
	
	protected RemoteFileReference getReference() {
		return this.wsReference;
	}
	
	public void setProject(Project project) {
		this.project = project;
	}
	
	protected Project getProject() {
		return this.project;
	}
	
	public HttpConnector getHttpConnector() {
		return httpConnector;
	}
	
	protected Button getButtonOK(){
		return getButton(OK);
	}
	
}
