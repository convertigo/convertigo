package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.CheckConnectedCallback;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.SummaryGenerator;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;
import com.twinsoft.convertigo.engine.ProxyManager;

public class ConfigureProxyPage extends WizardPage implements SummaryGenerator,CheckConnectedCallback  {
	private Combo proxyMode;
	private Text proxyPort;
	private Text proxyHost;
	private Text bypassDomains;
	private Combo proxyMethod;
	private Text proxyAutoConfUrl;
	private Text proxyUser;
	private Text proxyPassword;
	private Label statusConnection;
	private boolean isConnected;
	
	private Composite container;
	private ProxyManager proxyManager;
	
	public ConfigureProxyPage(ProxyManager proxyManager) {
		super("Configuration proxy");
		setTitle("Proxy settings");
		setDescription("This page configures the proxy settings. A proxy configuration is needed to let Convertigo Studio access the Internet in order to run demos or to be able to connect to any website or web service available on the Internet.");
		this.proxyManager = proxyManager;
	}
	
	@Override
	public IWizard getWizard() {
		SetupWizard wizard = (SetupWizard) super.getWizard();
		wizard.postRegisterState(this.getClass().getSimpleName().toLowerCase());
		return wizard;
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
		proxyMode.setLayoutData(layoutData);
		
		for (ProxyMode mode : ProxyMode.values()) {
			proxyMode.add(mode.getDisplay());
			if (proxyManager.proxyMode == mode) {
				proxyMode.select(proxyMode.getItemCount() - 1);
			}
		}
		
		label = new Label(container, SWT.NONE);
		label.setText("Proxy host");
		
		proxyHost = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyHost.setLayoutData(layoutData);
		proxyHost.setText(proxyManager.proxyServer);
		
		label = new Label(container, SWT.NONE);
		label.setText("Proxy port");
		
		proxyPort = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyPort.setLayoutData(layoutData);
		proxyPort.setText("" + proxyManager.getProxyPort());
		
		label = new Label(container, SWT.NONE);
		label.setText("Do not apply proxy settings on");

		bypassDomains = new Text(container, SWT.BORDER | SWT.SINGLE);
		bypassDomains.setLayoutData(layoutData);
		bypassDomains.setText(proxyManager.bypassDomains);
		
		label = new Label(container, SWT.NONE);
		label.setText("Autoconfiguration proxy URL");

		proxyAutoConfUrl = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyAutoConfUrl.setLayoutData(layoutData);
		proxyAutoConfUrl.setText(proxyManager.proxyUrl);
		
		label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gLayoutData.verticalIndent = 5;
		gLayoutData.horizontalSpan = 2;
		label.setLayoutData(gLayoutData);
		
		label = new Label(container, SWT.NONE);
		label.setText("Proxy authentication method");

		proxyMethod = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		proxyMethod.setLayoutData(layoutData);
		for (ProxyMethod method : ProxyMethod.values()) {
			proxyMethod.add(method.getDisplay());
			if (method == proxyManager.proxyMethod) {
				proxyMethod.select(proxyMethod.getItemCount() - 1);
			}
		}
		
		label = new Label(container, SWT.NONE);
		label.setText("Username");

		proxyUser = new Text(container, SWT.BORDER | SWT.SINGLE);
		proxyUser.setLayoutData(layoutData);
		proxyUser.setText(proxyManager.proxyUser);		
		
		label = new Label(container, SWT.NONE);
		label.setText("Password");
		
		proxyPassword = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
		proxyPassword.setLayoutData(layoutData);
		proxyPassword.setText(proxyManager.proxyPassword);
		
		proxyMode.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				ProxyMode mode = ProxyMode.values()[proxyMode.getSelectionIndex()];
				EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, mode.name());
				enableComponents(mode);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});	

		proxyHost.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_HOST, proxyHost.getText());
			}
			
		});

		proxyPort.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				if (proxyPort.getText().length() > 0) {
					try {
						Integer.parseInt(proxyPort.getText());

						EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_PORT, proxyPort.getText());
						
						setErrorMessage(null);
						setMessage(getDescription());
					} catch (NumberFormatException exp) {
						setErrorMessage("Please enter a number!");
					}
				}
			}
			
		});
		
		bypassDomains.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_BY_PASS_DOMAINS, bypassDomains.getText());
			}
			
		});

		proxyAutoConfUrl.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_AUTO, proxyAutoConfUrl.getText());
			}
		});
		
		proxyMethod.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				ProxyMethod method = ProxyMethod.values()[proxyMethod.getSelectionIndex()];
				EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_METHOD, method.name());
				enableComponents(method);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});
		
		proxyUser.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_USER, proxyUser.getText());
			}
			
		});
		
		proxyPassword.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_PASSWORD, proxyPassword.getText());
			}
			
		});
		statusConnection = new Label(container, SWT.NORMAL);
		statusConnection.setLayoutData(layoutData);
		
		Button checkConnection = new Button(container, SWT.BUTTON1);
		checkConnection.setLayoutData(layoutData);
		checkConnection.setText("Check connection");
		final SetupWizard wizard = (SetupWizard) super.getWizard();
		final CheckConnectedCallback callback = this;
		checkConnection.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent arg0) {
				statusConnection.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
				statusConnection.setText("Checking connection ...");
				wizard.checkConnected(callback);
			}
			
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		enableComponents(proxyManager.proxyMode);
		enableComponents(proxyManager.proxyMethod);
		
		// Required to avoid an error in the system
		setControl(container);
	}
	
	public void enableComponents(ProxyMode proxyMode) {
		if (ProxyMode.manual == proxyMode) {
			proxyHost.setEnabled(true);
			proxyPort.setEnabled(true);
			proxyAutoConfUrl.setEnabled(false);
			proxyMethod.setEnabled(true);
			bypassDomains.setEnabled(true);
		} else if (ProxyMode.auto ==proxyMode) {
			proxyHost.setEnabled(false);
			proxyPort.setEnabled(false);
			proxyAutoConfUrl.setEnabled(true);
			proxyMethod.setEnabled(true);
			bypassDomains.setEnabled(false);
		} else {
			proxyHost.setEnabled(false);
			proxyPort.setEnabled(false);
			proxyAutoConfUrl.setEnabled(false);
			proxyMethod.setEnabled(false);
			bypassDomains.setEnabled(false);
		}
		if (proxyMethod.isEnabled()) {
			proxyMethod.notifyListeners(SWT.Selection, null);
		} else {
			enableComponents(ProxyMethod.anonymous);
		}
	}
	
	public void enableComponents(ProxyMethod proxyMethod) {
		if (ProxyMethod.anonymous  == proxyMethod) {
			proxyUser.setEnabled(false);
			proxyPassword.setEnabled(false);
		} else if (ProxyMethod.basic == proxyMethod || ProxyMethod.ntlm == proxyMethod) {
			proxyUser.setEnabled(true);
			proxyPassword.setEnabled(true);
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
		return bypassDomains.getText();
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

	public String getSummary() {
		return "Proxy configuration:\n" +
				"\tmode: " + proxyMode.getText() + "\n" +
				(proxyManager.proxyMode == ProxyMode.auto ? (
					"\tpac url: " + proxyAutoConfUrl.getText() + "\n"
				): (
				proxyManager.proxyMode == ProxyMode.manual ? (
					"\thost: " + proxyHost.getText() + "\n" +
					"\tport: " + proxyPort.getText() + "\n" +
					"\tbypass domain: " + bypassDomains.getText() + "\n" +
					(proxyManager.proxyMethod != ProxyMethod.anonymous ? (
						"\tuser: " + proxyUser.getText() + "\n" +
						"\tpassword: *****\n"
					) : "")) : ""));
	}

	public void onCheckConnected(final boolean isConnected, final String message) {
		Display.getDefault().asyncExec(new Runnable() {

			public void run() {
				ConfigureProxyPage.this.setConnected(isConnected);
				String msg = message;
				
				if (!isConnected) {
					msg = "Connection error : " + message;
					statusConnection.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED)); 
				} else {
					msg = "The connection test was successful!";
					statusConnection.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN)); 
				}
				statusConnection.setText(msg);
			}
			
		});
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}
}
