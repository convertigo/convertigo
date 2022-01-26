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

package com.twinsoft.convertigo.eclipse.property_editors.validators;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ICellEditorValidator;

import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;

public class MobilePageSegmentValidator implements ICellEditorValidator {

	private PageComponent page;
	
	public MobilePageSegmentValidator(PageComponent page) {
		this.page = page;
	}

	@Override
	public String isValid(Object value) {
		if (page != null) {
			String segment = value.toString();
			
			if (segment.isEmpty()) {
				return "The segment must not be empty!";
			}
			if (segment.startsWith("/")) {
				return "The segment must not start with \"/\"!";
			}
			if (segment.endsWith("/")) {
				return "The segment must not end with \"/\"!";
			}

			Pattern pattern = Pattern.compile("\\$\\{(.*)\\}");
			Matcher matcher = pattern.matcher(segment);
			if (matcher.find()) {
				return "The segment must not contain symbols!";
			}
			
			try {
				new URI("http://example.com/"+segment);
			} catch (Exception e) {
				return "The segment is not valid!";
			}
			
			ApplicationComponent app = page.getApplication();
			if (app != null) {
				for (PageComponent p: app.getPageComponentList()) {
					if (!p.equals(page)) {
						if (toParamPath(p.getSegment()).equals(toParamPath(segment))) {
							if (p.getSegment().equals(segment))
								return "Segment \"" + p.getSegment() + "\" already exists for the application!";
							else
								return "A similar segment \"" + p.getSegment() + "\" already exists for the application!";
						}
						if (p.getName().equals(segment)) {
							return "Segment \"" + segment + "\" is invalid: It must not be the name of another page!";
						}
					}
				}
			}
		}
		return null;
	}

	private static String toParamPath(String segment) {
		try {
			String paramPath = "";
			URI uri = new URI("http://example.com/"+segment);
			for (String s: uri.getPath().split("/")) {
				paramPath += paramPath.isEmpty() ? "":"/";
				paramPath += s.startsWith(":") ? ":param":s;
			}
			return paramPath;
		} catch (Exception e) {}
		
		return segment;
	}
}
