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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.util.WsReference;

public class WsReferenceImportDialog extends MyAbstractDialog implements Runnable {

	private ProgressBar progressBar = null;
	private Label labelProgression = null;
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
			ImportWsReference wsr = new ImportWsReference(wsdlURL);
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

	class ImportWsReference extends WsReference {
		public ImportWsReference(String wsdlURL) {
			super(wsdlURL);
		}

		@Override
		public void setTaskLabel(String text) {
			setTextLabel(text);
		}

		@Override
		protected HttpConnector importInto(Project project) throws Exception {
			return super.importInto(project);
		}
	}
	
}
