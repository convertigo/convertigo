/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.baserow;

import java.util.Properties;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback.Response;
import com.teamdev.jxbrowser.cookie.Cookie;
import com.teamdev.jxbrowser.cookie.CookieStore;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin.PscException;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.Engine;

public class BaserowView extends ViewPart {
	private Composite main;
	private C8oBrowser browser;
	private String email;
	private String secret;
	
	public class StudioAPI {
		
		@JsAccessible
		public String email() {
			return email;
		}
		
		@JsAccessible
		public String secret() {
			return secret;
		}
		
		@JsAccessible
		public String postMessage(String message) {
			return BaserowView.this.postMessage(message);
		}
	}
	
	public BaserowView() {
		Properties pscProps;
		try {
			pscProps = ConvertigoPlugin.decodePsc();
		} catch (PscException e1) {
			return;
		}
		email = pscProps.getProperty("owner.email");
		secret = pscProps.getProperty("studio.secret");
	}

	@Override
	public void dispose() {
		browser.dispose();
		main.dispose();
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl;
		main = new Composite(parent, SWT.NONE);
		main.setLayout(gl = new GridLayout(1, true));
		gl.marginHeight = gl.marginWidth = 0;
		ConvertigoPlugin.asyncExec(() -> {
			browser = new C8oBrowser(main, SWT.NONE);
			browser.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			String url = "https://c8ocloud.convertigo.net/convertigo/projects/C8oCloudBaserow/DisplayObjects/mobile/";
			
			browser.getBrowser().set(InjectJsCallback.class, params -> {
				try {
					if (params.frame().browser().url().contains(url)) {
						JsObject window = params.frame().executeJavaScript("window"); 
						window.putProperty("studio", new StudioAPI());
					}
				} catch (Exception e) {
					Engine.logStudio.info("failure", e);
				}
				return Response.proceed();
			});
			
			browser.setUrl(url);
			Engine.logStudio.debug("Debug the NoCodeDB view: " + browser.getDebugUrl() + "/json");
			main.layout(true);
		});
	}
	
	private String postMessage(String message) {
		try {
			JSONObject obj = new JSONObject(message);
			if (obj.has("token")) {
				String token = obj.getString("token");
				CookieStore cookieStore = browser.getBrowser().engine().cookieStore();
				cookieStore.set(Cookie.newBuilder("baserow.convertigo.net")
					.name("jwt_token").value(token)
					.path("/").secure(true).build());
				cookieStore.persist();
				browser.setUrl("https://baserow.convertigo.net/dashboard");
			} else {
				Engine.logStudio.debug("(NoCode Databases) page response: " + message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public void setFocus() {
		main.setFocus();
	}
}
