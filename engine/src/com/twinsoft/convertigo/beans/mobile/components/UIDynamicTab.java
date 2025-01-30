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

package com.twinsoft.convertigo.beans.mobile.components;

public class UIDynamicTab extends UIDynamicElement {

	private static final long serialVersionUID = 4058987667153475733L;

	public UIDynamicTab() {
		super();
	}

	public UIDynamicTab(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicTab clone() throws CloneNotSupportedException {
		UIDynamicTab cloned = (UIDynamicTab) super.clone();
		return cloned;
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

	private String getPageName() {
		if (!tabpage.isEmpty()) {
			try {
				return tabpage.substring(tabpage.lastIndexOf('.')+1);
			} catch (Exception e) {}
		}
		return "";
	}
	
	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();
		String pageName = getPageName();
		if (!pageName.isEmpty()) {
			try {
				if (compareToTplVersion("7.7.0.2") < 0) {
					attributes.append(" [root]").append("=").append("\"router.pagesKeyValue['"+ pageName +"']\"");
				} else {
					attributes.append(" [root]").append("=").append("\"'"+ pageName +"'\"");
				}
			} catch (Exception e) {}
		}
		return attributes;
	}
	
	@Override
	public String toString() {
		String pageName = getPageName();
		return super.toString() + ": " + (pageName.isEmpty() ? "?":pageName);
	}
}
