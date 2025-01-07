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

package com.twinsoft.convertigo.beans.ngx.components;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIControlDirective extends UIElement implements IControl, ITagsProperty {
	
	private static final long serialVersionUID = 2750008565134796761L;

	public enum AttrDirective {
		CdkVirtualFor("*cdkVirtualFor"),
		ForEach("*ngFor"),
		If("*ngIf"),
		Switch("[ngSwitch]"),
		SwitchCase("*ngSwitchCase"),
		SwitchDefault("*ngSwitchDefault"),
		;
		
		String directive;
		AttrDirective(String directive) {
			this.directive = directive;
		}
		
		String directive() {
			return directive;
		}
		
		public static AttrDirective getDirective(String directiveName) {
			AttrDirective bindDirective = null;
			try {
				bindDirective = AttrDirective.valueOf(directiveName);
			} catch (Exception e) {};
			return bindDirective;
		}
		
		public static String getDirectiveAttr(String directiveName) {
			AttrDirective bindDirective = getDirective(directiveName);
			return bindDirective != null ? bindDirective.directive():directiveName;
		}
		
		public static boolean isForDirective(String directiveName) {
			AttrDirective bindDirective = getDirective(directiveName);
			return bindDirective != null && 
					(bindDirective.equals(AttrDirective.CdkVirtualFor) || bindDirective.equals(AttrDirective.ForEach));
		}
	}
	
	public UIControlDirective() {
		super("ng-container");
	}

	@Override
	public UIControlDirective clone() throws CloneNotSupportedException {
		UIControlDirective cloned = (UIControlDirective) super.clone();
		return cloned;
	}
	
	/*
	 * The directive to bind
	 */
	private String directiveName = AttrDirective.ForEach.name();

	public String getDirectiveName() {
		return directiveName;
	}

	public void setDirectiveName(String directiveName) {
		this.directiveName = directiveName;
	}

	private String directiveItemName = "";
	
	public String getDirectiveItemName() {
		return directiveItemName;
	}

	public void setDirectiveItemName(String directiveItemName) {
		this.directiveItemName = directiveItemName;
	}

	private String directiveIndexName = "";
	
	public String getDirectiveIndexName() {
		return directiveIndexName;
	}

	public void setDirectiveIndexName(String directiveIndexName) {
		this.directiveIndexName = directiveIndexName;
	}

	/*
	 * The directive value
	 */
	private String directiveExpression = "";

	public String getDirectiveExpression() {
		return directiveExpression;
	}

	public void setDirectiveExpression(String directiveExpression) {
		this.directiveExpression = directiveExpression;
	}
	
	/*
	 * The directive source
	 */
	private MobileSmartSourceType directiveSource = new MobileSmartSourceType();
	
	public MobileSmartSourceType getSourceSmartType() {
		return directiveSource;
	}

	public void setSourceSmartType(MobileSmartSourceType directiveSource) {
		this.directiveSource = directiveSource;
	}

	protected String getComputedValue() {
		StringBuilder sbSource = new StringBuilder();
		sbSource.append(directiveSource.getValue());
		
		StringBuilder sbListen = new StringBuilder();
		if (sbSource.length() > 0) {
			if (AttrDirective.isForDirective(getDirectiveName())) {
				String item = "item"+ this.priority;
				
				// add index
				String indexName = getDirectiveIndexName();
				if (!indexName.isEmpty()) {
					sbListen.append("let "+ indexName).append(" = ").append("index;");
				}
				// add item
				String itemName = getDirectiveItemName();
				if (!itemName.isEmpty()) {
					sbListen.append("let "+ itemName).append(" of ").append(sbSource).append(";");
				}
				// add source
				sbListen.append("let "+ item).append(" of ").append(sbSource);
				
				// add custom expression
				if (!directiveExpression.trim().isEmpty()) {
					if (StringUtils.isAlphanumeric(""+directiveExpression.trim().charAt(0))) {
						sbListen.append(";");
					}
				}
			}
			else {
				sbListen.append(sbSource);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(sbListen).append(directiveExpression);
		
		return sb.toString();
	}

	protected String getDirectiveTemplate() {
		if (isEnabled()) {
			String directiveTpl = "";
			String value = getComputedValue().replaceAll("\"", "'");
			String attr = AttrDirective.getDirectiveAttr(getDirectiveName());
			if (!attr.isEmpty()) {
				directiveTpl = " "+ attr + "=" + "\""+ value +"\"";
			}
			return directiveTpl;
		}
		return "";
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("directiveName")) {
			return EnumUtils.toNames(AttrDirective.class);
		}
		return new String[0];
	}

	@Override
	public String toString() {
		String label = getDirectiveName();
		return label = (label.isEmpty() ? "?":label) + " " 
							+ directiveSource.getLabel()
							+ (directiveExpression.trim().startsWith(";") ? "":";")
							+ directiveExpression;
	}

	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();
		attributes.append(getDirectiveTemplate());
		return attributes;
	}

	@Override
	public boolean updateSmartSource(String oldString, String newString) {
		boolean updated = false;
		String smartValue = directiveSource.getSmartValue();
		if (smartValue.indexOf(oldString) != -1 || Pattern.compile(oldString).matcher(smartValue).find()) {
			directiveSource.setSmartValue(smartValue.replaceAll(oldString, newString));
			updated = this.hasChanged = true;
		}
		return updated;
	}
}
