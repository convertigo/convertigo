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

var xmlOfTheProjectLoaded;
//var loadProject = true;
//var paramsDefined = [];
var $last_project_xml;
var $last_scheduler_xml;
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

function scheduler_ListTasks_init () {
	
	////////////////////////////////////////HELP MANAGEMENT//////////////////////////////////
	$("#helpJobs").attr("href", getHelpUrl("jobs-table/"));
	$("#helpSchedules").attr("href", getHelpUrl("schedules-table/"));
	$("#helpScheduledJobs").attr("href", getHelpUrl("scheduled-jobs-table/"));	
	////////////////////////////////////////INITIALIZATION OF THE TABLE//////////////////////
	$(".scheduledTableData").jqGrid({
		datatype : "local",
		colNames : ["Enabled", "Name", "Description", "Info", "Edit", "Delete"],
		colModel : [ {
				name : "enabled",
				index : "enabled",					
				align : "center",
				width : 12			
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
		ignoreCase : true,
		autowidth : true,
		viewrecords : true,
		height : "auto",
		sortable : true,
		pgbuttons : false,
		pginput : false,
		toppager : false,
		emptyrecords : "No element to display",
		altRows : true,	
		rowNum: '1000000'
	});

	
	$(".scheduledTableDataCron").jqGrid({
		datatype : "local",
		colNames : ["Enabled", "Name", "Description", "Info", "Next", "Edit", "Delete"],
		colModel : [ {
				name : "enabled",
				index : "enabled",					
				align : "center",
				width : 10			
			}, {
				name : "name",
				index : "name",					
				align : "left",
				width : 20
			}, {
				name : "description",
				index : "description",
				align : "left",
				width : 30
			}, {
				name : "info",
				index : "info",
				align : "center",
				width : 20
			}, {
				name : "next",
				classes : "nextCron",
				index : "next",
				align : "center",
				sortable : false,
				width : 20
			}, {
				name : "edit",
				index : "edit",
				align : "center",
				sortable : false,
				width : 8
			}, {
				name : "remove",
				index : "remove",
				align : "center",
				sortable : false,
				width : 8
		} ],
		ignoreCase : true,
		autowidth : true,
		viewrecords : true,
		height : "auto",
		sortable : true,
		pgbuttons : false,
		pginput : false,
		toppager : false,
		emptyrecords : "No element to display",
		altRows : true,	
		rowNum: '1000000'
	});

	
	//////////////////////////////////////////////INITIALISATION OF THE DIALOG OF CREATION/EDITION/////////////////////////// 
	$("#schedulerDialogAddEntry").dialog({
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
	
	$(".schedulerCreationButton").button({
		icons : {
			primary : "ui-icon-clock"
		}
	}).click(function () {
		var id = $(this).attr("id");
		
		/*
		 * jmc 01/06/2012 reset box
		 */ 
		$("#schedulerRequestableParameters").empty();		
		
		fillDialog($empty_element_xml);
		
		display_editor("New Entry", id);
		$last_element_xml = null;
		
		if (id === "schedulerNewScheduledJob") {
			$("#schedulerDialogNameField").val("...@...");
		}
	});
	
	$("#schedulerDialogNewScheduledJobPart").change(function(){	
		$("#schedulerDialogNameField").val($("#schedulerDialogJobNameField").val()+"@"+$("#schedulerDialogScheduleNameField").val());
	});
	


	//////////////////////FILLING THE PROJECT/CONTEXT/SEQUENCE/TRANSACTION FIELDS//////////////////////////////////////////
	
	$("#schedulerProjectSelect").change( function () {	
		var projectName = $(this).val();
		
		var $connectorSelect = $("#schedulerConnectorSelect");
		var $sequenceSelect = $("#schedulerSequenceSelect");
		
		$connectorSelect.add($sequenceSelect).hide().empty().append($("#schedulerTemplate .schedulerEmptyOption").clone());
		
		if (projectName != null && projectName.length > 0) {
			callService("projects.GetTestPlatform", function (xml) {
				$last_project_xml = $(xml);

				var project_xml = $last_project_xml.find("project");
				var type = $("#schedulerDialogAddEntry").attr("window_type");
				project_xml.each(function(){
					var $elt = $(this);
					var projName = $(this).attr("name");
					if (type === "schedulerNewSequenceConvertigoJob") {	
						if ($elt.find("> sequence").length == 0) {
							$("#schedulerProjectSelect").find("option[value='" + projName + "']").remove();
						}
					} 
				});
				
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
			$("#schedulerRequestableAllParameters").hide();
		}
	}).change();
	
	$("#schedulerConnectorSelect").change(function(){			
		var connectorName = $(this).val();
		var $transactionSelect = $("#schedulerTransactionSelect");
		
		$transactionSelect.hide().empty().append($("#schedulerTemplate .schedulerEmptyOption").clone());
		$("#schedulerRequestableParameters").empty();
		
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
		var elementName = $(this).val();
		var isVisible = $(this).is(":visible");
				
		if ($last_project_xml !== null && $last_project_xml !== undefined && isVisible) {
			var $requestable = (($(this).attr("id") === "schedulerTransactionSelect") ?
					$last_project_xml.find("project > connector[name=" + $("#schedulerConnectorSelect").val() + "] > transaction") :
					$last_project_xml.find("project > sequence")).filter("[name=" + elementName + "]");

			$("#schedulerRequestableAllParameters").empty();
			$("#schedulerRequestableParameters").empty();
			
			if ($requestable.find("> variable").length > 0) {
				$("#schedulerRequestableAllParameters").attr("name", "parameters").show();
				$("#schedulerRequestableAllParameters").append("<option name='custom' value='0'>Custom parameters</option>");
			} else {
				$("#schedulerRequestableAllParameters").hide();
				$("#schedulerRequestableParameters").hide();
			}
			
			if ($requestable.find("> testcase").length > 0) {
				$requestable.find("> testcase").each(function () {
					var testcase = $(this).attr('name');
					$("#schedulerRequestableAllParameters").append("<option name='__testcase' value='" + 
							testcase +  "'>Test case: " + testcase +  "</option>");
				});
			} 
			if ($last_element_xml !== null && $last_element_xml !== undefined) {
				var testcase = $last_element_xml.find("parameter[name='__testcase'] > value");
				if (testcase.length == 1 ){
					$("#schedulerRequestableAllParameters").val(testcase.text()).change();
				} else {
					$("#schedulerRequestableAllParameters").val("0").change();
				}
			} else {
				$("#schedulerRequestableAllParameters").val("0").change();
			}
		}
	}).change();
	
	$("#schedulerRequestableAllParameters").change(function(){
		var option = $(this).val();
		var isVisible = $(this).is(":visible");
		
		if (option == "0" && isVisible) {
			
			// test visibility in case of edition because this handler is launch for sequence and transaction select
			if ($last_element_xml === null || isVisible) {
				
				var $requestable = ( $("#schedulerTransactionSelect").is(":visible") ?
						$last_project_xml.find("project > connector[name=" + $("#schedulerConnectorSelect").val() + "] > transaction[name=" + $("#schedulerTransactionSelect").val() + "]") :
						$last_project_xml.find("project > sequence[name=" + $("#schedulerSequenceSelect").val() + "]") );
				
				var $requestableParameters = $("#schedulerRequestableParameters");
				$requestableParameters.hide().find("tr").has("td").remove();
				
				if ($requestable.find("> variable").length > 0) {
					
					$requestable.find("> variable").each(function () {
						var $variable = $(this);
						var isMasked = $variable.attr("isMasked") === "true";
						var isMultiValued = $variable.attr("isMultivalued") === "true";
						var isFileUpload = $variable.attr("isFileUpload") === "true";
						
						var $variable_div = $("#schedulerTemplate .variable").clone();
						$variable_div.find(".variable_name").text($variable.attr("name")).attr("title", $variable.attr("comment"));	
						$variable_div.find(".variable_desc").text($variable.attr("description"));	
						
						var $variable_type = $variable_div.find(".variable_type").data({
							name : $variable.attr("name"),
							isMultiValued : isMultiValued,
							isMasked : isMasked,
							isFileUpload : isFileUpload
						});
						
						if (isMultiValued) {
							var values_array = parseJSONarray($variable.attr("value"));
							$variable_type.append($("#schedulerTemplate .multi_valued").clone());
							
							if ($last_element_xml !== null) {
								$last_element_xml.find("> parameter[name='" + $variable.attr("name") + "']").each(function () {
									var $param = $(this);								
									$param.find("> value").each(function(){
										var $value = $(this);
										$variable_value_type = $("#schedulerTemplate .new_multi_valued").filter(isFileUpload ? ".value_file" : isMasked ? ".value_password" : ".value_text").clone();
										$variable_value_type.find(".variable_value").attr("ismultivalued", "true").attr("name", "requestable_parameter_" + $variable.attr("name")).not("[type=file]").val($value.text());
										$variable_type.append($variable_value_type);
									});
								});
							}
							
						} else {
							var $variable_value_type = $("#schedulerTemplate .single_valued").filter(isFileUpload ? ".value_file" : isMasked ? ".value_password" : ".value_text").clone();
							if ($last_element_xml !== null) {
								var value = "";
								if ($last_element_xml.find("> parameter[name='" + $variable.attr("name") + "']").has("value")) {
									value = $last_element_xml.find("> parameter[name='" + $variable.attr("name") + "']").find("> value").text();
								}
								$variable_value_type.find(".variable_value").attr("name", "requestable_parameter_" + $variable.attr("name")).not("[type=file]").val(value);
							}
							
							$variable_type.append($variable_value_type);
						}

						$requestableParameters.append($variable_div);
					});
					
					$(".link_value_add").on("click", function(){
						var $variable_type = $(this).parents(".variable_type");
						var $variable_multi_new = $("#schedulerTemplate .new_multi_valued").filter($variable_type.data("isFileUpload") ? ".value_file" : $variable_type.data("isMasked") ? ".value_password" : ".value_text").clone();
						$variable_multi_new.find(".variable_value").attr("name", "requestable_parameter_" + $variable_type.data("name")).attr("ismultivalued", "true").val("").change(function () {
							$(this).parents(".requestable").find("a.requestable_link").each(setLinkForRequestable);
						}).change();
						
						$variable_type.append($variable_multi_new);
						
						$(".link_value_remove").on("click", function () {
							var $requestable = $(this).parents(".requestable");
							$(this).parents(".new_multi_valued").remove();
							$requestable.find("a.requestable_link").each(setLinkForRequestable);
							return false;
						});
						
						return false;
					});
					
				} 
				
				$requestableParameters.show();	
			}
		} else {
			$("#schedulerRequestableParameters").hide();
		}
		

	}).change();
	
	//////////////////////////////////////////////////////CRON WIZARD////////////////////////////
	$("#schedulerCronWizardLink").click(function () {
		$("#schedulerCronWizard").slideToggle("fast");
		parseCron();
	});
	
	$("#schedulerCronWizardBtnGenerate").button({
		icons : {
			primary : "ui-icon-gear"
		}
	}).click(function () {
		generateCron();
		$("#schedulerCronWizard").slideUp("fast");
	});
	
	$("#schedulerCronWizardBtnCancel").button({
		icons : {
			primary : "ui-icon-cancel"
		}
	}).click(function () {		
		$("#schedulerCronWizard").slideUp("fast");
	});
	
	//filling cron wizard values
	for (var i = 0; i < 24; i++) {
		$("#schedulerCronWizardHours").append($("<option/>").attr("value", i).text(i));
	}
	for (var i = 0; i < 60 ; i++) {
		$("#schedulerCronWizardMinutes").append($("<option/>").attr("value", i).text(i));
	}
	for (var i = 1; i <= 31; i++) {
		$("#schedulerCronWizardDaysOfMonth").append($("<option/>").attr("value", i).text(i));
	}
	
	
	$("#schedulerNewScheduledJob").button("disable");	
	scheduler_ListTasks_update();
}

function display_editor (optTitle, id) {
	$(".schedulerNewScheduledJob:first").parent().children().add("#schedulerCronWizard").hide();
	if (id === "schedulerNewScheduledJob") {
		$("#schedulerDialogNameField").prop("disabled", true);
	} else {
		$("#schedulerDialogNameField").prop("disabled", false);
	}
	//activate the variable part
	$("." + id).show();	
	$("#schedulerDialogAddEntry").data("openner", id).dialog({ title: optTitle });
	$("#schedulerDialogAddEntry").attr("window_type", id);
	$("#schedulerDialogAddEntry").data("openner", id).dialog("open");
}

function scheduler_ListTasks_update () {
	$(".scheduledTableData, .scheduledTableDataCron").each(function () {
		$(this).jqGrid('clearGridData');	
	});

	$(".schedulerSelect").empty();
		
	callService("scheduler.List", function (xml) {
		var cpt = 1;
		$last_scheduler_xml = $(xml);
		$last_scheduler_xml.find("element").each(function () {
			var $element = $(this);
			var category = $element.attr("category");
			var name = $element.attr("name");
			var enabled = ("true" === $element.attr("enabled"));
			var row;
			if (category === "schedules") {
				var firstCron = "";
				var allCrons = "";
				callService("scheduler.CronCalculator", function (xml) {
					var iter = 0;
					$(xml).find("crons > nextTime").each(function () {
						if (iter === 0) {
							firstCron = $(this).text();
						}
						allCrons += ((iter+1) < 10 ? "0"+(iter+1)+" :  " : (iter+1)+" :  ") + $(this).text() + "\n";
						iter++;
					});
					
					row = {
						enabled : htmlCode($("#schedulerTemplate .schedulerElement" + (enabled ? "Enabled" : "Disabled"))),
						name : htmlEncode(name),
						description : $element.attr("description"),
						info : $element.attr("info"),
						next : firstCron,
						edit : htmlCode($("#schedulerTemplate .schedulerElementEdit")),
						remove : htmlCode($("#schedulerTemplate .schedulerElementDelete"))
					}
					$("#scheduled_" + category).jqGrid("addRowData", cpt, row);	
					$("#scheduled_schedules tr[id='" + (cpt++) + "'] .nextCron[title='" + firstCron + "']").attr("title", allCrons);
					$(".schedulerSelect_" + category).append($("<option/>").text(name));
				}, {input : $element.attr("info"), iteration : "20" });
				
			} else {
				row = {
					enabled : htmlCode($("#schedulerTemplate .schedulerElement" + (enabled ? "Enabled" : "Disabled"))),
					name : htmlEncode(name),
					description : $element.attr("description"),
					info : $element.attr("info"),
					edit : htmlCode($("#schedulerTemplate .schedulerElementEdit")),
					remove : htmlCode($("#schedulerTemplate .schedulerElementDelete"))
				}
				
				$("#scheduled_" + category).jqGrid("addRowData", cpt++, row);			
				$(".schedulerSelect_" + category).append($("<option/>").text(name));
			}
			
		});
		$(".scheduledTableData .schedulerElementEdit, .scheduledTableDataCron .schedulerElementEdit").click(function () {
			$last_element_xml = retrieveElementXml(this);
			if ($last_element_xml.length === 1) {
				fillDialog($last_element_xml);
				display_editor("Edit Entry", "schedulerNew" + $last_element_xml.attr("type"));
			}
		});
		$(".scheduledTableData .schedulerElementDelete, .scheduledTableDataCron .schedulerElementDelete").click(function () {
			$last_element_xml = retrieveElementXml(this);
			showConfirm("Are you sure you want to delete : " + $last_element_xml.attr("name"), function () {
				callService("scheduler.CreateScheduledElements", function () {		
					scheduler_ListTasks_update();
				}, {
					del : true,
					exname : $last_element_xml.attr("name"),
					type : "schedulerNew" + $last_element_xml.attr("type")
				});
			});
		});
		if(jQuery("#scheduled_jobs").getGridParam("records") >0 && jQuery("#scheduled_schedules").getGridParam("records") >0 ){
			$("#schedulerNewScheduledJob").button("enable");
		}else{
			$("#schedulerNewScheduledJob").button("disable");
		}
	});
	
	var $select = $("#schedulerProjectSelect");
	$select.empty().append($("#schedulerTemplate .schedulerEmptyOption").clone());
	callService("projects.List", function (xml) {
		$(xml).find("project").each(function () {
			$select.append($("<option/>").text($(this).attr("name")));
		});
	});
}

function retrieveElementXml(item) {
	var category = $(item).closest("table").attr("id").substring(10); // 10 == "scheduled_".length
	var name = $(item).closest("tr").find("td[aria-describedby=scheduled_" + category + "_name]").text();
	return $last_scheduler_xml.find("element[category=" + category + "]").filter(function () {
		return $(this).attr("name") === name;
	}).first();
}

function fillDialog ($element_xml) {
	for (var i in setting_order) {
		var key = setting_order[i];
		var attr = $element_xml.attr(key);
		if (typeof(attr) !== "undefined") {
			var $input = $("#schedulerDialogAddEntry *[name=" + key + "]");
			if ($input.is("[type=checkbox]")) {
				if ("true" === attr) {
					$input.prop("checked", true);
				} else {
					$input.prop("checked", false);
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
	var $form = $("#schedulerDialogAddEntry");
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
			var isMultiValued = $param.attr("ismultivalued") == "true";
			if ($param.is("[type=checkbox]")) {
				params[name] = $param.prop("checked");
			} else {
				if (name.indexOf("requestable_parameter_") === 0 && !$param.closest("tr").find("input[type=checkbox]").prop("checked")) {
					// ignore parameter not "send"
				} else {
					if (isMultiValued) {
						var multi = [];
						if (typeof params[name] === "object") {
							var x = 0;
							while (x < params[name].length) {
								multi.push(params[name][x]);
								x++;
							}
						}
						multi.push($param.val());
						params[name] = multi;
					} else {
						params[name] = $param.val();
					}
				}
			}
		});
		
		callService("scheduler.CreateScheduledElements", function (xml) {
			var $xml = $(xml);
			var $problems = $("<ul/>");
			$xml.find("problem").each(function () {
				$problems.append($("<li/>").text($(this).text()));
			});
			if ($problems.children().length > 0) {
				showError("There is some issues : " + $("<d/>").append($problems).html());
			} else {
				showInfo("The element was correctly saved.");
				scheduler_ListTasks_update();
				$("#schedulerDialogAddEntry").dialog('close');
			}
		}, params);
}

function setLinkForRequestable(a) {
	var $a = $(this);
	var params = addRequestableData({}, $a.parents(".requestable:first").find(".requestable_name:first"));
	$a.parents(".requestable").find(".variable .variable_value:enabled").each(function () {
		var variable_name = $(this).parents(".variable").find(".variable_name").text();
		if ($.isArray(params[variable_name])) {
			params[variable_name].push($(this).val());
		} else {
			params[variable_name] = [$(this).val()];
		}
	});
	setLink($a, params);
}

function parseJSONarray(value) {
	if (value.length) {
		try {
			return $.parseJSON(value);
		} catch (e) {
			
		}
	}
	return [];
}

function getHelpUrl(help_sub_url) {
	return "http://www.convertigo.com/document/latest/operating-guide/using-convertigo-administration-console/scheduler/" + help_sub_url;
}