package com.twinsoft.convertigo.beans.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;

/** Cookie changes made by one SiteClipper request, independent of other requests. */
final class SiteClipperCookieState {
	private record Key(String domain, String path, String name) {
		Key(Cookie cookie) {
			this(cookie.getDomain() == null ? "" : cookie.getDomain().toLowerCase(Locale.ROOT),
					cookie.getPath(), cookie.getName());
		}
	}

	private record Value(String value, Long expiry, boolean secure) {
		Value(Cookie cookie) {
			this(cookie.getValue(), cookie.getExpiryDate() == null ? null : cookie.getExpiryDate().getTime(),
					cookie.getSecure());
		}
	}

	private final HttpState original;
	private final Map<Key, Value> before = new HashMap<>();

	SiteClipperCookieState(HttpState state) {
		original = state;
		if (state != null) {
			for (Cookie cookie : state.getCookies()) {
				before.put(new Key(cookie), new Value(cookie));
			}
		}
	}

	Delta delta(HttpState after) {
		var current = new HashMap<Key, Cookie>();
		if (after != null) {
			for (Cookie cookie : after.getCookies()) {
				current.put(new Key(cookie), cookie);
			}
		}
		var removed = new HashSet<>(before.keySet());
		removed.removeAll(current.keySet());
		var changed = new HashMap<Key, Cookie>();
		for (var entry : current.entrySet()) {
			if (after != original || !Objects.equals(before.get(entry.getKey()), new Value(entry.getValue()))) {
				changed.put(entry.getKey(), entry.getValue());
			}
		}
		return new Delta(removed, changed);
	}

	static final class Delta {
		private final Set<Key> removed;
		private final Map<Key, Cookie> changed;

		private Delta(Set<Key> removed, Map<Key, Cookie> changed) {
			this.removed = removed;
			this.changed = changed;
		}

		boolean isEmpty() {
			return removed.isEmpty() && changed.isEmpty();
		}

		HttpState apply(HttpState state) {
			var merged = new HttpState();
			if (state != null) {
				merged.setCookiePolicy(state.getCookiePolicy());
			}
			for (Cookie cookie : state == null ? new Cookie[0] : state.getCookies()) {
				var key = new Key(cookie);
				if (!removed.contains(key) && !changed.containsKey(key)) {
					merged.addCookie(cookie);
				}
			}
			for (Cookie cookie : changed.values()) {
				merged.addCookie(cookie);
			}
			return merged;
		}
	}
}
