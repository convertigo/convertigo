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

var vars, defs = {
	dpi : 96,
	classnames : {
		Android : "HtcDesire",
		BlackBerry6 : "BlackBerryTorch",
		IPad : "iPad1",
		IPhone3 : "iPhone3",
		IPhone4 : "iPhone4"
	},
	phones : {
		iPad1 : { name : "iPad 1", dpi : 132, os: "ios", install: "IPA", useragent: "iphone" },
		iPhone4 : { name : "iPhone 4", dpi : 163, os: "ios", install: "IPA", useragent: "iphone" },
		iPhone3 : { name : "iPhone 3", dpi : 163, os: "ios", install: "IPA", useragent: "iphone" },
		HtcDesire : { name : "HTC Desire", dpi : 169, os: "android", install: "APK", useragent: "android" },
    	BlackBerryTorch : { name : "BlackBerry Torch", dpi : 187.5, os: "blackberry", install: "JAD", useragent: "blackberry" }
	}
};

function addMobileDevice($device, $parent) {
	var $device_div = $("#templates .device").clone();
	setName($device_div.find(".device_name"), $device);
	$device_div.attr('id', $device.attr('classname'));
	$device_div.attr('path', $device.attr('ressource_path'));
	
	var layout = defs.classnames[$device.attr("classname")];
	if (typeof(layout) === "undefined") {
		layout = "none";
	}
	$device_div.find(".device_layout").attr("value", layout);
	$parent.append($device_div);
}

function parseJSONarray(value) {
	if (value.length) {
		try {
			return $.parseJSON(value);
		} catch (e) {
			
		}
	}
	return [];
}

function addRequestable($requestable, $parent) {
	var $requestable_div = $("#templates .requestable").clone();
	setName($requestable_div.find(".requestable_name"), $requestable);
	
	var $form = $requestable_div.find("form:first");
	var requestable_type = $requestable[0].tagName;
	if (requestable_type == "sequence") {
		$("#templates .hidden_sequence").clone().appendTo($form).find(".sequence_value").val($requestable.attr("name"));
	} else if (requestable_type == "transaction") {
		var $hidden_transaction = $("#templates .hidden_transaction").clone().appendTo($form);
		$hidden_transaction.find(".transaction_value").val($requestable.attr("name"));
		$hidden_transaction.find(".connector_value").val($requestable.parent().attr("name"));
	} 
	
	// Add variables
	$requestable.find(">variable").each( function () {
		var $variable = $(this);
		var isMasked = $variable.attr("isMasked") === "true";
		var isMultiValued = $variable.attr("isMultivalued") === "true";
		var isFileUpload = $variable.attr("isFileUpload") === "true";
		
		var $variable_div = $("#templates .variable").clone();
		setName($variable_div.find(".variable_name"), $variable);
		
		var $variable_type = $variable_div.find(".variable_type").data({
			name : $variable.attr("name"),
			isMasked : isMasked,
			isFileUpload : isFileUpload
		});
		
		// Handle multi-valued variable
		if (isMultiValued) {
			var values_array = parseJSONarray($variable.attr("value"));
			
			$variable_type.append($("#templates .multi_valued").clone());
			for (var i = 0; i < values_array.length; i++) {
				$variable_value_type = $("#templates .new_multi_valued").filter(isFileUpload ? ".value_file" : isMasked ? ".value_password" : ".value_text").clone();
				$variable_value_type.find(".variable_value").attr("name", $variable.attr("name")).not("[type=file]").val(values_array[i]);
				$variable_type.append($variable_value_type);
			}
		} else {
			var $variable_value_type = $("#templates .single_valued").filter(isFileUpload ? ".value_file" : isMasked ? ".value_password" : ".value_text").clone();
			$variable_value_type.find(".variable_value").attr("name", $variable.attr("name")).not("[type=file]").val($variable.attr("value"));
			$variable_type.append($variable_value_type);
		}

		$requestable_div.find(".requestable_variables_footer").before($variable_div);
	});
	
	// Add Test cases
	var $testcases = $requestable.find(">testcase");
	if ($testcases.length === 0) {
		$requestable_div.find(".requestable_testcase_div").remove();
	} else  {
		$testcases.each( function () {
			var $testcase = $(this);
			var $testcase_div = $("#templates .testcase").clone();
			setName($testcase_div.find(".testcase_name"), $testcase);
			
			// Add Test case variables
			$testcase.find(">variable").each( function () {
				var $variable = $(this);
				var $variable_value = $variable.attr("value");
				var isMasked = $variable.attr("isMasked") === "true";
				var isMultiValued = $variable.attr("isMultivalued") === "true";
				var $variable_div = $("#templates .testcase_variable").clone();
				setName($variable_div.find(".testcase_variable_name"), $variable);
				if (isMultiValued) {
					$variable_div.find(".testcase_variable_value").attr("isMultivalued", "true");
				}
				$variable_div.find(".testcase_variable_value").text(isMasked ? "******":$variable_value);
				$testcase_div.find(".testcase_variables").append($variable_div);
			});
			$requestable_div.find(".requestable_testcase").append($testcase_div.children());
		});
	}
		
	$requestable_div.find("hr:last").remove();
	
	$parent.append($requestable_div);
}

function addRequestableData(params, $requestable_name) {
	var connectorName = $requestable_name.parents(".connector:first").find(".connector_name").text();
	if (connectorName.length > 0) {
		params.__connector = connectorName;
		params.__transaction = $requestable_name.text();
	} else {
		params.__sequence = $requestable_name.text();
	}
	return params;
}

function alignPusher() {
	if (!$("#window_exe_pin").hasClass("window_exe_pinned")) {
	    var windowpos = $(window).scrollTop();
	    var finaldestination = windowpos-vars.pusher_original_pos + 30;
	    $("#pusher").stop().animate({"height" : finaldestination}, 500);
	}
}

function showQRCode($parent, $device, isBlackberry) {
	//var $td = $(this);
	var $td = $parent;
	
	//var href = "projects/" + vars.projectName + "/DisplayObjects/mobile/index.html";
	var href = "projects/" + vars.projectName + "/" + $device.attr('ressource_path');
	var build_url = $td.find(".build_url").attr("value");
	var target_url = (build_url === "") ? vars.base_url + href : build_url;
	if (build_url != "") {
		href = build_url;
	}
	
	var img_url = "images/new/logo_c8o_bgQRcode.png";
	if (!isBlackberry) {
		img_url = vars.base_url + "qrcode?" + $.param({
			o : "image/png",
			e : "L",
			s : 4,
			d : target_url
		});
		
		$td.find(">a").empty().attr("href", href).append($("<img/>").attr("src", img_url).attr("title", "qrcode").attr("alt", "error, maybe too long url"));
	} else {
		$td.find(">a").empty().attr("href", href).append($("<img/>").attr("src", img_url));
		var $qrcode_platform = $td.find(".qrcode_platform");
		if ($qrcode_platform.length == 0) {
			$qrcode_platform =	$("<div/>").attr("class", "qrcode_platform").appendTo($td);
		}
		$qrcode_platform.text("Please find the generated application in your PhoneGap build platform.");
	}
}

function launchCliplet(url, mobile_layout, scale) {
	vars.last_url = url;
	vars.last_layout = mobile_layout;
	if (mobile_layout === "none") {
		var $iframe = $("#cliplet_div_iframe");
		if (isC8oCall() && $iframe.length && typeof($iframe[0].contentWindow.C8O) !== "undefined") {
			$iframe[0].contentWindow.C8O.call(url.substring(url.indexOf("?") + 1));
		} else {
			$("#window_exe_content").empty().append("<iframe id='cliplet_div_iframe' frameborder='0' src='" + url + "'></iframe>");
			$iframe.slideDown(500);
		}
	} else {
		var $phone = $("<div/>").addClass(mobile_layout + "_mask").addClass("common_mask").appendTo($("#window_exe_content").empty());
		$("<iframe frameborder='0' src='"+url+"'></iframe>").addClass(mobile_layout + "_contenu").addClass("common_contenu").appendTo($phone)[0].useragent = defs.phones[mobile_layout].useragent;
		setMobileScale(scale);
	}
	fixWidth();
}

function setMobileScale(scale) {
	var $phone = $("#window_exe_content > div");
	var $iframe = $phone.find("> iframe");
	
	vars.last_scale = scale = (scale === "auto") ? (defs.dpi / defs.phones[vars.last_layout].dpi) : (scale * 1.0);
	$phone.css("-webkit-transform", "scale(" + scale + ")").css("-webkit-transform-origin", "top left");
}

function setLink($a, params) {
	if ($a.hasClass("btn_gen_gadget")) {
		$a.attr("href", "widgets/" + vars.projectName + "?__widget_type=gadget&__widget_name=" + vars.projectName + '&' + toUrl(params));
	} else {
		$a.attr("href", "projects/" + vars.projectName + "/" + getRequester() + "?" + toUrl(params));
	}
}

//function setLinkForMobileDevice(a) {
function setLinkForMobileDevice($device, $parent) {
	var $a = $parent;
	//$a.attr("href", "projects/" + vars.projectName + "/DisplayObjects/mobile/index.html");
	$a.attr("href", "projects/" + vars.projectName + "/" + $device.attr('ressource_path'));
}

function setLinkForRequestable(a) {
	var $a = $(this);
	var params = addRequestableData({}, $a.parents(".requestable:first").find(".requestable_name:first"));
	$a.parents(".requestable").find(".variable .variable_value:enabled").each(function () {
		var variable_name = $(this).parents(".variable").find(".variable_name").text();
		if ($.isArray(params[variable_name])) {
			params[variable_name].push($(this).val());
		} else {
			params[variable_name] = [$(this).val()];
		}
	});
	setLink($a, params);
}

function setLinkForTestCase(a) {
	var $a = $(this);
	var params = addRequestableData({}, $a.parents(".requestable:first").find(".requestable_name:first"));
	params.__testcase = $a.parents("li:first").prev(".testcase_name").text();
	setLink($a, params);
}

function setName($elt, $xml) {
	$elt.text($xml.attr("name")).attr("title", $xml.attr("comment"));	
}

function setMobileDeviceLayout($elt, $xml) {
	var layout = "none";
	var classname = $xml.attr("classname");
	if (classname === "Android") layout = "HtcDesire";
	else if (classname === "BlackBerry6") layout = "BlackBerryTorch";
	else if (classname === "IPad") layout = "iPad1";
	else if (classname === "IPhone3") layout = "iPhone3";
	else if (classname === "IPhone4") layout = "iPhone4";
	$elt.attr("value", layout);
}

function toUrl(params) {
	var parts = [];
	$.each(params, function (key, value) {
		if ($.isArray(value)) {
			$.each(value, function(index, val) { 
				parts.push(encodeURIComponent(key) + "=" + encodeURIComponent(val));
			});
		} else {
			parts.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
		}
	});
	return parts.join("&");
}

function getLoadingImageUrl() {
	return vars.base_url + "images/loading.gif";
}

function launchPhoneGapBuild() {
	var endpoint = $("#build_endpoint").val();
	
	endpoint += (endpoint.match(/\/$/) ? "" : "/") + "projects/" + vars.projectName;
		
	var applicationID = $("#build_application_id").val();

	$("body").css("cursor", "progress");
	$(".device_status").attr("title","build requested").empty();
	$("#main .btn_build, .btn_get_source").button("disable");
	$.ajax({
		type : "POST",
		url : "admin/services/mobiles.LaunchBuild",
		data : { "application" : vars.projectName, "endpoint" : endpoint, "applicationID" : applicationID },
		dataType : "xml",
		success : function(xml) {
			$("#main .btn_build, .btn_get_source").button("enable");
			$("body").css("cursor", "auto");
			$("#main .install.qrcode_content").each(function() {
				$(this).find(".build_status").attr("value","starting");
				$(this).find("> a").empty().append($("<img/>").attr("src", getLoadingImageUrl()));
			});
			setTimeout(function() {$("#main .install.qrcode_content").each(getPhoneGapBuildStatus);}, 1000);
		},
		error : function(xml) {
			$("#main .btn_build, .btn_get_source").button("enable");
			$("body").css("cursor", "auto");
			var $message = $(xml.responseXML).find("message").text();
			alert("Build Error:\n" + $message);
		}
	});
}

function getPhoneGapBuildUrl() {
	var $td = $(this);
	var device_layout = $td.parents("li:first").find(".device_layout").attr("value");
	var device_platform = defs.phones[device_layout].os;
	var isBlackberry = (device_platform ===  defs.phones.BlackBerryTorch.os);
	
	var url = vars.base_url + "admin/services/mobiles.GetPackage?application=" + vars.projectName + "&platform=" + device_platform;
	$td.find(".build_url").attr("value",url);
	//showQRCode.call($td, isBlackberry);
	showQRCode($(this), $td, isBlackberry);
}

function getPhoneGapBuildStatus() {
	var $td = $(this);
	var device_layout = $td.parents("li:first").find(".device_layout").attr("value");
	var device_platform = defs.phones[device_layout].os;
	var device_install = defs.phones[device_layout].install;
	
	$td.parents("table:first").find(".qrcode_platform:first").empty().text(device_platform.toUpperCase());
	$td.parents("table:first").find(".qrcode_install:first").empty().text(device_install);
	
	$.ajax({
		type : "POST",
		url : "admin/services/mobiles.GetBuildStatus",
		data : { "application" : vars.projectName, "platform" : device_platform },
		dataType : "xml",
		success : function(xml) {
			var $build = $(xml).find("build:first");
			if ($build.length > 0) {
				var status = $build.attr("status");
				if (status === "none") {
					$td.parents("li:first").find(".device_status").attr("title","not built").empty().append("<img src=\"images/new/build_none.png\" />");
					$td.find(".build_status").attr("value","none");
					$td.find("> a").empty().append("NOT BUILT");
				}
				else if (status === "error") {
					$td.parents("li:first").find(".device_status").attr("title","build in error").empty().append("<img src=\"images/new/build_none.png\" />");
					$td.find(".build_status").attr("value","error");
					$td.find("> a").empty().append("BUILD ERROR:<br/>" + $build.attr("error"));
				}
				else if (status === "pending") {
					$td.parents("li:first").find(".device_status").attr("title","build pending").empty().append("<img src=\"images/new/build_pending.png\" />");
					$td.find(".build_status").attr("value","pending");
					$td.find("> a").empty().append($("<img/>").attr("src", getLoadingImageUrl()));
					setTimeout(function() { getPhoneGapBuildStatus.call($td); }, 10000);
				}
				else if (status === "complete") {
					$td.parents("li:first").find(".device_status").attr("title","build complete").empty().append("<img src=\"images/new/build_complete.png\" />");
					$td.find(".build_status").attr("value","complete");
					getPhoneGapBuildUrl.call($td);
				}
			}
		},
		error : function(xml) {
			var $message = $(xml.responseXML).find("message").text();
			$td.parents("li:first").find(".device_status").attr("title","build in error").empty().append("<img src=\"images/new/build_none.png\" />");
			$td.find(".build_status").attr("value","error");
			$td.find("> a").empty().append("BUILD ERROR:<br/>"+$message);

		}
	});
}

function fixWidth() {
	$("#window_exe_content").css("max-width", $("#column_right").width() - 32 + "px");
}

function copyVariables($testcase) {
	var $requestable = $testcase.parents(".requestable:first");
	$requestable.find(".variable").each(function () {
		var $variable = $(this);

		$variable.find(".new_multi_valued").remove();
		$variable.find(".variable_value").val("");
		
		var variable_name = $variable.find(".variable_name").text();
		var $testCase = $testcase.find(".testcase_variable").filter(function () {
			return $(this).find(".testcase_variable_name").text() == variable_name;
		});
		if ($testCase.length) {
			var $value = $testCase.find(".testcase_variable_value");
			var value = $value.text();
			if ($value.attr("ismultivalued") === "true") {
				var values_array = parseJSONarray(value);
				for (var j in values_array) {
					$variable.find(".link_value_add").click();
					$variable.find(".variable_value").last().val(values_array[j]);
				}
			} else {
				$variable.find(".variable_value").val(value);
			}
			variableEnableCheck($variable.find(".variable_enable").attr("checked", "checked"));
		} else {
			variableEnableCheck($variable.find(".variable_enable").removeAttr("checked"));
		}
	});
	$requestable.find("a.requestable_link").each(setLinkForRequestable);
}

function variableEnableCheck($check) {
	if ($check.attr("checked")) {
		$check.parent().prev().find(".variable_value").removeAttr("disabled");
	} else {
		$check.parent().prev().find(".variable_value").attr("disabled", "disabled");
	}
}

function isC8oCall() {
	var $c8o_call = $("#check_mode_c8o_call");
	return !$c8o_call.attr("disabled") && $c8o_call.attr("checked");
}

function isFullscreen() {
	return $('#check_mode_fullscreen').attr("checked");
}

function getRequester() {
	var selected_mode = $("input[type=radio][name=form_execution_mode]:checked").val();
	if (selected_mode == "xml") {
		return ".pxml";
	} else if (selected_mode == "json") {
		return ".json";
	}
	return "index.html";
}
function _getQuery() {
	var l = window.location,
		q = l.search.length > 0 ? l.search.substring(1) : "",
		h = l.hash.length > 0 ? l.hash.substring(1) : "";
	return (q.length > 0 && h.length > 0) ? (q + "&" + h) : (q.length > 0 ? q : h);
}

function isUndefined (obj) {
	return typeof(obj) === "undefined";
}

function parseQuery(params, query) {
	var data = {},
		vars = (query ? query : _getQuery() ).split("&"),
		i, id, key, value;
	for (i = 0; i < vars.length; i += 1) {
		if (vars[i].length > 0) {
			id = vars[i].indexOf("=");
			key = (id > 0)?vars[i].substring(0, id):vars[i];
			value = "";
			if (id > 0) {
				value = vars[i].substring(id + 1);
				if (value.length) {
					value = value.replace(new RegExp("\\+", "g"), " ");
					try {
						value = decodeURIComponent(value);
					} catch (err1) {
						try {
							value = unescape(value);
						} catch (err2) {}
					}
				}
			}
			if (this.isUndefined(data[key])) {
				data[key] = value;
			} else if ($.isArray(data[key])) {
				data[key].push(value);
			} else {
				data[key] = [data[key]].concat([value]);
			}
		}
	}
	return data;
}

$(document).ready(function() {
	if (window.location.hash.length === 0) {
		return;
	}
	var linkString = window.location.href.match(".*#([a-zA-Z0-9_]*)[\?]?(.*)?");
	var parameters = ( linkString[2] == undefined ? "" : parseQuery({}, linkString[2]) );
	
	vars = {
		projectName : linkString[1],
		pusher_original_pos : $("#pusher").offset().top,
		base_url : window.location.href
	};
	
	vars.base_url = vars.base_url.substring(0, vars.base_url.indexOf("project.html"));
	
	$(".project_name").text(vars.projectName);
	
	window.document.title = vars.projectName + " project";

	// Compute endpoint
	var endpoint = window.location.href;
	endpoint = endpoint.substring(0, endpoint.indexOf("project.html"));
	$("#build_endpoint").val(endpoint);

	// Compute application ID
	var applicationID = vars.projectName.replace(new RegExp("[^a-zA-Z0-9]", "g"), "");
	applicationID = applicationID.replace(new RegExp("_", "g"), "");
	var applicationID = "com.convertigo.mobile." + applicationID;
	$("#build_application_id").val(applicationID);
	
	$("#cliplet_div_bar img").click(function () {
		$("#cliplet_div").slideUp(250, function () {
			$("#window_exe_content").empty();
		});
	});
	
	$(window).resize(function () {
		fixWidth();
	});
	
	initCommon(function () {
		call("projects.GetRequestables", {projectName : vars.projectName}, function (xml) {
			$("#acc .connector").remove();
			$("#acc .device").remove();
			$("#acc .sequences .requestables").empty();
			$(".acc>li>h6").unbind('click');
			
			var $project = $(xml).find("project:first");
			
			if ($project.attr("mobileProjectName")) {
				$("#build_application_name").val($project.attr("mobileProjectName"));
			}
			
			$(".project_comment").text($project.attr("comment"));
			$project.find(">connector").each(function (i) {
				var $connector = $(this);
				var $connector_div = $("#templates .connector").clone();
				setName($connector_div.find(".connector_name"), $connector);
				$(".sequences:first").before($connector_div);
	
				var $requestables_div = $connector_div.find(".requestables:first");
				$requestables_div.attr("id", "nested_" + i);
				
				$connector.find(">transaction").each(function () {
					addRequestable($(this), $requestables_div);
				});
			});
			var $sequences = $project.find(">sequence");
			if ($sequences.length > 0) {
				$(".sequences:first").removeClass("hidden");
				$sequences.each(function () {
					addRequestable($(this), $(".sequences:first .requestables:first"));
				});
			}		
			
			var $devices = $project.find(">mobiledevice");
			if ($devices.length > 0) {
				$(".mobiles:first").removeClass("hidden");
				$(".btn_get_source").attr("href", vars.base_url + "admin/services/mobiles.GetSourcePackage").click(function() {
					$(this).attr("href", vars.base_url + "admin/services/mobiles.GetSourcePackage?application=" + vars.projectName + "&applicationID=" + $("#build_application_id").val() + "&endpoint=" + $("#build_endpoint").val());
				});
				$devices.each(function () {
					addMobileDevice($(this), $(".mobiles:first .requestables:first"));
					setLinkForMobileDevice($(this), $("#main a.device_link"));		
					showQRCode($("#main .webapp.qrcode_content"), $(this), false);
				});
			}	
			
			
			//$("#main a.device_link").each(setLinkForMobileDevice);
			
			$("#main a.requestable_link").each(setLinkForRequestable);
			
			$("#main a.requestable_testcase_link").each(setLinkForTestCase);
			
			$("#main .variable_value").change(function () {
				$(this).parents(".requestable").find("a.requestable_link").each(setLinkForRequestable);
			});
			
			$("#main input[type=radio][name=form_execution_mode]").change(function () {
				$("#main a.requestable_link").each(setLinkForRequestable);
				$("#main a.requestable_testcase_link").each(setLinkForTestCase);
			});
			
			if ($(".accordion_options").length == 1) {
				$("#column_left").append($(".accordion_options:first").clone());
			}
			
			$("#main .btn_exe_link").button({ icons : { primary : "ui-icon-play" }});
			$("#main .btn_gen_gadget").button({ icons : { primary : "ui-icon-gear" }});
			$("#main .btn_edit_testcase").button({ icons : { primary : "ui-icon-copy" }});
			$("#main .btn_build").button({ icons : { primary : "ui-icon-wrench" }});
			$("#main .btn_get_source").button({ icons : { primary : "ui-icon-arrowthickstop-1-s" }});
			
			$("#main .radio_mode").buttonset();
			$("#check_mode_fullscreen").button();
			$("#check_mode_c8o_call").button();

			$("#main .radio_mode, #check_mode_fullscreen").change(function() {
				var selected = $('input[type=radio][name=form_execution_mode]:checked').val();
				if (selected != "html" || isFullscreen()) {
					$('input[name=form_c8o_call_mode]').button("option", "disabled", true);
				} else {
					$('input[name=form_c8o_call_mode]').button("option", "disabled", false);
				}
			});
				
			$("#main .btn_build").click(function() {
				launchPhoneGapBuild();
				return false;
			});
			
			$("#main .btn_edit_testcase").click(function() {
				copyVariables($(this).parent());
				return false;
			});
			
			$("#main .btn_exe_link").click(function () {
				var $requestable = $(this).parents(".requestable:first");
				var layout = $(this).parent().find(".device_layout").attr("value");
				var href = $(this).parent().find("a").attr("href");
				
				var genUrl = window.location.href.replace(new RegExp("^(.*/).*$"), "$1") + href;
				
				$("#main .gen_url").text(genUrl).attr("href", genUrl);
				$("#main .window_exe_generated_url").css("display", "block");
				
				// check for file upload
				if ($requestable.find(".value_file").length > 0) {
					var url = genUrl.replace(new RegExp("^(.*?)\\?.*$"), "$1");
					var $form = $requestable.find("form").attr({
						"method" : "POST",
						"action" : url
					});
					
					if (isFullscreen()) {
						if (getRequester() === "index.html") {
							var name = "testplatform_" + new Date().getTime();
							var win = window.open(url + "#__first_call=false");
							var cpt = 20;
							var callback = function () {
								if (win.C8O) {
									win.C8O.call($form[0]);
								} else if (--cpt > 0) {
									window.setTimeout(callback, 100);
								} else {
									alert("Failed to call the requestable.");
								}
							};
							window.setTimeout(callback, 100);
						} else {
							$form.attr("target", "_blank").submit();
						}
					} else {
						var $iframe = $("#cliplet_div_iframe");
						if (isC8oCall() && $iframe.length && typeof($iframe[0].contentWindow.C8O) !== "undefined") {
							$iframe[0].contentWindow.C8O.call($form[0]);
						} else {
							if (getRequester() === "index.html") {
								$iframe = $("<iframe/>").attr({
									id : "cliplet_div_iframe",
									frameborder : "0",
									src : url + "#__first_call=false"
								}).one("load", function () {
									$iframe[0].contentWindow.C8O.call($form[0]);
								}).appendTo($("#window_exe_content").empty());
							} else {
								$iframe = $("<iframe/>").attr({
									id : "cliplet_div_iframe",
									frameborder : "0",
									src : ""
								}).one("load", function () {
									if (getRequester() === ".pxml") {
										$form.attr("action", $form.attr("action") + "?__content_type=text/plain");
									}
									$form.attr("target", this.contentWindow.name = "tesplatformIframe").submit();
								}).appendTo($("#window_exe_content").empty());
							}
							$iframe.slideDown(500);
						}
					}
					return false;
				}

				if (isFullscreen()) {
					window.open(href);
				} else {
					if (getRequester() === ".pxml") {
						href += "&__content_type=text/plain"
					}
					launchCliplet(href, layout, "auto");
				}
				return false;
			});
			
			$("#main .variable_enable").click(function () {
				var $check = $(this);
				variableEnableCheck($check);
				$check.parents(".requestable").find("a.requestable_link").each(setLinkForRequestable);
			});
			
			$("#main .link_value_remove").live("click", function () {
				var $requestable = $(this).parents(".requestable");
				$(this).parents(".new_multi_valued").remove();
				$requestable.find("a.requestable_link").each(setLinkForRequestable);
				return false;
			});
			$("#main .link_value_add").live("click", function () {
				var $variable_type = $(this).parents(".variable_type");
				var $variable_multi_new = $("#templates .new_multi_valued").filter($variable_type.data("isFileUpload") ? ".value_file" : $variable_type.data("isMasked") ? ".value_password" : ".value_text").clone();
				$variable_multi_new.find(".variable_value").attr("name", $variable_type.data("name")).val("").change(function () {
					$(this).parents(".requestable").find("a.requestable_link").each(setLinkForRequestable);
				}).change();
				$variable_type.append($variable_multi_new);
				return false;
			});
			
			var el = $('#pusher');
			var elpos_original = el.offset().top;
			$(window).scroll(alignPusher);
			
			$("#window_exe_pin").click(function () {
				$(this).toggleClass("window_exe_pinned");
				alignPusher();
			});
			
			$("#window_exe_close").click(function () {
				$("#window_exe_content").empty();
			});
			
			if ($.browser.webkit && ($devices.length > 0)) {
				$("#window_exe_increase").removeClass("hidden");
				$("#window_exe_decrease").removeClass("hidden");
			}
			
			$("#window_exe_increase").click(function () {
				if (vars.last_url) {
					var scale = vars.last_scale * 1.0 + 0.05;
					setMobileScale(scale <= 2 ? scale : 2);
				}
			});
	
			$("#window_exe_decrease").click(function () {
				if (vars.last_url) {
					var scale = vars.last_scale * 1.0 - 0.05;
					setMobileScale(scale >= 0.25 ? scale : 0.25);
				}
			});
			
			$(".acc>li>h6").click(function () {
				 $(this).toggleClass("acc-selected");
				 $(this).next().find(".acc>li>h6").add(this).each(function () {
					 if ($(this).hasClass("acc-selected")) {
						 $(this).next().slideDown("fast");
					 } else {
						 $(this).next().slideUp("fast");
					 }
				 });
			}).next().hide();
			
			if ($devices.length) {
				$("h6:first").click();
			} else {
				$(".connector_name").each(function(){
					if ($(this).text() == $project.attr("defaultConnector")) {
						$(this).click();
						return false;
					}
				});
				
				$(".requestable_name").each(function(){
					if ($(this).text() == $project.attr("defaultTransaction")) {
						$(this).click();
						return false;
					}
				});
			}
			
			$(".accordion_deploy").click(function () {
				$(".acc>li>h6").not(".acc-selected").click();
			});
			
			$(".accordion_colapse").click(function () {
				$(".acc>li>h6.acc-selected").click();
			});
			
			$("a.btn_gen_gadget").click(function () {
				var url = window.location.href.replace(new RegExp("(.*/).*$"), "$1") + $(this).attr("href");
				$("#main .window_exe_generated_url").css("display", "none");
				var $message = $("#templates .gadget_message").clone();
				$message.find(".gadget_url").text(url);
				$("#window_exe_content").empty().append($message);
				$.ajax({
					dataType : "text",
					url : url,
					success: function (data) {
						$message.find(".gadget_xml").text(data);
					}
				});
	
				return false;
			});
	
			$("#main .install.qrcode_content").each(getPhoneGapBuildStatus);
			//$("#main .webapp.qrcode_content").each(function () {
			//	showQRCode.call($(this), false)
			//});
			
			//Autorun the device passed in parameter
			if ( linkString[2] != undefined ){
					$( "#"+(parameters.device.match( "^i(.*)") == null ? 
							parameters.device : "I"+parameters.device.match( "^i(.*)")[1])+
							" h6" ).click();
					
					$( "#"+(parameters.device.match( "^i(.*)") == null ? 
							parameters.device : "I"+parameters.device.match( "^i(.*)")[1])+
							" a" ).click();
			}
		});
	});		
});