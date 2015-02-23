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

/*
 * See http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types
 */

public enum MimeType {
	Html ("text/html", new String[]{"html","htm"}),
	Xhtml ("application/xhtml+xml", new String[]{"xhtml","xht"}),
	Javascript ("application/javascript", new String[]{"js"}),
	JavascriptDeprecated ("text/javascript", new String[]{"js"}),
	Json ("application/json", new String[]{"json"}),
	Css ("text/css", new String[]{"css"}),
	Plain ("text/plain", new String[]{"txt","text","conf","def","list","log","in"}),
	VOID ("", new String[]{""}),
	
	OctetStream ("application/octet-stream", new String[]{"bin","dms","lrf","mar","so","dist","distz","pkg","bpk","dump","elc","deploy"}),
	Pdf ("application/pdf", new String[]{"pdf"}),
	MsWord ("application/msword", new String[]{"doc","dot"}),
	MsExcel ("application/vnd.ms-excel", new String[]{"xls","xlm","xla","xlc","xlt","xlw"}),
	MsPowerpoint ("application/vnd.ms-powerpoint", new String[]{"ppt","pps","pot"}),
	
	Bmp ("image/bmp", new String[]{"bmp"}),
	Cgm ("image/cgm", new String[]{"cgm"}),
	G3Fax ("image/g3fax", new String[]{"g3"}),
	Gif ("image/gif", new String[]{"gif"}),
	Ief ("image/ief", new String[]{"ief"}),
	Jpeg ("image/jpeg", new String[]{"jpeg","jpg","jpe"}),
	Ktx ("image/ktx", new String[]{"ktx"}),
	Png ("image/png", new String[]{"png"}),
	Sgi ("image/sgi", new String[]{"sgi"}),
	Svg ("image/svg+xml", new String[]{"svg","svgz"}),
	Tiff ("image/tiff", new String[]{"tiff","tif"});
	
	String value;
	String[] extensions;
	MimeType(String mimeType, String[] extensions) {
		this.value = mimeType;
		this.extensions = extensions;
	}
	
	public String value() {
		return this.value;
	}
	
	public String[] getExtensions() {
		return this.extensions;
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
