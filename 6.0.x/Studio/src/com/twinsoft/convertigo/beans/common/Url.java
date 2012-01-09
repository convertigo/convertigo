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

package com.twinsoft.convertigo.beans.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.engine.Context;

public class Url extends Criteria {

	private static final long serialVersionUID = 1864471147803472683L;

	private String regexp = "";
	private transient Pattern regexPattern = null;

	public Url() {
		super();
	}

	public String getRegexp() {
		return regexp;
	}

	public void setRegexp(String regexp) {
		this.regexp = regexp;
		regexPattern = null;
	}
	
	protected Pattern getRegexPattern() {
		if (regexPattern == null) {
			regexPattern = Pattern.compile(regexp);
		}
		return regexPattern;
	}
	
	@Override
	protected boolean isMatching0(Connector connector) {
		try {
			Context context = connector.context;
			String referer = ((HtmlConnector)connector).getHtmlParser().getReferer(context);
			Matcher regex_matcher = getRegexPattern().matcher(referer);
			return regex_matcher.find();
		}
		catch (Exception e) {}
		return false;
	}

}
