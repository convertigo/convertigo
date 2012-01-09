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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.DeploymentConfiguration;
import com.twinsoft.convertigo.engine.util.SimpleCipher;

public class TrialRegistrationDialog extends Dialog {
	
	private Text textRegistrationCertificate;
	private Label lblCheckRegistration;
	
	public DeploymentConfiguration deploymentConfiguration;

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public TrialRegistrationDialog(Shell parentShell) {
		super(parentShell);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Trial registration certificate");
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		Link lblEnterYourRegistration = new Link(container, SWT.WRAP);
		lblEnterYourRegistration.addListener (SWT.Selection, new Listener () {
			public void handleEvent(Event event) {
				try {
					Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + event.text);
				} catch (IOException e) {
					ConvertigoPlugin.logException(e, "Unable to launch the default browser!");
				} 
			}
		});
		GridData gd_lblEnterYourRegistration = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblEnterYourRegistration.widthHint = 487;
		gd_lblEnterYourRegistration.heightHint = 150;
		lblEnterYourRegistration.setLayoutData(gd_lblEnterYourRegistration);
		lblEnterYourRegistration.setText("If you have registered for a trial of Convertigo EMS on Convertigo web site (www.convertigo.com), "+
				"you should have received a registration certificate by email. Please enter your registration certificate "+
				"in order to finish your C-EMS studio installation and then click on the Register button.\r\n\r\n" +
				"If not, just click on the Ignore button.\r\n\r\n" +
				"If you want to register, just visit <a href=\"http://www.convertigo.com/download\">www.convertigo.com/download</a>");
		
		Label lblRegistrationCertificate = new Label(container, SWT.NONE);
		lblRegistrationCertificate.setText("Registration certificate:");
		
		textRegistrationCertificate = new Text(container, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);
		textRegistrationCertificate.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkRegistration();
			}
		});
		GridData gd_textRegistrationCertificate = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_textRegistrationCertificate.heightHint = 151;
		textRegistrationCertificate.setLayoutData(gd_textRegistrationCertificate);
		
		lblCheckRegistration = new Label(container, SWT.NONE);
		Display display = Display.getCurrent();
		lblCheckRegistration.setForeground(display.getSystemColor(SWT.COLOR_RED));
		lblCheckRegistration.setText("Wrong registration certificate!");
		GridData gd_lblCheckRegistration = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblCheckRegistration.heightHint = 33;
		lblCheckRegistration.setLayoutData(gd_lblCheckRegistration);
		lblCheckRegistration.setVisible(false);

		return container;
	}

	protected void checkRegistration() {
		try {
			String registrationCertificate = textRegistrationCertificate.getText();
			if (registrationCertificate.equals("")) {
				lblCheckRegistration.setVisible(false);
				return;
			}
			
			registrationCertificate = SimpleCipher.decode(registrationCertificate);
			
			Properties registrationProperties = new Properties();
			registrationProperties.load(new ByteArrayInputStream(registrationCertificate.getBytes()));
			
			String server = registrationProperties.getProperty("server");
			if (server == null) throw new Exception("Invalid registration certificate (missing server)");
			String user = registrationProperties.getProperty("admin.user");
			if (user == null) throw new Exception("Invalid registration certificate (missing user)");
			String password = registrationProperties.getProperty("admin.password");
			if (password == null) throw new Exception("Invalid registration certificate (missing password)");
			if (!user.equals(SimpleCipher.decode(password))) throw new Exception("Invalid registration certificate (invalid password)");
			boolean bHttps = Boolean.parseBoolean(registrationProperties.getProperty("https"));
			
			deploymentConfiguration = new DeploymentConfiguration(server, user, password, bHttps);
			
			lblCheckRegistration.setVisible(false);
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}
		catch(Exception exception) {
			//ConvertigoPlugin.logException(exception, "Error while analyzing the registration certificate", false);			
			lblCheckRegistration.setVisible(true);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.OK_ID, "Register trial",
				true);
		button.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, "Ignore", false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(510, 480);
	}

}
