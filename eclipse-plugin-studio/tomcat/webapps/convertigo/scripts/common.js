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

define = {
	service : "admin/services/"
}

function call(service, data, callback) {
	window.setTimeout(function () {
		$.ajax({
			cache : false,
			data : data,
			success : callback,
			url : define.service + service
		});
	}, 1);
}

function initCommon(callback) {
	call("engine.CheckAuthentication", {}, function (xml) {
		var $xml = $(xml);
		var $authenticated = $xml.find("authenticated");
		if (checkRoleTestPlatform($xml)) {
			callback();
		}
		if ($authenticated.text() != "true") {
			$("#loginForm").submit(function() {
				var $form = $(this);
				var username = $form.find("input[name='username']").val();
				var password = $form.find("input[name='password']").val();
				call("engine.Authenticate", {
					authUserName : username,
					authPassword : password,
					authType : "login"
				}, function (xml) {
					if (checkRoleTestPlatform($(xml))) {
						$form.slideUp("slow");
						callback();
					} else {
						for (var i = 0; i < 8 ; i++) {
							$form.animate({right : ((i % 2 == 0) ? "+" : "-") + "=10px"}, 50);
						}
					}
				});
				return false;
			}).slideDown("slow");
		}
	});
}

function checkRoleTestPlatform($xml) {
	if ($xml.find("roles>role[name=\"AUTHENTICATED\"]").length > 0) {
		var user = $xml.find("user").text();
		$("#user").text(user);
		$("#logout").slideDown("slow").click(function () {
			call("engine.Authenticate", {authType : "logout"}, function () {
				document.location.reload();
			});
			return false;
		});
	}
	else {
		$("#user").text("<anonymous>");		
	}
	
	return $xml.find("roles>role[name=\"TEST_PLATFORM\"]").length > 0;
}

function getEncodedYamlUri(project) {
	var yamlUrl = "";
	try {
		var location = document.location.href;
		yamlUrl = location.substring(0,location.lastIndexOf("/"))+"/openapi?YAML";
		if (typeof(project) !== "undefined") {
			yamlUrl += "&__project="+project;
		}
	}
	catch (e) {}
	return encodeURIComponent(yamlUrl);
}

$.ajaxSetup({
	type : "POST",
	dataType : "xml",
	complete: function (jqXHR) {
		var token = jqXHR.getResponseHeader("x-xsrf-token");
		if (token != null) {
			localStorage.setItem("x-xsrf-token", token);
		}
	},
	beforeSend: function (jqXHR) {
		jqXHR.setRequestHeader("x-xsrf-token", getXsrfToken());
	}
});

function getXsrfToken() {
	var token = localStorage.getItem("x-xsrf-token");
	return token == null ? "Fetch" : token;
}
