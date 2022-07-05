/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.beans.ngx.components;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.ITagsProperty;

public class UIFontStyle extends UIStyle implements ITagsProperty {

	private static final long serialVersionUID = -9208380003012682078L;

	public UIFontStyle() {
		super();
	}

	@Override
	public UIFontStyle clone() throws CloneNotSupportedException {
		UIFontStyle cloned = (UIFontStyle) super.clone();
		return cloned;
	}
	
	private String ruleTargets = "";
	
	public String getRuleTargets() {
		if (getParent() instanceof UIElement) {
			return "class"+ getParent().priority;
		}
		return ruleTargets;
	}

	public void setRuleTargets(String ruleTargets) {
		if (getParent() instanceof UIElement) {
			return;
		}
		this.ruleTargets = ruleTargets;
	}
	
	private String fontFamily = "";
	
	public String getFontFamily() {
		return fontFamily;
	}

	public void setFontFamily(String fontFamily) {
		this.fontFamily = fontFamily;
	}

	private String fontSize = "";
	
	public String getFontSize() {
		return fontSize;
	}

	public void setFontSize(String fontSize) {
		this.fontSize = fontSize;
	}

	private String fontStyle = "";
	
	public String getFontStyle() {
		return fontStyle;
	}

	public void setFontStyle(String fontStyle) {
		this.fontStyle = fontStyle;
	}

	private String fontWeight = "";
	
	public String getFontWeight() {
		return fontWeight;
	}

	public void setFontWeight(String fontWeight) {
		this.fontWeight = fontWeight;
	}

	@Override
	public FormatedContent getStyleContent() {
		String formated = "";
		boolean isSelf = getParent() instanceof UIElement;
		if (isSelf || !ruleTargets.isBlank()) {
			formated += isSelf ? "" : ruleTargets +" {\n";
			if (!fontFamily.isBlank()) {
				boolean addQuotes = fontFamily.indexOf(' ') != -1;
				if (addQuotes) {
					formated += "font-family: \""+ fontFamily +"\";\n";
				} else {
					formated += "font-family: "+ fontFamily +";\n";
				}
			}
			if (!fontSize.isBlank()) {
				formated += "font-size: "+ fontSize +";\n";
			}
			if (!fontStyle.isBlank()) {
				formated += "font-style: "+ fontStyle +";\n";
			}
			if (!fontWeight.isBlank()) {
				formated += "font-weight: "+ fontWeight +";\n";
			}
			formated += isSelf ? "" : "}\n";
		}
		
		return new FormatedContent(formated);
	}

	public boolean isReadOnlyProperty(String propertyName) {
		if (propertyName.equals("ruleTargets")) {
			return getParent() instanceof UIElement;
		}
		return false;
	}
	
	private String[] getFontFamilies() {
		TreeSet<String> families = new TreeSet<String>(new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		});
		
		UISharedComponent sc = getSharedComponent();
		List<UIFont> fontList = sc == null ? getApplication().getUIFontList() : sc.getUIFontList();
		for (UIFont font: fontList) {
			String fontFamily = font.getFontSource().getFontFamily();
			if (fontFamily != null) {
				families.add(fontFamily);
			}
		}
		return families.toArray(new String[families.size()]);
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("fontFamily")) {
			return getFontFamilies();
		}
		if (propertyName.equals("fontSize")) {
			return new String[] {"inherit","initial","unset","xx-small","x-small","small","medium","large","x-large","xx-large","larger","smaller","12px","0.8em"};
		}
		if (propertyName.equals("fontStyle")) {
			return new String[] {"normal","italic","oblique"};
		}
		if (propertyName.equals("fontWeight")) {
			return new String[] {"inherit","initial","revert","unset","normal","bold","lighter","bolder","100","200","300","400","500","600","700","800","900"};
		}
		if (propertyName.equals("ruleTargets")) {
			if (getParent() instanceof UIElement) {
				return new String[] {"class"+ getParent().priority};
			}
		}
		return new String[0];
	}

	@Override
	public String toString() {
		String formated = "";
		if (ruleTargets.isBlank()) {
			formated = "?";
		} else {
			formated += ruleTargets + " [";
			formated += fontFamily.isBlank() ? "inherit" : fontFamily;
			formated += fontSize.isBlank() ? "" : ";"+ fontSize;
			formated += fontStyle.isBlank() ? "" : ";"+ fontStyle;
			formated += fontWeight.isBlank() ? "" : ";"+ fontWeight;
			formated += "]";
		}
		if (!formated.isBlank()) {
			return formated;
		}
		return super.toString();
	}
	
}
