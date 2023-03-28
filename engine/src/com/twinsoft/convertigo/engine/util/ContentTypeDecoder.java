/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.enums.MimeType;

public class ContentTypeDecoder {
	private final static Pattern pattern = Pattern.compile("(?:.*, ?)?(.*?)(?: ?; ?(charset=(.*)|.*)|$)", Pattern.CASE_INSENSITIVE);
	
	String mimeType;
	String charset = null;
	String option = null;
	
	public ContentTypeDecoder(String contentType) {
		Matcher matcher = pattern.matcher(contentType == null ? "" : contentType);
		if (matcher.matches()) {
			mimeType = matcher.group(1);
			option = matcher.group(2);
			if (option != null && option.isEmpty()) {
				option = null;
			}
			charset = matcher.group(3);
			if (charset != null && charset.isEmpty()) {
				charset = null;
			}
		} else {
			mimeType = "";
		}
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public MimeType mimeType() {
		return MimeType.parse(mimeType);
	}
	
	public String getOption() {
		return option;
	}
	
	public String getCharset() {
		return charset;
	}
	
	public Charset charset() {
		try {
			return Charset.forName(charset);
		} catch (Exception e) {
			return null;
		}
	}
	
	public String getCharset(String defaultCharset) {
		return charset != null ? charset : defaultCharset;
	}
	
	public Charset charset(String defaultCharset) {
		try {
			return Charset.forName(getCharset(defaultCharset));
		} catch (Exception e) {
			return null;
		}
	}
}
