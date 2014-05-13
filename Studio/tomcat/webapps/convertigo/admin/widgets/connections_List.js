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

var params="";
var desc="&desc";

function updateConnectionsList(xml) {
	$("#connectionsContextsInUse").html($(xml).find("contextsInUse").text());
	$("#connectionsContextsNumber").html($(xml).find("contextsNumber").text());
	$("#connectionsThreadsInUse").html($(xml).find("threadsInUse").text());
	$("#connectionsThreadsNumber").html($(xml).find("threadsNumber").text());
	$("#connectionsHttpTimeout").html($(xml).find("httpTimeout").text());

	$("#connectionsList").jqGrid('clearGridData');

	$(xml).find("connection").each(function(index) {
		projectName = $(this).attr("name");
		$("#connectionsList").jqGrid("addRowData", $(this).attr("contextName"), {
				btnDelete: '<a title="Delete the connection"><img border="0" src="images/convertigo-administration-picto-delete.png" onClick="deleteConnection(\'' + $(this).attr("contextName") + '\')"></a></td>',
				connected: $(this).attr("connected") == "true" ? '<img border="0" title="Connected" src="images/convertigo-administration-picto-bullet-green.png" />' : '<img border="0" title="Connected" src="images/convertigo-administration-picto-bullet-red.png" />',
				contextName: '<a href="javascript: getLogs(\'' + $(this).attr("contextName") + '\')">' + $(this).attr("contextName") + '</a>',
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

function getLogs(contextId) {
	displayPage("Logs", { filter: "contextid == \"" + contextId + "\"" });
}

function connections_List_update() {
	callService("connections.List", function(xml){				
		if ($("#gview_connectionsList").is(":visible")) {
			
			updateConnectionsList(xml);

			setTimeout(function() {
				connections_List_update();
			}, 1000);
					
		}
	});
}

function connections_List_init() {
	callService("connections.List",
	    function(xml){			
			$("#connectionsContextsInUse").html($(xml).find("contextsInUse").text());
			$("#connectionsContextsNumber").html($(xml).find("contextsNumber").text());
			$("#connectionsThreadsInUse").html($(xml).find("threadsInUse").text());
			$("#connectionsThreadsNumber").html($(xml).find("threadsNumber").text());
			$("#connectionsHttpTimeout").html($(xml).find("httpTimeout").text());
			
			// Update connections list
			$("#connectionsList").jqGrid({
				datatype : "local",
				colNames : [
				    '',
				    '<img src="images/convertigo-administration-picto-bullet-gray.png" alt="Connector connection status"/>',
				    'Context',
				    'Project',
				    'Connector',
				    'Requested',
				    'Status',
				    'User',
				    '<img src="images/convertigo-administration-picto-creation-date.png" alt="Context creation date"/>',
				    '<img src="images/convertigo-administration-picto-last-date.png" alt="Context last access date"/>',
				    '<img src="images/convertigo-administration-picto-activity.png" alt="Copntext inactivity"/>',
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
						name : 'contextName',
						index : 'contextName',
						width : 120,
						align : "left"
					}, {
						name : 'project',
						index : 'project',
						width : 50,
						align : "left"
					}, {
						name : 'connector',
						index : 'connector',
						width : 60,
						align : "left"
					}, {
						name : 'requested',
						index : 'requested',
						width : 60,
						align : "left"
					}, {
						name : 'status',
						index : 'status',
						width : 50,
						align : "left"
					}, {
						name : 'user',
						index : 'user',
						width : 60,
						align : "left"
					}, {
						name : 'contextCreationDate',
						index : 'contextCreationDate',
						width : 50,
						align : 'left'
					}, {
						name : 'contextLastAccessDate',
						index : 'contextLastAccessDate',
						width : 50,
						align : 'left'
					}, {
						name : 'contextInactivityTime',
						index : 'contextInactivityTime',
						width : 50,
						align : 'left'
					}, {
						name : 'clientComputer',
						index : 'clientComputer',
						width : 100,						
						align : 'left'
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
				showConfirm("Are you sure you want to delete all the connections?",function(){
					$("#connectionsList tr:gt(0)").each(function(){					
						callService("connections.Delete",function(){},{"contextName":$(this).attr('id')});
					});
				});					
			});
			
			connections_List_update();
	    }
	);
}

function setParams(sortParam){	
	if(desc.length>0){
		desc="";
	}
	else{
		desc="&desc";
	}	
	params="sortParam="+sortParam+desc;	
	
}

function deleteConnection(contextName){	
	showConfirm("Do you want to delete the connection",function(){callService("connections.Delete",function(){},{"contextName":contextName})});	
}

