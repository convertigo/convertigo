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

function projects_List_init() {
	$("#projectsDeploy").button({
		icons : {
			primary : "ui-icon-circle-plus"
		}
	});

	callService("projects.List", function(xml) {
		$("#projectsList").jqGrid( {
			datatype : "local",
			colNames : [ 'Name', 'Comment', 'Version', 'Exported', 'Deployment', 'Delete', 'Reload', 'Edit', 'Export', 'Test' ],
			colModel : [ {
				name : 'name',
				index : 'name',
				width : 80,
				align : "left"
			}, {
				name : 'comment',
				index : 'comment',
				width : 120,
				align : "left"
			}, {
				name : 'version',
				index : 'version',
				width : 40,
				align : "left"
			}, {
				name : 'exported',
				index : 'exported',
				width : 50,
				align : "left"
			}, {
				name : 'deployDate',
				index : 'deployDate',
				width : 50,
				align : "left"
			}, {
				name : 'btnDelete',
				index : 'btnDelete',
				width : 20,
				sortable : false,
				align : "center"
			}, {
				name : 'btnReload',
				index : 'btnReload',
				width : 20,
				sortable : false,
				align : "center"
			}, {
				name : 'btnEdit',
				index : 'btnEdit',
				width : 20,
				sortable : false,
				align : "center"
			}, {
				name : 'btnExport',
				index : 'btnExport',
				width : 20,
				sortable : false,
				align : "center"
			}, {
				name : 'btnTest',
				index : 'btnTest',
				width : 20,
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
			emptyrecords : 'No projects',
			altRows : true,		
			rowNum : 10000
		});
		updateProjectsList(xml);
	});

	var ajaxUpload = new AjaxUpload("projectsDeploy", {
		action : "services/projects.Deploy",			
		responseType : "xml",		
		onSubmit : function(file, ext) {			
			this._settings.action = "services/projects.Deploy?bAssembleXsl=" + $("#projectsAssembleXsl").is(":checked");
			var str = ".car";
			if (file.match(str + "$") != str) {
				showError("<p>The project '" + file + "' is not a valid archive (*.car)</p>");
				return false;
			}
	
			startWait(50);
		},
		onComplete : function(file, response) {
			clearInterval(this.tim_progress);
			endWait();
			if ($(response).find("error").length > 0) {
				showError("<p>An unexpected error occurs.</p>", $(response).text());
			} else {
				showInfo($(response).text());
				projects_List_update();
			}
		}
	});	
}

function projects_List_update() {
	callService("projects.List", function(xml) {
		updateProjectsList(xml);
	});
}

function updateProjectsList(xml) {
	$("#projectsList").jqGrid('clearGridData');
	var projectName = "";
	$(xml)
			.find("project")
			.each(
					function(index) {
						projectName = $(this).attr("name");
						$("#projectsList")
								.jqGrid(
										"addRowData",
										projectName,
										{
											name : projectName,
											comment : $(this).attr("comment"),
											version : $(this).attr("version"),
											exported : $(this).attr("exported"),
											deployDate : $(this).attr("deployDate"),
											btnDelete : "<a href=\"javascript: deleteProject('"
													+ projectName
													+ "')\"><img border=\"0\" title=\"Delete the project\" src=\"images/convertigo-administration-picto-delete.png\"></a>",
											btnReload : "<a href=\"javascript: reloadProject('"
													+ projectName
													+ "')\"><img border=\"0\" title=\"Reload the project\" src=\"images/convertigo-administration-picto-reload.png\"></a>",
											btnEdit : "<a href=\"javascript: editProject('"
													+ projectName
													+ "')\"><img border=\"0\" title=\"Edit the project\" src=\"images/convertigo-administration-picto-edit.png\"></a>",
											btnExport : "<a href=\"services/projects.Export?projectName="
												+ projectName
												+ "\"><img border=\"0\" title=\"Make CAR archive from the project\" src=\"images/convertigo-administration-picto-save.png\"></a>",
											btnTest : "<a target=\"_new\" href=\"../project.html#" + projectName
												+ "\"><img border=\"0\" title=\"Test the project\" src=\"images/convertigo-administration-picto-test-platform.png\"></a>"
										});
					});
}

function deleteProject(projectName) {
	$("#project_Edit").hide();
	$('<div></div>').html("<p>Do you really want to delete the project '" + projectName + "'?</p>")
			.dialog(
					{
						autoOpen : true,
						title : "Confirmation",
						modal : true,
						buttons : {
							Yes : function() {
								$(this).dialog('close');
								startWait(50);
								$.get("services/projects.Delete", {
									projectName : projectName
								}, function(xml) {
									endWait();
									if ($(xml).find("error").length != 0) {
										var message = $(xml).find("message").text()
										var stacktrace = $(xml).find("stacktrace").text()
										showError(message, stacktrace);
									} else {
										showInfo("The project '" + projectName
												+ "' has been successfully deleted.");
									}
									$("#projectsList").jqGrid("delRowData", projectName);
								});
								return false;
							},
							No : function() {
								$(this).dialog('close');
								return false;
							}
						}
					});
}

function reloadProject(projectName) {
	$("#project_Edit").hide();
	$('<div></div>').html("<p>Do you really want to reload the project '" + projectName + "'?</p>")
			.dialog(
					{
						autoOpen : true,
						title : "Confirmation",
						modal : true,
						buttons : {
							Yes : function() {
								$(this).dialog('close');
								startWait(50);
								$.get("services/projects.Reload", {
									projectName : projectName
								}, function(xml) {
									endWait();
									if ($(xml).find("error").length != 0) {
										var message = $(xml).find("message").text()
										var stacktrace = $(xml).find("stacktrace").text()
										showError(message, stacktrace);
									} else {
										showInfo("The project '" + projectName
												+ "' has been successfully reloaded.");
									}
								});
								return false;
							},
							No : function() {
								$(this).dialog('close');
								return false;
							}
						}
					});
}

function editProject(projectName) {
	//see projectEdit.js
	loadProject(projectName);
}
