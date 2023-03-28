/*
 * Copyright (c) 2001-2023 Convertigo SA.
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
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
		Label label = new Label(this, SWT.WRAP);
		label.setText("Choose a valid mobile end point.\n\n"
				+ "The mobile end point will be the URL the mobile application will\n"
				+ "use to interact with the Convertigo Server.\n\n"
				+ "If you want to test your mobile application using sequences running in your studio,\n"
				+ "use one of the localhost or local IP port 18080 end points.\n\n"
				+ "If you build for production and want to run your apps interacting\n"
				+ "with cloud or on premises servers, use one of the deployment end points.");
		Group group = new Group(this, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.marginHeight = 5;
		fillLayout.marginWidth = 5;
		group.setLayout(fillLayout);
		group.setText("End point: ");
		Text tEndpoint = new Text(group, SWT.NONE);
		tEndpoint.setText(value);
		tEndpoint.addModifyListener((ModifyEvent e) -> {
			value = tEndpoint.getText();
		});
		
		group = new Group(this, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		group.setLayout(new GridLayout(2, false));
		group.setText("You can choose one of the following possible end point: ");
		
		
		LinkedHashSet<Pair<String, String>> endpoints = getEndpoints(cellEditor.databaseObjectTreeObject.getObject().getProject().getName());
		
		SelectionListener sl = new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				tEndpoint.setText((String) e.widget.getData());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		
		for (Pair<String, String> endpoint: endpoints) {
			label = new Label(group, SWT.NONE);
			label.setText(endpoint.getKey());
			label.setToolTipText(endpoint.getValue());
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
			Button button = new Button(group, SWT.PUSH);
			button.setText("Choose and edit ✎");
			button.setToolTipText(endpoint.getValue());
			button.addSelectionListener(sl);
			button.setData(endpoint.getKey());
			button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		}
	}

	@Override
	public String getValue() {
		return value;
	}
	
	public static LinkedHashSet<Pair<String, String>> getEndpoints(String projectName) {
		LinkedHashSet<Pair<String, String>> endpoints = new LinkedHashSet<>();
		try {
			DeploymentConfiguration defaultDeploymentConfiguration = ConvertigoPlugin.deploymentConfigurationManager.getDefault(projectName);
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
			String ip = IOUtils.toString(is, StandardCharsets.US_ASCII).trim();
			endpoints.add(Pair.of("http://" + ip + ":18080/convertigo", "from public IP (please check your port forwarding)"));
		} catch (Exception e) {
		}
		
		try {
			for (NetworkInterface netint: Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (InetAddress addr: Collections.list(netint.getInetAddresses())) {
					String ip = addr.getHostAddress();
					if (!ip.contains(":")) {
						endpoints.add(Pair.of("http://" + ip + ":18080/convertigo", "from " + netint.getDisplayName()));
					}
				}
			}
		} catch (SocketException e) {}
		
		return endpoints;
	}
}
