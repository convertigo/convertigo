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

package com.twinsoft.convertigo.engine.enums;

import java.util.HashMap;
import java.util.Map;

public enum MimeType {
	Html ("text/html"),
	Xhtml ("application/xhtml+xml"),
	Javascript ("application/javascript"),
	JavascriptDeprecated ("text/javascript"),
	Css ("text/css"),
	Plain ("text/plain"),
	VOID ("");
	
	String value;
	MimeType(String mimeType) {
		this.value = mimeType;
	}
	
	private static Map<String, MimeType> cache = new HashMap<String, MimeType>();
	static {
		for (MimeType mimeType : MimeType.values()) {
			cache.put(mimeType.value, mimeType);
		}
	}
	
	public static MimeType parse(String type) {
		MimeType mimeType = cache.get(type.toLowerCase());
		return mimeType != null ? mimeType : VOID;
	}
}
