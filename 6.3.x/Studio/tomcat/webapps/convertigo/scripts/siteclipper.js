/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

$(window).ready(function () {
	if (typeof(C8O_postInstructions) !== "undefined") {
		for (var i in C8O_postInstructions) {
			var postInstruction = C8O_postInstructions[i];
			var type = postInstruction.type;
			if (type == "SetValue") {
				$(postInstruction.path).val(postInstruction.value);
			} else if (type == "SetChecked") {
				if (postInstruction.checked) {
					$(postInstruction.path).attr("checked", "checked");
				} else {
					$(postInstruction.path).removeAttr("checked");
				}
			} else if (type == "Click") {
				$(postInstruction.path).click();
			}
		}
	}
});