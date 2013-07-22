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

public class MatchUrl extends CriteriaWithRegex implements ISiteClipperRequestCriteria {

	private static final long serialVersionUID = 4421092188025517793L;

	private boolean includeQuery = false;
	
	public MatchUrl() {
		super();
	}

	@Override
	public boolean isMatchingRequest(Shuttle shuttle) {
		String url = includeQuery ? shuttle.getRequestUrlAndQuery() : shuttle.getRequestUrl();
		Matcher regex_matcher = getRegexPattern().matcher(url);
		return regex_matcher.find();
	}

	public void setIncludeQuery(boolean includeQuery) {
		this.includeQuery = includeQuery;
	}

	public boolean isIncludeQuery() {
		return includeQuery;
	}
}
