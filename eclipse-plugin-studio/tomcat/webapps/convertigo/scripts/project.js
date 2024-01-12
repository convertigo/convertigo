/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

var vars;

function addMobilePlatform($platform, $parent) {
	var $platform_div = $("#templates .platform").clone();
		
	setName($platform_div.find(".platform_name"), $platform);
	$platform_div.find(".qrcode_platform").text($platform.attr("displayName"));
	$platform_div.find(".qrcode_install").text($platform.attr("packageType"));
	$platform_div.find(".platform_revision").text($platform.attr("revision"));
	
	call("mobiles.GetLocalRevision", {project: vars.projectName, platform: $platform.attr("name")}, function (xml) {
		$platform_div.find(".platform_revision").text($(xml).find("revision").text());
	});
	
	var params = $.param({
		project: vars.projectName,
		platform: $platform.attr("name")
	});
	
	var token = localStorage.getItem("x-xsrf-token");
	if (token) {
		params += "&__xsrfToken=" + token;
	}
	
	$platform_div.find(".btn_get_source").attr("href", vars.base_url + "admin/services/mobiles.GetSourcePackage?" + params);
	$platform_div.find(".btn_build").attr("href", vars.base_url + "admin/services/mobiles.LaunchBuild?" + params);
	
	$parent.append($platform_div);
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

function waitDivBuildHide() {
	$("#wait_div_build").hide();
}

function waitDivBuildShow() {
	$("#wait_div_build img").attr("src", getLoadingImageUrl());
	$("#wait_div_build p").text("Uploading project to cloud build");
	$("#wait_div_build").show();
	$("#wait_div_build").css("display", "block");
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
		params.__transaction = $requestable_name.attr("displayname");
	} else {
		params.__sequence = $requestable_name.attr("displayname");
	}
	return params;
}

// function alignPusher() {
// 	if (!$("#window_exe_pin").hasClass("window_exe_pinned")) {
// 	    var windowpos = $(window).scrollTop();
// 	    var finaldestination = windowpos-vars.pusher_original_pos + 30;
// 	    $("#pusher").stop().animate({"height" : finaldestination}, 500);
// 	}
// }

function showQRCode($td) {
	var href = "projects/" + vars.projectName + "/DisplayObjects/mobile/index.html";
	$td.parents(".mobile_platform:first").find("a.btn_exe_link").attr("href",  href);
	var build_url = $td.find(".build_url").attr("value");
	var target_url = (build_url === "") ? vars.base_url + href : build_url;
	if (build_url != "") {
		href = build_url;
		var token = localStorage.getItem("x-xsrf-token");
		if (token) {
			href += "&__xsrfToken=" + token;
		}
	}
	
	var img_url = vars.base_url + "qrcode?" + $.param({
		o : "image/png",
		e : "L",
		s : 4,
		d : target_url
	});
	
	$td.find(">a").empty().attr("href", href).append($("<img/>").attr("src", img_url).attr("title", "qrcode").attr("alt", "error, maybe too long url"));
}

function launchCliplet(url) {
	vars.last_url = url;
	var $iframe = $(".cliplet_div_iframe");
	if (isC8oCall() && $iframe.length && typeof($iframe[0].contentWindow.C8O) !== "undefined") {
		$iframe[0].contentWindow.C8O.call(url.substring(url.indexOf("?") + 1));
	} else {
		//If the project is for mobile device
		var ismobile = $("#window_exe_device").css("display") == "block";
		let split = url.split("/")[2];
		if(split.indexOf(".json?") == 0){
			window.localStorage.setItem("__store_c8o_pref_exec_engine", "json");
			document.getElementById("jsonchooser").removeEventListener("click", window.l);
			document.getElementById("copyContent").removeEventListener("click", window.ll);
	
			fetch(vars.last_url).then(function(response) {
				return response.json();
			}).then(function(json) {	
				setJSONContent(json);
				window.l = function(e) {
					setJSONContent(json);
				};
				window.ll = function(e) {
					copyToClipboard(JSON.stringify(json, 3));
				};
				document.getElementById("jsonchooser").addEventListener("click", window.l);
				document.getElementById("jsonchooser").style.display = "block";
				document.getElementById("headerJson").style.display = "flex";
				document.getElementById("spanType").innerText = "JSON";
				document.getElementById("copyContent").innerText = "Copy JSON";
				document.getElementById("copyContent").addEventListener("click", window.ll);
				document.getElementById("copyContent").style.display = "flex";
			});
		}
		else if(split.indexOf(".pxml?") == 0){
			window.localStorage.setItem("__store_c8o_pref_exec_engine", "xml");
			document.getElementById("jsonchooser").style.display = "none";
			document.getElementById("jsonchooser").removeEventListener("click", window.l);
			document.getElementById("copyContent").removeEventListener("click", window.ll);
			fetch(vars.last_url).then(function(response) {
				return response.text();
			}).then(function(text) {
				let txt = setXMLContent(text);
				window.ll = function(e) {
					copyToClipboard(text);
				};
				document.getElementById("headerJson").style.display = "flex";
				document.getElementById("spanType").innerText = "XML";
				document.getElementById("copyContent").innerText = "Copy XML";
				document.getElementById("copyContent").addEventListener("click", window.ll);
				document.getElementById("copyContent").style.display = "flex";
			});
		}
		else{
			if(split.indexOf(".bin?") == 0){
				window.localStorage.setItem("__store_c8o_pref_exec_engine", "bin");
			}
			else if(split.indexOf(".index.html") == 0){
				window.localStorage.setItem("__store_c8o_pref_exec_engine", "html");
			}
			document.getElementById("jsonchooser").removeEventListener("click", window.l);
			document.getElementById("copyContent").removeEventListener("click", window.ll);
			document.getElementById("headerJson").style.display = "none";
			if (ismobile) {
				$("<iframe/>").attr({
					class: "cliplet_div_iframe",
					frameborder: "0",
					src: url
				}).appendTo($("#window_exe_content_mobile").empty());
			} else {
				$("<iframe/>").attr({
					class: "cliplet_div_iframe",
					frameborder: "0",
					src: url
				}).appendTo($("#window_exe_content").empty());	
			}
		}
		$iframe.slideDown(500);
	}
	fixWidth();
}

function setJSONContent(json, ismobile){
	var selected_mode = $("input[type=radio][name=form_json_display_mode]:checked").val();
	$("#window_exe_content").empty();
	$("#window_exe_content").css("background-color", "#282c34");
	$("#window_exe_content").css("text-align", "left");
	$("#window_exe_content").css("max-height", "calc(100vh - 300px)");
	$("#window_exe_content").css("overflow-y", "auto");
	$("#window_exe_content").css("font-family", "consolas");
	
	if(selected_mode == "radio_json_formatted"){
		const formatter = new JSONFormatter(json, 2, {
			hoverPreviewEnabled: true,
			hoverPreviewArrayCount: 100,
			hoverPreviewFieldCount: 5,
			theme: 'dark',
			animateOpen: true,
			animateClose: true,
			useToJSON: true,
			maxArrayItems: 100,
			exposePath: false
		  });
			if (ismobile) {
				$("#window_exe_content_mobile").empty().append(formatter.render());
			}
			else{
				
				$("#window_exe_content").append(formatter.render());
			}
	}
	else{
		let pre = document.createElement('pre');
		pre.innerHTML = prettyPrintJson.toHtml(json)//, null, 3);
		$("#window_exe_content").append(pre)
	}
}
function setXMLContent(xml, ismobile){
	$("#window_exe_content").empty();
	$("#window_exe_content").css("background-color", "#282c34");
	$("#window_exe_content").css("text-align", "left");
	$("#window_exe_content").css("max-height", "calc(100vh - 300px)");
	$("#window_exe_content").css("overflow-y", "auto");
	$("#window_exe_content").css("font-family", "consolas");
	
	let res = hljs.highlight(
		xml,
		{ language: 'xml' }
	  ).value
	let pre = document.createElement('pre');
	pre.innerHTML = res;//, null, 3);
	$("#window_exe_content").append(pre);
	return res;
	
}
function copyToClipboard(text) {
    var dummy = document.createElement("textarea");
    document.body.appendChild(dummy);dummy.value = text;
    dummy.select();
    document.execCommand("copy");
    document.body.removeChild(dummy);
	document.getElementById("copyContent").style.display = "none";
	document.getElementById("copyContentDone").style.display = "flex";
	setTimeout(function() {
		document.getElementById("copyContentDone").style.display = "none";
		document.getElementById("copyContent").style.display = "flex";
	}, 2000);
}

function setScale(scale) {
	var $phone = $("#window_exe_device > div");
	var $iframe = $phone.find("> iframe");
	
	vars.last_scale = scale = (scale === "auto") ? 0.5 : (scale * 1.0);
	$phone.css("-webkit-transform", "scale(" + scale + ")").css("-webkit-transform-origin", "top left");
}

function setLink($a, params) {
	var token = localStorage.getItem("x-xsrf-token");
	if (token) {
		params["__xsrfToken"] = token;
	}
	$a.attr("href", "projects/" + vars.projectName + "/" + getRequester() + "?" + toUrl(params));
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
	var accessibilityIcon = $xml.attr("accessibility") === "Public" ? "🚪" : 
		($xml.attr("accessibility") === "Private" ? "🔒" : 
			($xml.attr("accessibility") === "Hidden" ? "👓" : "" ));
	var autostartIcon = $xml.attr("autostart") === "true" ? "💡" : "";
	$elt.text($xml.attr("name")).attr("title", $xml.attr("comment")).attr("displayname", $xml.attr("name"));
	if (autostartIcon != "") {
		$elt.prepend("<span class='accessibility-icon'>" + autostartIcon + "</span>");
	}
	if (accessibilityIcon != "") {
		$elt.prepend("<span class='accessibility-icon'>" + accessibilityIcon + "</span>");
	}
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

function getCurrentEndpoint() {
	var endpoint = $("#build_endpoint").text();
	return endpoint + (endpoint.match(/\/$/) ? "" : "/") + "projects/" + vars.projectName + "/";
}

function launchPhoneGapBuild($li) {
	$("body").css("cursor", "progress");
	$li.find(".platform_status").attr("title","Build requested").empty();
	$("#main .btn_build, #main .btn_get_source").button("disable");
	
	$(".qrcode_version").removeClass("not_updated");
	$(".warning_icon").css("display", "none");
	
	waitDivBuildShow();
	
	$.ajax({
		type : "GET",
		url : $li.find(".btn_build").attr("href"),
		dataType : "xml",
		success : function(xml) {
			waitDivBuildHide();
			$("#main .btn_build, #main .btn_get_source").button("enable");			
			$("body").css("cursor", "auto");
			$li.find(".build_status").attr("value","starting");
			$li.find("> a").empty().append($("<img/>").attr("src", getLoadingImageUrl()));
			setTimeout(function() {$li.find(".install.qrcode_content").each(getPhoneGapBuildStatus);}, 1000);
		},
		error : function(xml) {			
			waitDivBuildHide();
			$("#main .btn_build, #main .btn_get_source").button("enable");
			$("body").css("cursor", "auto");
			var message = $(xml.responseXML).find("message").text();
			alert("Build Error:\n" + message);
		}
	});
	$("#main .wait_status_build").css("display","none");
}

function getPhoneGapBuildUrl($td) {
	var url, platform_name = $td.parents(".platform:first").find(".platform_name").text();
	
	if ($td.is(".qrcode_ota")) {
		url = $td.find(".bp_url").val() + "/ota-ios.html#"
			+ "app=" + encodeURIComponent($("#build_application_name").text())
			+ "&ep=" + encodeURIComponent($("#build_endpoint").text())
			+ "&bn=" + encodeURIComponent($td.find(".bn").val())
			+ "&pl=" + encodeURIComponent(platform_name);
	} else {
		url = vars.base_url + "admin/services/mobiles.GetPackage?project=" + vars.projectName + "&platform=" + platform_name;
	}
	
	$td.find(".build_url").attr("value", url);
	showQRCode($td);
}

function getPhoneGapBuildStatus() {
	var $td = $(this);
	var $platform = $td.parents(".platform:first");
	var platform_name = $platform.find(".platform_name").text();
	
	$.ajax({
		type : "POST",
		url : "admin/services/mobiles.GetBuildStatus",
		data : { "project" : vars.projectName, "platform" : platform_name },
		dataType : "xml",
		success : function(xml) {
			var $build = $(xml).find("build:first");
			if ($build.length > 0) {
				var status = $build.attr("status");
				$td.find(".qrcode_message").hide();
				if (status === "none") {
					$platform.find(".platform_status").attr("title","Not built").empty().append("<img src=\"images/new/build_none.png\" />");
					$td.find(".build_status").attr("value","none");
					$td.find("> a").empty().append("NOT BUILT");
				}
				else if (status === "error") {
					$platform.find(".platform_status").attr("title","Build in error").empty().append("<img src=\"images/new/build_none.png\" />");
					$td.find(".build_status").attr("value","error");
					$td.find("> a").empty().append("BUILD ERROR:<br/>" + $build.attr("error"));		
				}
				else if (status === "pending") {
					$platform.find(".platform_status").attr("title","Build pending").empty().append("<img src=\"images/new/build_pending.png\" />");
					$td.find(".build_status").attr("value","pending");
					$td.find("> a").empty().append($("<img/>").attr("src", getLoadingImageUrl()));
					setTimeout(function() { getPhoneGapBuildStatus.call($td); }, 5000);
				}
				else if (status === "complete") {
					$platform.find(".platform_status").attr("title","Build complete").empty().append("<img src=\"images/new/build_complete.png\" />");
					$td.find(".build_status").attr("value","complete");
					getPhoneGapBuildUrl($td);
					var bn = $build.attr("bn");
					if (bn != null) {
						$td.parent().find(".bn").val(bn);
						$td.parent().find(".bp_url").val($build.attr("bp_url"));
						getPhoneGapBuildUrl($td.siblings(".qrcode_ota").show());
					}
					$td.find(".qrcode_message").show();
					 
					if ($build.attr("endpoint") !== $("#build_endpoint").text() && $build.attr("endpoint") !== "n/a" ) {
						$platform.find(".built_endpoint").text( $build.attr("endpoint") );
						$platform.find(".row_built_endpoint").css("display", "block");
					}
				}
				$platform.find(".qrcode_version").text($build.attr("version"));
				$platform.find(".built_revision").text($build.attr("revision"));
				
				if ($build.attr("version") !== $("#build_application_version").text() && $build.attr("version") !== "n/a") {
					$platform.find(".qrcode_version").addClass("not_updated");
					$platform.find(".warning_icon").css("display", "block").attr("title", "The local version is different from the remote built version");
				}
				$platform.find(".qrcode_phonegap_version").text($build.attr("phonegap_version"));
			}
		},
		error : function(xml) {
			var $message = $(xml.responseXML).find("message").text();
			$td.parents("li:first").find(".platform_status").attr("title"," Build in error").empty().append("<img src=\"images/new/build_none.png\" />");
			$td.find(".build_status").attr("value", "error");
			$td.find("> a").empty().append("BUILD ERROR:<br/>"+$message);

		}
	});
}

function fixWidth() {
	//$("#window_exe_content").css("max-width", $("#column_right").width() - 32 + "px");
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
			variableEnableCheck($variable.find(".variable_enable").prop("checked", true));
		} else {
			variableEnableCheck($variable.find(".variable_enable").prop("checked", false));
		}
	});
	$requestable.find("a.requestable_link").each(setLinkForRequestable);
}

function variableEnableCheck($check) {
	if ($check.prop("checked")) {
		$check.parent().prev().find(".variable_value").prop("disabled", false);
	} else {
		$check.parent().prev().find(".variable_value").prop("disabled", true);
	}
}

function isC8oCall() {
	var $c8o_call = $("#check_mode_c8o_call");
	return !$c8o_call.prop("disabled") && !$c8o_call.prop("checked");
}

function isFullscreen() {
	return $('#check_mode_fullscreen').prop("checked");
}

function getRequester() {
	var selected_mode = $("input[type=radio][name=form_execution_mode]:checked").val();
	if (selected_mode == "xml") {
		return ".pxml";
	} else if (selected_mode == "json") {
		return ".json";
	} else if (selected_mode == "bin") {
		return ".bin";
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
	//initialize exe content bloc
	$("#window_exe_content").show();
	$("#window_exe_device").hide();
	
	if (window.location.hash.length === 0) {
		return;
	}
	var linkString = window.location.href.match(".*#([a-zA-Z0-9_]*)[\?]?(.*)?");
	var parameters = ( linkString[2] == undefined ? "" : parseQuery({}, linkString[2]) );
	
	vars = {
		projectName : linkString[1],
		// pusher_original_pos : $("#pusher").offset().top,
		base_url : window.location.href
	};
	
	vars.base_url = vars.base_url.substring(0, vars.base_url.indexOf("project.html"));
		
	$(".project_name").text(vars.projectName);
	
	window.document.title = vars.projectName + " project";

	$("#cliplet_div_bar img").click(function () {
		$("#cliplet_div").slideUp(250, function () {
			$("#window_exe_content").empty();
		});
	});
	
	$("#swaggerLink").attr("href", "swagger/dist/index.html?url="+getEncodedYamlUri(vars.projectName));
	
	$(window).resize(function () {
		fixWidth();
	});
	
	initCommon(function () {	 
		try{
			let enginType = window.localStorage.getItem("__store_c8o_pref_exec_engine")
			if(enginType != null){
				switch(enginType){
					case "html":
						$("#radio_html").prop("checked", true);
					break;
					case "xml":
						$("#radio_xml").prop("checked", true);
					break;
					case "json":
						$("#radio_json").prop("checked", true);
					break;
					case "bin":
						$("#radio_bin").prop("checked", true);
					break;
				}
			}
		}
		catch(e){

		}
		call("projects.GetTestPlatform", {projectName : vars.projectName}, function (xml) {
			$("#acc .connector").remove();
			$("#acc .platform").remove();
			$("#acc .sequences .requestables").empty();
			$(".acc>li>h6").unbind('click');
			
			var $project = $(xml).find("project:first");
			
			$(".project_comment").html(marked.parse($project.attr("comment")));
			//$(".project_comment").html($(".project_comment").html().replace(new RegExp("\\n","g"), "<br/>"));
			
			if ($project.attr("version") != "") {
				$(".project_version").text("(" + $project.attr("version") + ")");
			}
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

			// Mobile application
			var $mobileApplication = $project.find(">mobileapplication");
			if ($mobileApplication.length > 0) {
				if ($mobileApplication.attr("mobileProjectName")) {
					$("#build_application_name").text($mobileApplication.attr("mobileProjectName"));
				}
				
				// Endpoint
				var endpoint = $mobileApplication.attr("endpoint");
				$("#build_endpoint").text(endpoint);
				
				// Application ID
				var applicationID = $mobileApplication.attr("applicationID");
				$("#build_application_id").text(applicationID);
				
				var applicationVersion = $mobileApplication.attr("applicationVersion");
				$("#build_application_version").text(applicationVersion);
				
				var unbuiltMessage = $mobileApplication.attr("unbuiltMessage");
				if (typeof unbuiltMessage != "undefined") {
					$(".mobile_unbuilt").text(unbuiltMessage);
				} else {
					$(".mobile_unbuilt").remove();
				}
				
				showQRCode($("#main .webapp.qrcode_content"), false);
				
				// Add mobile platforms
				var $platforms = $mobileApplication.find(">mobileplatform");
				if ($platforms.length > 0) {
					$(".mobiles:first").removeClass("hidden");
					$platforms.each(function () {
						addMobilePlatform($(this), $(".mobiles:first .requestables:first"));
					});
					
					$("#window_exe_increase").removeClass("hidden");
					$("#window_exe_decrease").removeClass("hidden");
					//$("h6:first").click();
				}
			}
						
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
				var currentEndpoint = getCurrentEndpoint();
				var localIpFound = "";
				if( currentEndpoint.indexOf("localhost") != -1) 
					localIpFound = "localhost";
				if( currentEndpoint.indexOf("127.0.0.1") != -1) 
					localIpFound = "127.0.0.1";
				
				if(localIpFound != ""){
					alert("You will not be able to test your application on a mobile device with a Convertigo endpoint pointing to \"" + localIpFound +"\".\n\n" +
							"You have to configure your mobile application's endpoint to the URL of your Convertigo, accessible from your device (Intranet or Internet).\n\n" +
							"To do so, in the mobile project, edit the \"Convertigo Server endpoint\" property of the \"Mobile application\".\n\n" +
							"It should contain a URL of the following form: \n" +
							"http://<your convertigo ip or dns>:<port>/convertigo \n" +
							"or \n" +
							"https://<your cloud name>.convertigo.net/cems \n" +
							"You can also directly access this Test Platform page using the correct URL.\n");
				} else {
					launchPhoneGapBuild($(this).parents(".platform:first"));
				}
				return false;
			});
			
			$("#main .btn_edit_testcase").click(function() {
				copyVariables($(this).parent());
				return false;
			});
			
			$("#main .btn_exe_link").click(function () {				
				var $requestable = $(this).parents(".requestable:first");
				var href = $(this).parent().find("a").attr("href");
				
				var genUrl = window.location.href.replace(new RegExp("^(.*/).*$"), "$1") + href;
				var requester = getRequester();
				
				$("#main .gen_url").text(genUrl).attr("href", genUrl);
				$("#main .window_exe_generated_url").css("display", "block");
				
				// check mobile or not
				var $window_exe_content;
				if( $(this).parents(".mobiles").length ){
					$window_exe_content = $("#window_exe_content_mobile");
					$("#window_exe_device").show();
					$("#window_exe_content").hide();
				}else{
					$window_exe_content = $("#window_exe_content").show();
					$("#window_exe_device").hide();
				}
				
				// check for file upload
				if ($requestable.find(".value_file").length > 0) {
					var url = genUrl.replace(new RegExp("^(.*?)\\?.*$"), "$1");
					var $form = $requestable.find("form").attr({
						"method" : "POST",
						"action" : url
					});
					
					if (isFullscreen()) {
						if (requester == "index.html") {
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
						var $iframe = $(".cliplet_div_iframe");
						if (isC8oCall() && $iframe.length && typeof($iframe[0].contentWindow.C8O) !== "undefined") {
							$iframe[0].contentWindow.C8O.call($form[0]);
						} else {
							if (requester == "index.html") {
								$iframe = $("<iframe/>").attr({
									class : "cliplet_div_iframe",
									frameborder : "0",
									src : url + "#__first_call=false"
								}).one("load", function () {
									$iframe[0].contentWindow.C8O.vars.endpoint_url = url.replace("/index.html","/");
									$iframe[0].contentWindow.C8O.call($form[0]);
								}).appendTo($window_exe_content.empty());
							} else {
								$iframe = $("<iframe/>").attr({
									class : "cliplet_div_iframe",
									frameborder : "0",
									src : ""
								}).one("load", function () {
									if (!$("#window_exe_device").is(":visible")) {
										$form.attr("action", $form.attr("action") + "?__content_type=text/plain");
									}
									$form.attr("enctype", "multipart/form-data");
									$form.attr("target", this.contentWindow.name = "tesplatformIframe").submit();
								}).appendTo($window_exe_content.empty());
							}
							$iframe.slideDown(500);
						}
					}
					return false;
				}

				if (isFullscreen()) {
					window.open(href);
				} else {
					if (requester != "index.html") {
						if (!$("#window_exe_device").is(":visible")) {
							href += "&__content_type=text/plain";
						}
					}
					launchCliplet(href);
				}
				return false;
			});
			
			$("#main .variable_enable").click(function () {
				var $check = $(this);
				variableEnableCheck($check);
				$check.parents(".requestable").find("a.requestable_link").each(setLinkForRequestable);
			});
			
			$("#main .link_value_remove").on("click", function () {
				var $requestable = $(this).parents(".requestable");
				$(this).parents(".new_multi_valued").remove();
				$requestable.find("a.requestable_link").each(setLinkForRequestable);
				return false;
			});
			$("#main .link_value_add").on("click", function () {
				var $variable_type = $(this).parents(".variable_type");
				var $variable_multi_new = $("#templates .new_multi_valued").filter($variable_type.data("isFileUpload") ? ".value_file" : $variable_type.data("isMasked") ? ".value_password" : ".value_text").clone();
				$variable_multi_new.find(".variable_value").attr("name", $variable_type.data("name")).val("").change(function () {
					$(this).parents(".requestable").find("a.requestable_link").each(setLinkForRequestable);
				}).change();
				$variable_type.append($variable_multi_new);
				return false;
			});
			
			// var el = $('#pusher');
			// var elpos_original = el.offset().top;
			// $(window).scroll(alignPusher);
			
			$("#window_exe_pin").click(function () {
				$(this).toggleClass("window_exe_pinned");
				// alignPusher();
			});
			
			$("#window_exe_close").click(function () {
				$("#window_exe_content, #window_exe_content_mobile").empty();
				$("#window_exe_device").hide();
			});
			
			$("#window_exe_increase").click(function () {
				if (vars.last_url) {
					var scale = vars.last_scale * 1.0 + 0.05;
					setScale(scale <= 2 ? scale : 2);
				}
			});
	
			$("#window_exe_decrease").click(function () {
				if (vars.last_url) {
					var scale = vars.last_scale * 1.0 - 0.05;
					setScale(scale >= 0.25 ? scale : 0.25);
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
			
			$(".accordion_deploy").click(function () {
				$(".acc>li>h6").not(".acc-selected").click();
			});
			
			$(".accordion_colapse").click(function () {
				$(".acc>li>h6.acc-selected").click();
			});
			
			$("#main .install.qrcode_content").each(getPhoneGapBuildStatus);
			
			if (typeof parameters.launch != "undefined") {
				if (parameters.launch == "webapp") {
					$(".mobiles .sub_connectors:first").click().parent().find(".requestable h6:first").click().parent().find(".btn_exe_link").click();
				}
			} else {
				if ($(".mobiles").hasClass("hidden")) {
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
				else {
					$(".mobiles .sub_connectors:first").click().parent().find(".requestable h6:first").click();
				}
			}
		});
	});		
});