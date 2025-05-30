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

package com.twinsoft.convertigo.engine.enums;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum HtmlLocation {
	head_top("(?i:<head[^>]*>())"),
	head_bottom("(?i:()</head>)"),
	body_top("(?i:<body[^>]*>())"),
	body_bottom("(?i:()</body>)"),
	custom();

	Pattern pattern = null;

	HtmlLocation () {
	}

	HtmlLocation (String s_pattern) {
		pattern = Pattern.compile(s_pattern);
	}

	public Matcher matcher(String s) {
		return pattern.matcher(s);
	}
}
