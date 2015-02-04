package com.twinsoft.convertigo.engine.util;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.enums.MimeType;

public class ContentTypeDecoder {
	private final static Pattern pattern = Pattern.compile("(.*?)(?: ?; ?charset=(.*)|$)", Pattern.CASE_INSENSITIVE);
	
	String mimeType;
	String charset = null;
	
	public ContentTypeDecoder(String contentType) {
		Matcher matcher = pattern.matcher(contentType);
		if (matcher.matches()) {
			mimeType = matcher.group(1);
			charset = matcher.group(2);
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
