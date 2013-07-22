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

import java.util.regex.Matcher;

import com.twinsoft.convertigo.beans.common.ISiteClipperRequestCriteria;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;

public class MatchHeaderOfRequest extends MatchHeader implements ISiteClipperRequestCriteria {

	private static final long serialVersionUID = -5185301998486386177L;

	public MatchHeaderOfRequest() {
		super();
	}

	@Override
	public boolean isMatchingRequest(Shuttle shuttle) {
		String headerName = getHeaderName();
		String headerValue = shuttle.getRequestHeader(headerName);
		if (headerValue == null) {
			Engine.logSiteClipper.trace("(MatchHeaderOfRequest) header \"" + headerName + "\" doesn't exist");
			return false;
		}
		Matcher regex_matcher = getRegexPattern().matcher(headerValue);
		Engine.logSiteClipper.trace("(MatchHeaderOfRequest) header \"" + headerName + "\" found : " + headerValue);
		return regex_matcher.find();
	}
}
