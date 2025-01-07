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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;

public class UICustom extends UIComponent {
	
	private static final long serialVersionUID = -6407500310571517432L;

	public UICustom() {
		super();
	}

	@Override
	public UICustom clone() throws CloneNotSupportedException {
		UICustom cloned = (UICustom) super.clone();
		return cloned;
	}

	protected String htmlTemplate = "";

	public String getCustomTemplate() {
		return htmlTemplate;
	}

	public void setCustomTemplate(String htmlTemplate) {
		this.htmlTemplate = htmlTemplate;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			Map<Long, String> subBeans = new LinkedHashMap<>();
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				String uicTpl = component.computeTemplate();
				Long uicPriority = component.priority;
				subBeans.put(uicPriority, uicTpl);
			}
			
			// Retrieve HTML
			String template = getCustomTemplate();
			
			// Replace each <!--<c8o-sub-bean id="xxx" />--> with corresponding sub-bean template
			Pattern pattern = Pattern.compile("<!--\\s*<c8o-sub-bean\\s*(id=['\"](.*)['\\\"])\\s*/>\\s*-->");
			Matcher matcher = pattern.matcher(template);
			while (matcher.find()) {
				String sequence = matcher.group(0);
				String marker = matcher.group(1);
				if (marker != null) {
					String id = matcher.group(2);
					if (id != null) {
						try {
							Long uicPriority = Long.valueOf(id);
							String uicTpl = subBeans.get(uicPriority);
							if (!uicTpl.isEmpty()) {
								template = template.replace(sequence, System.lineSeparator() + uicTpl);
							}
							subBeans.remove(uicPriority);
						} catch (Exception e) {
							Engine.logStudio.warn("Could not replace template for component with id='"+ id +"' in fragment");
						}
					}
				}
			}
			
			// Replace first <!--<c8o-sub-beans />--> with remaining sub-bean templates
			pattern = Pattern.compile("<!--\\s*<c8o-sub-beans\\s*/>\\s*-->"); // begin c8o marker
			matcher = pattern.matcher(template);
			while (matcher.find()) {
				String sequence = matcher.group(0);
				if (sequence != null) {
					try {
						String uicTpl = "";
						for (String s: subBeans.values()) {
							uicTpl += s;
						}
						if (!uicTpl.isEmpty()) {
							template = template.replace(sequence, System.lineSeparator() + uicTpl);
						}
						break;
					} catch (Exception e) {
						Engine.logStudio.warn("Could not replace template of sub components in fragment");
					}
				}
			}
			
			return template;
		} else {
			return "";
		}
	}

}
