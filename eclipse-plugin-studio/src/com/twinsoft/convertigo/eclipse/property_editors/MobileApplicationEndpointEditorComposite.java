/*
 * Copyright (c) 2001-2021 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.DeploymentConfiguration;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public class MobileApplicationEndpointEditorComposite extends AbstractDialogComposite {
	private String value;
	
	public MobileApplicationEndpointEditorComposite(Composite parent, int style, MobileApplication mobileApplication) {
		super(parent, style, null);
		value = mobileApplication.getEndpoint();
		initialize();
	}
	
	public MobileApplicationEndpointEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		value = (String) cellEditor.getValue();
		initialize();
	}
	
	private void initialize() {
		GridLayout gl = new GridLayout(1, false);
		setLayout(gl);
		Group group = new Group(this, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.marginHeight = 5;
		fillLayout.marginWidth = 5;
		group.setLayout(fillLayout);
		group.setText("Please check this is the valid mobile endpoint. The mobile have to connect this Convertigo server: ");
		Text tEndpoint = new Text(group, SWT.NONE);
		tEndpoint.setText(value);
		tEndpoint.addVerifyListener((VerifyEvent e) -> {
			value = e.text;
		});
		
		group = new Group(this, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.spacing = 10;
		fillLayout.marginHeight = 10;
		fillLayout.marginWidth = 10;
		group.setLayout(fillLayout);
		group.setText("You can choose one of the folling possible Endpoint: ");
		
		LinkedHashSet<Pair<String, String>> endpoints = new LinkedHashSet<>();
		try {
			DeploymentConfiguration defaultDeploymentConfiguration = ConvertigoPlugin.deploymentConfigurationManager.getDefault(cellEditor.databaseObjectTreeObject.getObject().getProject().getName());
			String url = "http" + (defaultDeploymentConfiguration.isBHttps() ? "s" : "") + "://" + defaultDeploymentConfiguration.getServer();
			endpoints.add(Pair.of(url, "default deployment"));
		} catch (NullPointerException e) {
			// No default configuration
		}
		
		for (String deploymentConfigurationName: new TreeSet<String>(ConvertigoPlugin.deploymentConfigurationManager.getAllDeploymentConfigurationNames())) {
			DeploymentConfiguration deploymentConfiguration = ConvertigoPlugin.deploymentConfigurationManager.get(deploymentConfigurationName);
			String url = "http" + (deploymentConfiguration.isBHttps() ? "s" : "") + "://" + deploymentConfiguration.getServer();
			endpoints.add(Pair.of(url, "from deployment"));
		}
		
		HttpGet http = new HttpGet("https://ifconfig.io");
		HeaderName.UserAgent.addHeader(http, "curl");
		try (InputStream is = Engine.theApp.httpClient4.execute(http).getEntity().getContent()) {
			String ip = IOUtils.toString(is, StandardCharsets.US_ASCII);
			endpoints.add(Pair.of("http://" + ip + "/convertigo", "from public IP (please check your port forwarding)"));
		} catch (Exception e) {
		}
		
		try {
			for (NetworkInterface netint: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (InetAddress addr: Collections.list(netint.getInetAddresses())) {
					String ip = addr.getHostAddress();
					if (!ip.contains(":")) {
						endpoints.add(Pair.of("http://" + ip + ":18080/convertigo", "Net Interface: " + netint.getDisplayName()));
					}
				}
			}
		} catch (SocketException e) {
		}
		
		SelectionListener sl = new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				tEndpoint.setText(((Button) e.widget).getText());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		
		for (Pair<String, String> endpoint: endpoints) {
			Button button = new Button(group, SWT.PUSH);
			button.setText(endpoint.getKey());
			button.setToolTipText(endpoint.getValue());
			button.addSelectionListener(sl);
		}
	}

	@Override
	public String getValue() {
		return value;
	}

}
