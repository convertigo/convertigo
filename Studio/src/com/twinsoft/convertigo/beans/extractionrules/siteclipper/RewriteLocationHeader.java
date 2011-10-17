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

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public class RewriteLocationHeader extends BaseRule implements IResponseRule {

	private static final long serialVersionUID = 3172137285962203943L;
	
	public RewriteLocationHeader() {
		super();
	}

	@Override
	public boolean applyOnResponse(Shuttle shuttle) {
		String url = shuttle.getCustomHeader(HeaderName.Location);
		if ((url != null) && (!url.equals(""))) {
			if (getConnector().shouldRewrite(url)) {
				String absoluteUrl = shuttle.makeAbsoluteURL(url, true);
				Engine.logSiteClipper.trace("(RewriteLocationHeader) Rewriting Location url \"" + url + "\" to \"" + absoluteUrl +"\"");
				shuttle.setCustomHeader(HeaderName.Location, absoluteUrl);
				return true;
			}
			Engine.logSiteClipper.trace("(RewriteLocationHeader) Rewriting Location url failed because it was black listed");
			return false;
		}
		Engine.logSiteClipper.trace("(RewriteLocationHeader) Rewriting Location url failed because header was not found");
		return false;
	}
}
