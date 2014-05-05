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

function trace_List_init() {
	if (cloud_instance) {
		$("#cloud_trace_message").show();
	}
	
	$("#traceListCreateTrace").button( {
		icons : {
			primary : "ui-icon-circle-plus"
		}
	}).click(function() {
		updateTraces($("#traceListPortSelected").val(), $("#traceListEtrAvailable").val());
	});

	$("#traceList").jqGrid( {
		datatype : "local",
		colNames : [ 'Enabled', 'Port', 'File', 'Delete' ],
		colModel : [ {
			name : 'enabledDisabeled',
			index : 'enabledDisabeled',
			width : 10,
			align : "center"
		}, {
			name : 'port',
			index : 'port',
			width : 20,
			sorttype:'int',
			align : "left"
		}, {
			name : 'file',
			index : 'file',
			width : 50,
			align : "left"
		}, {
			name : 'btnDelete',
			index : 'btnDelete',
			width : 10,
			sortable : false,
			align : "center"
		} ],
		autowidth : true,
		viewrecords : true,
		height : 'auto',
		sortable : true,
		pgbuttons : false,
		pginput : false,
		toppager : false,
		emptyrecords : 'No traces configured',
		altRows : true,
		rowNum: '1000000'
	});

	trace_List_update();
}

function trace_List_update() {
	clearTable();
	callService(
			"trace.List",
			function(xml) {
				$(xml)
						.find("trace")
						.each(
								function() {
									var state;
									var port;

									port = $(this).attr("port");
									if ($(this).attr("enabled") == "true")
										state = '<a href="javascript: enableTrace(\'' + port + '\',\'disableTrace\')"><img border="0" src="images/convertigo-administration-picto-bullet-green.png"></a>';
									else
										state = '<a href="javascript: enableTrace(\'' + port + '\',\'enableTrace\')"><img border="0" src="images/convertigo-administration-picto-bullet-red.png"></a>';

									$("#traceList").jqGrid(
										"addRowData",
										port,
										{
											enabledDisabeled : state,
											port : port,
											file : $(this).attr("file"),
											group : "<input type='text' value='" + $(this).attr("group") + "' />",
											btnDelete : "<a href=\"javascript: deleteTraces('" + port + "')\"><img border=\"0\" title=\"Delete the trace\" src=\"images/convertigo-administration-picto-delete.png\"></a>"
										});
								});
				$("#traceListEtrAvailable").empty();
				var hasTrace=false;
				$(xml).find("etr").each(function() {
					hasTrace=true;
					$("#traceListEtrAvailable").append("<option>" + $(this).text() + "</option>");
				});
				if(hasTrace){
					$("#traceListCreateTrace").button("enable");
				}else{
					$("#traceListCreateTrace").button("disable");
				}
			});
}

function clearTable() {
	$("#traceList").jqGrid('clearGridData');
}

function updateTraces(port, file) {
	var errMsg = "";
	if (port.length === 0) {
		showError("The port field must be filled");
	} else {
		callService("trace.Create", function (xml) {
			showInfo("Service response : " + $(xml).find("message").text());
			trace_List_update();
		}, {port: port, file: file});
	}
}

function deleteTraces(port) {
	showConfirm("Are you sure you want to delete the trace on port " + port + " ?", function() {
		callService("trace.Create", trace_List_update, {port: port, del: ""});
	});
}

function enableTrace(port, state) {
	var params = {port: port};
	params[state] = "";
	callService("trace.Create", trace_List_update, params);
}
