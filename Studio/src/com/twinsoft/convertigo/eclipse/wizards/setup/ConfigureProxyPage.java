package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.util.Arrays;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ConfigureProxyPage extends WizardPage {
	
	private Combo proxyMode;
	private Text proxyPort;
	private Text proxyHost;
	private Text proxyExceptions;
	private Combo proxyMethod;
	private Text proxyAutoConfUrl;
	private Text proxyUser;
	private Text proxyPassword;
	
	private Composite container;

	public ConfigureProxyPage() {
		super("Configuration proxy");
		setTitle("Proxy settings");
		setDescription("This page configures the proxy settings. By default, no proxy is configured.");
	}
	
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.marginWidth = 30;
		
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		layoutData.verticalIndent = 5;
		
		Label label;
		label = new Label(container, SWT.NONE);
		label.setText("Proxy mode");
		
		proxyMode = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		proxyMode.add("off");
		proxyMode.add("auto");
		proxyMode.add("manual");
		proxyMode.select(0);
		
		proxyMode.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				enableComponents(proxyMode.getText());
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});			
		
		proxyMode.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Proxy host");
		
		proxyHost = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyHost.setText("localhost");
		proxyHost.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (proxyHost.getText().length() > 0) {
						setMessage(getDescription());
				} else {
					setErrorMessage("Please enter the host name!");
				}
			}
		});
		
		proxyHost.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Proxy port");
		
		proxyPort = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyPort.setText("8080");
		proxyPort.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (proxyPort.getText().length() > 0) {
					try {
						Integer.parseInt(proxyPort.getText());
						setErrorMessage(null);
						setMessage(getDescription());
					} catch (NumberFormatException exp) {
						setErrorMessage("Please enter a number!");
					}
				}
			}
		});
		proxyPort.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Do not apply proxy settings on");

		proxyExceptions = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyExceptions.setText("localhost,127.0.0.1");
		proxyExceptions.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (proxyExceptions.getText().length() > 0) {
					setMessage(getDescription());
				} else {
					setErrorMessage("Please enter a number!");
				}
			}
		});
		
		proxyExceptions.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Autoconfiguration proxy URL");

		proxyAutoConfUrl = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyAutoConfUrl.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (proxyAutoConfUrl.getText().length() > 0) {
						setMessage(getDescription());
				} else {
					setErrorMessage("Please enter the autonconfiguration proxy URL!");
				}
			}
		});
		
		proxyAutoConfUrl.setLayoutData(layoutData);
		
		label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gLayoutData.verticalIndent = 5;
		gLayoutData.horizontalSpan = 2;
		label.setLayoutData(gLayoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Proxy authentication method");

		proxyMethod = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		proxyMethod.add("anonymous");
		proxyMethod.add("basic");
		proxyMethod.add("NTLM");
		proxyMethod.select(0);
		
		proxyMethod.setLayoutData(layoutData);
		
		proxyMethod.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				enableComponents(proxyMethod.getText());
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});	
		
		label = new Label(container, SWT.NONE);
		label.setText("Username");

		proxyUser = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyUser.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (proxyUser.getText().length() > 0) {
						setMessage(getDescription());
						setPageComplete(true);
				} else {
					setErrorMessage("Please enter the proxy user name!");
				}
			}
		});
		
		proxyUser.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Password");
		
		proxyPassword = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
		proxyPassword.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (proxyPassword.getText().length() > 0) {
						setMessage(getDescription());
						setErrorMessage(null);
						setPageComplete(true);
				} else {
					setErrorMessage("Please enter the proxy password!");
				}
			}
		});
		
		proxyPassword.setLayoutData(layoutData);		
		
		enableComponents("off");
		
		// Required to avoid an error in the system
		setControl(container);
	}
	
	public enum ProxyMode {
    	off,
    	auto,
    	manual;

		final String value;
		
		ProxyMode() {
			this.value = name();
		}

		public String getValue() {
			return value;
		}
		
		public int index() {
			return Arrays.binarySearch(ProxyMode.values(), this);
		}
	}
	
	public enum ProxyMethod {
		anonymous,
		basic,
		ntlm;
		
		final String value;
		
		ProxyMethod() {
			this.value = name();
		}

		public String getValue() {
			return value;
		}
		
		public int index() {
			return Arrays.binarySearch(ProxyMode.values(), this);
		}
	}
	
	public void enableComponents(String proxyMode) {
		if ("manual".equals(proxyMode)) {
			proxyHost.setEnabled(true);
			proxyPort.setEnabled(true);
			proxyAutoConfUrl.setEnabled(false);
			proxyMethod.setEnabled(true);
			proxyExceptions.setEnabled(true);
		} else if ("auto".equals(proxyMode)) {
			proxyHost.setEnabled(false);
			proxyPort.setEnabled(false);
			proxyAutoConfUrl.setEnabled(true);
			proxyUser.setEnabled(false);
			proxyPassword.setEnabled(false);
			proxyMethod.setEnabled(false);
			proxyExceptions.setEnabled(false);
		} else if ("anonymous".equals(proxyMode)) {
			proxyUser.setEnabled(false);
			proxyPassword.setEnabled(false);
		}else if ("NTLM".equals(proxyMode)) {
			proxyUser.setEnabled(false);
			proxyPassword.setEnabled(false);
		}else if ("basic".equals(proxyMode)) {
			proxyUser.setEnabled(true);
			proxyPassword.setEnabled(true);
		} else {
			proxyHost.setEnabled(false);
			proxyPort.setEnabled(false);
			proxyAutoConfUrl.setEnabled(false);
			proxyUser.setEnabled(false);
			proxyPassword.setEnabled(false);
			proxyMethod.setEnabled(false);
			proxyExceptions.setEnabled(false);
			proxyUser.setEnabled(false);
			proxyPassword.setEnabled(false);
		}
	}
	
	public String getProxyMode() {
		return proxyMode.getText();
	}
	
	public String getProxyPort() {
		return proxyPort.getText();
	}
	public String getProxyHost() {
		return proxyHost.getText();
	}
	public String getDoNotApplyProxy() {
		return proxyExceptions.getText();
	}
	public String getProxyAutoConfUrl() {
		return proxyAutoConfUrl.getText();
	}
	public String getProxyMethod() {
		return proxyMethod.getText();
	}
	public String getProxyUser() {
		return proxyUser.getText();
	}
	public String getProxyPassword() {
		return proxyPassword.getText();
	}

	@Override
	public IWizardPage getNextPage() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		((SummaryPage) setupWizard.getPage("SummaryPage")).updateSummary();
		return super.getNextPage();
	}
	
	@Override
	public IWizardPage getPreviousPage() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		((SummaryPage) setupWizard.getPage("SummaryPage")).updateSummary();
		return super.getPreviousPage();
	}
}
