package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class RegistrationPage extends WizardPage {	
	private final static Pattern pCheckEmail = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+");
	private Text firstName;
	private Text lastName;
	private Text userName;
	private Text password;
	private Text mail;
	private Button acceptTerms;
	private Button sendInfos;
	
	public RegistrationPage() {
		super("Registration");
		setTitle("Convertigo Forum Account and PSC.");
		setDescription("Convertigo Forum Account Creation and Personal Studio Configuration.");
	}

	public void createControl(Composite parent) {
		final Color colorcondition = new  Color(parent.getDisplay(), 204,0,0);
		final Color colorpart = new  Color(parent.getDisplay(), 51,102,255);
		final Font ftArial7i = new Font(parent.getDisplay(),"Arial", 7, SWT.ITALIC);
		final Font ftArial8b = new Font(parent.getDisplay(),"Arial", 8, SWT.BOLD);
		final Font ftArial10 = new Font(parent.getDisplay(),"Arial", 10, SWT.NONE);
		final Font ftArial14b = new Font(parent.getDisplay(),"Arial", 14, SWT.BOLD);
		final Font ftArial16b = new Font(parent.getDisplay(),"Arial", 16, SWT.BOLD);
		
		final int nbCol = 3;
		
		final SetupWizard setupWizard = (SetupWizard) getWizard();
		final Boolean isConnect = isInternetAccess();
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
				
		layout.numColumns = nbCol;
		layout.marginWidth = 30;		
		
		GridData layoutDataDescription = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataDescription.horizontalSpan = nbCol;
		layoutDataDescription.widthHint = 300;
		
		
		Label description = new Label(container, SWT.WRAP);
		description.setText("Filling this form will automatically create a Convertigo Forum account for you to be able " +
				"to consult the forum and also ask for help or advice." +
				"This process will also provide you a Personal Studio Configuration that will automatically configure " +
				"your Studio for projects deployments on Convertigo Cloud.");
		description.setLayoutData(layoutDataDescription);
		description.setFont(ftArial10);
		
		GridData gLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gLayoutData.heightHint = 10;
		gLayoutData.horizontalSpan = nbCol;
		Label label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(gLayoutData);
		
		Label text = new Label(container, SWT.WRAP);
		if (!isConnect) {
			text.setText("This procedure requires Internet access.");
			text.setFont(ftArial8b);
			text.setForeground(colorpart);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));

			text = new Label(container, SWT.WRAP);
			text.setText("If you can not access the Internet from this computer click \"Next\"," +
					"or if you already own a Personal Studio Configuration, choose the following option:");
			text.setFont(ftArial8b);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		} else { 
			text.setText("If you already own a Personal Studio Configuration, choose the following option:");
			text.setFont(ftArial8b);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		}
		
		label = new Label(container, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, nbCol - 1, 1));
		label.setText("Do you own a PSC ?");
		
		final Button psc = new Button(container, SWT.CHECK | SWT.CENTER);
		psc.setText("Yes, I already own a PSC");
		psc.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		label = new Label(container, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		label = new Label(container, SWT.WRAP);
		label.setText("Otherwise, please fill in the following form:");
		label.setFont(ftArial8b);	
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		
		GridData layoutDataTitle = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataTitle.horizontalSpan = nbCol;
		
		Label title = new Label (container, SWT.NONE);
		title.setFont(ftArial14b);
		title.setForeground(colorpart);
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
		condition.setFont(ftArial16b);
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		userName = new Text(container, SWT.BORDER);
		userName.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Password");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(ftArial16b);
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		password = new Text(container, SWT.PASSWORD | SWT.BORDER);
		password.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Confirm password");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(ftArial16b);
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		final Text passwordAgain = new Text(container, SWT.PASSWORD | SWT.BORDER);
		passwordAgain.setLayoutData(layoutData);
		
		Label title2 = new Label (container, SWT.NONE);
		title2.setFont(ftArial14b);
		title2.setForeground(colorpart);
		title2.setLayoutData(layoutDataTitle);
		title2.setText("Your Personal Details");
		
		label = new Label(container, SWT.NONE);
		label.setText("First name");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(ftArial16b);
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		firstName = new Text(container, SWT.BORDER);
		firstName.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Last name");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(ftArial16b);
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		lastName = new Text(container, SWT.BORDER);
		lastName.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Mail");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(ftArial16b);
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		mail = new Text(container, SWT.BORDER);
		mail.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Confirm mail");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(ftArial16b);
		condition.setForeground(colorcondition);
		condition.setText("*");
		condition.setLayoutData(lData);
		
		final Text mailAgain = new Text(container, SWT.BORDER);
		mailAgain.setLayoutData(layoutData);
		
		label = new Label (container, SWT.NONE);
		label.setFont(ftArial7i);
		label.setForeground(colorcondition);
		label.setText("* required fileds");
		GridData layoutDataFields = new GridData(GridData.HORIZONTAL_ALIGN_END);
		layoutDataFields.horizontalSpan = nbCol;
		label.setLayoutData(layoutDataFields);
		
		condition = new Label(container, SWT.NONE);
		condition.setFont(ftArial16b);
		condition.setForeground(colorcondition);
		condition.setText("*");
		GridData lD = new GridData(GridData.HORIZONTAL_ALIGN_END);
		lD.horizontalSpan = 2;
		condition.setLayoutData(lD);

		acceptTerms = new Button(container, SWT.CHECK);
		acceptTerms.setFont(ftArial8b);
		acceptTerms.setText("Accept terms and conditions !");
		acceptTerms.setLayoutData(layoutData);
		
		//Button permit to send informations of the registration form to the web service 
		sendInfos = new Button(container, SWT.BUTTON1);
		sendInfos.setFont(ftArial8b);
		sendInfos.setText("Send registration");
		sendInfos.setEnabled(false);
		
		ModifyListener unAccept = new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (e.data == null) {
					acceptTerms.setSelection(false);
					sendInfos.setEnabled(false);
					setPageComplete(false);
				}
			}
			
		};
		
		final List<Text> fields = Arrays.asList(userName, password, passwordAgain, firstName, lastName, mail, mailAgain);
		
		for (Text txt : fields) {
			txt.addModifyListener(unAccept);
		}
		
		userName.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (userName.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter the user name!");
				}
			}
			
		});
		
		password.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (e.data == null) {
					passwordAgain.setText("");
				}
				int nbPassword = password.getText().length();
				if (nbPassword > 0) {
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
		
		passwordAgain.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (passwordAgain.getText().length() > 0) {
					if (password.getText().equals(passwordAgain.getText())) {
						setErrorMessage(null);
						setMessage(getDescription());
					} else {
						setErrorMessage("Incorrect password!");
					}
				} else {
					setErrorMessage("Please enter the your password again!");
				}
			}
			
		});
		
		firstName.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (firstName.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter your first name!");
				}
			}
			
		});

		lastName.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (lastName.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter your last name!");
				}
			}
			
		});
		
		mail.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (e.data == null) {
					mailAgain.setText("");
				}
				if (mail.getText().length() > 0) {
					if (isEmailAdress(mail.getText())) {
						setErrorMessage(null);
						setMessage(getDescription());
					} else {
						setErrorMessage("Please enter a valid mail!");
					}
				} else {
					setErrorMessage("Please enter your mail!");
				}
			}
			
		});
		
		mailAgain.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (mailAgain.getText().length() > 0) {
					if (mail.getText().equals(mailAgain.getText())) {
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
		
		sendInfos.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent evt) {
				boolean isEnabled = sendInfos.getEnabled();
				if(isEnabled){
					if(registrationToWebService("https://c8o_dev.convertigo.net/cems/projects/studioRegistration/.xml") != HttpStatus.SC_OK){
							ConvertigoPlugin.logError("Error when sending the registration to the web service!");
					}
				}
			}
			
		});
		
		acceptTerms.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent evt) {
				boolean isChecked = acceptTerms.getSelection();
				if (isChecked) {
					Event event = new Event();
					event.data = this;
					
					boolean valid = true;
					
					for (Iterator<Text> i = fields.iterator(); i.hasNext() && valid;) {
						i.next().notifyListeners(SWT.Modify, event);
						valid = getErrorMessage() == null;
					}
					
					if (valid) {
						sendInfos.setEnabled(true);
					} else {
						acceptTerms.setSelection(false);
						sendInfos.setEnabled(false);
					}
				} else {
					sendInfos.setEnabled(false);
				}
			}
			
		});
		
		psc.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void  widgetSelected(SelectionEvent event) {
				boolean isChecked = psc.getSelection();
				if (isChecked) {
					for (Text field : fields) {
						field.setEnabled(false);
					}
					acceptTerms.setEnabled(false);
					sendInfos.setEnabled(false);
					setPageComplete(true);
					
					setupWizard.pscKeyPage.setDescription("You already own a Personal Studio Configuration.");
					setupWizard.pscKeyPage.setInfo("Please copy your PSC in the following text area:");
				} else {					
					if (isConnect) {
						for (Text field : fields) {
							field.setEnabled(true);
						}
						acceptTerms.setEnabled(true);
						
						if (acceptTerms.getSelection()) {
							setPageComplete(true);
							sendInfos.setEnabled(true);
						} else {
							setPageComplete(false);
							sendInfos.setEnabled(false);
						}
						
						setupWizard.pscKeyPage.setDescription("Thanks for filling the form. " +
							"A Convertigo Forum account will automatically be created for you as well as a Personal Studio Configuration.");
						setupWizard.pscKeyPage.setInfo("Once your account is created on the Convertigo forum, you will receive an email including " +
							"your Personal Studio Configuration. Please copy your PSC in the following text area:");
					} else {	
						for (Text field : fields) {
							field.setEnabled(false);
						}
						acceptTerms.setEnabled(false);
						sendInfos.setEnabled(false);
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
			for (Text field : fields) {
				field.setEnabled(false);
			}
			acceptTerms.setEnabled(false);
			sendInfos.setEnabled(false);
			setPageComplete(true);
		} else {
			sendInfos.setEnabled(false);
			setPageComplete(false);
		}
		
		// Required to avoid an error in the system
		setControl(container);
	}

	
	public void check (boolean bool, Label title, Label title2) {
		title.setEnabled(bool);
		title2.setEnabled(bool);
	}
	public static boolean isEmailAdress(String email) {
		Matcher m = pCheckEmail.matcher(email.toUpperCase());
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
			
		} catch (Exception e) {
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
	
	/**Added by julienda - 01/ 10/2012
	 * send informations(first name, last name, etc.) to the web service using POST method */
	private int registrationToWebService(String targetUrl){
		int statuscode = 0;
		
		PostMethod method = new PostMethod(targetUrl);
		method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		
		// set parameters for POST method
		method.setParameter("__sequence", "checkEmail");
		method.setParameter("username", getUserName());
		method.setParameter("password", getPassword());
		method.setParameter("firstname", getFirstName());
		method.setParameter("lastname", getLastName());
		method.setParameter("email", getMail());
		
		try {
			HttpClient httpClient = new HttpClient();
			
			// execute HTTP post with parameters
			statuscode = httpClient.executeMethod(HostConfiguration.ANY_HOST_CONFIGURATION, method, new HttpState());
			String body = method.getResponseBodyAsString();
			Document document = XMLUtils.parseDOMFromString(body);
			NodeList nd = document.getElementsByTagName("errorCode");
			
			Node node = nd.item(0);
			String errorCode = node.getTextContent();
			sendInfos.setEnabled(false);
			setPageComplete(true);
			
			// put the error details into the logs
			if(!errorCode.equals("0")){
				PostMethod method2 = new PostMethod(targetUrl);
				
				// set parameters for POST method to get the details of error messages
				method2.setParameter("__sequence", "getErrorMessages");
				Engine.theApp.httpClient.executeMethod(HostConfiguration.ANY_HOST_CONFIGURATION, method2, new HttpState());
				body = method2.getResponseBodyAsString();
				
				document = XMLUtils.parseDOMFromString(body);
				nd = document.getElementsByTagName("label");
				Node nodeDetails = nd.item(Integer.parseInt(errorCode));
				
				ConvertigoPlugin.logError(nodeDetails.getTextContent());
				// modify the state of button to can retry
				sendInfos.setEnabled(true);
				setPageComplete(false);
			}
			
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error when using HTTP POST:\n"+e.getMessage());
		}
		return statuscode;	
	}
}
