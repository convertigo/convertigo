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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.Shuttle;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public class RemoveCacheHeadersFromRequest extends RemoveCacheHeaders implements IRequestRule {
	private static final long serialVersionUID = 5909037333915096783L;
	private static final Collection<HeaderName> requestCacheHeaders = Collections.unmodifiableList(Arrays.asList(
		HeaderName.CacheControl,
		HeaderName.IfMatch,
		HeaderName.IfModifiedSince,
		HeaderName.IfRange,
		HeaderName.IfNoneMatch,
		HeaderName.IfUnmodifiedSince
	));
	
	public RemoveCacheHeadersFromRequest() {
		super();
	}

	@Override
	public boolean applyOnRequest(Shuttle shuttle) {
		return removeCacheHeaders(shuttle, requestCacheHeaders);
	}
}