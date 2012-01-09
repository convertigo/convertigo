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

package com.twinsoft.convertigo.engine.cache;

import java.util.*;

public abstract class CacheEntry implements java.io.Serializable {

	private static final long serialVersionUID = 5332805151868391430L;

	public String requestString = null;
	
	public long expiryDate = 0;
	
	public String sheetUrl = null;
	public String absoluteSheetUrl = null;
	public String contentType = null;
	
	public CacheEntry() {
	}
	
	public String toString() {
		return "requestString=\"" + requestString + "\", expiryDate=" + (expiryDate == -1 ? "(never expires)" : (new Date(expiryDate)).toString()) + ", sheetUrl=\"" + sheetUrl + "\", absoluteSheetUrl=\"" + absoluteSheetUrl + "\", contentType=\"" + contentType + "\"";
	}
}
