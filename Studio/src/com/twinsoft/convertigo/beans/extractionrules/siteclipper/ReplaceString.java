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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;

public class ReplaceString extends BaseRule implements IResponseRule {

	private static final long serialVersionUID = -5169971576599772010L;

	private String regexp = "";
	private String replacement = "";

	private transient Pattern regexPattern = null;
	
	public ReplaceString() {
		super();
	}

	/**
	 * @return the regexp
	 */
	public String getRegexp() {
		return regexp;
	}

	/**
	 * @param regexp the regexp to set
	 */
	public void setRegexp(String regexp) {
		this.regexp = regexp;
		regexPattern = null;
	}

	/**
	 * @return the replacement
	 */
	public String getReplacement() {
		return replacement;
	}

	/**
	 * @param replacement the replacement to set
	 */
	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	@Override
	public boolean applyOnResponse(Shuttle shuttle) {
		try {			
			if (!regexp.equals("")) {
				String content = shuttle.getResponseAsString();
				Matcher matcher = getRegexPattern().matcher(content);
				if (matcher.find()) {
					String resolved_replacement = shuttle.resolveVariables(replacement);
					Engine.logSiteClipper.trace("(ReplaceString) Replacing regular expression \"" + regexp + "\" with \"" + resolved_replacement +"\"");
					content = matcher.replaceAll(resolved_replacement);
					shuttle.setResponseAsString(content);
					return true;
				} else {
					Engine.logSiteClipper.trace("(ReplaceString) Replacing regular expression \"" + regexp + "\" failed because it was not found");
				}
			}
		} catch (Exception e) {
			Engine.logSiteClipper.warn("Unable to apply 'ReplaceString' rule : "+ getName(), e);
		}
		return false;
	}

	private Pattern getRegexPattern() {
		if (regexPattern == null) {
			regexPattern = Pattern.compile(regexp);
		}
		return regexPattern;
	}	
}
