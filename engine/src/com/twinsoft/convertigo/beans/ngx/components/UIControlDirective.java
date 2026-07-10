/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
		CdkVirtualFor("*cdkVirtualFor", "CdkVirtualFor"),
		ForEach("*ngFor","ForEach"),
		If("*ngIf","If"),
		Switch("[ngSwitch]","Switch"),
		SwitchCase("*ngSwitchCase","SwitchCase"),
		SwitchDefault("*ngSwitchDefault","SwitchDefault"),
		NewFor("@for","@for"),
		NewForEmpty("@empty", "@empty"),
		NewIf("@if","@if"),
		NewElseIf("@else if","@else if"),
		NewElse("@else","@else"),
		NewSwitch("@switch","@switch"),
		NewSwitchCase("@case","@case"),
		NewSwitchDefault("@default","@default");
		
		String directive;
		String displayName;
		
		AttrDirective(String directive, String displayName) {
			this.directive = directive;
			this.displayName = displayName;
		}
		
		String directive() {
			return directive;
		}
		
		String displayName() {
			return displayName;
		}
		
		public static AttrDirective getDirective(String directiveName) {
			AttrDirective bindDirective = null;
			try {
				bindDirective = AttrDirective.valueOf(directiveName);
			} catch (Exception e) {};
			return bindDirective;
		}
		
		public static String getDisplayName(String directiveName) {
			AttrDirective bindDirective = getDirective(directiveName);
			return bindDirective != null ? bindDirective.displayName():directiveName;
		}
		
		public static String getDirectiveAttr(String directiveName) {
			AttrDirective bindDirective = getDirective(directiveName);
			return bindDirective != null ? bindDirective.directive():directiveName;
		}
		
		public static boolean isForDirective(String directiveName) {
			AttrDirective bindDirective = getDirective(directiveName);
			return bindDirective != null && 
					(bindDirective.equals(AttrDirective.CdkVirtualFor) || bindDirective.equals(AttrDirective.ForEach) || bindDirective.equals(AttrDirective.NewFor));
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
		String label = AttrDirective.getDisplayName(getDirectiveName());
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
	public boolean updateSmartSourceModelPath(MobileSmartSource oldSource, String newPath) {
		boolean updated = false;
		MobileSmartSource mss = directiveSource.getSmartSource();
		if (mss != null) {
			MobileSmartSource newMss = mss.from(oldSource, newPath);
			if (newMss != null) {
				directiveSource.setSmartValue(newMss.toJsonString());
				updated = this.hasChanged = true;
			}
		}
		return updated;
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
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			AttrDirective attrDirective = AttrDirective.getDirective(getDirectiveName());
			if (attrDirective == AttrDirective.NewFor || 
					attrDirective == AttrDirective.NewForEmpty) {
				return computeTemplateForNewFor(attrDirective);
			}
			else if (attrDirective == AttrDirective.NewIf || 
					attrDirective == AttrDirective.NewElseIf || 
					attrDirective == AttrDirective.NewElse) {
				return computeTemplateForNewIf(attrDirective);
			}
			else if (attrDirective == AttrDirective.NewSwitch || 
					attrDirective == AttrDirective.NewSwitchCase || 
					attrDirective == AttrDirective.NewSwitchDefault) {
				return computeTemplateForNewSwitch(attrDirective);
			}
			else {
				return super.computeTemplate();
			}
		}
		return "";
	}
	
	private String computeTemplateForNewFor(AttrDirective directive) {
		StringBuilder sb = new StringBuilder();
		String signature = getNewForSignature();
		String childrenHtml = computeChildrenTemplate();
		sb.append(directive.directive()).append(directive != AttrDirective.NewForEmpty ? " " + signature : "").append(" {\n")
		  .append(childrenHtml)
		  .append("\n}\n");
		
		return sb.toString();
	}
	private String computeTemplateForNewIf(AttrDirective directive) {
		StringBuilder sb = new StringBuilder();
		String signature = getNewIfSignature();
		String childrenHtml = computeChildrenTemplate();
		sb.append(directive.directive()).append(directive != AttrDirective.NewElse ? " " + signature : "").append(" {\n")
		  .append(childrenHtml)
		  .append("\n}\n");
		
		return sb.toString();
	}
	
	private String computeTemplateForNewSwitch(AttrDirective directive) {
		StringBuilder sb = new StringBuilder();
		String signature = getNewSwitchSignature();
		String childrenHtml = computeChildrenTemplate();
		
		boolean supportConsecutiveCases = false; // TPL dependent: needs Angular 21.1.0 - angular-eslint 21.2.0
/* Commented out until Angular 21 */
//		ComponentManager cm = ComponentManager.of(this);
//		if (cm != null && !cm.isInstance()) {
//			JSONObject jsonOb = cm.getTemplateProjectFileAsJson("ionicTpl/package.json");
//			if (jsonOb != null) {
//				try {
//					String v1 = jsonOb.getJSONObject("dependencies").getString("@angular/core");
//					String v2 = jsonOb.getJSONObject("devDependencies").getString("@angular-eslint/builder");
//					supportConsecutiveCases = v1.compareTo("21.1.0") >= 0 && v2.compareTo("21.2.0") >= 0;
//				} catch (JSONException e) {}
//			}
//		}
		
		sb.append(directive.directive()).append(directive != AttrDirective.NewSwitchDefault ? " " + signature : "")
		  .append(supportConsecutiveCases && childrenHtml.isBlank() ? "" : " {").append("\n")
		  .append(childrenHtml)
		  .append(supportConsecutiveCases && childrenHtml.isBlank() ? "" : "\n}").append("\n");
		
		return sb.toString();
	}
	
	private String getNewForSignature() {
		String itemName = (getDirectiveItemName().isEmpty()) ? "item"+priority : getDirectiveItemName().trim();
		String indexName = getDirectiveIndexName().trim(); // ex : idx
		String src      = getSourceSmartType().getValue(); // ex : "items"
		String expr     = getDirectiveExpression().trim(); // ex : "track item.name"
		
		StringBuilder sb = new StringBuilder();
		sb.append("(")
		  .append(itemName).append(" of ").append(src);
		
		if (!expr.matches(".*\\b(track)\\b.*")) {
			expr += (expr.endsWith(";") ? "":";") + " track $index";
		}
		if (!indexName.isEmpty()) {
			if (!expr.matches(".*\\b("+ indexName +")\\b.*")) {
				expr += (expr.endsWith(";") ? "":";") + " let "+ indexName + " = $index";
			}
		}
		
		if (!expr.trim().isEmpty()) {
			if (!expr.trim().startsWith(";")) {
				sb.append("; ");
			}
			sb.append(expr.trim());
		}
		sb.append(")");
		
		return sb.toString();
	}
	
	private String getNewIfSignature() {
		String src      = getSourceSmartType().getValue(); // ex : "items"
		String expr     = getDirectiveExpression();        // ex : "track item.name"
		
		StringBuilder sb = new StringBuilder();
		sb.append("(")
		  .append(src);
		
		if (!expr.trim().isEmpty()) {
			if (!expr.trim().startsWith(";")) {
				sb.append("; ");
			}
			sb.append(expr.trim());
		}
		sb.append(")");
		
		return sb.toString();
	}
	
	private String getNewSwitchSignature() {
		String src      = getSourceSmartType().getValue(); // ex : "items"
		String expr     = getDirectiveExpression();        // ex : "track item.name"
		
		StringBuilder sb = new StringBuilder();
		sb.append("(")
		  .append(src);
		
		if (!expr.trim().isEmpty()) {
			if (!expr.trim().startsWith(";")) {
				sb.append("; ");
			}
			sb.append(expr.trim());
		}
		sb.append(")");
		
		return sb.toString();
	}
	
	private String computeChildrenTemplate() {
		StringBuilder sb = new StringBuilder();
		if(!getDirectiveItemName().isEmpty() && AttrDirective.getDirective(getDirectiveName()) == AttrDirective.NewFor) {
			sb.append("@let item"+ priority + " = "+getDirectiveItemName()+";\n");
		}
		for (UIComponent child : getUIComponentList()) {
			sb.append(child.computeTemplate());
		}
		return sb.toString();
	}
}
