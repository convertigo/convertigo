package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.CheckConnectedCallback;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.RegisterCallback;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class RegistrationPage extends WizardPage implements CheckConnectedCallback {
	private static final Pattern pCheckEmail = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+");
	public static final String registrationLink = "<a href=\"http://register.convertigo.com\">register.convertigo.com</a>";
	private Text firstname;
	private Text lastname;
	private Text email;
	private Text company;
	private Combo companyHeadcount;
	private Combo country;
	private Text username;
	private Text password;
	private List<Text> texts;
	private List<Control> controls;
	private Link notConnectedLink;
	
	private boolean isConnected;
	private boolean changed = true;

	public RegistrationPage() {
		super("RegistrationPage");
		setTitle("Convertigo Trial Cloud account creation");
		setDescription("Convertigo provides a free convertigo cloud account for you test and run your projects.");
	}
	
	public void createControl(Composite parent) {
		final Color colorcondition = new  Color(parent.getDisplay(), 204,0,0);
		
		final int nbCol = 3;
		
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
				
		layout.numColumns = nbCol;
		
		GridData layoutDataDescription = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataDescription.horizontalSpan = nbCol;		
		
		Label label = new Label(container, SWT.WRAP);
		label.setText("Filling this form will automatically create for you a Convertigo Trial Cloud account, and send you by email the corresponding Personal Studio Configuration.\n\n" +
				"This process will also create for you a Convertigo Forum account, enabling you to request help, tips and tricks in the forum:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		
		FontData fontDefaultData = label.getFont().getFontData()[0];
		fontDefaultData.setStyle(SWT.BOLD);
		final Font fontBold = new Font(parent.getDisplay(), fontDefaultData);
		fontDefaultData.setHeight(Math.round(fontDefaultData.getHeight() * 1.4f));
		final Font fontTitle = new Font(parent.getDisplay(), fontDefaultData);
		
		Link link = new Link(container, SWT.WRAP);
		link.setText("<a href=\"http://www.convertigo.com/en/how-to/developer-forum.html\">www.convertigo.com/en/how-to/developer-forum.html</a>");
		link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, nbCol, 1));
		
		GridData gLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gLayoutData.heightHint = 10;
		gLayoutData.horizontalSpan = nbCol;
		label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(gLayoutData);
		
		notConnectedLink = new Link(container, SWT.WRAP);
		notConnectedLink.setFont(fontBold);
		notConnectedLink.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, nbCol, 1));
		
		GridData layoutDataTitle = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataTitle.horizontalSpan = nbCol;
		
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.verticalIndent = 5;
		
		GridData lData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		lData.widthHint = 10;
		
		label = new Label (container, SWT.NONE);
		label.setFont(fontTitle);
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
		label.setText("Company headcount");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		label = new Label(container, SWT.NONE);
		label.setLayoutData(lData);
		
		companyHeadcount = new Combo(container, SWT.READ_ONLY);
		companyHeadcount.setLayoutData(layoutData);
		
		companyHeadcount.add("Unknown");
		companyHeadcount.select(0);
		
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
		label.setText("* required fields");
		GridData layoutDataFields = new GridData(GridData.HORIZONTAL_ALIGN_END);
		layoutDataFields.horizontalSpan = nbCol;
		label.setLayoutData(layoutDataFields);
		
		ModifyListener unAccept = new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (e.data == null) {
					checkValidity();
					changed = true;
				}
			}
			
		};
		
		texts = Arrays.asList(firstname, lastname, email, company, username, password, passwordAgain);
		controls = Arrays.asList(firstname, lastname, email, company, (Control) companyHeadcount, (Control) country, username, password, passwordAgain);
		
		for (Text txt : texts) {
			txt.addModifyListener(unAccept);
		}
		country.addModifyListener(unAccept);
		
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
		
		SelectionListener handleLink = new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.program.Program.launch(e.text);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
			
		};
		
		link.addSelectionListener(handleLink);
		notConnectedLink.addSelectionListener(handleLink);
		
		try {
			for (String line : GenericUtils.<List<String>>cast(IOUtils.readLines(getClass().getResourceAsStream("countries.txt"), "utf-8"))) {
				country.add(line);
			}
		} catch (IOException e1) {
			country.add("Failed to load countries …");
		}
		
		for (String line : new String[] {
			"< 10",
			"11- 50",
			"51- 100",
			"101-500",
			"501- 1000",
			"1001-5000",
			"5001-10000",
			"> 10000"
		}) {
			companyHeadcount.add(line);
		}
		
		// Required to avoid an error in the system
		setControl(container);
	}
	
	private void setControlsEnabled(boolean enable) {
		for (Control control : controls) {
			control.setEnabled(enable);
		}
	}
	
	private void checkValidity() {
		Event event = new Event();
		event.data = this;
		
		boolean valid = true;
		
		for (Iterator<Text> i = texts.iterator(); i.hasNext() && valid;) {
			i.next().notifyListeners(SWT.Modify, event);
			valid = getErrorMessage() == null;
		}

		setErrorMessage(null);
		setMessage(getDescription());
		
		setPageComplete(valid);
	}
	
	@Override
	public IWizard getWizard() {
		checkValidity();
		SetupWizard wizard = (SetupWizard) super.getWizard();
		setControlsEnabled(false);
		notConnectedLink.setText("Checking for connection to " + registrationLink + " …");
		((GridData) notConnectedLink.getLayoutData()).exclude = false;
		notConnectedLink.setVisible(true);
		notConnectedLink.getParent().layout();
		wizard.checkConnected(this);
		return wizard;
	}
	
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
	
	public boolean isConnected() {
		return isConnected;
	}
	
	public void onCheckConnected(final boolean isConnected, final String message) {
		Display.getDefault().asyncExec(new Runnable() {

			public void run() {
				RegistrationPage.this.isConnected = isConnected;
				String msg = message;
				setControlsEnabled(isConnected);
				setPageComplete(!isConnected);
				notConnectedLink.setVisible(!isConnected);
				((GridData) notConnectedLink.getLayoutData()).exclude = isConnected;
				if (!isConnected) {
					setErrorMessage("No Internet connection! Please check your proxy settings.");
					msg = "Your Internet connection seems to be broken.\n" +
							"Please go to " + registrationLink + " with your browser to request a PSC.\n" +
							"Then click Next on this wizard and follow instructions.\n" +
							"Connection error : " + message;
				} else {
					checkValidity();
				}
				notConnectedLink.setText(msg);
				notConnectedLink.getParent().layout(true);
			}
			
		});
	}
	
	public boolean register(RegisterCallback callback) {
		if (isConnected && changed) {
			changed = false;
			((SetupWizard) super.getWizard()).register(
					username.getText(),
					password.getText(),
					firstname.getText(),
					lastname.getText(),
					email.getText(),
					country.getText(),
					company.getText(),
					companyHeadcount.getText(),
					callback
			);
			return true;
		}
		return false;
	}
}
