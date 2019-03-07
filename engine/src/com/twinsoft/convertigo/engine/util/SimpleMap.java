package com.twinsoft.convertigo.engine.util;

import java.util.HashMap;
import java.util.Map;

public class SimpleMap {
	private Map<String, Object> map = new HashMap<>();
	
	public void set(String key, Object value) {
		synchronized (map) {
			if (value == null) {
				map.remove(key);
			} else {
				map.put(key, value);
			}
		}
	}
	
	public Object get(String key) {
		synchronized (map) {
			return map.get(key);
		}
	}
}
