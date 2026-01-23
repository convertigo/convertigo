/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import java.util.function.Consumer;

import org.codehaus.jettison.json.JSONObject;

import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback.Response;
import com.teamdev.jxbrowser.event.Observer;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished;

public class C8oBrowserPostMessageHelper {

	private final C8oBrowser browser;
	private Consumer<JSONObject> onMessage;
	private Observer<FrameLoadFinished> onLoad;

	public C8oBrowserPostMessageHelper(C8oBrowser browser) {
		this.browser = browser;
		var bro = browser.getBrowser(); 
		bro.set(InjectJsCallback.class, event -> {
			var frame = event.frame();
			JsObject window = frame.executeJavaScript("window");
			window.putProperty("java", new BrowserInterface());
			return Response.proceed();
		});
		
		bro.navigation().on(FrameLoadFinished.class, event -> {
			if (onLoad != null) {
				onLoad.on(event);
			}
		});
	}
	
	public void onLoad(Observer<FrameLoadFinished> handler) {
		this.onLoad = handler;
	}
	
	public void onMessage(Consumer<JSONObject> handler) {
		this.onMessage = handler;
	}
	
	public void postMessage(JSONObject message) {
		var js = String.format("window.receiveFromJava(%s);", message.toString());
		browser.getBrowser().mainFrame().ifPresent(frame -> frame.executeJavaScript(js));
	}
	
	public class BrowserInterface {
		@JsAccessible
		public void receiveFromJS(String message) {
			try {
				JSONObject json = new JSONObject(message);
				if (onMessage != null) {
					onMessage.accept(json);
				}
			} catch (Exception e) {
				System.err.println("Invalid JSON received from JS: " + message);
				e.printStackTrace();
			}
		}
	}
}
