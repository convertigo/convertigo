package com.twinsoft.convertigo.engine;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;

public class ReverseProxyManager {
	private Map<String, HttpHost> reverseProxyHttp = new HashMap<>();
	
	public ReverseProxyManager() {
		try {
			addReverseProxyHttp("hello", "http://localhost:5173");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
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
