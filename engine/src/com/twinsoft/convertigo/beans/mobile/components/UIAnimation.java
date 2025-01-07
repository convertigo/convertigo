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

public class UIAnimation extends UIAttribute {

	private static final long serialVersionUID = -2272380428896703765L;

	public UIAnimation() {
		super();
	}

	@Override
	public UIAnimation clone() throws CloneNotSupportedException {
		UIAnimation cloned = (UIAnimation) super.clone();
		return cloned;
	}

	@Override
	public String getAttrName() {
		return "class";
	}

	@Override
	protected String getAttrValue() {
		return animationName.isEmpty() ? "":"animated "+ (isInfinite ? "infinite ":"") +animationName;
	}
	
	private String animationName = "";

	public String getAnimationName() {
		return animationName;
	}

	public void setAnimationName(String animationName) {
		this.animationName = animationName;
	}
	
	private boolean isInfinite = false;
	
	public boolean isInfinite() {
		return isInfinite;
	}
	
	public void setInfinite(boolean isInfinite) {
		this.isInfinite = isInfinite;
	}

	@Override
	public String toString() {
		String label = "animation=" + (animationName.isEmpty() ? "none":animationName);
		return label;
	}

	@Override
	public boolean updateSmartSource(String oldString, String newString) {
		return false;
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("animationName")) {
			return CssAnimation.names();
		}
		return new String[0];
	}
	
}
