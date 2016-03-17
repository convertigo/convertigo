/*
 * Copyright (c) 2001-2014 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/tomcat/webapps/convertigo/admin/widgets/globalSymbols_List.js $
 * $Author: rahmanf $
 * $Revision: 28390 $
 * $Date: 2012-06-22 16:52:53 +0200 (Wed, 22 Jun 2012) $
 */

var $last_roles_list_xml;

function roles_List_init() {
	var rowIDCell, cellnameCell, valueCell, iRowCell, iColCell;
	
	$("#addUser").button({
		icons : {
			primary : "ui-icon-plus"
		}
	}).click(function(){
		addUser();
	});
	
	initializeImportUser();	
	$("#importUser").button({
		icons : {
			primary : "ui-icon-arrowthick-1-s"
		}
	});
	$("#exportUser").button({
		icons : {
			primary : "ui-icon-arrowthick-1-n"
		}
	}).click(function(){
		exportUserButtonsToggle();
	});
	$("#importUserUpload").button({
		icons : {
			primary : "ui-icon-document"
		}
	});
	$("#importUserCancel").button({
		icons : {
			primary : "ui-icon-circle-close"
		}
	}).click(function(){
		$("#dialog-confirm-users").dialog("close");
	});
	
	$("#usersList").on("change", ".selected-users", function(){
		var symb = $(".selected-users:checked");
		if (symb.size() > 0) {
			$("#validExport").button("enable");
		} else {
			$("#validExport").button("disable");
		}
		
		if (symb.size()==$(".selected-users").size()){
			$("#selectAll .ui-button-text").text("Deselect all");
		} else {
			$("#selectAll .ui-button-text").text("Select all");
		}
	});
	
	$("#usersListButtonDeleteAll").button({				
		icons : {
			primary : "ui-icon-trash"
		}
	}).click(function(){
		showConfirm("Are you sure you want to delete all users?", function() {
			callService("roles.DeleteAll", function(xml) {
				var $response = $(xml).find("response:first");  
				if ($response.attr("state") == "success") {
					roles_List_update();
				}
				showInfo($(xml).find("response").attr("message"));
			});
		});					
	});
	
	//hidden buttons bars
	$("#selectAll").button({				
		icons : {
			primary : "ui-icon-close"
		}
	}).click(function(){
		$(".selected-users").prop("checked", 
				$("#selectAll .ui-button-text").text() == "Select all" ? true : false );
		$("#validExport").button(
				$("#selectAll .ui-button-text").text() == "Select all" ? "enable" : "disable");
		$("#selectAll .ui-button-text").text(
				$("#selectAll .ui-button-text").text() == "Select all" ? "Deselect all" : "Select all");
		
	});
	
	$("#validExport").button({				
		icons : {
			primary : "ui-icon-check"
		}
	}).click(function(){
		exportUserFile();
	});
	
	$("#cancelExport").button({				
		icons : {
			primary : "ui-icon-cancel"
		}
	}).click(function(){
		hideExportUsersPanel();
	});
	
	$("#check_view").button({
		icons : {
			primary : "ui-icon-plus"
		}
	}).click(function(){
		$("#roles input[name$='_VIEW']").prop("checked", true);
	});
	
	$("#check_config").button({
		icons : {
			primary : "ui-icon-plus"
		}
	}).click(function(){
		$("#roles input[name$='_CONFIG']").prop("checked", true);
	});
	
	$("#uncheck_view").button({
		icons : {
			primary : "ui-icon-minus"
		}
	}).click(function(){
		$("#roles input[name$='_VIEW']").prop("checked", false);
	});
	
	$("#uncheck_config").button({
		icons : {
			primary : "ui-icon-minus"
		}
	}).click(function(){
		$("#roles input[name$='_CONFIG']").prop("checked", false);
	});
	
	callService("roles.List", function(xml) {
		$last_roles_list_xml = $(xml);
		$("#roles").empty();
		var $tr;
		$(xml).find(">admin>roles>role").each(function (i) {
			var name = $(this).attr("name");
			if (i%2 == 0) {
				$tr = $("<tr/>").appendTo("#roles");
			}
			$tr.append(
				'<td title="' + $(this).attr("description") + '">'
				+ '<input type="checkbox" name="' + name + '" id="c' + name + '"/>'
				+ '<label for="c' + name + '">' + name + '</label>' + 
				'</td>'
			);
		});
		$("#usersList").jqGrid( {
			datatype : "local",
			colNames : ['', 'Name', 'Value', 'Edit','Delete'],
			colModel : [ {
				name : 'checkboxes',
				index : 'checkboxes',
				hidden : true,
				width : 8,
				align : "center"
			}, {
				name : 'name',
				index : 'name',
				width : 80,
				align : "left",
				formatter : $.jgrid.htmlEncode
			}, {
				name : 'value',
				index : 'value',
				width : 120,
				align : "left",
				formatter : $.jgrid.htmlEncode
			}, {
				name : 'btnEdit',
				index : 'btnEdit',
				width : 10,
				sortable : false,
				align : "center"
			}, {
				name : 'btnDelete',
				index : 'btnDelete',
				width : 20,
				sortable : false,
				align : "center"
			} ],
			ignoreCase : true,
			autowidth : true,
			cellEdit : false,
			viewrecords : true,
			height : 'auto',
			sortable : true,
			pgbuttons : true,
			pginput : true,
			toppager : false,
			altRows : false,	
			rowNum: '1000000'
		});
		updateUsersList(xml);
	});
	
	$("#dialog-confirm-users").dialog({
		resizable: false,
		autoOpen: false,
		modal: true,
		width: 500
	});
	
	$("#importUser").click(function () {
		$("#dialog-confirm-users").dialog("open");
	});
	
	$(document).on("click", ".userEdit", function () {
		var $row = $(this).parents("tr:first");
		editUser($row.find(">td:eq(1)").text());
		return false;
	});
	
	$(document).on("click", ".userDelete", function () {
		deleteUser($(this).parents("tr:first").find(">td:eq(1)").text());
		return false;
	});
}

function hideExportUsersPanel() {
	$(".selected-users").prop("checked",false);
	$('#usersList').hideCol('checkboxes');
	$('#usersList').showCol('btnEdit');
	$('#usersList').showCol('btnDelete');
	$("#addUser").button("enable");
	$("#importUser").button("enable");
	$("#usersListButtonDeleteAll").button("enable");
	$("#exportUsersButtonAction").hide();

	$("#validExport").button("disable");
	$("#selectAll .ui-button-text").text("Select all");	
}

function roles_List_update() {
	callService("roles.List", function(xml) {
		updateUsersList(xml);
	});
	hideExportUsersPanel();
}

function updateUsersList(xml) {
	if ($(xml).find("user")) {
		$("#usersList").jqGrid("clearGridData");
	}
	
	var username = "";
	$(xml).find("user").each(function(index) {
		var roles = $(this).find("role").map(function(){
			return $(this).attr("name")
		}).get();
		$("#usersList").jqGrid(
			"addRowData",
			"usersRow" + index,
			{
				checkboxes: "<input type='checkbox' class='selected-users' value='"+$(this).attr("name")+"'/>",
				name : $(this).attr("name"),
				value : roles,
				btnEdit : "<a class=\"userEdit\" href=\"#edit\"><img border=\"0\" title=\"Edit\" src=\"images/convertigo-administration-picto-edit.png\"></a>",
				btnDelete : "<a class=\"userDelete\" href=\"#delete\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>"
			});
	});
	if($("#usersList tr:gt(0)").length) {
		$("#usersList_name .ui-jqgrid-sortable").click().click();
		if ($("#exportUsersButtonAction").css("display") == "none"){
			$("#usersListButtonDeleteAll").button("enable");
		}
	} else {
		$("#usersListButtonDeleteAll").button("disable");
	}
}

function deleteUser(username) {
	$('<div></div>').text("Do you really want to delete the user '" + username + "'?")
		.dialog({
			autoOpen : true,
			title : "Confirmation",
			modal : true,
			buttons : {
				Yes : function() {
					callService("roles.Delete", function(xml) {
						var $response = $(xml).find("response:first");  
						if ($response.attr("state") == "success") {
							$("#username").val("");
							$("#password").val("");
							roles_List_update();
						}
						showInfo($(xml).find("response").attr("message"));
					}, {username: username});
					$(this).dialog('close');
				},
				No : function() {
					$(this).dialog('close');
					return false;
				}
			}
		});
}

function addUser(xml, mode) {
	$("#username").val("");
	$("#password").val("").parent().attr("title", "Cannot be empty");
	$("#roles input").prop("checked", false);
	$("#dialog-add-user").dialog({
			autoOpen : true,
			title : "Add user",
			modal : true,
			minWidth: 400,
			buttons : {
				"Ok" : function () {
					var roles = $("#roles input:checked").map(function(){
						return $(this).attr("name")
					}).get();
					callService("roles.Add", function(xml) {
						var $response = $(xml).find("response:first");  
						if ($response.attr("state") == "success") {
							$("#username").val("");
							$("#password").val("");
							roles_List_update();
						}
						showInfo($(xml).find("response").attr("message"));
					}, {username: $("#username").val(), password: $("#password").val(), roles: roles});
				},
				Cancel : function() {
					$(this).dialog('close');
					return false;
				}
			}
		});
}

function editUser(username) {
	$("#username").val(username);
	$("#roles input").prop("checked", false);
	var $user = $last_roles_list_xml.find("user[name='" + username + "']");
	$user.find("role").each(function () {
		$("#c" + $(this).attr("name")).prop("checked", true);
	});
	$("#password").val("").parent().attr("title", "Empty to not change the password");
	
	$("#dialog-add-user").dialog({
			autoOpen : true,
			title : "Edit user",
			modal : true,
			minWidth: 400,
			buttons : {
				"Ok" : function() {
					roles = $("#roles input:checked").map(function(){
						return $(this).attr("name")
					}).get();
					callService("roles.Edit", function(xml) {
						var $response = $(xml).find("response:first");  
						if ($response.attr("state") == "success") {
							roles_List_update();
							$("#dialog-add-user").dialog("close");
						}
						showInfo($(xml).find("response").attr("message"));
					}, {oldUsername: username, username: $("#username").val(), password: $("#password").val(), roles: roles});
				},
				Cancel : function() {
					$(this).dialog("close");
					return false;
				}
			}
		}
	);
}

function initializeImportUser() {
	var actionForm = "services/roles.Import";
	
	var ajaxUpload = new AjaxUpload("importUserUpload", {
		action : actionForm,			
		responseType : "xml",		
		onSubmit : function(file, ext) {
			$("#dialog-confirm-users").dialog("close");
			var str = ".json";
			if (file.match(str + "$") != str) {
				showError("<p>The users file '" + file + "' is not a valid db file</p>");
				return false;
			} else {
				this._settings.action = this._settings.action+"?"+ $("#dialog-import-users").serialize();
			}
			startWait(50);
		},
		onComplete : function(file, response) {
			this._settings.action = actionForm;
			clearInterval(this.tim_progress);
			endWait();
			if ($(response).find("error").length > 0) {
				showError("<p>An unexpected error occurs.</p>", $(response).text());
			} else {
				showInfo($(response).text());
				$("").dialog("close");
			}
			roles_List_update();
		}
	});	
}

function exportUserButtonsToggle() {
	var status;
	$("#validExport").button("disable");
	$(".selected-users").prop("checked",false);
	
	if ($("#exportUsersButtonAction").css("display") == "block"){
		status = "enable";
		$('#usersList').hideCol('checkboxes');
		$('#usersList').showCol('btnEdit');
		$('#usersList').showCol('btnDelete');
	} else {
		status = "disable";
		$('#usersList').showCol('checkboxes');
		$('#usersList').hideCol('btnEdit');
		$('#usersList').hideCol('btnDelete');

		$("#validExport").button("disable");
		$("#selectAll .ui-button-text").text("Select all");	
	}
	//Disable buttons from buttons bar
	$("#addUser").button(status);
	$("#importUser").button(status);
	$("#usersListButtonDeleteAll").button(status);

	//hide/show the second buttons bars
	$("#exportUsersButtonAction").toggle();
}

function exportUserFile(){
	var userstoExport = "";

	$(".selected-users:checked").each(function(index) {
		if (userstoExport.length != 0) {
			userstoExport += ",";	
		} 
		userstoExport += "{ 'name' : "+$(this).prop('value')+" }";
	});

	window.open("services/roles.Export?users=" + 
			userstoExport  );
}