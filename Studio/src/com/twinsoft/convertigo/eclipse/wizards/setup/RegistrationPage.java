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
import org.apache.commons.io.IOUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class RegistrationPage extends WizardPage {	
	private final static Pattern pCheckEmail = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+");
	private Text firstname;
	private Text lastname;
	private Text email;
	private Text company;
	private Combo country;
	private Text username;
	private Text password;
	private Button acceptTerms;
	private Button sendInfos;
	
	public RegistrationPage() {
		super("RegistrationPage");
		setTitle("Convertigo trial cloud account creation");
		setDescription("Convertigo provides a free convertigo cloud account for you test and run your projects.");
	}

	public void createControl(Composite parent) {
		final Color colorcondition = new  Color(parent.getDisplay(), 204,0,0);
		final Color colorpart = new  Color(parent.getDisplay(), 51,102,255);
		
		final int nbCol = 3;
		final Boolean isConnect = isInternetAccess();
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
				
		layout.numColumns = nbCol;
		layout.marginWidth = 30;		
		
		GridData layoutDataDescription = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataDescription.horizontalSpan = nbCol;
//		layoutDataDescription.widthHint = 300;
		
		
		Label label = new Label(container, SWT.WRAP);
		label.setText("Filling this form will automatically create for you a Convertigo Trial Cloud account, and send you by email the corresponding Personal Studio Configuration.\n\n" +
				"This process will also create for you a Convertigo Forum account, enabling you to request help, tips and tricks in the forum:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		
		FontData fontDefaultData = label.getFont().getFontData()[0];
		fontDefaultData.setStyle(SWT.BOLD);
		final Font fontBold = new Font(parent.getDisplay(), fontDefaultData);
		fontDefaultData.setHeight(fontDefaultData.getHeight() * 2);
		final Font fontTitle = new Font(parent.getDisplay(), fontDefaultData);
		
		Link link = new Link(container, SWT.WRAP);
		link.setText("<a href=\"http://www.convertigo.com/en/how-to/developer-forum.html\">www.convertigo.com/en/how-to/developer-forum.html</a>");
		link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		
		GridData gLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gLayoutData.heightHint = 10;
		gLayoutData.horizontalSpan = nbCol;
		label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(gLayoutData);
		
		Label text = new Label(container, SWT.WRAP);
		if (!isConnect) {
			text.setText("This procedure requires Internet access.");
			text.setFont(fontBold);
			text.setForeground(colorpart);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));

			text = new Label(container, SWT.WRAP);
			text.setText("If you can not access the Internet from this computer click \"Next\"," +
					"or if you already own a Personal Studio Configuration, choose the following option:");
			text.setFont(fontBold);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		}
		
		GridData layoutDataTitle = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataTitle.horizontalSpan = nbCol;
		
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.verticalIndent = 5;
		
		GridData lData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		lData.widthHint = 10;
		
		label = new Label (container, SWT.NONE);
		label.setFont(fontTitle);
		label.setForeground(colorpart);
		label.setLayoutData(layoutDataTitle);
		label.setText("Personal Data");
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setText("Firstname");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setForeground(colorcondition);
		label.setText("*");
		label.setLayoutData(lData);
		
		firstname = new Text(container, SWT.BORDER);
		firstname.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setText("Lastname");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setForeground(colorcondition);
		label.setText("*");
		label.setLayoutData(lData);
		
		lastname = new Text(container, SWT.BORDER);
		lastname.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setText("Email");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setForeground(colorcondition);
		label.setText("*");
		label.setLayoutData(lData);
		
		email = new Text(container, SWT.BORDER);
		email.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setText("Company");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE);
		label.setLayoutData(lData);
		
		company = new Text(container, SWT.BORDER);
		company.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setText("Country");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE);
		label.setLayoutData(lData);
		
		country = new Combo(container, SWT.READ_ONLY);
		country.setLayoutData(layoutData);
		
		country.add("Select a country");
		country.select(0);
		
		label = new Label (container, SWT.NONE);
		label.setFont(fontTitle);
		label.setForeground(colorpart);
		label.setLayoutData(layoutDataTitle);
		label.setText("Forum account");
		
		label = new Label(container, SWT.WRAP);
		label.setText("If you already have a Convertigo Forum account, just type your username, password will be ignored.");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setText("Username");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE | SWT.FILL);
		label.setFont(fontBold);
		label.setForeground(colorcondition);
		label.setText("*");
		label.setLayoutData(lData);
		
		username = new Text(container, SWT.BORDER);
		username.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setText("Password");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE);
		label.setLayoutData(lData);
		
		password = new Text(container, SWT.PASSWORD | SWT.BORDER);
		password.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setText("Confirm password");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE);
		label.setLayoutData(lData);
		
		final Text passwordAgain = new Text(container, SWT.PASSWORD | SWT.BORDER);
		passwordAgain.setLayoutData(layoutData);
				
		label = new Label (container, SWT.NONE);
		label.setForeground(colorcondition);
		label.setText("* required fileds");
		GridData layoutDataFields = new GridData(GridData.HORIZONTAL_ALIGN_END);
		layoutDataFields.horizontalSpan = nbCol;
		label.setLayoutData(layoutDataFields);
		
		label = new Label(container, SWT.NONE);
		label.setFont(fontBold);
		label.setForeground(colorcondition);
		label.setText("*");
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, nbCol - 1, 1));
		
		acceptTerms = new Button(container, SWT.CHECK);
		acceptTerms.setFont(fontBold);
		acceptTerms.setText("Accept terms and conditions !");
		acceptTerms.setLayoutData(layoutData);
		
		//Button permit to send informations of the registration form to the web service 
		sendInfos = new Button(container, SWT.BUTTON1);
		sendInfos.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		sendInfos.setFont(fontBold);
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
		
		final List<Text> fields = Arrays.asList(firstname, lastname, email, company, username, password, passwordAgain);
		
		for (Text txt : fields) {
			txt.addModifyListener(unAccept);
		}
		
		username.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (username.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter the username!");
				}
			}
			
		});
		
		password.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (e.data == null) {
					passwordAgain.setText("");
				}
			}
			
		});
		
		passwordAgain.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (password.getText().equals(passwordAgain.getText())) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Confirm password incorrect!");
				}
			}
			
		});
		
		firstname.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (firstname.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter your firstname!");
				}
			}
			
		});

		lastname.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (lastname.getText().length() > 0) {
					setErrorMessage(null);
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter your lastname!");
				}
			}
			
		});
		
		email.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (email.getText().length() > 0) {
					if (isEmailAdress(email.getText())) {
						setErrorMessage(null);
						setMessage(getDescription());
					} else {
						setErrorMessage("Please enter a valid email!");
					}
				} else {
					setErrorMessage("Please enter your email!");
				}
			}
			
		});
		
		sendInfos.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent evt) {
				boolean isEnabled = sendInfos.getEnabled();
				if(isEnabled){
					if (registrationToWebService("https://c8o_dev.convertigo.net/cems/projects/studioRegistration/.xml") != HttpStatus.SC_OK){
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
		
		link.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.program.Program.launch(e.text);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
			
		});
		
		try {
			for (String line : GenericUtils.<List<String>>cast(IOUtils.readLines(getClass().getResourceAsStream("countries.txt"), "utf-8"))) {
				country.add(line);
			}
		} catch (IOException e1) {
			country.add("Failed to load countries …");
		}
		
//		psc.addSelectionListener(new SelectionAdapter() {
//			
//			@Override
//			public void  widgetSelected(SelectionEvent event) {
//				boolean isChecked = psc.getSelection();
//				if (isChecked) {
//					for (Text field : fields) {
//						field.setEnabled(false);
//					}
//					acceptTerms.setEnabled(false);
//					sendInfos.setEnabled(false);
//					setPageComplete(true);
//					
//					setupWizard.pscKeyPage.setDescription("You already own a Personal Studio Configuration.");
//					setupWizard.pscKeyPage.setInfo("Please copy your PSC in the following text area:");
//				} else {					
//					if (isConnect) {
//						for (Text field : fields) {
//							field.setEnabled(true);
//						}
//						acceptTerms.setEnabled(true);
//						
//						if (acceptTerms.getSelection()) {
//							setPageComplete(true);
//							sendInfos.setEnabled(true);
//						} else {
//							setPageComplete(false);
//							sendInfos.setEnabled(false);
//						}
//						
//						setupWizard.pscKeyPage.setDescription("Thanks for filling the form. " +
//							"A Convertigo Forum account will automatically be created for you as well as a Personal Studio Configuration.");
//						setupWizard.pscKeyPage.setInfo("Once your account is created on the Convertigo forum, you will receive an email including " +
//							"your Personal Studio Configuration. Please copy your PSC in the following text area:");
//					} else {	
//						for (Text field : fields) {
//							field.setEnabled(false);
//						}
//						acceptTerms.setEnabled(false);
//						sendInfos.setEnabled(false);
//						setPageComplete(true);
//						
//						setupWizard.pscKeyPage.setDescription("Fill the form accessible on the following page in order " +
//							"to automatically create a Convertigo Forum account to get a Personal Studio Configuration.");
//						setupWizard.pscKeyPage.setInfo("Once you filled the form on the Internet, you will receive an email including" +
//							" your Personal Studio Configuration. Please copy your PSC in the following text area:");
//					}
//				}
//			}
//			
//		});
		
		if (!isConnect) {
//			title.setEnabled(false);
//			title2.setEnabled(false);
//			check(false, title,title2);
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

	
//	public void check (boolean bool, Label title, Label title2) {
//		title.setEnabled(bool);
//		title2.setEnabled(bool);
//	}
	public static boolean isEmailAdress(String email) {
		Matcher m = pCheckEmail.matcher(email.toUpperCase());
		return m.matches();
	}

	public String getFirstname() {
		return firstname.getText();
	}
	
	public String getLastname() {
		return lastname.getText();
	}
	public String getUsername() {
		return username.getText();
	}
	
	public String getPassword() {
		return password.getText();
	}
	
	public String getEmail() {
		return email.getText();
	}
	
	public String getCountry() {
		return country.getSelectionIndex() == 0 ? "" : country.getText();
	}
	
	public String getCompany() {
		return company.getText();
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
		method.setParameter("username", getUsername());
		method.setParameter("password", getPassword());
		method.setParameter("firstname", getFirstname());
		method.setParameter("lastname", getLastname());
		method.setParameter("email", getEmail());
		method.setParameter("country", getCountry());
		method.setParameter("company", getCountry());
		
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
			ConvertigoPlugin.logException(e, "Error while trying to send registration");
		}
		return statuscode;	
	}
}
