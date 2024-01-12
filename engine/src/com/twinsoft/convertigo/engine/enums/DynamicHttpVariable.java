/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.DownloadHttpTransaction;

public enum DynamicHttpVariable {
	__body("HTTP body", "Content <b><u>body</u></b> of the HTTP POST or PUT request."),
	__uri("URI", "Override <b><u>URI</u></b> of the request."),
	__POST_("POST variable", "Add dynamic <b><u>POST variable</u></b> for this request. You have to rename the <b><u>custom</u></b> part of the variable name.", ""),
	__GET_("GET variable", "Add dynamic <b><u>POST variable</u></b> for this request. You have to rename the <b><u>custom</u></b> part of the variable name.", ""),
	__contentType("Content-Type", "Override the <b><u>Content-Type</u></b> header of the request. Can be useful in combination of <b><u>body</u></b>."),
	__header_("Custom Header", "Add dynamic <b><u>custom header</u></b> for this request. You have to rename the <b><u>custom</u></b> part of the variable name.", "__header_"),
	__download_folder("Download folder", "Override the <b><u>Folder</u></b> property of this transaction", DownloadHttpTransaction.class),
	__download_filename("Download filename", "Override the <b><u>Filename</u></b> property of this transaction", DownloadHttpTransaction.class);
	
	String display;
	String description;
	String prefix;
	Class<?>[] onlyFor;
	
	DynamicHttpVariable(String display, String description, String prefix, Class<?>... onlyFor) {
		this.display = display;
		this.description = description;
		this.onlyFor = onlyFor;
		this.prefix = prefix;
	}
	
	DynamicHttpVariable(String display, String description, Class<?>... onlyFor) {
		this(display, description, null, onlyFor);
	}
	
	public String display() {
		return display;
	}
	
	public String description() {
		return description;
	}
	
	public String prefix() {
		return prefix;
	}
	
	public boolean can(AbstractHttpTransaction transaction) {
		if (onlyFor.length == 0) {
			return true;
		}
		Class<?> tc = transaction.getClass();
		for (Class<?> c: onlyFor) {
			if (tc.isAssignableFrom(c)) {
				return true;
			}
		}
		return false;
	}
}
