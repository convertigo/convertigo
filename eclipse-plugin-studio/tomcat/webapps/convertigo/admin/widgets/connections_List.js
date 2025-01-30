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

var connections_List_timeout;

function updateConnectionsList(xml) {
	var $xml = $(xml);
	$("#connectionsContextsInUse").text($xml.find("contextsInUse").text());
	$("#connectionsContextsNumber").text($xml.find("contextsNumber").text());
	$("#sessionsInUse").text($xml.find("sessionsInUse").text());
	$("#sessionsNumber").text($xml.find("sessionsNumber").text());
	$("#connectionsThreadsInUse").text($xml.find("threadsInUse").text());
	$("#connectionsThreadsNumber").text($xml.find("threadsNumber").text());
	$("#connectionsHttpTimeout").text($xml.find("httpTimeout").text());
	
	if ($xml.find("sessionsIsOverflow").text() == "true") {
		$("#sessionOverflow").show();
	} else {
		$("#sessionOverflow").hide();
	}
	
	$("#sessionsList").jqGrid('clearGridData');
	
	$xml.find("session").each(function(index) {
		$("#sessionsList").jqGrid("addRowData", $(this).attr("sessionID"), {
				btnDelete: '<a title="Delete the session"><img border="0" src="images/convertigo-administration-picto-delete.png" onClick="deleteSession(\'' + $(this).attr("sessionID") + '\')"></a></td>',
				showlogs: '<a href="javascript: getLogs(\'' + $(this).attr("sessionID") + '\', true)"><img src="images/edit.gif" /></a>',
				sessionID: '<a href="javascript: filterSession(\'' + $(this).attr("sessionID") + '\')">' + $(this).attr("sessionID") + '</a>',
				contexts: $(this).attr("contexts"),
				user: $(this).attr("authenticatedUser"),
				adminRoles: $(this).attr("adminRoles"),
				isFullSyncActive: $(this).attr("isFullSyncActive") == "true" ? '<img border="0" title="Connected" src="images/convertigo-administration-picto-bullet-green.png" />' : '<img border="0" title="Connected" src="images/convertigo-administration-picto-bullet-red.png" />',
				clientIP: $(this).attr("clientIP"),
				deviceUUID: $(this).attr("deviceUUID"),
				sessionLastAccessDate: $(this).attr("lastSessionAccessDate"),
				sessionInactivityTime: $(this).attr("sessionInactivityTime"),
				clientComputer: $(this).attr("clientComputer")
			}
		)
		if ($(this).attr("isCurrentSession")) {
			$("#" + $(this).attr("sessionID") + ">td").css("background-color", "lightgreen");
		};
	});
	
	$("#connectionsList").jqGrid('clearGridData');

	$xml.find("connection").each(function(index) {
		projectName = $(this).attr("name");
		$("#connectionsList").jqGrid("addRowData", $(this).attr("contextName"), {
				btnDelete: '<a title="Delete the connection"><img border="0" src="images/convertigo-administration-picto-delete.png" onClick="deleteConnection(\'' + $(this).attr("contextName") + '\')"></a></td>',
				connected: $(this).attr("connected") == "true" ? '<img border="0" title="Connected" src="images/convertigo-administration-picto-bullet-green.png" />' : '<img border="0" title="Connected" src="images/convertigo-administration-picto-bullet-red.png" />',
				showlogs: '<a href="javascript: getLogs(\'' + $(this).attr("contextName") + '\')"><img src="images/edit.gif" /></a>',
				contextName: $(this).attr("contextName"),
				project: $(this).attr("project"),
				connector: $(this).attr("connector"),
				requested: $(this).attr("requested"),
				status: $(this).attr("status"),
				user: $(this).attr("user"),
				contextCreationDate: $(this).attr("contextCreationDate"),
				contextLastAccessDate: $(this).attr("lastContextAccessDate"),
				contextInactivityTime: $(this).attr("contextInactivityTime"),
				clientComputer: $(this).attr("clientComputer")
			}
		);
	});
}

function filterSession(sessionId) {
	$("#selectedSession").text(sessionId).parent().show();
	connections_List_update();
}

function clearSelectedSession() {
	$("#selectedSession").text("").parent().hide();
	connections_List_update();
}

function toggleSessions() {
	$("#gbox_sessionsList").toggle();
	connections_List_update();
}

function getLogs(contextId, isSession) {
	if (isSession) {
		displayPage("Logs", { filter: "contextid.startsWith(\"" + contextId + "\")" });		
	} else {
		displayPage("Logs", { filter: "contextid == \"" + contextId + "\"" });		
	}
}

function connections_List_update() {
	clearTimeout(connections_List_timeout);
	
	callService("connections.List", function(xml){				
		if ($("#gview_connectionsList").is(":visible")) {
			updateConnectionsList(xml);

			connections_List_timeout = setTimeout(function() {
				connections_List_update();
			}, 2500);
					
		}
	}, {
		session: $("#selectedSession").text(),
		sessions: $("#gbox_sessionsList").is(":visible")
	});
}

function connections_List_init() {
			
	// Update connections list
	$("#sessionsList").jqGrid({
		datatype : "local",
		colNames : [
			'',
			'<img src="images/edit.gif" alt="Show logs"/>',
			'ID',
			'Contexts',
			'User',
			'Roles',
			'UUID',
			'<span title="is FullSync active request">FS</span>',
			'<img src="images/convertigo-administration-picto-last-date.png" alt="Session last access date"/>',
			'<img src="images/convertigo-administration-picto-activity.png" alt="Session inactivity"/>',
			'Client IP'
		],
		colModel : [
			{
				name : 'btnDelete',
				index : 'btnDelete',
				sortable : false,
				width : 20,
				align : "center"
			}, {
				name : 'showlogs',
				index : 'showlogs',
				width : 20,
				align : "center"
			}, {
				name : 'sessionID',
				index : 'sessionID',
				width : 120,
				align : "left"
			}, {
				name : 'contexts',
				index : 'contexts',
				width : 50,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'user',
				index : 'user',
				width : 60,
				align : "left",
				formatter : htmlEncode				
			}, {
				name : 'adminRoles',
				index : 'adminRoles',
				width : 30,
				align : "center",
				formatter : htmlEncode
			}, {
				name : 'deviceUUID',
				index : 'deviceUUID',
				width : 50,
				align : "center",
				formatter : htmlEncode
			}, {
				name : 'isFullSyncActive',
				index : 'isFullSyncActive',
				width : 20,
				align : "center"
			}, {
				name : 'sessionLastAccessDate',
				index : 'sessionLastAccessDate',
				width : 50,
				align : 'left',
				formatter : htmlEncode
			}, {
				name : 'sessionInactivityTime',
				index : 'sessiontInactivityTime',
				width : 50,
				align : 'left',
				formatter : htmlEncode
			}, {
				name : 'clientIP',
				index : 'clientIP',
				width : 60,
				align : "left",
				formatter : htmlEncode
			} ],
			ignoreCase : true,
			autowidth : true,
			viewrecords : true,
			height : 'auto',
			sortable : true,
			pgbuttons : false,
			pginput : false,
			toppager : false,
			emptyrecords : 'No sessions',
			altRows : true,		
			rowNum: '1000000'
			
		});
	
	// Update connections list
	$("#connectionsList").jqGrid({
		datatype : "local",
		colNames : [
		    '',
		    '<img src="images/convertigo-administration-picto-bullet-gray.png" alt="Connector connection status"/>',
		    '<img src="images/edit.gif" alt="Show logs"/>',
		    'Context',
		    'Project',
		    'Connector',
		    'Requested',
		    'Status',
		    'User',
		    '<img src="images/convertigo-administration-picto-creation-date.png" alt="Context creation date"/>',
		    '<img src="images/convertigo-administration-picto-last-date.png" alt="Context last access date"/>',
		    '<img src="images/convertigo-administration-picto-activity.png" alt="Context inactivity"/>',
		    'Client computer'
		],
		colModel : [
	        {
				name : 'btnDelete',
				index : 'btnDelete',
				sortable : false,
				width : 20,
				align : "center"
			}, {
				name : 'connected',
				index : 'connected',						
				width : 20,
				align : "center"
			}, {
				name : 'showlogs',
				index : 'showlogs',						
				width : 20,
				align : "center"
			}, {
				name : 'contextName',
				index : 'contextName',
				width : 120,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'project',
				index : 'project',
				width : 50,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'connector',
				index : 'connector',
				width : 60,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'requested',
				index : 'requested',
				width : 60,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'status',
				index : 'status',
				width : 50,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'user',
				index : 'user',
				width : 60,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'contextCreationDate',
				index : 'contextCreationDate',
				width : 50,
				align : 'left',
				formatter : htmlEncode
			}, {
				name : 'contextLastAccessDate',
				index : 'contextLastAccessDate',
				width : 50,
				align : 'left',
				formatter : htmlEncode
			}, {
				name : 'contextInactivityTime',
				index : 'contextInactivityTime',
				width : 50,
				align : 'left',
				formatter : htmlEncode
			}, {
				name : 'clientComputer',
				index : 'clientComputer',
				width : 100,						
				align : 'left',
				formatter : htmlEncode
			} ],
			ignoreCase : true,
			autowidth : true,
			viewrecords : true,
			height : 'auto',
			sortable : true,
			pgbuttons : false,
			pginput : false,
			toppager : false,
			emptyrecords : 'No connections',
			altRows : true,		
			rowNum: '1000000'
			
		});
	
	$("#connectionsListButtonDeleteAll").button({				
		icons : {
			primary : "ui-icon-closethick"
		}
	}).click(function(){
		showConfirm("Are you sure you want to delete all the sessions?",function(){
			callService("connections.Delete",function(){},{"removeAll": true});
		});					
	});
	
	connections_List_update();
}

function deleteSession(sessionId){	
	showConfirm("Do you want to delete the session: " + sessionId,function(){callService("connections.Delete",function(){},{"sessionId":sessionId})});	
}

function deleteConnection(contextName){	
	showConfirm("Do you want to delete the context: " + contextName,function(){callService("connections.Delete",function(){},{"contextName":contextName})});	
}

