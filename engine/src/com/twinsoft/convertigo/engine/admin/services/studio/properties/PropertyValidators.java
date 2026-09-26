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

package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import java.net.URI;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;

/**
 * The checks the Eclipse Studio makes before it sets some properties: the namespace URI of a project, the
 * segment of an NGX page, the module of an NGX shared component.
 */
class PropertyValidators {
	private static final Pattern SYMBOL = Pattern.compile("\\$\\{(.*)\\}");
	private static final Pattern MODULE = Pattern.compile("[A-Z]{1}[a-zA-Z]*Module\\b");

	private PropertyValidators() {
	}

	/**
	 * @return why the value is not valid for the property, or null
	 */
	static String invalid(DatabaseObject dbo, String name, String value) {
		if (dbo instanceof Project && "namespaceUri".equals(name)) {
			if (value.isEmpty()) {
				return null;
			}
			try {
				new URI(value).toURL();
			} catch (Exception e) {
				return "The value \"" + value + "\" isn't a valid URL syntax! It must start with \"http:\"";
			}
		} else if (dbo instanceof PageComponent page && "segment".equals(name)) {
			return invalidSegment(page, value);
		} else if (dbo instanceof UISharedRegularComponent && "sharedModule".equals(name) && !value.isEmpty()) {
			if (!value.equals(value.trim())) {
				return "The module name must not contain space(s)";
			}
			if (!MODULE.matcher(value).find()) {
				return "The module name must only contain letters, start with a capital letter and end with 'Module': e.g. MyCommonModule";
			}
		}
		return null;
	}

	private static String invalidSegment(PageComponent page, String segment) {
		if (segment.isEmpty()) {
			return "The segment must not be empty!";
		}
		if (segment.startsWith("/")) {
			return "The segment must not start with \"/\"!";
		}
		if (segment.endsWith("/")) {
			return "The segment must not end with \"/\"!";
		}
		if (SYMBOL.matcher(segment).find()) {
			return "The segment must not contain symbols!";
		}
		try {
			new URI("http://example.com/" + segment);
		} catch (Exception e) {
			return "The segment is not valid!";
		}
		if (page.getParent() instanceof ApplicationComponent application) {
			for (var other : application.getPageComponentList()) {
				if (other.equals(page)) {
					continue;
				}
				if (paramPath(other.getSegment()).equals(paramPath(segment))) {
					return other.getSegment().equals(segment)
							? "Segment \"" + other.getSegment() + "\" already exists for the application!"
							: "A similar segment \"" + other.getSegment() + "\" already exists for the application!";
				}
				if (other.getName().equals(segment)) {
					return "Segment \"" + segment + "\" is invalid: It must not be the name of another page!";
				}
			}
		}
		return null;
	}

	/**
	 * @return the path of a segment, its parameters as :param
	 */
	private static String paramPath(String segment) {
		try {
			var path = new StringBuilder();
			for (var part : new URI("http://example.com/" + segment).getPath().split("/")) {
				path.append(path.length() == 0 ? "" : "/").append(part.startsWith(":") ? ":param" : part);
			}
			return path.toString();
		} catch (Exception e) {
			return segment;
		}
	}
}
