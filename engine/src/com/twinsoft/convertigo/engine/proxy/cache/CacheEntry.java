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

package com.twinsoft.convertigo.engine.proxy.cache;

import java.util.*;

public abstract class CacheEntry implements java.io.Serializable {
	private static final long serialVersionUID = 8146896324932035741L;
	String resourceUrl = null;
	long expiryDate = 0;
	public String contentType;
	public int contentLength = 0;
	
	public CacheEntry() {
	}
	
	@Override
	public String toString() {
		return "resourceUrl=\"" + resourceUrl + "\", expiryDate=" + (expiryDate == -1 ? "(never expires)" : (new Date(expiryDate)).toString()) + ", contentType=\"" + contentType + "\", contentLength=" + contentLength;
	}
}
