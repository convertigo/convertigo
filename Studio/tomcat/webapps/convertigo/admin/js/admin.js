/*
 * Copyright (c) 2001-2014 Convertigo SA.
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

//use for the interactiio between logs_Show and Configuration_List
callFromLogShow=false;
var widget_container;
var instanceid = new Date().getTime();
var functionConfirm;
var cloud_instance = false;
//written into engine_GetStatus
var startDate=new Date();
var serverDate;
var serverOffset;
var initialDiffClientServer;
var engineVersion = "latest";

$(window).ready(function() {
	$.ajaxSetup({
		traditional : true
	});
	
	$(document).ajaxStart(function () {
		$("#ajax_wait").show();
	}).ajaxStop(function () {
		$("#ajax_wait").fadeOut(1000);
	});

	callService("engine.GetSystemInformation", function (xml) {
		var key_button = $("#widgetButtonKeys").closest("li");
		if ($(xml).find("cloud_instance").text() !== "false") {
			cloud_instance = true;
			key_button.hide();
		} else {
			key_button.show();
		}
	});
	
	$("#swaggerLink").attr("href", "../swagger/ui/index.html?url="+getEncodedYamlUri());
	
	$("#logOut div").click(function() {
		$.ajax( {
			type : "POST",
			url : "services/engine.Authenticate",
			data : "authType=logout",
			dataType : "xml",
			success : function(xml) {
				document.location.href = "login.html";
			}
		});
	});

	$(".loadWidgetsButton").click(function() {
		onLoadWidgetsButtonClick($(this).attr('id'));
	});

	$(".mainmenu_text").click(function() {
		onLoadWidgetsButtonClick($(this).prev(".loadWidgetsButton").attr('id'));
	});

	initCommonDialogs();
	initWait();

	initWidgetContainer();

	$("select").on("mousewheel", {fn: function (delta) {
		if (!$(this).prop("disabled")) {
			var index = this.selectedIndex + delta;
			if (0 <= index && index < this.length) {
				this.selectedIndex = index;
				$(this).change();
			}
		}
	}}, simpleWheel);
	
	$("button").on("click", function () {
		$(this).blur();
	});
	
	callService("engine.GetStatus", function(xml) {		
		serverDate = parseInt($(xml).find("time").text());	
		var currentDate = new Date();
		initialDiffClientServer=serverDate-currentDate.getTime();
		engineVersion = $(xml).find("version").attr("engine");
		if (!engineVersion || !engineVersion.length) {
			engineVersion = "latest";
		}
		updateDate();
	});
	
	var layoutName = window.location.hash;
	if (layoutName) {
		displayPage(layoutName.substr(1)); // remove the # sign
	}
	else {
		displayPage("Home");
	}

	checkAuthentication();
});

function checkAuthentication(){
	$.ajax( {
		type : "POST",
		url : "services/engine.CheckAuthentication",
		dataType : "xml",
		data : {},
		success : function(xml) {
			var $xml = $(xml);
			var $authenticated = $xml.find("authenticated");
			if ($authenticated.text() == "true") {
				setTimeout(function() {
					checkAuthentication();
				}, 5000);
			} else {
				document.location.href = "login.html";
			}
		},
		error : function (xhr, ajaxOptions, thrownError) {
			if (xhr.status==503) {
				showError("Server seems unreacheable");
			}
		},
        global: false
	});
}

function updateDate(){		
	var currentDate = new Date();
	var diff = currentDate.getTime()-startDate.getTime();
	var utc = serverDate + diff + initialDiffClientServer;
	$("#mainDate").html(new Date(utc).toLocaleString());
	setTimeout(function() {
		updateDate();
	}, 10000);
}

function onLoadWidgetsButtonClick(widgetButtonName) {
	var layoutName = widgetButtonName.substr(12); // 12 = "widgetButton".length
	displayPage(layoutName);
}

function displayPage(layoutName, widgetOptions) {
	clear();

	// if the layout does not exist we add it to the DOM
	if ($("#" + layoutName + "_layout").length == 0) {
		loadLayout(layoutName, function() {
			loadScripts(layoutName, widgetOptions);
		});
	} else {
		loadWidgets(layoutName, widgetOptions);
	}
}

function initWidgetContainer() {
	loadHTML(
		"widget_container.html",
		function(html) {
			widget_container = html;
		}
	);
}

function initCommonDialogs () {
	initCommonDialog( $("#dlgError"), {
		title : "Error"
	});
	$("#dlgErrorJavaStackTraceTitle").click(function () {
		$("#dlgErrorJavaStackTrace").toggle(500);
	});
	
	initCommonDialog( $("#dlgInfo") );
	
	initCommonDialog( $("#dlgConfirm"), {
		buttons : {
			Yes : function() {
				$(this).dialog('close');
				functionConfirm();
				return false;
			},
			No : function() {
				$(this).dialog('close');
				return false;
			}
		}
	});
}

function initCommonDialog ($elt, opts) {
	if (typeof(opts) === "undefined") {
		opts = {};
	}
	return $elt.dialog( $.extend({}, {
		autoOpen : false,
		title : "Information",
		modal : true,
		zIndex : 50000,
		width : "auto",
		buttons : {
			Ok : function() {
				$(this).dialog('close');
				return false;
			}
		}
	}, opts) );
}

function initWait() {
	$("#progressBarDialog").html("Please wait a while<div id='progressBar'/>")
			.dialog( {
				autoOpen : false,
				title : "Wait",
				modal : true
			});
	$("#progressBar").progressbar().hide();
}

var progressTimer;
function startWait(intervalInMs) {
	$("#progressBarDialog").dialog("open");
	$("#progressBar").progressbar("value", 1).show();
	progressTimer = setInterval(function() {
		$("#progressBar").progressbar("value", ($("#progressBar").progressbar("value") + 1) % 100);
	}, intervalInMs);
}

function endWait() {
	clearInterval(progressTimer);
	$("#progressBarDialog").dialog("close");
}

function showError(message, javaStackTrace) {
	$("#dlgErrorMessage").html(message);
	
	if(typeof(javaStackTrace) === "undefined"){
		$("#dlgErrorJavaStackTraceTitle").hide();
		$("#dlgErrorJavaStackTrace").empty();
	}else{
		$("#dlgErrorJavaStackTrace").hide().text(javaStackTrace);
		$("#dlgErrorJavaStackTraceTitle").show();
	}	
	
	$("#dlgError").dialog("open").parent().css("width", "auto");
}

function showInfo(message) {
	$("#dlgInfoMessage").html(message);
	$("#dlgInfo").dialog('open');
	if($("#dlgInfo").width() > $(window).width()/2){
		$("#dlgInfo").parent().css("width", $(window).width()/2).css("left", "" + $(window).width()/4 + "px");
	}
}

function showConfirm(message, staff) {
	$("#dlgConfirmMessage").html(message);
	$("#dlgConfirm").dialog('open');
	functionConfirm = staff;
}

function clear() {
	$(window).unbind("scroll");
	$("#logNavigationBar").hide();
	$(".widget").hide();
}

function loadWidgets(layoutName, widgetOptions) {
	$("#" + layoutName + "_layout").find("div[class~=widget]").each(function() {
		loadWidget($(this).attr("id"), widgetOptions);
	});
}

function loadWidget(widgetId, widgetOptions) {
	var $widget = $("#" + widgetId);
	if ($widget.text().length > 0) {
		$widget.show();
		window[widgetId + "_update"](widgetOptions);
	} else {	
		loadHTML(
			"widgets/" + widgetId + ".html",
			function(xml) {
				var $tmp = $(widget_container);
				$tmp.find(".widget_content_center").first().append(xml);
				//Condition added by alexandret
				//Get Url for each widgets to go to help informations
				var urlHelp = "http://www.convertigo.com/document/latest/operating-guide/using-convertigo-administration-console/";
				switch (widgetId)
				{
					//home page
					case "engine_GetStatus":
					case "engine_GetSystemInformation":
					case "engine_Monitor":
						urlHelp += "general-presentation-of-the-administration-console/home-page/";
						break;
					//Configuration page
					case "configuration_List":
						urlHelp += "configuration/configuration-page/";
						break;
					//Connections page
					case "connections_List":
						urlHelp += "connections/";
						break;
					//Projects page
					case "projects_List":
						urlHelp += "projects/";
						break;
					//Certificates page
					case "certificates_List":
						urlHelp += "certificates/";
						break;
					//Logs page
					case "logs_Show":
						urlHelp += "logs/";
						break;
					//Trace player page
					case "trace_List":
						urlHelp += "trace-player/";
						break;
					//Cache page
					case "cache":
						urlHelp += "cache/";
						break;
					//Scheduler page
					case "scheduler_ListTasks":
						urlHelp += "scheduler/scheduler-page/";
						break;
					//Keys page
					case "keys_List":
						urlHelp += "keys/";
						break;
					//Symbols page
					case "globalSymbols_List":
						urlHelp += "global-symbols/";
						break;
					//Environment variables
					case "environmentVariables_List":
						urlHelp += "global-symbols/#environment";
						break;
					//Store page
					case "store":
						urlHelp += "store/";
						break;	
				}
				//show icon help button
				$tmp.find(".widget_content_topcenter").first().html('<a href="' + urlHelp + '" class="widget_content_help" target="_blank">' + '</a>' + '<h3>' + $("#" + widgetId).attr("displayName") + '</h3>');

				// End condition
				$("#" + widgetId).append($tmp.find("div[class=widget]").children().first());
				window[widgetId + "_init"](widgetOptions);
			}
		);
	}

}

function loadScripts(layoutName, widgetOptions) {
	var first = 0;
	$("#" + layoutName + "_layout").find("div[class~=widget]").each(function() {
		var widgetId = $(this).attr("id");
		getScript('widgets/' + widgetId + '.js', function() {
			loadWidget(widgetId, widgetOptions);
		})
	});

}

function loadLayout(layoutName, callBack) {
	loadHTML(
		"layouts/" + layoutName + ".html",
		function(xml) {
			$modif = $(xml);
			$modif.first().attr("id", layoutName + "_layout");
			$("#body").append($modif);
			callBack();
		}
	);
}

function genericError(XMLHttpRequest, type, extra, callback) {
	if (type == "error") {
		var xml = XMLHttpRequest.responseXML;
		var $xml = $(xml);
		
		var $error = $xml.find("error > message:contains('Authentication failure')");
		if ($error.length != 0) {
			//document.location.href = "login.html";
		} else if (typeof(callback) !== "undefined") {
			callback(httpRequest.responseXML);
		} else if ("" + XMLHttpRequest.status === "0") {
			showError("Server seems unreacheable");
		} else {
			showError($xml.find("message:first").text(), $xml.find("stacktrace:first").text());
		}
	} else if (type == "parsererror") {
		showError(extra.message);		
	}
}

function loadHTML(url, successFunction, parameters, errorFunction, extra) {
	return ajaxCall("POST", url, "text", parameters, successFunction, errorFunction, extra);
}

function callService(serviceName, successFunction, parameters, errorFunction, extra) {
	return ajaxCall("POST", "services/" + serviceName, "xml", parameters, successFunction, errorFunction, extra);
}

function callJSONService(serviceName, successFunction, parameters, errorFunction, extra) {
	return ajaxCall("POST", "services/" + serviceName, "json", parameters, successFunction, errorFunction, extra);
}

function ajaxCall(httpMethod, url, dataType, data, successFunction, errorFunction, extra) {
	var parameters = {
		type : httpMethod,
		url : url,
		dataType : dataType,
		data : data,
		success : successFunction,
		error : function (XMLHttpRequest, typeError, extra) {
			if(dataType!= undefined && XMLHttpRequest.responseText!=undefined && dataType=="json"){
				try{
					var jsonResult;	
					if(JSON!=undefined){
						try{
							jsonResult=JSON.parse(XMLHttpRequest.responseText);
						}catch(err){
							jsonResult=eval("("+XMLHttpRequest.responseText+")");
						}
					}else{
						jsonResult=eval("("+XMLHttpRequest.responseText+")");	
					}
					successFunction.call(this,jsonResult);
				}
				catch(err){
					showError(err.description);
				}
			}else{
				genericError(XMLHttpRequest, typeError, extra, errorFunction);
			}			
		},
		beforeSend : function (XMLHttpRequest) {
			XMLHttpRequest.setRequestHeader("Admin-Instance", instanceid);
		}
	};
	if (typeof(extra) !== undefined) {
		$.extend(parameters, extra)
	}
	return $.ajax(parameters);
}

function getScript(url, callback) {
	var script = document.createElement("script");
	script.src = url;
	var done = false;
	script.onload = script.onreadystatechange = function() {
		if (!done
				&& (!this.readyState || this.readyState == "loaded" || this.readyState == "complete")) {
			done = true;
			if (callback)
				callback();
			// Handle memory leak in IE
			script.onload = script.onreadystatechange = null;
		}
	};
	document.getElementsByTagName("head")[0].appendChild(script);
}

function changecss(cssClass, element, value) {
	return changecss0("admin.css", cssClass, element, value);
}

function changecss0(cssFile, cssClass, element, value) {
	var cssRules;
	var added = false;

	for (var sIndex = 0; sIndex < document.styleSheets.length; sIndex++) {
		var stylesheet = document.styleSheets[sIndex];
		if (stylesheet.href.indexOf(cssFile) != -1) {
			if (stylesheet['rules']) {
				cssRules = stylesheet['rules'];
			} else if (stylesheet['cssRules']) {
				cssRules = stylesheet['cssRules'];
			} else {
				// no rules found... browser unknown
				return "";
			}
	
			for (var rIndex = 0; rIndex < cssRules.length; rIndex++) {
				var cssRule = cssRules[rIndex];
				if (cssRule.selectorText == cssClass) {
					if (cssRule.style[element]) {
						var previousValue = cssRule.style[element];
						cssRule.style[element] = value;
						added = true;
						return previousValue;
					}
				}
			}
	
			if (!added) {
				try {
					stylesheet.insertRule(cssClass + ' { ' + element + ': ' + value + '; }', cssRules.length);
				} catch (err) {
					try {
						stylesheet.addRule(cssClass, element + ': ' + value + ';');
					} catch (err) {
					}
				}
			}
			break;
		}
	}
	
	return "";
}


//to allow only digits on an input
function chiffres(event) {
	// IE / Firefox compatibility
	if(!event&&window.event) {
	event=window.event;
	}
	
	   var KeyCode = (event.keyCode) ? event.keyCode : event.which;
	   if ((KeyCode == 8) // backspace
	        || (KeyCode == 9) // tab
	        || (KeyCode == 37) // left arrow
	        || (KeyCode == 39) // right arrow
	        || (KeyCode == 46)) // delete
	   {
		   return true;
	   }   	 
	// IE
	if(KeyCode < 48 || KeyCode > 57) {
		event.returnValue = false;
		event.cancelBubble = true;
	}
	// DOM
	if(KeyCode < 48 ||KeyCode > 57) {
		event.preventDefault();
		event.stopPropagation();
	}
}

function domToString($xml) {
	return "<?xml version=\"1.0\"?>" + $("<d/>").append($xml).html();
}

function domToString2(xmlNode) {
	try {
		// Gecko- and Webkit-based browsers (Firefox, Chrome), Opera.
		return (new XMLSerializer()).serializeToString(xmlNode);
	} catch (e) {
		try {
			// Internet Explorer.
			return xmlNode.xml;
		} catch (e) {
			// Other browsers without XML Serializer
			alert('Xmlserializer not supported');
		}
	}
	return false;
}

function htmlEncode(text) {
	return $("<d/>").text(text).html();
}

function htmlCode($elt) {
	return $("<d/>").append($elt.clone()).html();
}

function simpleWheel(event, delta) {
	if (typeof(delta) === "undefined") {
		delta = event.wheelDelta;
	}
	if (typeof(event.data.fn) === "function") {
		event.data.fn.apply(this, [delta < 0 ? 1 : -1]);
		return false;
	}
}

function getEncodedYamlUri(project) {
	var yamlUrl = "";
	try {
		var location = document.location.href;
		yamlUrl = location.substring(0,location.indexOf("/admin"))+"/api?YAML";
		if (typeof(project) !== "undefined") {
			yamlUrl += "&__project="+project;
		}
	}
	catch (e) {}
	return encodeURIComponent(yamlUrl);
}