/*
 * Copyright (c) 2001-2012 Convertigo SA.
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

var xmlOfTheProjectLoaded;
//var loadProject = true;
//var paramsDefined = [];
var $last_project_xml;
var $last_schedulers_xml;
var $last_element_xml = null;
var $empty_element_xml = $("<element/>")
	.attr("name", "")
	.attr("enabled", "true")
	.attr("description", "")
	.attr("jobName", "")
	.attr("scheduleName", "")
	.attr("serial", "false")
	.attr("writeOutput", "false")
	.attr("cron", "0 0 0 * * ?")
	.attr("context", "")
	.attr("project", "");
var setting_order = ["name", "enabled", "description", "jobName", "scheduleName", "serial", "writeOutput", "cron", "context", "project"];

function schedulers_ListTasks_init () {
	////////////////////////////////////////INITIALIZATION OF THE TABLE//////////////////////
	$(".scheduledTableData").jqGrid({
		datatype : "local",
		colNames : ["Enabled", "Name", "Description", "Info", "Edit", "Delete"],
		colModel : [ {
				name : "enabled",
				index : "enabled",					
				align : "center",
				width : 10			
			}, {
				name : "name",
				index : "name",					
				align : "left",
				width : 30
			}, {
				name : "description",
				index : "description",
				align : "left",
				width : 50
			}, {
				name : "info",
				index : "info",
				align : "center",
				width : 50
			}, {
				name : "edit",
				index : "edit",
				align : "center",
				sortable : false,
				width : 10
			}, {
				name : "remove",
				index : "remove",
				align : "center",
				sortable : false,
				width : 10
		} ],
		autowidth : true,
		viewrecords : true,
		height : "auto",
		sortable : true,
		pgbuttons : false,
		pginput : false,
		toppager : false,
		emptyrecords : "No element to display",
		altRows : true,	
		rownumbers : false
	});
	
	//////////////////////////////////////////////INITIALISATION OF THE DIALOG OF CREATION/EDITION/////////////////////////// 
	$("#schedulersDialogAddEntry").dialog({
		autoOpen : false,
		width : 495,		
		title : "New entry",		
		modal : true,
		buttons : {
			Save : saveElement,
			Cancel : function () {
				$(this).dialog('close');
				return false;
			}
		}
	});	
	
	$(".schedulersCreationButton").button({
		icons : {
			primary : "ui-icon-clock"
		}
	}).click(function () {
		var id = $(this).attr("id");
		
		fillDialog($empty_element_xml);
		
		display_editor("New Entry", id);
		$last_element_xml = null;
		
		if (id === "schedulersNewScheduledJob") {
			$("#schedulersDialogNameField").val("...@...");
		}
	});
	
	$("#schedulersDialogNewScheduledJobPart").change(function(){	
		$("#schedulersDialogNameField").val($("#schedulerDialogJobNameField").val()+"@"+$("#schedulerDialogScheduleNameField").val());
	});
	
	

	//////////////////////FILLING THE PROJECT/CONTEXT/SEQUENCE/TRANSACTION FIELDS//////////////////////////////////////////
	
	$("#schedulerProjectSelect").change( function () {	
		var projectName = $(this).val();
		
		var $connectorSelect = $("#schedulerConnectorSelect");
		var $sequenceSelect = $("#schedulerSequenceSelect");
		
		$connectorSelect.add($sequenceSelect).hide().empty().append($("#schedulersTemplate .schedulersEmptyOption").clone());
		
		if (projectName != null && projectName.length > 0) {
			callService("projects.GetRequestables", function (xml) {
				$last_project_xml = $(xml);
				$last_project_xml.find("project > connector").each(function () {
					$connectorSelect.append($("<option/>").text($(this).attr("name")));			
				});
				$last_project_xml.find("project > sequence").each(function () {
					$sequenceSelect.append($("<option/>").text($(this).attr("name")));			
				});
				if ($last_element_xml != null) {
					var attr = $last_element_xml.attr("sequence");
					if (typeof(attr) !== "undefined") {
						$sequenceSelect.val(attr);
					} else {
						$connectorSelect.val($last_element_xml.attr("connector"));
					}
				}
				$connectorSelect.add($sequenceSelect).show().change();
			},{projectName : projectName});		
		} else {
			$connectorSelect.change();	
		}
	}).change();
	
	$("#schedulerConnectorSelect").change(function(){			
		var connectorName = $(this).val();
		
		var $transactionSelect = $("#schedulerTransactionSelect");
		
		$transactionSelect.hide().empty().append($("#schedulersTemplate .schedulersEmptyOption").clone());
		
		if (connectorName != null && connectorName.length > 0) {
			$last_project_xml.find("project > connector[name=" + connectorName + "] > transaction").each(function () {
				$transactionSelect.append($("<option/>").text($(this).attr("name")));
			});
			if ($last_element_xml !== null) {
				$transactionSelect.val($last_element_xml.attr("transaction"));
			}
			$transactionSelect.show();
		}
		$transactionSelect.change();
	}).change();
	
	$("#schedulerTransactionSelect, #schedulerSequenceSelect").change(function () {
		// test visibility in case of edition because this handler is launch for sequence and transaction select
		if ($last_element_xml === null || $(this).is(":visible")) {
			var requestableName = $(this).val();
			var $requestableParameters = $("#schedulerRequestableParameters");
			
			$requestableParameters.hide().find("tr").has("td").remove();
			
			if (requestableName != null && requestableName.length > 0) {
				var $requestable = (($(this).attr("id") === "schedulerTransactionSelect") ?
					$last_project_xml.find("project > connector[name=" + $("#schedulerConnectorSelect").val() + "] > transaction") :
					$last_project_xml.find("project > sequence")).filter("[name=" + requestableName + "]");
				
				$requestable.find("> variable").each(function () {
					var $variable = $(this);
					var $row = $("#schedulersTemplate .schedulersRequestableParameterRow").clone();
					$row.find(".schedulersRequestableParameterName").text($variable.attr("name"));
					$row.find(".schedulersRequestableParameterDescription").text($variable.attr("description"));
					$row.find(".schedulersRequestableParameterValue").attr("name", "requestable_parameter_" + $variable.attr("name")).val($variable.attr("value"));
					$requestableParameters.append($row);
				});
				
				if ($last_element_xml !== null) {
					$requestableParameters.find("input[type=checkbox]").attr("checked", null);
					$last_element_xml.find(">parameter").each(function () {
						var $input = $requestableParameters.find("input[name=requestable_parameter_" + $(this).attr("name") + "]");
						$input.val($(this).text());
						$input.closest("tr").find("input[type=checkbox]").attr("checked", "checked");
					});
				}
				
				$requestableParameters.show();
			}
		}
	}).change();
	
	//////////////////////////////////////////////////////CRON WIZARD////////////////////////////
	$("#schedulersCronWizardLink").click(function () {
		$("#schedulersCronWizard").slideToggle("fast");
		parseCron();
	});
	
	$("#schedulersCronWizardBtnGenerate").button({
		icons : {
			primary : "ui-icon-gear"
		}
	}).click(function () {
		generateCron();
		$("#schedulersCronWizard").slideUp("fast");
	});
	
	$("#schedulersCronWizardBtnCancel").button({
		icons : {
			primary : "ui-icon-cancel"
		}
	}).click(function () {		
		$("#schedulersCronWizard").slideUp("fast");
	});
	
	//filling cron wizard values
	for (var i = 0; i < 24; i++) {
		$("#schedulersCronWizardHours").append($("<option/>").attr("value", i).text(i));
	}
	for (var i = 0; i < 60 ; i++) {
		$("#schedulersCronWizardMinutes").append($("<option/>").attr("value", i).text(i));
	}
	for (var i = 1; i <= 31; i++) {
		$("#schedulersCronWizardDaysOfMonth").append($("<option/>").attr("value", i).text(i));
	}
	
	
	$("#schedulersNewScheduledJob").button("disable");	
	schedulers_ListTasks_update();
}

function display_editor (optTitle, id) {
	$(".schedulersNewScheduledJob:first").parent().children().add("#schedulersCronWizard").hide();
	if (id === "schedulersNewScheduledJob") {
		$("#schedulersDialogNameField").attr("disabled", "disabled");
	} else {
		$("#schedulersDialogNameField").removeAttr("disabled");
	}
	//activate the variable part
	$("." + id).show();	
	$("#schedulersDialogAddEntry").data("openner", id).dialog({ title: optTitle });
	$("#schedulersDialogAddEntry").data("openner", id).dialog("open");
}

function schedulers_ListTasks_update () {
	$(".scheduledTableData").each(function () {
		$(this).jqGrid('clearGridData');	
	});
	
	$(".schedulersSelect").empty();
		
	callService("schedulers.List", function (xml) {
		var cpt = 1;
		$last_schedulers_xml = $(xml);
		$last_schedulers_xml.find("element").each(function () {
			var $element = $(this);
			var category = $element.attr("category");
			var name = $element.attr("name");
			var enabled = ("true" === $element.attr("enabled"));
			var row = {
				enabled : htmlCode($("#schedulersTemplate .schedulersElement" + (enabled ? "Enabled" : "Disabled"))),
				name : htmlEncode(name),
				description : $element.attr("description"),
				info : $element.attr("info"),
				edit : htmlCode($("#schedulersTemplate .schedulersElementEdit")),
				remove : htmlCode($("#schedulersTemplate .schedulersElementDelete"))
			}
			$("#scheduled_" + category).jqGrid("addRowData", cpt++, row);			
			$(".schedulersSelect_" + category).append($("<option/>").text(name));
		});
		$(".scheduledTableData .schedulersElementEdit").click(function () {
			$last_element_xml = retrieveElementXml(this);
			if ($last_element_xml.length === 1) {
				fillDialog($last_element_xml);
				display_editor("Edit Entry", "schedulersNew" + $last_element_xml.attr("type"));
			}
		});
		$(".scheduledTableData .schedulersElementDelete").click(function () {
			$last_element_xml = retrieveElementXml(this);
			showConfirm("Are you sure you want to delete : " + $last_element_xml.attr("name"), function () {
				callService("schedulers.CreateScheduledElements", function () {		
					schedulers_ListTasks_update();
				}, {
					del : true,
					exname : $last_element_xml.attr("name"),
					type : "schedulersNew" + $last_element_xml.attr("type")
				});
			});
		});
		if(jQuery("#scheduled_jobs").getGridParam("records") >0 && jQuery("#scheduled_schedules").getGridParam("records") >0 ){
			$("#schedulersNewScheduledJob").button("enable");
		}else{
			$("#schedulersNewScheduledJob").button("disable");
		}
	});

	var $select = $("#schedulerProjectSelect");
	$select.empty().append($("#schedulersTemplate .schedulersEmptyOption").clone());
	callService("projects.List", function (xml) {
		$(xml).find("project").each(function () {
			$select.append($("<option/>").text($(this).attr("name")));
		});
	});
}

function retrieveElementXml(item) {
	var category = $(item).closest("table").attr("id").substring(10); // 10 == "scheduled_".length
	var name = $(item).closest("tr").find("td[aria-describedby=scheduled_" + category + "_name]").text();
	return $last_schedulers_xml.find("element[category=" + category + "]").filter(function () {
		return $(this).attr("name") === name;
	}).first();
}

function fillDialog ($element_xml) {
	for (var i in setting_order) {
		var key = setting_order[i];
		var attr = $element_xml.attr(key);
		if (typeof(attr) !== "undefined") {
			var $input = $("#schedulersDialogAddEntry *[name=" + key + "]");
			if ($input.is("[type=checkbox]")) {
				if ("true" === attr) {
					$input.attr("checked", "checked");
				} else {
					$input.removeAttr("checked");
				}
			} else {
				$input.val(attr).change();	
			}
		}
	}
	
	var jobs_group = $element_xml.find("job_group_member").map(function () {
		return $(this).text();
	});
	$("#schedulerDialogGroupJobField").val(jobs_group);
}

function saveElement () {
	var $form = $("#schedulersDialogAddEntry");
		var params = {
			type : $form.data("openner")
		};
		if ($last_element_xml != null) {
			params.exname = $last_element_xml.attr("name");
			params.edit = true;
		}
		$form.find(":visible[name]").each(function () {
			var $param = $(this);
			var name = $param.attr("name");
			if ($param.is("[type=checkbox]")) {
				params[name] = $param.is(":checked");
			} else {
				if (name.indexOf("requestable_parameter_") === 0 && !$param.closest("tr").find("input[type=checkbox]").is(":checked")) {
					// ignore parameter not "send"
				} else {
					params[name] = $param.val();
				}
			}
		});
		callService("schedulers.CreateScheduledElements", function (xml) {
			var $xml = $(xml);
			var $problems = $("<ul/>");
			$xml.find("problem").each(function () {
				$problems.append($("<li/>").text($(this).text()));
			});
			if ($problems.children().length > 0) {
				showError("There is some issues : " + $("<d/>").append($problems).html());
			} else {
				showInfo("The element was correctly saved.");
				schedulers_ListTasks_update();
				$("#schedulersDialogAddEntry").dialog('close');
			}
		}, params);
}