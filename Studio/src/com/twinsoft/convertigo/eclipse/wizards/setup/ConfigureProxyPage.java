package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.util.Arrays;

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
	private Text doNotApplyProxy;
	private Combo proxyMethode;
	private Text proxyUrl;
	private Text proxyUser;
	private Text proxyPassword;
	
	private Composite container;

	public ConfigureProxyPage() {
		super("Configuration proxy");
		setTitle("Proxy");
		setDescription("Configuration proxy.");
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
		
		proxyMode = new Combo(container, SWT.BORDER);
		proxyMode.setText("off");
		proxyMode.add("off");
		proxyMode.add("auto");
		proxyMode.add("manual");
		
		proxyMode.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				System.out.println(proxyMode.getText());
				isEnable(proxyMode.getText());
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});			
		
		proxyMode.setLayoutData(layoutData);
		
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
//						setPageComplete(true);
					} catch (NumberFormatException exp) {
						setErrorMessage("Please enter an int!");
					}
				}
			}
		});
		proxyPort.setLayoutData(layoutData);
		
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
//						setPageComplete(true);
				} else {
					setErrorMessage("Please enter the host name!");
				}
			}
		});
		
		proxyHost.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Do not apply proxy settings on");

		doNotApplyProxy = new Text(container, SWT.BORDER | SWT.SINGLE);
		doNotApplyProxy.setText("localhost,127.0.0.1");
		doNotApplyProxy.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (doNotApplyProxy.getText().length() > 0) {
					setMessage(getDescription());
//					setPageComplete(true);
				} else {
					setErrorMessage("Please enter an int!");
				}
			}
		});
		
		doNotApplyProxy.setLayoutData(layoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Autoconfiguration proxy url");

		proxyUrl = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyUrl.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (proxyUrl.getText().length() > 0) {
						setMessage(getDescription());
//						setPageComplete(true);
				} else {
					setErrorMessage("Please enter url!");
				}
			}
		});
		
		proxyUrl.setLayoutData(layoutData);
		
		label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gLayoutData.verticalIndent = 5;
		gLayoutData.horizontalSpan = 2;
		label.setLayoutData(gLayoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Proxy authentication methode");

		proxyMethode = new Combo(container, SWT.BORDER);
		proxyMethode.setText("anonymous");
		proxyMethode.add("anonymous");
		proxyMethode.add("basic");
		proxyMethode.add("NTLM");
		
		proxyMethode.setLayoutData(layoutData);
		
		proxyMethode.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				isEnable(proxyMethode.getText());
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
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
					setErrorMessage("Please enter user name!");
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
						setPageComplete(true);
				} else {
					setErrorMessage("Please enter password!");
				}
			}
		});
		
		proxyPassword.setLayoutData(layoutData);		
		
		isEnable("off");
		
		// Required to avoid an error in the system
		setControl(container);
//		setPageComplete(true);
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
	
	public void isEnable(String proxyMode) {
		if ("manual".equals(proxyMode)) {
			proxyHost.setEnabled(true);
			proxyPort.setEnabled(true);
			proxyUrl.setEnabled(false);
			isEnable(getProxyMethode());
			proxyMethode.setEnabled(true);
			doNotApplyProxy.setEnabled(true);
		} else if ("auto".equals(proxyMode)) {
			proxyHost.setEnabled(false);
			proxyPort.setEnabled(false);
			proxyUrl.setEnabled(true);
			proxyUser.setEnabled(false);
			proxyPassword.setEnabled(false);
			proxyMethode.setEnabled(false);
			doNotApplyProxy.setEnabled(false);
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
			proxyUrl.setEnabled(false);
			proxyUser.setEnabled(false);
			proxyPassword.setEnabled(false);
			proxyMethode.setEnabled(false);
			doNotApplyProxy.setEnabled(false);
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
		return doNotApplyProxy.getText();
	}
	public String getProxyUrl() {
		return proxyUrl.getText();
	}
	public String getProxyMethode() {
		return proxyMethode.getText();
	}
	public String getProxyUser() {
		return proxyUser.getText();
	}
	public String getProxyPassword() {
		return proxyPassword.getText();
	}
	
	
}
