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

jQuery().ready(function() {
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
	
	var authToken = location.hash.match(new RegExp("#authToken=(.*)"));
	
	if (authToken != null) {
		authenticate({authToken: authToken[1]});
	} else {
		checkAuthentication();
	}
	
    $(".index").show();

	$("#FieldConvAdminUserLogin").focus();

	$("#dlgAuthFailed").dialog( {
		autoOpen : false,
		title : "Error",
		modal : true,
		buttons : {
			Ok : function() {
				$(this).dialog('close');
				return false;
			}
		}
	});

	$("#loginForm").submit(function() {
		authenticate({
			authUserName: $("#FieldConvAdminUserLogin").val(),
			authPassword: $("#FieldConvAdminUserPassword").val()
		});
		return false;
	});
});

function checkFromStudioWeb(page) {
	var studioWebDev = sessionStorage.getItem("studioWebDev");
	sessionStorage.removeItem("studioWebDev");
	if (studioWebDev == 'true') {
		document.location.href = "/";
	} else if (studioWebDev == 'false') {
		document.location.href = document.location.href.replace(/\/admin\/.*/, '/studio/');
	} else {
		document.location.href = page;
	}
}

function checkAuthentication() {
	$.ajax( {
		url : "services/engine.CheckAuthentication",
		data : {},
		success : function(xml) {
			var $xml = $(xml);
			var $authenticated = $xml.find("authenticated");
			if ($authenticated.text() == "true") {
				checkFromStudioWeb("main.html");
			}
		}
	});
}

function authenticate(data) {
	data.authType = "login";
	
	request = $.ajax( {
		url : "services/engine.Authenticate",
		data : data,
		success : function(xml) {
			var $xml = $(xml);
			if ($xml.find("success").length > 0) {
				checkFromStudioWeb($("form").attr("action"));
			} else {
				$("#dlgAuthFailed_message").text($xml.find("error").text());
				$("#dlgAuthFailed").dialog('open');
			};
		}
	});
}

function getXsrfToken() {
	var token = localStorage.getItem("x-xsrf-token");
	return token == null ? "Fetch" : token;
}
