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

C8O._init_siteclipper = function (params) {
	if (params.redirect_location) {
		var $siteclipped = $("<iframe id=\"siteclipped\" src=\"" + params.redirect_location + "\" height=\"100%\" width=\"100%\" frameborder=\"0\"></iframe>");
		
		C8O.vars.resize_offset = "0";
		C8O._fillBody($siteclipped, false);

		$siteclipped.load(function () {
			var timer = false;
			
			if (C8O._define.iframe && C8O.vars.auto_resize === "true") {
				var $doc = $(this.contentWindow.document);
				var exHeight = 0;
				var reHeight = function () {
					if (exHeight !== $doc.height()) {
						var curHeight = 0;
						$doc.find("*:not(html, body)").each(function () {
							curHeight = Math.max(curHeight, this.offsetTop + this.offsetHeight);
						});
						C8O.doResize(curHeight + 25, {
							complete : function () {
								exHeight = $doc.height();
							}
						});
					}
				}
				
				timer = window.setInterval(reHeight, 250);
				reHeight();
			}
			var onunload = function () {
				if (timer) {
					window.clearInterval(timer);
				}
				if (C8O._hook("siteclipper_page_unloaded", $siteclipped)) {
					C8O.doResize(50);
				};
			}
			
			if (window.attachEvent) {
				this.contentWindow.attachEvent('onunload', onunload);
			} else {
				$(this.contentWindow).bind("unload", onunload);
			}
			
			C8O._hook("siteclipper_page_loaded", this.contentWindow.document);
		});
	}
}