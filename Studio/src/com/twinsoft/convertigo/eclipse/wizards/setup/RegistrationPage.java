package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class RegistrationPage extends WizardPage {
	
	private Text userName;
	private Text password;
	private Text mail;
	private Button acceptCondition;
	
	private Composite container;
	
	public RegistrationPage() {
		super("Registration");
		setTitle("Registration");
		setDescription("User registration.");
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.marginWidth = 30;
		
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.verticalIndent = 5;
		
		Label label = new Label(container, SWT.NONE);
		label.setText("User name *");
//		label.setLayoutData(layoutData);
		
		userName = new Text(container, SWT.BORDER);
		userName.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (userName.getText().length() > 0) {
					setMessage(getDescription());;
//					setPageComplete(true);
				} else {
					setErrorMessage("Please enter the user name!");
				}
			}
		});
		userName.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Password *");
		
		password = new Text(container, SWT.PASSWORD | SWT.BORDER);
		password.setText("0000");
		password.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				int nbPassword = password.getText().length();
				if (password.getText().length() > 0) {
					if (nbPassword >= 4 && nbPassword <= 18) {
						setErrorMessage(null);
						setMessage(getDescription());
	//					setPageComplete(true);
					} else {
						setErrorMessage("The password should be from 4 to 18 characters long.!");
					}
				} else {
					setErrorMessage("Please enter your password!");
				}
			}
		});
		password.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Confirm password *");
		
		final Text passwordAgain = new Text(container, SWT.PASSWORD | SWT.BORDER);
		passwordAgain.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (passwordAgain.getText().length() > 0) {
					String pass = password.getText();
					if (pass.equals(passwordAgain.getText())) {
						setErrorMessage(null);
						setMessage(getDescription());
	//					setPageComplete(true);
					} else {
						setErrorMessage("Incorrect password!");
					}
				} else {
					setErrorMessage("Please enter your password again!");
				}
			}
		});
		passwordAgain.setLayoutData(layoutData);
		
		label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gLayoutData.verticalIndent = 5;
		gLayoutData.horizontalSpan = 2;
		label.setLayoutData(gLayoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Mail *");
		
		mail = new Text(container, SWT.BORDER);
		mail.setText("user@convertigo.com");
		mail.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				String email = mail.getText();
				if (isEmailAdress(email)) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter a valid mail!");
				}
				if (mail.getText().length() > 0) {
					setMessage(getDescription());
//					setPageComplete(true);
				} else {
					setErrorMessage("Please enter your mail!");
				}
			}
		});
		mail.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Confirm mail *");
		
		final Text mailAgain = new Text(container, SWT.BORDER);
		mailAgain.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (mailAgain.getText().length() > 0) {
					String pass = mail.getText();
					if (pass.equals(mailAgain.getText())) {
						setErrorMessage(null);
						setMessage(getDescription());
//						setPageComplete(true);
					} else {
						setErrorMessage("Incorrect mail!");
					}
				} else {
					setErrorMessage("Please enter your mail again!");
				}
			}
		});
		mailAgain.setLayoutData(layoutData);
		
		GridData gdData = new GridData(GridData.VERTICAL_ALIGN_END);
		gdData.verticalIndent = 20;
		acceptCondition = new Button(container, SWT.CHECK);
		acceptCondition.setText("Accept Terms and Conditions!");
//		acceptCondition.setEnabled(false);
		
		acceptCondition.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				boolean isChecked = acceptCondition.getSelection();
				if (isChecked) {
					if (userName.getText().length() == 0 && password.getText().length() == 0 && mail.getText().length() == 0) {
						setErrorMessage("please complete the fields required");
						acceptCondition.setSelection(false);
					} else {
						if (password.getText().equals(passwordAgain.getText()) && mail.getText().equals(mailAgain.getText())) {
							setErrorMessage(null);
							setMessage(getDescription());
							setPageComplete(true);
						} else {
							setErrorMessage("please complete the fields required");
							acceptCondition.setSelection(false);
						}
					}
				} else {
					setPageComplete(false);
				}
			}
		});
		acceptCondition.setLayoutData(gdData);
		
		

		// Required to avoid an error in the system
		setControl(container);
		setPageComplete(false);
	}
	
	

	public static boolean isEmailAdress(String email){
		Pattern p = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@" +
		           "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" +  "\\." +
		           "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+");
		Matcher m = p.matcher(email.toUpperCase());
		return m.matches();
		}

	public String getUserName() {
		return userName.getText();
	}
	
	public String getPassword() {
		return password.getText();
	}
	
	public String getMail() {
		return mail.getText();
	}
}
