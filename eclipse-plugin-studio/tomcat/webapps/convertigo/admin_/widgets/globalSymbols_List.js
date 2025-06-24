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

function globalSymbols_List_init() {
	var rowIDCell, cellnameCell, valueCell, iRowCell, iColCell;
	
	$("#addSymbol").button({
		icons : {
			primary : "ui-icon-plus"
		}
	}).click(addSymbol);
	
	$("#addSymbolSecret").button({
		icons : {
			primary : "ui-icon-key"
		}
	}).click(addSymbolSecret);
	
	initializeImportSymbol();	
	$("#importSymbol").button({
		icons : {
			primary : "ui-icon-arrowthick-1-s"
		}
	});
	$("#exportSymbol").button({
		icons : {
			primary : "ui-icon-arrowthick-1-n"
		}
	}).click(function(){
		exportSymbolButtonsToggle();
	});
	$("#importSymbolUpload").button({
		icons : {
			primary : "ui-icon-document"
		}
	});
	$("#importSymbolCancel").button({
		icons : {
			primary : "ui-icon-circle-close"
		}
	}).click(function(){
		$("#dialog-confirm-symbols").dialog("close");
	});
	
	$("#symbolsList").on("change", ".selected-symbols", function(){
		var symb = $(".selected-symbols:checked");
		if (symb.size() > 0) {
			$("#validExport").button("enable");
		} else {
			$("#validExport").button("disable");
		}
		
		if (symb.size()==$(".selected-symbols").size()){
			$("#selectAll .ui-button-text").text("Deselect all");
		} else {
			$("#selectAll .ui-button-text").text("Select all");
		}
	});
	
	$("#symbolsListButtonDeleteAll").button({				
		icons : {
			primary : "ui-icon-trash"
		}
	}).click(function(){
		showConfirm("Are you sure you want to delete all symbols?", function() {
			callService("global_symbols.DeleteAll", function(xml) {
				var $response = $(xml).find("response:first");  
				if ($response.attr("state") == "success") {
					globalSymbols_List_update();
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
		$(".selected-symbols").prop("checked", 
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
		exportSymbolFile();
	});
	
	$("#cancelExport").button({				
		icons : {
			primary : "ui-icon-cancel"
		}
	}).click(function(){
		hideExportSymbolsPanel();
	});
	
	callService("global_symbols.List", function(xml) {
		$("#symbolsList").jqGrid( {
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
				formatter : htmlEncode
			}, {
				name : 'value',
				index : 'value',
				width : 120,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'btnEdit',
				index : 'btnEdit',
				width : 25,
				sortable : false,
				align : "center"
			}, {
				name : 'btnDelete',
				index : 'btnDelete',
				width : 25,
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
		$("#defaultSymbolsList").jqGrid( {
			datatype : "local",
			colNames : ['Project', 'Name', 'Value', 'Add'],
			colModel : [ {
				name : 'project',
				index : 'project',
				width : 80,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'name',
				index : 'name',
				width : 80,
				align : "left",
				formatter : htmlEncode
			}, {
				name : 'value',
				index : 'value',
				width : 120,
				align : "left",
				formatter : globalSymbolsCheckSecret
			}, {
				name : 'btnAdd',
				index : 'btnAdd',
				width : 25,
				sortable : false,
				align : "center"
			}],
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
		updateGlobalSymbolsList(xml);
	});
	
	$("#dialog-confirm-symbols").dialog({
		resizable: false,
		autoOpen: false,
		modal: true,
		width: 500
	});
	
	$("#importSymbol").click(function () {
		$("#dialog-confirm-symbols").dialog("open");
	});
	
	$(document).on("click", ".symbolEdit", function () {
		var $row = $(this).parents("tr:first");
		editSymbol($row.find(">td:eq(1)").text(), $row.find(">td:eq(2)").text());
		return false;
	});
	
	$(document).on("click", ".symbolDelete", function () {
		deleteSymbol($(this).parents("tr:first").find(">td:eq(1)").text());
		return false;
	});
	
	$(document).on("click", ".defaultSymbolAdd", function () {
		var $row = $(this).parents("tr:first");
		callService("global_symbols.Add", function(xml) {
			var $response = $(xml).find("response:first");
			if ($response.attr("state") == "success") {
				globalSymbols_List_update();
			}
			showInfo($(xml).find("response").attr("message"));
		}, {
			symbolName: $row.find(">td:eq(1)").text(),
			symbolValue: $row.find(">td:eq(2)>span").data("val") || $row.find(">td:eq(2)").text()
		});
		return false;
	});
}

function globalSymbolsCheckSecret(val, col, row) {
	return row.name.endsWith(".secret") ? "**********" + $("<span/>").attr("data-val", val).prop("outerHTML") : htmlEncode(val);
}

function hideExportSymbolsPanel() {
	$(".selected-symbols").prop("checked",false);
	$('#symbolsList').hideCol('checkboxes');
	$('#symbolsList').showCol('btnEdit');
	$('#symbolsList').showCol('btnDelete');
	$("#addSymbol").button("enable");
	$("#addSymbolSecret").button("enable");
	$("#importSymbol").button("enable");
	$("#symbolsListButtonDeleteAll").button("enable");
	$("#exportSymbolsButtonAction").hide();

	$("#validExport").button("disable");
	$("#selectAll .ui-button-text").text("Select all");	
}

function globalSymbols_List_update() {
	callService("global_symbols.List", function(xml) {
		updateGlobalSymbolsList(xml);
	});
	hideExportSymbolsPanel();
}

function updateGlobalSymbolsList(xml) {
	if ($(xml).find("symbol")) {
		$("#symbolsList").jqGrid('clearGridData');
		$("#defaultSymbolsList").jqGrid('clearGridData');
	}
	
	var symbolName = "";
	$(xml).find("symbol").each(function(index) {
		$("#symbolsList").jqGrid(
			"addRowData",
			"symbolsRow" + index,
			{
				checkboxes: "<input type='checkbox' class='selected-symbols' value='"+$(this).attr("name")+"'/>",
				name : $(this).attr("name"),
				value : $(this).attr("value"),
				btnEdit : "<a class=\"symbolEdit\" href=\"#edit\"><img border=\"0\" title=\"Edit\" src=\"images/convertigo-administration-picto-edit.png\"></a>",
				btnDelete : "<a class=\"symbolDelete\" href=\"#delete\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>"
			});
	});
	if($("#symbolsList tr:gt(0)").length) {
		$("#symbolsList").jqGrid().setGridParam({sortname: 'name', sortorder: 'asc'}).trigger("reloadGrid");
		if ($("#exportSymbolsButtonAction").css("display") == "none"){
			$("#symbolsListButtonDeleteAll").button("enable");
		}
	} else {
		$("#symbolsListButtonDeleteAll").button("disable");
	}
	var $defaultSymbolsDiv = $("#defaultSymbolsList").parents(".ui-jqgrid:first")

	if ($(xml).find("defaultSymbol").length) {
		$(xml).find("defaultSymbol").each(function(index) {
			$("#defaultSymbolsList").jqGrid(
				"addRowData",
				"defaultSymbolsRow" + index,
				{
					project: $(this).attr("project"),
					name : $(this).attr("name"),
					value : $(this).attr("value"),
					btnAdd : "<a class=\"defaultSymbolAdd\" href=\"#defaultadd\"><img border=\"0\" title=\"Add\" src=\"images/convertigo-administration-picto-add.png\"></a>"
				});
		});
		$defaultSymbolsDiv.show();
		$defaultSymbolsDiv.prev().show();
	} else {
		$defaultSymbolsDiv.hide();
		$defaultSymbolsDiv.prev().hide();
	}
}

function deleteSymbol(symbolName) {
	$('<div></div>').text("Do you really want to delete the symbol '" + symbolName + "'?")
		.dialog({
			autoOpen : true,
			title : "Confirmation",
			modal : true,
			buttons : {
				Yes : function() {
					callService("global_symbols.Delete", function(xml) {
						var $response = $(xml).find("response:first");  
						if ($response.attr("state") == "success") {
							$("#addName").val("");
							$("#addValue").val("");
							globalSymbols_List_update();
						}
						showInfo($(xml).find("response").attr("message"));
					}, {symbolName: symbolName});
					$(this).dialog('close');
				},
				No : function() {
					$(this).dialog('close');
					return false;
				}
			}
		});
}

function addSymbol(xml, mode) {
	$("#addName").val("");
	$("#addValue").val("");
	
	$("#dialog-add-symbol").dialog({
		autoOpen : true,
		title : "Add symbol",
		modal : true,
		buttons : {
			"Ok" : function () {
				callService("global_symbols.Add", function(xml) {
					var $response = $(xml).find("response:first");  
					if ($response.attr("state") == "success") {
						$("#addName").val("");
						$("#addValue").val("");
						globalSymbols_List_update();
					}
					showInfo($(xml).find("response").attr("message"));
				}, {symbolName: $("#addName").val(), symbolValue: $("#addValue").val()});
			},
			Cancel : function() {
				$(this).dialog('close');
				return false;
			}
		}
	});
}

function addSymbolSecret(xml, mode) {
	$("#addNameSecret").prop("disabled", false).val("");
	$("#addValueSecret").val("");
	
	$("#dialog-add-symbol-secret").dialog({
		autoOpen : true,
		title : "Add secret symbol",
		modal : true,
		buttons : {
			"Ok" : function () {
				callService("global_symbols.Add", function(xml) {
					var $response = $(xml).find("response:first");  
					if ($response.attr("state") == "success") {
						$("#addNameSecret").val("");
						$("#addValueSecret").val("");
						globalSymbols_List_update();
					}
					showInfo($(xml).find("response").attr("message"));
				}, {symbolName: $("#addNameSecret").val() + ".secret", symbolValue: $("#addValueSecret").val()});
			},
			Cancel : function() {
				$(this).dialog('close');
				return false;
			}
		}
	});
}

function editSymbol(symbolName, symbolValue) {
	if (symbolName.endsWith(".secret")) {
		editSymbolSecret(symbolName, symbolValue);
		return;
	}
	$("#addName").val(symbolName);
	$("#addValue").val(symbolValue);
	
	$("#dialog-add-symbol").dialog({
			autoOpen : true,
			title : "Edit symbol",
			modal : true,
			buttons : {
				"Ok" : function() {
					callService("global_symbols.Edit", function(xml) {
						var $response = $(xml).find("response:first");  
						if ($response.attr("state") == "success") {
							globalSymbols_List_update();
							$("#dialog-add-symbol").dialog("close");
						}
						showInfo($(xml).find("response").attr("message"));
					}, {oldSymbolName: symbolName, symbolName: $("#addName").val(), symbolValue: $("#addValue").val()});
				},
				Cancel : function() {
					$(this).dialog("close");
					return false;
				}
			}
		}
	);
}

function editSymbolSecret(symbolName, symbolValue) {
	$("#addNameSecret").prop("disabled", true).val(symbolName.replace(/\.secret$/, ""));
	$("#addValueSecret").val(symbolValue);
	
	$("#dialog-add-symbol-secret").dialog({
			autoOpen : true,
			title : "Edit secret symbol",
			modal : true,
			buttons : {
				"Ok" : function() {
					if (symbolValue == $("#addValueSecret").val()) {
						showInfo("You haven't change the value!");
						return;
					}
					callService("global_symbols.Edit", function(xml) {
						var $response = $(xml).find("response:first");  
						if ($response.attr("state") == "success") {
							globalSymbols_List_update();
							$("#dialog-add-symbol-secret").dialog("close");
						}
						showInfo($(xml).find("response").attr("message"));
					}, {oldSymbolName: symbolName, symbolName: $("#addNameSecret").val() + ".secret", symbolValue: $("#addValueSecret").val()});
				},
				Cancel : function() {
					$(this).dialog("close");
					return false;
				}
			}
		}
	);
}

function initializeImportSymbol() {
	var actionForm = "services/global_symbols.Import?__xsrfToken=" + encodeURIComponent(getXsrfToken());
	
	var ajaxUpload = new AjaxUpload("importSymbolUpload", {
		action : actionForm,
		responseType : "xml",		
		onSubmit : function(file, ext) {
			$("#dialog-confirm-symbols").dialog("close");
			var str = ".properties";
			if (file.match(str + "$") != str) {
				showError("<p>The global symbols file '" + file + "' is not a valid properties file</p>");
				return false;
			} else {
				this._settings.action = this._settings.action + "&" + $("#dialog-import-symbols").serialize();
			}
			startWait(50);
		},
		onComplete : function(file, response) {
			this._settings.action = actionForm;
			clearInterval(this.tim_progress);
			endWait();
			if ($(response).find("error").length > 0) {
				showError("An unexpected error occurs.", $(response).text());
			} else {
				showInfo($(response).text());
				$("").dialog("close");
			}
			globalSymbols_List_update();
		}
	});	
}

function exportSymbolButtonsToggle() {
	var status;
	$("#validExport").button("disable");
	$(".selected-symbols").prop("checked",false);
	
	if ($("#exportSymbolsButtonAction").css("display") == "block"){
		status = "enable";
		$('#symbolsList').hideCol('checkboxes');
		$('#symbolsList').showCol('btnEdit');
		$('#symbolsList').showCol('btnDelete');
	} else {
		status = "disable";
		$('#symbolsList').showCol('checkboxes');
		$('#symbolsList').hideCol('btnEdit');
		$('#symbolsList').hideCol('btnDelete');

		$("#validExport").button("disable");
		$("#selectAll .ui-button-text").text("Select all");	
	}
	//Disable buttons from buttons bar
	$("#addSymbol").button(status);
	$("#addSymbolSecret").button(status);
	$("#importSymbol").button(status);
	$("#symbolsListButtonDeleteAll").button(status);

	//hide/show the second buttons bars
	$("#exportSymbolsButtonAction").toggle();
}

function exportSymbolFile(){
	var symbols = [];

	$(".selected-symbols:checked").each(function() {
		symbols.push({name: $(this).prop('value')});
	});
	
	ajaxCall("POST", "services/global_symbols.Export", "text", {symbols: JSON.stringify(symbols)}, function (data) {
		var blob = new Blob([data], {type: "text/plain"});
		if (window.navigator.msSaveOrOpenBlob) {
			window.navigator.msSaveOrOpenBlob(blob, "global_symbols.properties");
		} else {
			var a = document.createElement("a");
	        var url = window.URL.createObjectURL(blob);
	        a.href = url;
	        a.download = "global_symbols.properties";
	        a.click();
	        window.URL.revokeObjectURL(url);
		}
	});
}