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
import java.util.List;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIControlDirective extends UIComponent implements ITagsProperty {
	
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
		
		public static String getDirective(String directiveName) {
			AttrDirective bindDirective = null;
			try {
				bindDirective = AttrDirective.valueOf(directiveName);
			} catch (Exception e) {};
			return bindDirective != null ? bindDirective.directive():directiveName;
		}
	}
	
	public UIControlDirective() {
		super();
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

	protected String getDirectiveValue() {
		StringBuilder children = new StringBuilder();
		
		List<UIComponent> list = getUIComponentList();
		Iterator<UIComponent> it = list.iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIControlDirectiveValue) {
				UIControlDirectiveValue source = (UIControlDirectiveValue)component;
				String value = source.computeTemplate();
				boolean needComma = list.size() > 0;
				if (!value.isEmpty()) {
					children.append(value).append(needComma ? ";":"");
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(children.length()>0 ? children:"");
		
		return sb.toString();
	}

	public String getDirectiveTemplate() {
		if (isEnabled()) {
			String directiveTpl = AttrDirective.getDirective(getDirectiveName());
			if (!directiveTpl.isEmpty()) {
				directiveTpl = " "+ directiveTpl + "=" + "\""+ getDirectiveValue()+"\"";
			}
			if (parent != null && parent instanceof UIControlDirective) {
				directiveTpl = ((UIControlDirective)parent).getDirectiveTemplate() + directiveTpl;
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
		return label = (label.isEmpty() ? "?":label) + " " + getDirectiveValue();
	}

	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			StringBuilder children = new StringBuilder();
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (!(component instanceof UIControlDirectiveValue)) {
					children.append(component.computeTemplate());
				}
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append(children.length()>0 ? children:"");
			
			return sb.toString();
		}
		return "";
	}
}
