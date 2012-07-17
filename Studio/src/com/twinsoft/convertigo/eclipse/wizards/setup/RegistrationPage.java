package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class RegistrationPage extends WizardPage {
	
	private Text firstName;
	private Text lastName;
	private Text userName;
	private Text password;
	private Text mail;
	private Button acceptTerms;
	private Button psc;
	
	private Composite container;
	
	public RegistrationPage() {
		super("Registration");
		setTitle("Convertigo Forum Account and PSC.");
		setDescription("Convertigo Forum Account Creation and Personal Studio Configuration.");
	}

	public void createControl(Composite parent) {
		final SetupWizard setupWizard = (SetupWizard) getWizard();
		final Boolean isConnect = isInternetAccess();
		
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		
		layout.numColumns = 3;
		layout.marginWidth = 30;		
		
		GridData layoutDataDescription = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataDescription.horizontalSpan = 3;
		layoutDataDescription.widthHint = 300;
		
		
		Label description = new Label(container, SWT.WRAP);
		description.setText("Filling this form will automatically create a Convertigo Forum account for you to be able " +
				"to consult the forum and also ask for help or advice." +
				"This process will also provide you a Personal Studio Configuration that will automatically configure " +
				"your Studio for projects deployments on Convertigo Cloud.");
		description.setLayoutData(layoutDataDescription);
		description.setFont(new Font(container.getDisplay(),"Arial", 10, SWT.NONE));
		
		GridData gLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gLayoutData.heightHint = 10;
		gLayoutData.horizontalSpan = 3;
		Label label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(gLayoutData);
		
		GridData layoutDataText = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataText.horizontalSpan = 3;
		
		Label text = new Label(container, SWT.WRAP);
		if (!isConnect) {
			text.setText("This procedure requires Internet access.");
			text.setFont(new Font(container.getDisplay(),"Arial", 8, SWT.BOLD));
			text.setForeground(new  Color(container.getDisplay(), 51,102,255));
			text.setLayoutData(layoutDataText);

			text = new Label(container, SWT.WRAP);
			text.setText("If you can not access the Internet from this computer click \"Next\"," +
					"or if you already own a Personal Studio Configuration, choose the following option:");
			text.setFont(new Font(container.getDisplay(),"Arial", 8, SWT.BOLD));
			text.setLayoutData(layoutDataText);
		} else { 
			text.setText("If you already own a Personal Studio Configuration, choose the following option:");
			text.setFont(new Font(container.getDisplay(),"Arial", 8, SWT.BOLD));
			text.setLayoutData(layoutDataText);
		}
		
		
		psc = new Button(container, SWT.CHECK);
		psc.setText("I already own a PSC\n");
		psc.setLayoutData(layoutDataText);
		
		label = new Label(container, SWT.WRAP);
		label.setLayoutData(layoutDataText);
		label = new Label(container, SWT.WRAP);
		label.setText("Otherwise, please fill in the following form:");
		label.setFont(new Font(container.getDisplay(),"Arial", 8, SWT.BOLD));	
		label.setLayoutData(layoutDataText);
		
		GridData layoutDataTitle = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataTitle.horizontalSpan = 3;
		
		Label title = new Label (container, SWT.NONE);
		title.setFont(new Font(container.getDisplay(),"Arial", 14, SWT.BOLD));
		Color color = new  Color(container.getDisplay(), 51,102,255);
		title.setForeground(color);
		title.setLayoutData(layoutDataTitle);
		title.setText("Your Account Details");
		
		
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.verticalIndent = 5;
		
		label = new Label(container, SWT.NONE);
		label.setText("User name");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		GridData lData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		lData.widthHint = 10;
		
		Label condition = new Label(container, SWT.NONE | SWT.FILL);
		condition.setFont(new Font(container.getDisplay(),"Arial", 16, SWT.BOLD));
		Color colorcondition = new  Color(container.getDisplay(), 204,0,0);
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		userName = new Text(container, SWT.BORDER);
		userName.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (userName.getText().length() > 0) {
					acceptTerms.setSelection(false);
					setPageComplete(false);
				} else {
					setErrorMessage("Please enter your user name!");
				}
			}

			public void keyReleased(KeyEvent e) {
				if (userName.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter the user name!");
				}
			}
		});
		userName.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Password");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(new Font(container.getDisplay(),"Arial", 16, SWT.BOLD));
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		password = new Text(container, SWT.PASSWORD | SWT.BORDER);
		password.setText("0000");
		password.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (password.getText().length() > 0) {
					acceptTerms.setSelection(false);
					setPageComplete(false);
				} else {
					setErrorMessage("Please enter your password!");
				}
			}

			public void keyReleased(KeyEvent e) {
				int nbPassword = password.getText().length();
				if (password.getText().length() > 0) {
					if (nbPassword >= 4 && nbPassword <= 18) {
						setErrorMessage(null);
						setMessage(getDescription());
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
		label.setText("Confirm password");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(new Font(container.getDisplay(),"Arial", 16, SWT.BOLD));
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		final Text passwordAgain = new Text(container, SWT.PASSWORD | SWT.BORDER);
		passwordAgain.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (passwordAgain.getText().length() > 0) {
					acceptTerms.setSelection(false);
					setPageComplete(false);
				} else {
					setErrorMessage("Please enter the your password again!");
				}
			}

			public void keyReleased(KeyEvent e) {
				if (passwordAgain.getText().length() > 0) {
					String pass = password.getText();
					if (pass.equals(passwordAgain.getText())) {
						setErrorMessage(null);
						setMessage(getDescription());
					} else {
						setErrorMessage("Incorrect password!");
					}
				} else {
					setErrorMessage("Please enter your password again!");
				}
			}
		});
		passwordAgain.setLayoutData(layoutData);
		
		Label title2 = new Label (container, SWT.NONE);
		title2.setFont(new Font(container.getDisplay(),"Arial", 14, SWT.BOLD));
		color = new  Color(container.getDisplay(), 51,102,255);
		title2.setForeground(color);
		title2.setLayoutData(layoutDataTitle);
		title2.setText("Your Personal Details");
		
		label = new Label(container, SWT.NONE);
		label.setText("First name");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(new Font(container.getDisplay(),"Arial", 16, SWT.BOLD));
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		firstName = new Text(container, SWT.BORDER);
		firstName.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (firstName.getText().length() > 0) {
					acceptTerms.setSelection(false);
					setPageComplete(false);
				} else {
					setErrorMessage("Please enter your first name!");
				}
			}
			public void keyReleased(KeyEvent e) {
				if (firstName.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter your first name!");
				}
			}
		});
		firstName.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Last name");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(new Font(container.getDisplay(),"Arial", 16, SWT.BOLD));
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		lastName = new Text(container, SWT.BORDER);
		lastName.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (lastName.getText().length() > 0) {
					acceptTerms.setSelection(false);
					setPageComplete(false);
				} else {
					setErrorMessage("Please enter your last name!");
				}
			}
			public void keyReleased(KeyEvent e) {
				if (lastName.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter your last name!");
				}
			}
		});
		lastName.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Mail");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(new Font(container.getDisplay(),"Arial", 16, SWT.BOLD));
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		mail = new Text(container, SWT.BORDER);
		mail.setText("user@convertigo.com");
		mail.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (mail.getText().length() > 0) {
					acceptTerms.setSelection(false);
					setPageComplete(false);
				} else {
					setErrorMessage("Please enter your mail!");
				}
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
				} else {
					setErrorMessage("Please enter your mail!");
				}
			}
		});
		mail.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Confirm mail");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(new Font(container.getDisplay(),"Arial", 16, SWT.BOLD));
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		final Text mailAgain = new Text(container, SWT.BORDER);
		mailAgain.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (mailAgain.getText().length() > 0) {
					acceptTerms.setSelection(false);
					setPageComplete(false);
				} else {
					setErrorMessage("Please enter your mail again!");
				}
			}

			public void keyReleased(KeyEvent e) {
				if (mailAgain.getText().length() > 0) {
					String pass = mail.getText();
					if (pass.equals(mailAgain.getText())) {
						setErrorMessage(null);
						setMessage(getDescription());
					} else {
						setErrorMessage("Incorrect mail!");
					}
				} else {
					setErrorMessage("Please enter your mail again!");
				}
			}
		});
		mailAgain.setLayoutData(layoutData);
		
		Label fields = new Label (container, SWT.NONE);
		fields.setFont(new Font(container.getDisplay(),"Arial", 7, SWT.ITALIC));
		fields.setForeground(colorcondition);
		fields.setText("* required fileds");
		GridData layoutDataFields = new GridData(GridData.HORIZONTAL_ALIGN_END);
		layoutDataFields.horizontalSpan = 3;
		fields.setLayoutData(layoutDataFields);
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(new Font(container.getDisplay(),"Arial", 16, SWT.BOLD));
		condition.setForeground(colorcondition);
		condition.setText("*");
		GridData lD = new GridData(GridData.HORIZONTAL_ALIGN_END);
		lD.horizontalSpan = 2;
		condition.setLayoutData(lD);
		
		acceptTerms = new Button(container, SWT.CHECK);
		acceptTerms.setFont(new Font(container.getDisplay(),"Arial", 8, SWT.BOLD));
		acceptTerms.setText("Accept terms and conditions !");
		
		acceptTerms.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				boolean isChecked = acceptTerms.getSelection();
				if (isChecked) {
					if (userName.getText().length() == 0 || password.getText().length() == 0 || firstName.getText().length() == 0 || lastName.getText().length() == 0 || mail.getText().length() == 0) {
						setErrorMessage("please complete the fields required");
						acceptTerms.setSelection(false);
					} else {
						if (password.getText().equals(passwordAgain.getText()) && mail.getText().equals(mailAgain.getText())) {
							setErrorMessage(null);
							setMessage(getDescription());
							isChecked = acceptTerms.getSelection();
							if (isChecked) {
								setPageComplete(true);
							}
							
						} else {
							setErrorMessage("please complete the fields required");
							acceptTerms.setSelection(false);
						}
					}
				} else {
					setPageComplete(false);
				}
			}
		});
		acceptTerms.setLayoutData(layoutData);
		
		psc.addSelectionListener(new SelectionAdapter() {
			public void  widgetSelected(SelectionEvent event) {
				boolean isChecked = psc.getSelection();
				if (isChecked) {
					firstName.setEnabled(false);
					lastName.setEnabled(false);
					userName.setEnabled(false);
					mail.setEnabled(false);
					mailAgain.setEnabled(false);
					password.setEnabled(false);
					passwordAgain.setEnabled(false);
					acceptTerms.setEnabled(false);
					setPageComplete(true);
					
					setupWizard.pscKeyPage.setDescription("You already own a Personal Studio Configuration.");
					setupWizard.pscKeyPage.setInfo("Please copy your PSC in the following text area:");
				} else {					
					if (isConnect) {
						firstName.setEnabled(true);
						lastName.setEnabled(true);
						userName.setEnabled(true);
						mail.setEnabled(true);
						mailAgain.setEnabled(true);
						password.setEnabled(true);
						passwordAgain.setEnabled(true);
						acceptTerms.setEnabled(true);
						if (acceptTerms.getSelection()) {
							setPageComplete(true);
						} else {
							setPageComplete(false);
						}
						setupWizard.pscKeyPage.setDescription("Thanks for filling the form. " +
							"A Convertigo Forum account will automatically be created for you as well as a Personal Studio Configuration.");
						setupWizard.pscKeyPage.setInfo("Once your account is created on the Convertigo forum, you will receive an email including " +
							"your Personal Studio Configuration. Please copy your PSC in the following text area:");
					} else {	
						firstName.setEnabled(false);
						lastName.setEnabled(false);
						userName.setEnabled(false);
						mail.setEnabled(false);
						mailAgain.setEnabled(false);
						password.setEnabled(false);
						passwordAgain.setEnabled(false);
						acceptTerms.setEnabled(false);
						setPageComplete(true);
						
						setupWizard.pscKeyPage.setDescription("Fill the form accessible on the following page in order " +
							"to automatically create a Convertigo Forum account to get a Personal Studio Configuration.");
						setupWizard.pscKeyPage.setInfo("Once you filled the form on the Internet, you will receive an email including" +
							" your Personal Studio Configuration. Please copy your PSC in the following text area:");
					}
				}
			}
		});

		if (!isConnect) {
//			title.setEnabled(false);
//			title2.setEnabled(false);
			check(false, title,title2);
			firstName.setEnabled(false);
			lastName.setEnabled(false);
			userName.setEnabled(false);
			mail.setEnabled(false);
			mailAgain.setEnabled(false);
			password.setEnabled(false);
			passwordAgain.setEnabled(false);
			acceptTerms.setEnabled(false);
			setPageComplete(true);
		} else {
			setPageComplete(false);
		}

		
		// Required to avoid an error in the system
		setControl(container);
	}

	public void check (boolean bool, Label title, Label title2) {
		title.setEnabled(bool);
		title2.setEnabled(bool);
	}
	public static boolean isEmailAdress(String email){
		Pattern p = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@" +
		           "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" +  "\\." +
		           "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+");
		Matcher m = p.matcher(email.toUpperCase());
		return m.matches();
		}

	public String getFirstName() {
		return firstName.getText();
	}
	
	public String getLastName() {
		return lastName.getText();
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
	
	public Boolean getPSC() {
		return psc.getSelection();
	}
	
	private boolean isInternetAccess() {
		String urlSource = "http://www.convertigo.com";
		HttpClient client = new HttpClient();
		
		try {
			URL url = new URL(urlSource);
			
			HostConfiguration hostConfiguration = null;
			hostConfiguration = client.getHostConfiguration();
			
			HttpState httpState = new HttpState();
			client.setState(httpState);
			
			Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);
			
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (EngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GetMethod method = new GetMethod(urlSource);
		
		try {
			int statusCode = client.executeMethod(method);
			if (statusCode == HttpStatus.SC_OK) {
				return true;
			}
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	    return false;
	}
}
