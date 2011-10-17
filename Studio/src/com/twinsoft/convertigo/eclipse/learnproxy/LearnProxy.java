/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.learnproxy;

import com.twinsoft.convertigo.eclipse.learnproxy.http.HttpProxy;
import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEventListener;

public class LearnProxy {
	
	private HttpProxy httpProxy = null;
	private Thread proxyThread = null;
	
	public LearnProxy() {
		this.httpProxy = new HttpProxy();
		start();
	}
	
	public void addProxyEventListener(HttpProxyEventListener listener) {
		if (httpProxy != null) {
			httpProxy.addProxyEventListener(listener);
		}
	}
	
	public void removeProxyEventListener(HttpProxyEventListener listener) {
		if (httpProxy != null)
			httpProxy.removeProxyEventListener(listener);
	}
	
	public void start() {
		if (httpProxy != null) {
			if (proxyThread == null) {
				proxyThread = new Thread(httpProxy);
				proxyThread.start();
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		if (proxyThread != null)
			proxyThread.destroy();
		super.finalize();
	}

	public static void main(String[] parm) {
		/*HttpProxy proxy = new HttpProxy();
		HttpEventLogger logger = new HttpEventLogger();
		proxy.addProxyEventListener(logger);
		Thread t = new Thread(proxy);
		t.start();*/
		LearnProxy learnProxy = new LearnProxy();
		if (learnProxy != null) {
			HttpEventLogger logger = new HttpEventLogger();
			learnProxy.addProxyEventListener(logger);
			learnProxy.start();
		}
	}
}
