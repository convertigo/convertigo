/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;

public class ReverseProxyManager {
	private Map<String, HttpHost> reverseProxyHttp = new HashMap<>();
	
	public ReverseProxyManager() {
//		try {
//			addReverseProxyHttp("hello", "http://localhost:5173");
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
	}
	
	public void addReverseProxyHttp(String prefix, String target) throws URISyntaxException {
		var uri = new URI(target);
        var httpHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
		reverseProxyHttp.put(prefix, httpHost);
	}
	
	public HttpHost getHttpHost(String key) {
		return reverseProxyHttp.get(key);
	}
}
