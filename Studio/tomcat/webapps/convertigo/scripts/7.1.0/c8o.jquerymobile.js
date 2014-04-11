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
	init_vars: {
	},
	
	ro_vars: {
	},
	
	vars: {
	},
		
	options: {
		loading: {}
	},
	
	waitHide: function () {
		if (C8O._hook("wait_hide") && C8O._hook("loading_stop")) {
			try {
				$.mobile.loading("hide");
			} catch (e) {
				if (C8O.isDefined(e.message) && e.message.indexOf("'loader'") == -1) {
					C8O.log.error("c8o.jqm : failed to hide loading", e);
				} else {
					C8O.log.trace("c8o.jqm : failed to hide loading " + e);
				}
			}
			$("#c8oloading").hide();
		}
	},
	
	waitShow: function () {
		if (C8O._hook("wait_show") && C8O._hook("loading_start")) {
			$("#c8oloading").show();
			try {
				$.mobile.loading("show", C8O.options.loading);
			} catch (e) {
				if (C8O.isDefined(e.message) && e.message.indexOf("'loader'") == -1) {
       		 		C8O.log.error("c8o.jqm : failed to show loading", e);
				} else {
					C8O.log.trace("c8o.jqm : failed to hide loading " + e);
				}
			}
		}
	},
	
	_define: {
	},
	
	_jqm_onDocumentReady: C8O._onDocumentReady,
	_onDocumentReady: function (params) {
		if (!$.mobile.ajaxBlacklist) {
			$("<div id=\"c8oloading\"/>").css({backgroundColor: "grey", position: "fixed", width: "100%", height: "100%", opacity: 0.5, "z-index": 99}).hide().appendTo("body");
		}
		
		$(document).on("pageshow", function () {
			if ($("#c8oloading").is(":visible")) {
				try {
					$.mobile.loading("show", C8O.options.loading);
				} catch (e) {
					if (C8O.isDefined(e.message) && e.message.indexOf("'loader'") == -1) {
	       		 		C8O.log.error("c8o.jqm : failed to show loading", e);
					} else {
						C8O.log.trace("c8o.jqm : failed to show loading " + e);
					}
				}
			}
		});
		
		C8O._jqm_onDocumentReady(params);
	},
	
	_onJqmInitFinished: function () {
		C8O.log.info("c8o.jqm : initializing jquery mobile");
		$("[data-role=page]").data("c8o-translated", true);
		
		$.mobile.initializePage();
		
		$(document).on("pagebeforecreate", "[data-role=page]", function () {
			C8O._onPageBeforeCreate($(this));
		});
	},
	
	_onPageBeforeCreate: function ($page) {
		if (!$page.data("c8o-translated")) {
			C8O.log.debug("c8o.jqm : new DOM page loaded, try to translate it");
			$page.data("c8o-translated", true);
			C8O.translate($page[0]);
		}
		
	}
});

$(document).ready(function () {
	$.mobile.autoInitializePage = false;
});
//For WindowsPhone accept the cross-plateform
$.support.cors = true;

C8O.addHook("init_finished", function () {
	C8O._onJqmInitFinished();
});