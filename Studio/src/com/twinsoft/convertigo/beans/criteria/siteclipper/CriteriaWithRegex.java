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

import java.util.regex.Pattern;

public abstract class CriteriaWithRegex extends BaseCriteria {

	private static final long serialVersionUID = 5764882110150448490L;

	public CriteriaWithRegex() {
		super();
	}

	private String regexp = "";
	private transient Pattern regexPattern = null;

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
}
