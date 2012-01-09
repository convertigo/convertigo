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

package com.twinsoft.convertigo.beans.criteria.siteclipper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;

import com.twinsoft.convertigo.beans.common.ISiteClipperResponseCriteria;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;

public class MatchRegExp extends CriteriaWithRegex implements ISiteClipperResponseCriteria {

	private static final long serialVersionUID = -2103022190194810731L;
		
	public MatchRegExp() {
		super();		
	}

	@Override
	public boolean isMatchingResponse(Shuttle shuttle) {
		try {			
			String content = shuttle.getResponseAsString();	
			Matcher regex_matcher = getRegexPattern().matcher(content);
			return regex_matcher.find();
		} catch (UnsupportedEncodingException e) {
			Engine.logSiteClipper.warn("(MatchRegExp) failed to retrieve data content", e);
		} catch (IOException e) {
			Engine.logSiteClipper.warn("(MatchRegExp) failed to retrieve data content", e);
		}			
		return false;
	}
	

}
