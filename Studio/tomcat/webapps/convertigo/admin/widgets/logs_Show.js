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

var regexpAllInferiorChars = /</g;
var regexpAllSuperiorChars = />/g;
var regexpDateTimeSeparatorChars = /[-:,\s]/;
var regexpLF = /\n/g;
var regexpSpace = / /g;
var regexpTabulation = /\t/g;

var $logTableBody;
var $logDivTable;

var bLogHelpVisible = true;
var bRealTime = false;
var realtimeAutoScroll = true;

var columnVisibility = {};
var bHasMoreResults;
var bGoToEnd = false;
var currentNbLine;
var filter = "";
var lineFormatMaxNbChars = 90;
var lineFormatDeltaChars = 10;
var previousTimestamp = 0;

var purgeDates = [];

function logs_Show_init(options) {	
	// The menu element should be moved to the body element in order
	// to match the popup menu library requirements
	if ($("body>#logContextualMenu").length == 0) {
		$("#logContextualMenu").remove().appendTo("body");
	}
	
	$(".date-pick").datepicker({ dateFormat: "yy-mm-dd" }).bind("mousewheel", {fn: function (delta) {		
		var ts = Date.parse($(this).datepicker('getDate'));		
		ts += (delta * 86400000 ); // +/- 1 day : 86400000 = 24 x 60 x 60 x 1000		
		$(this).datepicker('setDate',dateFormat(ts, "yyyy-mm-dd"));
	}}, simpleWheel);

	for (var i = 0; i < 10; i++) {
		$(".log-field-hour").append("<option>0" + i + "</option>");
		$(".log-field-minute").append("<option>0" + i + "</option>");
	}
	for (var i = 10; i < 24; i++) {
		$(".log-field-hour").append("<option>" + i + "</option>");
	}

	for (var i = 10; i < 60; i++) {
		$(".log-field-minute").append("<option>" + i + "</option>");
	}

	resetOptions();
	
	$(window).resize(onWindowResize);
	
	$("#logToggleOptions").button({ icons : { primary : "ui-icon-wrench" } });
	$("#logTogglePurge").button({ icons : { primary : "ui-icon-trash" } });
	$("#logToggleLevel").button({ icons : { primary : "ui-icon-gear" } });
	$("#logToggleHelp").button({ icons : { primary : "ui-icon-help" } });

	$("#logOptionsFullScreen").button({ icons : { primary : "ui-icon-zoomin" } });
	$("#logOptionsRealTime").button({ icons : { primary : "ui-icon-arrowrefresh-1-w" } });
	$("#logOptionsRealTimeAutoScroll").button({ icons : { primary : "ui-icon-arrowstop-1-s" } });
	$("#logOptionsUpdate").button({ icons : { primary : "ui-icon-refresh" } });
	$("#logOptionsDownload").button({ icons : { primary : "ui-icon-document" } });
	$("#logOptionsReset").button({ icons : { primary : "ui-icon-shuffle" } });
	$("#logOptionsGoToEnd").button({ icons : { primary : "ui-icon-arrowstop-1-e" } });
	$("#logOptionsClearFilter").button({ icons : { primary : "ui-icon-close" } });
	$("#logOptionsApplyOptions").button({ icons : { primary : "ui-icon-check" } });

	$logTableBody = $("#logTable > tbody:last");
	$logDivTable = $("#logDivTable");
	
	onWindowResize();
	
	$logDivTable.scroll(onLogDivTableScroll);
	
	$("#logOptionsRealTimeAutoScroll").attr("disabled", "disabled");
	$("#logOptionsRealTime").click(onLogOptionsRealTimeClick);
	$("#logOptionsUpdate").click(onLogOptionsUpdateClick);
	$("#logOptionsRealTimeAutoScroll").click(onLogOptionsRealTimeAutoScrollClick);
	$("#logOptionsFullScreen").click(onLogOptionsFullScreenClick);
	$("#logOptionsDownload").click(onLogOptionsDownloadClick);
	$("#logOptionsReset").click(onLogOptionsResetClick);
	$("#logOptionsGoToEnd").click(onLogOptionsGoToEndClick);
	$("#logOptionsClearFilter").click(onLogOptionsClearFilterClick);

	$("#logOptionsApplyOptions").click(onLogOptionsApplyOptionsClick);

	$("#logPurgeSlider").slider({
		disabled : true,
		range: "min",
		slide : function (event, ui) {
			onLogPurgeSlide(ui.value);
		}
	});
	
	$("#logPurgeButton").button({
		disabled : true,
		icons : { primary : "ui-icon-trash" }
	}).click(function () {
		$("#logPurgeConfirm").dialog("open");
	});
	
	initCommonDialog( $("#logPurgeConfirm"), {
		title : "Confirmation",
		open : function () {
			$(this).text($("#logPurgeMsgNotEmpty").text());
		},
		buttons : {
			Confirm : function () {
				onLogConfirmPurgeClick();
				$(this).dialog("close");
			},
			Cancel : function () {
				$(this).dialog("close");
			}
		}
	});
	
	initCommonDialog( $("#logPurgeSuccess") );
	
	var $visible = [];
	$("#logToggleButtons").click(function () {
		var top = "0px";
		if ($(this).parent().css("top") === top) {
			$(this).parent().find("button").button("disable");
			$visible = $("#logOptions, #logPurge, #logHelp, #logLevel").filter(":visible");
			top = (8 - $(this).parent().height()) + "px";
		} else {
			$(this).parent().find("button").button("enable");
		}
		if ($visible.length == 1) {
			$visible.slideToggle();
		}
		$(this).parent().animate({ "top" : top });
	});
	
	$("#logToggleOptions").click(onLogToggleOptionsClick);
	$("#logTogglePurge").click(onLogTogglePurgeClick);
	$("#logToggleLevel").click(onLogToggleLevelClick);
	$("#logToggleHelp").click(onLogToggleHelpClick);

	$(".log-column-selector").click(onLogColumnSelectorClick);

	//related to #2228 to allow extra column to be resized
	if ($.browser.mozilla) {
		var maxExtraSize = $("#logTable").width() / 4;
		changecss(".log-column-extra", "max-width", maxExtraSize + "px"); 
	}
	
	setOptions(options);
	
	logs_Show_update();	
}

function setOptions(options) {
	if (options) {
		if (options.filter) {
			$("#logOptionsFilter").val(options.filter);
			filter = options.filter;
		}
	}
}

function setDivTableSize() {
	var newSize = $(window).height();
	if (!$logDivTable.hasClass("log-full-screen")) {
		newSize = newSize - 270;
	}
	$logDivTable.css("height", newSize);
}

function onLogDivTableScroll() {
	if (bHasMoreResults && !bRealTime) {
		var tableDivScrollPosition = this.scrollTop + $logDivTable[0].clientHeight;
		var tableDivScrollHeight = this.scrollHeight;
		if (tableDivScrollPosition > 0.8 * tableDivScrollHeight) {
			getLines();
			bHasMoreResults = false;
		}
	}
}

function onLogOptionsRealTimeClick() {
	bRealTime = this.checked;
	$("#logOptionsDivDate").fadeToggle();
	if (bRealTime) {
		$("#logOptionsRealTimeAutoScroll").removeAttr("disabled");
		$("#logOptionsRealTimeAutoScroll").button("refresh");
		$("#logOptionsUpdate").attr("disabled", "disabled");
		$("#logOptionsUpdate").button("refresh");
		$("#logOptionsGoToEnd").attr("disabled", "disabled");
		$("#logOptionsGoToEnd").button("refresh");
		$logTableBody.empty();
		currentNbLine = 1;
		getLines();
	}
	else {
		$("#logOptionsRealTimeAutoScroll").attr("disabled", "disabled");
		$("#logOptionsRealTimeAutoScroll").button("refresh");
		$("#logOptionsUpdate").removeAttr("disabled");
		$("#logOptionsUpdate").button("refresh");
		$("#logOptionsGoToEnd").removeAttr("disabled");
		$("#logOptionsGoToEnd").button("refresh");
	}
}

function onLogOptionsUpdateClick() {
	bHasMoreResults = true;
	getLines();
}

function onLogOptionsRealTimeAutoScrollClick() {
	realtimeAutoScroll = $("#logOptionsRealTimeAutoScroll").attr("checked");
}

function onLogOptionsFullScreenClick() {
	$logDivTable.toggleClass("log-full-screen");
	onWindowResize();
}

function onLogOptionsDownloadClick() {
	filter = $("#logOptionsFilter").val();
	var startDate = getStartDateAsString();
	var endDate = getEndDateAsString();
	
	var oQueryString = {
		filter : filter,
		startDate : startDate,
		endDate : endDate
	}
	var queryString = $.param(oQueryString);
	window.open("services/logs.Download?" + queryString);
}

function onLogOptionsClearFilterClick() {
	$("#logOptionsFilter").val("");
}

function onLogToggleOptionsClick() {
	$("#logOptions").slideToggle();
	$("#logHelp, #logPurge, #logLevel").slideUp();
}

function onLogTogglePurgeClick() {
	if (!$("#logPurge").is(":visible")) {
		updatePurgeSlider();
	}
	$("#logPurge").slideToggle();
	$("#logOptions, #logHelp, #logLevel").slideUp();
}



function onLogToggleLevelClick() {
	
	if($("#Configuration_layout").length==0){			
		//load the configuration page		
		callFromLogShow=true;
		$("#widgetButtonConfiguration").click();
	}else{		
		if($("#logLevelCopyFromConfigurationButton").html().length == 0){
			var $configTable=$("div.config-category").has("h2:contains('Logs')").find("table").first().find("tbody");
			$("#logLevelCopyFromConfiguration").html($configTable);	
			var $buttonUpdate=$("#configFirstUpdateButtonLocation").find("button");
			$("#logLevelCopyFromConfigurationButton")
			.append($buttonUpdate)
			.click(function(){
				updateConfiguration();					
			});						
		}
		$("#logLevel").slideToggle();
		$("#logOptions, #logPurge, #logHelp").slideUp();
	}	
	
	
}

function testConfigurationLoaded(){
	return $("div.config-category").has("h2:contains('Log management')").find("table").length >0
}

function onLogToggleHelpClick() {
	$("#logHelp").slideToggle();
	$("#logOptions, #logPurge, #logLevel").slideUp();
}

function onLogOptionsResetClick() {
	resetOptions();
}

function onLogOptionsGoToEndClick() {
	if (bHasMoreResults) {
		bGoToEnd = true;
		getLines();
	}
	else {
		var logDivTable = $logDivTable[0];
		logDivTable.scrollTop = logDivTable.scrollHeight;
	}
}

function onLogColumnSelectorClick() {
	toggleColumnVisibility(this.value);
}

function onLogOptionsApplyOptionsClick() {
	filter = $("#logOptionsFilter").val();
	if (!bRealTime) logs_Show_update();
	return false;
}

function onLogConfirmPurgeClick() {
	var selectedDate = $("#logSelectedDate").text();
	callService("logs.Purge", function (xml) {
		var $xml = $(xml);
		var purgedDates = $.map($xml.find("date").toArray(), function (elt) {
			return $(elt).text();
		});
		var diff = [], i, j = 0;
		for (i in purgeDates) {
			if (purgeDates[i] === purgedDates[j] ) {
				j++;
			} else {
				diff.push(purgeDates[i]);
			}
			if (selectedDate === purgeDates[i]) {
				break;
			}
		}
		
		if (diff.length == 0) {
			$("#logPurgeFailed").parent().hide();
		} else {
			$("#logPurgeFailed").empty();
			for (i in diff) {
				$("#logPurgeFailed").append("<li>" + diff[i] + "</li>");
			}
			$("#logPurgeFailed").parent().show();
		}
		
		$("#logPurgeSuccess").dialog("open");
		
		updatePurgeSlider();
	}, { action : "delete_files", date : selectedDate });
}

function onLogPurgeSlide (value) {
	if (value > 0) {
		$("#logSelectedDate").text(purgeDates[value - 1]);
		$("#logPurgeMsgEmpty").hide();
		$("#logPurgeMsgNotEmpty").show();
		$("#logPurgeButton").button("enable");
	} else {
		$("#logPurgeMsgEmpty").show();
		$("#logPurgeMsgNotEmpty").hide();
		$("#logPurgeButton").button("disable");
	}
}

function onWindowResize() {
	setDivTableSize();
	resizeLogMessage();
}

function toggleColumnVisibility(columnName) {
	var display = $(".log-column-" + columnName).css("display");
	var newdisplay = "none";
	if (display == "none") {
		newdisplay = columnVisibility[columnName];
	}
	changecss(".log-column-" + columnName, "display", newdisplay);
	columnVisibility[columnName] = display;
	resizeLogMessage();
}

function resetOptions() {
	$(".log-reset-to-checked").attr("checked", "checked");
	$(".log-reset-to-unchecked").removeAttr("checked");
	
	$("#logOptionsRealTimeAutoScroll").attr("disabled", "disabled");
	$("#logOptionsDivDate:not(:visible)").fadeToggle();
	if ($(".log-column-level:visible").length > 0) {
		toggleColumnVisibility("level");
	}
	if ($(".log-column-delta-time:visible").length > 0) {
		toggleColumnVisibility("delta-time");
	}
	
	var now = new Date();
	$("#logOptionsStartDate").val(now.format("yyyy-mm-dd"));
	$("#logOptionsEndDate").val(now.format("yyyy-mm-dd"));
	
	now.setTime(now.getTime() - 600000); // -10 min
	
	$("#logOptionsStartHour").val(now.format("HH"));
	$("#logOptionsStartMinute").val(now.format("MM"));
	$("#logOptionsEndHour").val("23");
	$("#logOptionsEndMinute").val("59");
	
	$("#logOptionsFilter").val("");
	
	$("#logOptionsUpdate").removeAttr("disabled");
	$("#logOptionsUpdate").button("refresh");

	$("#logDivOptionsToolbar input").button("refresh");
}

function logs_Show_update(options) {
	setOptions(options);
	bHasMoreResults = false;
	currentNbLine = 1;
	$logTableBody.empty();
	getLines();
}

function addContextMenuToNewLines(startIndex) {
	$("#logTable tr:gt(" + (startIndex - 1) + ") td").filter(".log-column-category, .log-column-time, .log-column-level,.log-column-thread, .log-column-message, .log-column-extra").mousedown(function (event) {
		$(this).unbind("mousedown").contextMenu(
				{ menu: "logContextualMenu" },
				function(action, el, pos) {
					return onLogContextualMenu(action, el, pos);
				},
				function(action, el, pos) {
					return onLogContextualMenuShowing(action, el, pos);
				}
		).trigger(event);
	});
}


function onLogGetSuccess(json) {
	var i = currentNbLine;
	var linesToAdd = "";
	for ( var line in json.lines) {
		linesToAdd += formatLine(i, json.lines[line]);
		i++;
	}
	
	var startIndex = currentNbLine;
	
	currentNbLine = i;
	bHasMoreResults = json.hasMoreResults;
	$logTableBody.append(linesToAdd);
	
	resizeLogMessage();

	addContextMenuToNewLines(startIndex);
	
	var logDivTable = $logDivTable[0];
	if (bRealTime) {
		if (realtimeAutoScroll) {
			logDivTable.scrollTop = logDivTable.scrollHeight;
		}
		getLines();
	}
	else {
		$("#logOptionsUpdate").removeAttr("disabled");
		$("#logOptionsUpdate").button("refresh");
		
		if (bHasMoreResults) {
			$("#logMessageEndOfLogs").hide();
			$("#logMessageMoreResults").show();
			$("#logMessageSearching").hide();
			if (bGoToEnd) {
				logDivTable.scrollTop = logDivTable.scrollHeight;
			}
			if (logDivTable.scrollHeight <= logDivTable.clientHeight) {
				getLines();
			}
		}
		else {
			$("#logMessageEndOfLogs").show();
			$("#logMessageMoreResults").hide();
			$("#logMessageSearching").hide();
			logDivTable.scrollTop = logDivTable.scrollHeight;
			bGoToEnd = false;
		}
	}
}

function getStartDateAsString() {
	return $("#logOptionsStartDate").val() + " " + $("#logOptionsStartHour").val() + ":" + $("#logOptionsStartMinute").val();
}

function getEndDateAsString() {
	return $("#logOptionsEndDate").val() + " " + $("#logOptionsEndHour").val() + ":" + $("#logOptionsEndMinute").val();
}

function getLines() {
	$("#logMessageEndOfLogs").hide();
	$("#logMessageMoreResults").hide();
	$("#logMessageSearching").show();

	$("#logOptionsUpdate").attr("disabled", "disabled");
	$("#logOptionsUpdate").button("refresh");

	var startDate = getStartDateAsString();
	var endDate = getEndDateAsString();
	
	if (Date.parse(startDate.replace(" ", "T")) >= Date.parse(endDate.replace(" ", "T"))) {
		showError("End date must be greater than start date<br/>" + startDate + " >= " + endDate);
	} else {
		callJSONService(
			"logs.Get",
			onLogGetSuccess, {
				filter : filter,
				startDate : startDate,
				endDate : endDate,
				timeout : 1000,
				nbLines : 50,
				moreResults : bHasMoreResults,
				realtime : bRealTime
		});
	}
}

function onLogContextualMenuShowing(el, target, pos) {
	var variableName = el[0].className.substr(11); // 11 = "log-column-".length

	$("#logContextualMenu").disableContextMenuItems("#addVariable,#setStartDate,#setEndDate");
	
	switch(variableName) {
	case "time":
		$("#logContextualMenu").enableContextMenuItems("#setStartDate,#setEndDate");
		break;
	case "extra":
		$("#logContextualMenu").enableContextMenuItems("#addVariable");
		var extraVariableName = target.text();
		extraVariableName = extraVariableName.split("=")[0];
		$("#logContextMenuItemExtra").text("Add '" + extraVariableName + "' variable");
		break;
	}
}

function onLogContextualMenu(action, el, target, pos) {
	var cellText = $(el).text();
	var data = cellText;
	var selectedText = getSelectedText();
	if (selectedText != "") data = "" + selectedText;
	var variableName = el[0].className.substr(11); // 11 = "log-column-".length

	var date, hour, minute;
	if (variableName == "time") {
		var aCellText = cellText.split(" ");
		date = aCellText[0];
		var time = aCellText[1];
		var aTime = time.split(":");
		hour = aTime[0];
		minute = aTime[1];
	}
	
	var $filter = $("#logOptionsFilter");
	var filter = $filter.val();

	// Escape double quotes
	data = data.replace(/"/g, "\\\"");
	
	switch(action) {
	case "addCommandEquals":
		if (filter != "") {
			filter = "(" + filter + ") and ";
		}
		$filter.val(filter + variableName + " == \"" + data + "\"");
		break;
	case "addCommandContains":
		if (filter != "") {
			filter = "(" + filter + ") and ";
		}
		$filter.val(filter + variableName + ".contains(\"" + data + "\")");
		break;
	case "addCommandStartsWith":
		if (filter != "") {
			filter = "(" + filter + ") and ";
		}
		$filter.val(filter + variableName + ".startsWith(\"" + data + "\")");
		break;
	case "addCommandEndsWith":
		if (filter != "") {
			filter = "(" + filter + ") and ";
		}
		$filter.val(filter + variableName + ".endsWith(\"" + data + "\")");
		break;
	case "addVariable":
		var extraVariable = target.text();
		var aExtraVariable = extraVariable.split("=");
		var extraVariableName = aExtraVariable[0];
		var extraVariableValue = aExtraVariable[1];
		if (filter != "") {
			filter = "(" + filter + ") and ";
		}
		$filter.val(filter + extraVariableName + " == \"" + extraVariableValue + "\"");
		$("#logContextMenuItemExtra").text("Add variable");
		break;
	case "setStartDate":
		$("#logOptionsStartDate").val(date);
		$("#logOptionsStartHour").val(hour);
		$("#logOptionsStartMinute").val(minute);
		break;
	case "setEndDate":
		$("#logOptionsEndDate").val(date);
		$("#logOptionsEndHour").val(hour);
		$("#logOptionsEndMinute").val(minute);
		break;
	}
}

function formatLine(nLine, line) {
	var category = line[0];
	var time = line[1];
	
	var deltaTime;
	var tdate = time.split(regexpDateTimeSeparatorChars);
	var currentTimestamp = (new Date(tdate[0], tdate[1], tdate[2], tdate[3], tdate[4], tdate[5], tdate[6])).getTime();
	if (nLine == 1) {
		deltaTime = "+0ms";
	}
	else {
		deltaTime = currentTimestamp - previousTimestamp;
		if (deltaTime < 1000) deltaTime = "+" + deltaTime + "ms";
		else if (deltaTime < 60000) deltaTime = "+" + (deltaTime / 1000) + "s";
		else {
			deltaTime = Math.round(deltaTime / 1000);
			var minutes = Math.floor(deltaTime / 60);
			var seconds = deltaTime % 60;
			deltaTime = "+" + minutes + "min" + seconds + "s";
		}
	}
	previousTimestamp = currentTimestamp;
	
	var level = line[2];
	var thread = line[3];

	// The message is build with its main part optionnaly followed by a LF character
	// and then then sublines part of the message
	var message = line[4].replace(regexpAllInferiorChars, "&lt;").replace(regexpAllSuperiorChars, "&gt;");
	var posLF = message.indexOf('\n');
	if (posLF != -1) {
		var mainMessage = message.substring(0, posLF);
		var sublinesMessage = message.substring(posLF + 1)
			.replace(regexpSpace, "&nbsp;")
			.replace(regexpTabulation, "&nbsp;&nbsp;&nbsp;&nbsp;")
			.replace(regexpLF, "<br/>");
		message = "<div class=\"log-message\">" + mainMessage + "<br/>" +
			"<div class=\"log-message-sublines\">" + sublinesMessage + "</div></div>";
	}
	else {
		message = "<div class=\"log-message\">" + message + "</div>";
	}

	// Extra columns are columns that contain "key=value" 
	var extra = "";
	var j = 1;
	for (var i = 5; i < line.length; i++) {
		extra += "<span class=\"log-extra log-extra" + i + "\">" +
		line[i] + "</span>";
		if( i != line.length -1)
			extra += " <br/>"; //blank important		
		j++;
	}

	var formattedLine =
		"<tr class=\"log-line log-line-" + (nLine % 2 == 0 ? "odd-" : "even-") + level + "\">" +
			"<td class=\"log-column-line-number\">" + nLine + "</td>" +
			"<td class=\"log-column-category\">" + category + "</td>" +
			"<td class=\"log-column-time\">" + time + "</td>" +
			"<td class=\"log-column-delta-time\">" + deltaTime + "</td>" +
			"<td class=\"log-column-level\">" + level + "</td>" +
			"<td class=\"log-column-thread\">" + thread + "</td>" +
			"<td class=\"log-column-message\">" + message + "</td>" +
			"<td class=\"log-column-extra\">" + extra + "</td>" +
		"</tr>";
	
	return formattedLine;
}

function getSelectedText() {
	if (window.getSelection) {
		return window.getSelection();
	} else if (document.getSelection) {
		return document.getSelection();
	} else {
		var selection = document.selection && document.selection.createRange();
		if (selection.text) {
			return selection.text;
		}
		return false;
	}
	return false;
}

function updatePurgeSlider () {
	callService("logs.Purge", function (xml) {
		var $xml = $(xml);
		purgeDates = $.map($xml.find("date").toArray(), function (elt) {
			return $(elt).text();
		});
		$("#logPurgeSlider").slider("value", 0);
		onLogPurgeSlide(0);
		if (purgeDates.length > 0) {
			$("#logPurgeSlider").slider("option", { disabled : false, max : purgeDates.length });
		} else {
			$("#logPurgeSlider").slider("option", { disabled : true, max : 0 });
		}
	}, { action : "list_files" });
}

// fix #1739 - Unwanted horizontal scroll bar with Firefox in the logviewer
function resizeLogMessage() {
	if ($.browser.mozilla) {
		var log_message_width = $(".log-message:first").width();
		if (log_message_width != undefined) {
			var overwidth = $logDivTable.hasClass("log-full-screen") ?
				$("#logTable").width() - $logDivTable.width() :
				$("#maincontent").width() - $("#mainheader").width();
			if (overwidth <= 0) { // lets message take its fullsize then recompute its best size
				changecss(".log-message", "width", "auto");
				if (arguments.length === 0) { // prevent loop, recall only one time
					resizeLogMessage(true);
				}
			} else {
				changecss(".log-message", "width", (log_message_width - overwidth - 25) +"px"); // 25 ~ scrollbar width								
			}
		}
	}
}