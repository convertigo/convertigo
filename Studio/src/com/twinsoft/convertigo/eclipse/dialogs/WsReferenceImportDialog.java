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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.util.ImportWsReference;

public class WsReferenceImportDialog extends MyAbstractDialog implements Runnable {

	private ProgressBar progressBar = null;
	private Label labelProgression = null;
	private Button useAuthentication = null;
	private Text loginText = null, passwordText = null;
	private Combo combo = null;
	private String wsdlURL = null;
	private Project project;
	private HttpConnector httpConnector = null;
	
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
			combo = ((WsReferenceImportDialogComposite)dialogComposite).combo;
			progressBar = ((WsReferenceImportDialogComposite)dialogComposite).progressBar;
			labelProgression = ((WsReferenceImportDialogComposite)dialogComposite).labelProgression;
			useAuthentication = ((WsReferenceImportDialogComposite)dialogComposite).useAuthentication;
			loginText = ((WsReferenceImportDialogComposite)dialogComposite).loginText;
			passwordText = ((WsReferenceImportDialogComposite)dialogComposite).passwordText;
			
			wsdlURL = combo.getText();
			if (wsdlURL.startsWith("http://") || wsdlURL.startsWith("https://") || wsdlURL.startsWith("file://")) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				getButton(IDialogConstants.CANCEL_ID).setEnabled(false);

				Thread thread = new Thread(this);
				thread.start();
			}
			else {
				setTextLabel("You must enter a valid URL!");
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to import WSDL reference!");
		}
		finally {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
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

		Throwable ex = null;
		try {		
			progressBarThread.start();
			ImportWsReference wsr = new ImportWsReference(wsdlURL, null);
			if (!isAuthenticated(display)) {
				httpConnector = wsr.importInto(project); 
			} else { 
				httpConnector = wsr.importIntoAuthenticated(project, getLogin(display), getPassword(display)); 
			}
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
	
	private boolean isAuthenticated(Display display) {
		final boolean[] isAuthenticated = new boolean[1];
		display.syncExec(new Runnable() {
			public void run() {
				isAuthenticated[0] = useAuthentication.getSelection();
			}
		});
		return isAuthenticated[0];
	}
	
	private String getLogin(Display display) {
		final String[] login = new String[1];
		display.syncExec(new Runnable() {
			public void run() {
				login[0] = loginText.getText();
			}
		});
		return login[0];
	}
	
	private String getPassword(Display display) {
		final String[] password = new String[1];
		display.syncExec(new Runnable() {
			public void run() {
				password[0] = passwordText.getText();
			}
		});
		return password[0];
	}

	/**
	 * @param project
	 */
	public void setProject(Project project) {
		this.project = project;
	}
	
	public HttpConnector getHttpConnector() {
		return httpConnector;
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
