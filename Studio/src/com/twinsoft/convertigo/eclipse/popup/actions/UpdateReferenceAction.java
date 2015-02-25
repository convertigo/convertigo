/**
 * @author julienda
 * @date 23/02/2015
 */
package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlImporter;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.settings.ProxySettings;
import com.eviware.soapui.settings.WsdlSettings;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.AuthenticationDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;
import com.twinsoft.convertigo.engine.PacManager.PacInfos;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class UpdateReferenceAction extends MyAbstractAction {

	public UpdateReferenceAction() { }

	/*
	 * (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.popup.actions.MyAbstractAction#run()
	 */
	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();
    			
    			if ((databaseObject != null) && (databaseObject instanceof WebServiceReference)) {
    				WebServiceReference webServiceReference = (WebServiceReference) databaseObject;
					updateReferences(webServiceReference, shell);
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to download the selected reference!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
	private void updateReferences(WebServiceReference webServiceReference, Shell shell) throws Exception {
		ProxyMode proxyMode = Engine.theApp.proxyManager.proxyMode;
		String proxyExcludes = StringUtils.join(Engine.theApp.proxyManager.getBypassDomains(), ",");
		String proxyServer = Engine.theApp.proxyManager.getProxyServer();
		int proxyPort = Engine.theApp.proxyManager.getProxyPort();
		String proxyUser = Engine.theApp.proxyManager.getProxyUser();
		String proxyPwd = Engine.theApp.proxyManager.getProxyPassword();
		
		String wsdlUrl = webServiceReference.getUrlpath();
		
		boolean soapuiSettingsChanged = false;
		Settings settings = SoapUI.getSettings();
		
		if (settings != null) {
			// WSDL
			if (!settings.getBoolean(WsdlSettings.XML_GENERATION_ALWAYS_INCLUDE_OPTIONAL_ELEMENTS)) {
				settings.setBoolean(WsdlSettings.XML_GENERATION_ALWAYS_INCLUDE_OPTIONAL_ELEMENTS, true);
				soapuiSettingsChanged = true;
			}
			if (!settings.getBoolean(WsdlSettings.XML_GENERATION_TYPE_COMMENT_TYPE)) {
				settings.setBoolean(WsdlSettings.XML_GENERATION_TYPE_COMMENT_TYPE, true);
				soapuiSettingsChanged = true;
			}
			if (!settings.getBoolean(WsdlSettings.XML_GENERATION_TYPE_EXAMPLE_VALUE)) {
				settings.setBoolean(WsdlSettings.XML_GENERATION_TYPE_EXAMPLE_VALUE, true);
				soapuiSettingsChanged = true;
			}
			if (settings.getBoolean(WsdlSettings.XML_GENERATION_SKIP_COMMENTS)) {
				settings.setBoolean(WsdlSettings.XML_GENERATION_SKIP_COMMENTS, false);
				soapuiSettingsChanged = true;
			}
			
			boolean enableProxy = Engine.theApp.proxyManager.isEnabled();
			if (enableProxy) {
				if (proxyMode == ProxyMode.auto) {
					try {
						URL url = new URL(wsdlUrl);
						PacInfos pacInfos = Engine.theApp.proxyManager.getPacInfos(url.toString(), url.getHost());
						if (pacInfos != null) {
							proxyServer = pacInfos.getServer();
							proxyPort = pacInfos.getPort();
						}
					}
					catch (Exception e) {}
				}
				
				if (!settings.getBoolean(ProxySettings.ENABLE_PROXY)) {
					settings.setBoolean(ProxySettings.ENABLE_PROXY, true);
					soapuiSettingsChanged = true;
				}
				if (!proxyExcludes.equals(settings.getString(ProxySettings.EXCLUDES, null))) {
					settings.setString(ProxySettings.EXCLUDES, proxyExcludes);
					soapuiSettingsChanged = true;
				}
				if (!proxyServer.equals(settings.getString(ProxySettings.HOST, null))) {
					settings.setString(ProxySettings.HOST, proxyServer);
					soapuiSettingsChanged = true;
				}
				if (!String.valueOf(proxyPort).equals(settings.getString(ProxySettings.PORT, null))) {
					settings.setString(ProxySettings.PORT, String.valueOf(proxyPort));
					soapuiSettingsChanged = true;
				}
				if (!proxyUser.equals(settings.getString(ProxySettings.USERNAME, null))) {
					settings.setString(ProxySettings.USERNAME, proxyUser);
					soapuiSettingsChanged = true;
				}
				if (!proxyPwd.equals(settings.getString(ProxySettings.PASSWORD, null))) {
					settings.setString(ProxySettings.PASSWORD, proxyPwd);
					soapuiSettingsChanged = true;
				}
			}
			else {
				if (settings.getBoolean(ProxySettings.ENABLE_PROXY)) {
					settings.setBoolean(ProxySettings.ENABLE_PROXY, false);
					soapuiSettingsChanged = true;
				}
			}
		}
		
		if (soapuiSettingsChanged)
			SoapUI.saveSettings();
		
		// Import WSDL using SoapUI
		String projectDir = Engine.PROJECTS_PATH + "/"+ webServiceReference.getProject().getName();
		
	   	WsdlProject wsdlProject = new WsdlProject();
		WsdlInterface[] wsdls = null;
		
   		if (webServiceReference.needAuthentication()) {
   			Display display = Display.getDefault();
   			AuthenticationDialog authenticationDialog = new AuthenticationDialog(display.getActiveShell());
   		
   			if (authenticationDialog.open() == 0){
   				System.setProperty("soapui.loader.username", authenticationDialog.getLogin());
   				System.setProperty("soapui.loader.password", authenticationDialog.getPassword());

   				wsdls = WsdlImporter.importWsdl(wsdlProject, wsdlUrl);
		   		
   				System.setProperty("soapui.loader.username", "");
   				System.setProperty("soapui.loader.password", "");
   			} else {
   				return;
   			}
   		} else {
   			wsdls = WsdlImporter.importWsdl(wsdlProject, wsdlUrl);
   		}
   		
	   	WsdlInterface WSDLInterface;
	   	
	   	for (int i = 0; i < wsdls.length; i++) {
		   	WSDLInterface = wsdls[i];
		   	if (WSDLInterface != null) {
			   	
			   	// Export WSDL file(s) to project directory and modify reference local file path
		   		String folderName = WSDLInterface.getBindingName().getLocalPart();
		   		File filePath = new File(projectDir + "/wsdl/" + folderName);
		   		String wsdlPath = null;
		   		
		   		wsdlPath = WSDLInterface.getWsdlContext().export(filePath.getPath());
		   					   	
			   	wsdlPath = new File(wsdlPath).toURI().getPath();
			   	webServiceReference.setFilepath(".//" + wsdlPath.substring(wsdlPath.indexOf("/wsdl") + 1));
			   	webServiceReference.setName("Import_WS_" + filePath.getName());
			   	
		   	}
	   	}
	   	
	   	MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
		dialog.setText("SUCCESS");
		dialog.setMessage("The reference file has been downloaded with success!");
		dialog.open();
	}	
}
