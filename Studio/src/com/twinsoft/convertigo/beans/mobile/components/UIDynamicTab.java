/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

public class UIDynamicTab extends UIDynamicElement {

	private static final long serialVersionUID = 4058987667153475733L;

	public UIDynamicTab() {
		super();
	}

	public UIDynamicTab(String tagName) {
		super(tagName);
	}

	/*
	 * The page associated with tab
	 */
	private String tabpage = "";
	
	public String getTabPage() {
		return tabpage;
	}

	public void setTabPage(String tabpage) {
		this.tabpage = tabpage;
	}

	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();
		if (!tabpage.isEmpty()) {
			try {
				String page = tabpage.substring(tabpage.lastIndexOf('.')+1);
				attributes.append(" [root]").append("=").append("\"getPageByName('"+ page +"')\"");
			} catch (Exception e) {}
		}
		return attributes;
	}
	
}
