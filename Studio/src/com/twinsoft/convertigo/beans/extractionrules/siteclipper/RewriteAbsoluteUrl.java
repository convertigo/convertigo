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

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.QueryPart;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.MimeType;

public class RewriteAbsoluteUrl extends BaseRule implements IResponseRule {
	private static final long serialVersionUID = 8649548613750737915L;
	
	// Use of HashSet to speedup Collection.contains because hash checking seams speeder than list walking
	private final static Collection<MimeType> htmlTypes = Collections.unmodifiableCollection(new HashSet<MimeType>(Arrays.asList(
		MimeType.Html, MimeType.Xhtml
	)));
	
	private final static Collection<MimeType> cssTypes;
	static {
		Collection<MimeType> c = new HashSet<MimeType>(Arrays.asList(
				MimeType.Css
		));
		c.addAll(htmlTypes);
		cssTypes = Collections.unmodifiableCollection(c);
	}
	
	private final static Pattern htmlCssPattern;
	private final static Pattern htmlPattern;
	private final static Pattern cssPattern;
	static {
		String html_keywords = "href|src|background|action|codebase|longdesc|usemap|cite|data|classid|profile";
		String html_start = "(?i:" + html_keywords +")\\s*=\\s*[\"']?";
		String css_start = "url\\s*\\(\\s*[\"']?";
		String common = "((?:((?:https?:)?//)|/).*?)(/|\\)|'|\"|\\s|\\?|#|$)";
		
		htmlPattern = Pattern.compile("(" + html_start + ")" + common);
		cssPattern = Pattern.compile("(" + css_start + ")" + common);
		htmlCssPattern = Pattern.compile("((?:" + html_start + ")|(?:" + css_start + "))" + common);
	}
	
	private boolean rewriteHtml = true;
	private boolean rewriteCss = true;
	
	public RewriteAbsoluteUrl() {
		super();
	}

	@Override
	public boolean applyOnResponse(Shuttle shuttle) {
		try {
			MimeType mimeType = MimeType.parse(shuttle.getResponseMimeType());
			
			Pattern pattern = null;
			if (rewriteHtml && htmlTypes.contains(mimeType)) {
				if (rewriteCss && cssTypes.contains(mimeType)) {
					Engine.logSiteClipper.trace("(RewriteAbsoluteUrl) Prepare rewriting to use Html + Css replacement pattern");
					pattern = htmlCssPattern;
				} else {
					Engine.logSiteClipper.trace("(RewriteAbsoluteUrl) Prepare rewriting to use Html replacement pattern");
					pattern = htmlPattern;
				}
			} else {
				if (rewriteCss && cssTypes.contains(mimeType)) {
					Engine.logSiteClipper.trace("(RewriteAbsoluteUrl) Prepare rewriting to use Css replacement pattern");
					pattern = cssPattern;
				}
			}
			
			if (pattern != null) {
				replace(pattern, shuttle);
				return true;
			} else {
				Engine.logSiteClipper.trace("(RewriteAbsoluteUrl) Do not rewrite this resource");
			}
		} catch (Exception e) {
			Engine.logSiteClipper.warn("Unable to apply 'RewriteRule' rule : "+ getName(), e);
		}
		return false;
	}
	
	private void replace(Pattern pattern, Shuttle shuttle) throws UnsupportedEncodingException, IOException {
		String content = shuttle.getResponseAsString();
		if (!content.equals("")) {
			StringBuffer sb = new StringBuffer(content.length());
			Matcher m = pattern.matcher(content);
			while (m.find()) {
				String domain = m.group(2);
				String scheme = m.group(3);
				boolean rewrite = true;
				if (scheme != null) {
					if ("//".equals(scheme)) {
						domain = shuttle.getRequest(QueryPart.scheme) + ":" + domain;
					}
					rewrite = getConnector().shouldRewrite(domain);
				}
				if (rewrite) {
					String replace = m.group(1) + shuttle.makeAbsoluteURL(domain) + m.group(4);
					Engine.logSiteClipper.trace("(RewriteAbsoluteUrl) Rewriting \"" + m.group() + "\" to \"" +replace +"\"");
					m.appendReplacement(sb, replace); 
				} else {
					Engine.logSiteClipper.trace("(RewriteAbsoluteUrl) Rewriting \"" + m.group() + "\" failed because \"" + domain + "\" was black listed");
				}
			}
			m.appendTail(sb);
			shuttle.setResponseAsString(sb.toString());
		}
	}

	public boolean isRewriteHtml() {
		return rewriteHtml;
	}

	public void setRewriteHtml(boolean rewriteHtml) {
		this.rewriteHtml = rewriteHtml;
	}

	public boolean isRewriteCss() {
		return rewriteCss;
	}

	public void setRewriteCss(boolean rewriteCss) {
		this.rewriteCss = rewriteCss;
	}
}
