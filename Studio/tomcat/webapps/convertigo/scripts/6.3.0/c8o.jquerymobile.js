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

$.extend(true, C8O, {
	init_vars : {
	},
	
	ro_vars : {
	},
	
	vars : {
		endpoint_url : "../../"
	},
		
	options : {
		loading : {}
	},
	
	_define : {
	},
	
	_onDocumentReady : function () {
		if (!$.mobile.ajaxBlacklist) {
			$("<div id=\"c8oloading\"/>").css({backgroundColor : "grey", position : "absolute", width : "100%", height : "100%", opacity : 0.5, "z-index" : 99}).hide().appendTo("body");
		}
		
		if (C8O._hook("document_ready")) {
			C8O._init({});
		};
	},
	
	__onSuccess : C8O._onSuccess,
	_onSuccess : function (xml, status, jqXHR) {
		C8O.__onSuccess(xml, status, jqXHR);
		C8O._hook("xml_response", xml, jqXHR.C8O_data);
	},
	
	waitShow : function () {
		if (C8O._hook("loading_start")) {
			$("#c8oloading").show();
			try {
				$.mobile.loading("show", C8O.options.loading);
			} catch (e) {
//				console.log(e);
			}
		}
	},
	
	waitHide : function () {
		if (C8O._hook("loading_stop")) {
			try {
				$.mobile.loading("hide");
			} catch (e) {
//				console.log(e);
			}
			$("#c8oloading").hide();
		}
	}
});