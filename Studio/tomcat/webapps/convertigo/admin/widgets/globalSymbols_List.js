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

function globalSymbols_List_init() {
	var rowIDCell, cellnameCell, valueCell, iRowCell, iColCell;
	
	$("#addSymbol").button({
		icons : {
			primary : "ui-icon-circle-plus"
		}
	}).click(function(){
		addSymbol();
	});
	
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
		exportSymbol();
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
	
	$("#symbolsListButtonDeleteAll").button({				
		icons : {
			primary : "ui-icon-closethick"
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
	
	callService("global_symbols.List", function(xml) {
		$("#symbolsList").jqGrid( {
			datatype : "local",
			colNames : ['Name', 'Value', 'Edit','Delete'],
			colModel : [ {
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
		editSymbol($row.find(">td:eq(0)").text(), $row.find(">td:eq(1)").text());
		return false;
	});
	
	$(document).on("click", ".symbolDelete", function () {
		deleteSymbol($(this).parents("tr:first").find(">td:eq(0)").text());
		return false;
	});
}

function globalSymbols_List_update() {
	callService("global_symbols.List", function(xml) {
		updateGlobalSymbolsList(xml);
	});
}

function updateGlobalSymbolsList(xml) {
	if ($(xml).find("symbol")) {
		$("#symbolsList").jqGrid('clearGridData');
	}
	
	var symbolName = "";
	$(xml).find("symbol").each(function(index) {
		$("#symbolsList").jqGrid(
			"addRowData",
			"symbolsRow" + index,
			{
				name : $(this).attr("name"),
				value : $(this).attr("value"),
				btnEdit : "<a class=\"symbolEdit\" href=\"#edit\"><img border=\"0\" title=\"Edit\" src=\"images/convertigo-administration-picto-edit.png\"></a>",
				btnDelete : "<a class=\"symbolDelete\" href=\"#delete\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>"
			});
	});
	if($("#symbolsList tr:gt(0)").length) {
		$("#symbolsList_name .ui-jqgrid-sortable").click().click();
		$("#symbolsListButtonDeleteAll").button("enable");
	} else {
		$("#symbolsListButtonDeleteAll").button("disable");
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

function editSymbol(symbolName, symbolValue) {
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

function initializeImportSymbol() {
	var actionForm = "services/global_symbols.Import";
	
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
				this._settings.action = this._settings.action+"?"+ $("#dialog-import-symbols").serialize();
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
			globalSymbols_List_update();
		}
	});	
}

function exportSymbol() {
	window.open("services/global_symbols.Export");
}