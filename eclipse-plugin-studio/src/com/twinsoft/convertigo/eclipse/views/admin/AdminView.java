/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.admin;

import java.security.InvalidParameterException;
import java.util.UUID;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class AdminView extends ViewPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.views.admin.AdminView";

	
	private static String lastAuthToken;
	private static long lastAuthTokenExpiration;

	private C8oBrowser browser = null;
		
	@Override
	public void dispose() {
		if (browser != null) {
			browser.dispose();
		}
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		SwtUtils.refreshTheme();
		
		parent.setLayout(new GridLayout(1, true));
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		tb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		browser = new C8oBrowser(parent, SWT.NONE);
		
		browser.addToolItemNavigation(tb);
		new ToolItem(tb, SWT.SEPARATOR);
		
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(false);
		Engine.logStudio.debug("Admin debug : "+ browser.getDebugUrl());
		
		ToolItem ti = new ToolItem(tb, SWT.NONE);
		ti.setImage(ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/statement.png"));
		ti.setText("View with your external browser");
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(getUrl());
			}
			
		});
		String url = getUrl();

		browser.setUrl(url);
	}

	@Override
	public void setFocus() {
	}
	
	public static void checkAuthToken(String authToken) throws InvalidParameterException {
		boolean isAuth = authToken != null && authToken.equals(lastAuthToken) && System.currentTimeMillis() < lastAuthTokenExpiration;
		lastAuthToken = null;
		lastAuthTokenExpiration = 0;
		if (!isAuth) {
			throw new InvalidParameterException("authToken not valid");
		}
	}

	private String getUrl() {
		lastAuthToken = UUID.randomUUID().toString();
		lastAuthTokenExpiration = System.currentTimeMillis() + 30000;
		
		return EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/admin/login.html#authToken=" + lastAuthToken;
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}
}
