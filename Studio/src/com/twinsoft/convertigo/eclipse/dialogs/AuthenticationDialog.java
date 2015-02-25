/**
 * @author julienda
 */
package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AuthenticationDialog extends Dialog{
	
	private Text loginText = null, passwordText = null;
	private String login, password;
	
	/**
	 * @param parentShell
	 */
	public AuthenticationDialog(Shell parentShell) {
		super(parentShell);
		this.setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Authentication required");
		newShell.setSize(350, 120); 
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = parent;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.makeColumnsEqualWidth = false;

		container.setLayout(gridLayout);
		
		GridData gridData = new GridData ();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		
		ModifyListener ml = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
		
		loginText = new Text(container, SWT.NONE);
		loginText.setMessage("Login");
		loginText.addModifyListener(ml);
		loginText.setLayoutData(gridData);
		
		passwordText = new Text(container, SWT.SINGLE | SWT.PASSWORD);
		passwordText.setMessage("Password");
		passwordText.addModifyListener(ml);
		passwordText.setLayoutData(gridData);
		
		return container;
	}

	private void dialogChanged() {
		if (loginText != null && passwordText != null) {
			login = loginText.getText();
			password = passwordText.getText();
		}
	}
	
	
	public String getLogin() {
		return login;
	}
	
	public String getPassword(){
		return password;
	}
}
