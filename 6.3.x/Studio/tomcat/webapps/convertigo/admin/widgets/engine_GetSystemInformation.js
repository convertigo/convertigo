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

function engine_GetSystemInformation_init(){
	$("#sysinfoGetJavaSystemProperties").button({ icons: { primary: "ui-icon-script" } });

	initSystemInformationDialogs();
	engine_GetSystemInformation_update();
}

function engine_GetSystemInformation_update() {
	callService("engine.GetSystemInformation", function(xml) {
		$("#sysinfoHostName").html($(xml).find("host").attr("name"));
		$("#sysinfoOsArchitecture").html($(xml).find("os").attr("architecture"));
		$("#sysinfoOsAvailableProcessors").html($(xml).find("os").attr("availableProcessors"));
		$("#sysinfoOsName").html($(xml).find("os").attr("name"));
		$("#sysinfoOsVersion").html($(xml).find("os").attr("version"));
		$("#sysinfoHostAddresses").html($(xml).find("host").attr("addresses"));
		$("#sysinfoJavaVendor").html($(xml).find("java").attr("vendor"));
		$("#sysinfoJavaVersion").html($(xml).find("java").attr("version"));
		$("#sysinfoJavaClassVersion").html($(xml).find("java").attr("classVersion"));
		$("#sysinfoMemoryTotal").html("" + parseInt(parseInt($(xml).find("memory").attr("total")) / 1024 / 1024));
		$("#sysinfoMemoryAvailable").html("" + parseInt(parseInt($(xml).find("memory").attr("available")) / 1024 / 1024));
		$("#sysinfoMemoryMaximal").html("" + parseInt(parseInt($(xml).find("memory").attr("maximal")) / 1024 / 1024));
		$("#sysinfoBrowser").html($(xml).find("browser").text());
	});
}

function initSystemInformationDialogs() {
	$("<div id=\"sysinfoDialogJavaSystemProperties\"></div>")
		.html("<textarea style=\"width: 98%\; height: 98%\" readonly/>")
		.dialog({
			autoOpen: false,
			title: "Java system properties",
		    modal: true,
		    width: 800,
		    height: 600,
		    buttons: {
		        Ok: function() {
		            $(this).dialog('close');
		        }
		    }
		});

	$("#sysinfoGetJavaSystemProperties").click(function() {	
		$.get("services/engine.GetJavaSystemProperties", { }, 
			function(xml) {
				$("#sysinfoDialogJavaSystemProperties > textarea").text($(xml).find("admin").text());
				$("#sysinfoDialogJavaSystemProperties").dialog('open');
			}
		);

		return false;
	});
};
