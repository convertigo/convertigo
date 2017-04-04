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

import java.util.Iterator;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIControlDirective extends UIElement implements IControl, ITagsProperty {
	
	private static final long serialVersionUID = 2750008565134796761L;

	public enum AttrDirective {
		RepeatForEach("*ngFor"),
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
	private String directiveName = AttrDirective.If.name();

	public String getDirectiveName() {
		return directiveName;
	}

	public void setDirectiveName(String directiveName) {
		this.directiveName = directiveName;
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
	 * The iterative item name
	 */
	private String itemName = "";
	
	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	
	protected String getItemVariable() {
		String varItem = itemName.isEmpty() ? "item":itemName;
		return varItem + String.valueOf(this.priority);
	}
	
	/*
	 * The iterative item path
	 */
	private String itemPath = "";
	
	public String getItemPath() {
		return itemPath;
	}

	public void setItemPath(String itemPath) {
		this.itemPath = itemPath;
	}
	
	protected String getComputedValue() {
		StringBuilder listeners = new StringBuilder();
		StringBuilder customs = new StringBuilder();
		
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIControlSource) {
				if (component instanceof UIControlListenSource) {
					String tpl = component.computeTemplate();
					if (!tpl.isEmpty()) {
						listeners.append(listeners.length() > 0 ? ",":"");
						listeners.append(tpl);
					}
				}
				if (component instanceof UIControlCustomSource) {
					String tpl = component.computeTemplate();
					if (!tpl.isEmpty()) {
						customs.append(customs.length() > 0 ? ";":"");
						customs.append(tpl);
					}
				}
			}
		}
		
		StringBuilder sbListen = new StringBuilder();
		if (listeners.length() > 0) {
			String path = getItemPath();
			sbListen.append("listen([").append(listeners).append("])").append(path);
		}
		
		StringBuilder children = new StringBuilder();
		if (sbListen.length() > 0) {
			AttrDirective attrDirective = AttrDirective.getDirective(getDirectiveName());
			if (AttrDirective.RepeatForEach.equals(attrDirective)) {
				String item = getItemName();
				children.append("let "+ (item.isEmpty() ? "item":item)).append(" of ").append(sbListen);
			}
			else {
				children.append(sbListen);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(children).append(directiveExpression);
		
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
		return label = (label.isEmpty() ? "?":label) + " " + getComputedValue();
	}

	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			StringBuilder attributes = new StringBuilder();
			StringBuilder children = new StringBuilder();
			
			attributes.append(getDirectiveTemplate());
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIControlSource) {
					;// ignore
				} else if (component instanceof UIAttribute) {
					attributes.append(component.computeTemplate());
				} else {
					children.append(component.computeTemplate());
				}
			}
			
			String attrId = " id=\"tag"+ priority +"\"";
			
			StringBuilder sb = new StringBuilder();
			sb.append("<").append(getTagName())
				.append(attrId)
				.append(attributes.length()>0 ? attributes:"");
			
			if (isSelfClose()) {
				sb.append("/>").append(System.getProperty("line.separator"));
			}
			else {
				sb.append(">").append(System.getProperty("line.separator"))
					.append(children.length()>0 ? children:"")
				  .append("</").append(getTagName())
				  	.append(">").append(System.getProperty("line.separator"));
			}
			
			return sb.toString();
		}
		return "";
	}
}
