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

package com.twinsoft.convertigo.eclipse.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback;
import com.teamdev.jxbrowser.browser.event.BrowserClosed;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.view.swt.BrowserView;
import com.twinsoft.convertigo.engine.Engine;

public class RegistrationBrowser extends Composite {
	public interface OnPSC {
		void onPSC(String psc);
	}
	
	public interface OnReady {
		void onReady(boolean ok);
	}
	
	private C8oBrowser browser;
	private String username;
	private String secret;
	private OnPSC onPSC;
	private OnReady onReady;

	public class StudioAPI {
		
		@JsAccessible
		public String getUsername() {
			return username;
		}
		
		@JsAccessible
		public String getSecret() {
			return secret;
		}
		
		@JsAccessible
		public void setPSC(String psc) {
			try {
				onPSC.onPSC(psc);
			} catch (Throwable t) {
			}
		}
		
		@JsAccessible
		public void setReady(boolean ok) {
			try {
				onReady.onReady(ok);
			} catch (Throwable t) {
			}
		}
	}
	
	public RegistrationBrowser(Composite parent, int style) {
		super(parent, style);
		Composite composite = this;
		StackLayout stack = new StackLayout();
		composite.setLayout(stack);
		
		stack.topControl = browser = new C8oBrowser(this, SWT.NONE);
		
		browser.setUseExternalBrowser(false);
		browser.getBrowser().set(InjectJsCallback.class, params -> {
			String url = params.frame().browser().url();
			if (url != null) {
				browser.setUseExternalBrowser(!url.contains("c8ocloud.convertigo.net"));
				try {
					JsObject window = params.frame().executeJavaScript("window"); 
					window.putProperty("studio", new StudioAPI());
				} catch (Exception e) {
					Engine.logStudio.info("onScriptContextCreate failed for '" + url + "': " + e.getMessage());
				}
			}
			return com.teamdev.jxbrowser.browser.callback.InjectJsCallback.Response.proceed();
		});
		browser.getBrowser().set(OpenPopupCallback.class , params -> {
			getDisplay().asyncExec(() -> {
				Browser br = params.popupBrowser();
				BrowserView bv = BrowserView.newInstance(composite, br);
				stack.topControl = bv;
				composite.layout(true);
				br.on(BrowserClosed.class, event ->
					getDisplay().asyncExec(() -> {
						stack.topControl = browser;
						composite.layout(true);
					})
				);
			});
			return com.teamdev.jxbrowser.browser.callback.OpenPopupCallback.Response.proceed();
		});
	}

	public RegistrationBrowser setUsername(String username) {
		this.username = username;
		return this;
	}

	public RegistrationBrowser setSecret(String secret) {
		this.secret = secret;
		return this;
	}
	
	public RegistrationBrowser onPSC(OnPSC onPSC) {
		this.onPSC = onPSC;
		return this;
	}
	
	public RegistrationBrowser onReady(OnReady onReady) {
		this.onReady = onReady;
		return this;
	}
	
	public void goRegister() {
		browser.setUrl("https://c8ocloud.convertigo.net/convertigo/projects/convertigo_signup/DisplayObjects/mobile/login/true/" + SwtUtils.isDark());
	}
	
	public void goTrial() {
		browser.setUrl("https://www.convertigo.com/startprivatecloud");
	}
}
