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

package com.twinsoft.convertigo.eclipse.property_editors.validators;

import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ICellEditorValidator;

public class NgxSharedComponentModuleValidator implements ICellEditorValidator {

	@Override
	public String isValid(Object value) {
		if (value != null && !("".equals(value))) {
			String module = value.toString();
			
			if (!module.equals(module.trim())) {
				return "The module name must not contain space(s)";
			}
			if (!Pattern.compile("[A-Z]{1}[a-zA-Z]*Module\\b").matcher(module).find()) {
				return "The module name must only contain letters, start with a capital letter and end with 'Module': e.g. MyCommonModule";
			}
		}	
		return null;
	}
}
