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

package com.twinsoft.convertigo.beans;

public class BeansUtils {

	public static String cleanDescription(String description, boolean bHtml) {
		String cleanDescription = description;
		// Replace first space
		if (cleanDescription.charAt(0) == ' ') {
			cleanDescription = cleanDescription.substring(1);
		}

		// replace orangetwinsoft class by text color style
		cleanDescription = cleanDescription.replace("class=\"orangetwinsoft\"", (bHtml ? "style=\"color=#FC870A;\"" : ""));

		// replace computer class by new font
		cleanDescription = cleanDescription.replace("class=\"computer\"", (bHtml ? "style=\"font-family: lucida Console;\"" : ""));

		// Double BR tags
		cleanDescription = cleanDescription.replaceAll("<br/>(?:<br/>)?", (bHtml ? "<br/><br/>" : ""));

		return cleanDescription;
	}

}
