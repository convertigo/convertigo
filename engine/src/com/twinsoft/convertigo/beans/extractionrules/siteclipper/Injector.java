/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HtmlLocation;
import com.twinsoft.convertigo.engine.util.RegexpUtils;

public abstract class Injector extends BaseRule implements IResponseRule {
	private static final long serialVersionUID = -2865404303732327509L;
	
	private HtmlLocation location = HtmlLocation.head_bottom;
	private String customRegexp = "";
	transient private Pattern custom_pattern = null;
	
	public Injector() {
		super();
	}

	@Override
	public boolean applyOnResponse(Shuttle shuttle) {
		try {
			String content = shuttle.getResponseAsString();
			Matcher matcher = (location == HtmlLocation.custom) ? getCustomMatcher(content) : location.matcher(content);
			String newContent = RegexpUtils.inject(matcher, codeToInsert(shuttle));
			if (newContent != null) {
				shuttle.setResponseAsString(newContent);
				return true;
			} else {
				Engine.logSiteClipper.debug("(Injector) The selected pattern was not found in the current ressource");
			}
		} catch (Exception e) {
			Engine.logSiteClipper.warn("Unable to apply 'Injector' rule : "+ getName(), e);
		}
		return false;
	}
	
	abstract protected String codeToInsert(Shuttle shuttle);

	private Matcher getCustomMatcher(String s) {
		if (custom_pattern == null) {
			custom_pattern = Pattern.compile(customRegexp);
		}
		return custom_pattern.matcher(s);
	}
	
	public void setCustomRegexp(String custom_regexp) {
		this.customRegexp = custom_regexp;
		custom_pattern = null;
	}

	public String getCustomRegexp() {
		return customRegexp;
	}

	public HtmlLocation getLocation() {
		return location;
	}

	public void setLocation(HtmlLocation location) {
		this.location = location;
	}
}
