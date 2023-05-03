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

package com.twinsoft.convertigo.eclipse.views.mobile;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jettison.json.JSONArray;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class MobileDebugView extends ViewPart implements IPartListener2 {
	
	private C8oBrowser c8oBrowser;
	private String currentUrl = null;
	
	public MobileDebugView() {
		
	}

	@Override
	public void dispose() {
		getSite().getPage().removePartListener(this);
		c8oBrowser.dispose();
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		c8oBrowser = new C8oBrowser(parent, SWT.MULTI | SWT.WRAP);
		c8oBrowser.setLayout(new FillLayout());
		c8oBrowser.setZoomEnabled(false);
		
		if (!onActivated(getSite().getPage().getActiveEditor())) {
			parent.getDisplay().asyncExec(() -> {
				c8oBrowser.setText("<html><head><style>html {color: $foreground$; background-color: $background$; font-family: sans-serif }</style></head>"
						+ "<body>"
						+ "<center>"
						+ "Please click on an application viewer in the right pane to enable a <br />"
						+ "debuging session. </br>"
						+ "</center>"
						+ "</body>"
						+ "</html>");
			});
		}
		
		getSite().getPage().addPartListener(this);
	}

	@Override
	public void setFocus() {
		c8oBrowser.setFocus();
	}

	private String getDebugUrl(IWorkbenchPart part) {
		if (part instanceof com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor) {
			com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditor editorPart = GenericUtils.cast(part);
			return editorPart.getDebugUrl();
		} else if (part instanceof com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditor) {
			com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditor editorPart = GenericUtils.cast(part);
			return editorPart.getDebugUrl();
		}
		return null;
	}
	
	public boolean onActivated(IWorkbenchPart part) {
		String url = getDebugUrl(part);
		if (url != null) {
			String key = part.toString() + ":" + url;
			if (!key.equals(currentUrl)) {
				currentUrl = key;
				Engine.execute(() -> {
					String u = url;
					try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(new HttpGet(u + "/json"))) {
						JSONArray json = new JSONArray(IOUtils.toString(response.getEntity().getContent(), "UTF-8"));
						u += json.getJSONObject(0).getString("devtoolsFrontendUrl");
					} catch (Exception e) {
					}
					c8oBrowser.loadURL(u);
				});
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		onActivated(part);
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub
		
	}

}
