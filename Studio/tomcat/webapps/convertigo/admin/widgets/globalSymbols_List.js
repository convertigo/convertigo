/*
 * Copyright (c) 2001-2011 Convertigo SA.
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
			primary : "ui-icon-arrowthick-1-n"
		}
	});
	$("#exportSymbol").button({
		icons : {
			primary : "ui-icon-arrowthick-1-s"
		}
	}).click(function(){
		exportSymbol();
	});
	
	$("#updateSymbols").button("disable");
	
	$("#symbolsListButtonDeleteAll").button({				
		icons : {
			primary : "ui-icon-closethick"
		}
	}).click(function(){
		showConfirm("Are you sure you want to delete all symbols?",function(){
			$("#symbolsList tr:gt(0)").each(function(){			
				$("#symbolsList").jqGrid('delRowData',$(this).attr('id'));
			});
			updateSymbol();
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
				editable : true
			}, {
				name : 'value',
				index : 'value',
				width : 120,
				align : "left",
				editable : true
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
			autowidth : true,
			cellEdit : false,
//			cellsubmit : 'clientArray',
			viewrecords : true,
			height : 'auto',
			sortable : true,
			pgbuttons : true,
			pginput : true,
			toppager : false,
			altRows : false,	
			rowNum: '1000000',
			afterEditCell: function(rowid, cellname, value, iRow, iCol) {	
				$("#updateSymbols").button("disable")
	            e = jQuery.Event("keydown");
	            e.keyCode = $.ui.keyCode.ENTER;
	            edit = $(".edit-cell > *");
	            edit.blur(function() {
	            	//Simulate press ENTER event
	                edit.trigger(e);
	                if (rowid==rowIDCell && cellname==cellnameCell && value!=valueCell && iRow==iRowCell && iCol==iColCell) {
	                	$("#updateSymbols").button("enable");
	                } else {
	                	rowIDCell = "";
						cellnameCell = "";
						valueCell = "";
						iRowCell = "";
						iColCell = "";
	                }
	            });
	        },
	        beforeEditCell: function(rowid, cellname, value, iRow, iCol) {
	        	rowIDCell = rowid;
	        	cellnameCell = cellname;
	        	valueCell = value;
	        	iRowCell = iRow;
	        	iColCell = iCol;
	        }
		});
		updateGlobalSymbolsList(xml);
		
		if($("#symbolsList tr:gt(0)").length>0){
			$("#symbolsListButtonDeleteAll").button("enable");
		} else {
			$("#symbolsListButtonDeleteAll").button("disable");
		}
	});
	
	//When press ENTER
	$("#symbolsList").change(function() {
		$("#updateSymbols").button("enable");
	});
}

function globalSymbols_List_update() {
	callService("global_symbols.List", function(xml) {
		updateGlobalSymbolsList(xml);
	});
}

function updateGlobalSymbolsList(xml) {
	if ($(xml).find("symbol"))
		$("#symbolsList").jqGrid('clearGridData');
	
	var symbolName = "";
	$(xml)
			.find("symbol")
			.each(
					function(index) {
						symbolName = $(this).attr("name");
						$("#symbolsList")
								.jqGrid(
										"addRowData",
										symbolName,
										{
											name : symbolName,
											value : $(this).attr("value"),
											btnEdit : "<a href=\"javascript: editSymbol('"
												+ symbolName
												+ "','"
												+ $(this).attr("value")
												+"')\"><img border=\"0\" title=\"Edit\" src=\"images/convertigo-administration-picto-edit.png\"></a>",
											btnDelete : "<a href=\"javascript: deleteSymbol('"
													+ symbolName
													+ "')\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>"
										});
					});
}

function deleteSymbol(symbolName) {
		$('<div></div>').html("<p>Do you really want to delete the symbol '" + symbolName + "'?</p>")
			.dialog({
				autoOpen : true,
				title : "Confirmation",
				modal : true,
				buttons : {
					Yes : function() {
						$(this).dialog('close');
						var del = $("#symbolsList").jqGrid('delRowData',symbolName);
						
						if (del) {
							showInfo("The symbol '" + symbolName
									+ "' has been successfully deleted.");

							updateSymbol();
						} else {
							showError("Allready deleted or not in list");
						}
						return false;
					},
					No : function() {
						$(this).dialog('close');
						return false;
					}
				}
			});
}

function addSymbol(xml) {
	$("#addName").val("");
	$("#addValue").val("");
	
	$("#dialog-add-symbol").dialog({
			autoOpen : true,
			title : "Add symbol",
			modal : true,
			buttons : {
				"Add" : function() {
					var symbolName = $("#addName").val();
					var value = $("#addValue").val();
					value = value.replace(">","&gt;").replace("<","&lt;");
					
					if (symbolName && value) {
						var add = $("#symbolsList").jqGrid(
							"addRowData",
							symbolName,	{
								name : symbolName,
								value : value,
								btnDelete : "<a href=\"javascript: deleteSymbol('"
										+ symbolName
										+ "')\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>",
								btnEdit : "<a href=\"javascript: editSymbol('"
										+ symbolName
										+ "','"
										+ value
										+"')\"><img border=\"0\" title=\"Edit\" src=\"images/convertigo-administration-picto-edit.png\"></a>",
							});
						if (add) {
							updateSymbol();
							showInfo("The symbol '" + symbolName + "' has been successfully added.");
						} else {
							showError("Can not update");
						}
					} else {
						showInfo("Please enter name and value"); 
					}

					return false;
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
				"Edit" : function() {
					var name = $("#addName").val();
					var value = $("#addValue").val();
					value = value.replace(">","&gt;").replace("<","&lt;");
					
					if (name && value) {
						//delete old symbol
						var del = $("#symbolsList").jqGrid('delRowData',symbolName);				
						if (del) {
							//add new symbol
							var edit = $("#symbolsList").jqGrid(
								"addRowData",
								symbolName, {
									name : name,
									value : value,
									btnDelete : "<a href=\"javascript: deleteSymbol('"
											+ name
											+ "')\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>",
									btnEdit : "<a href=\"javascript: editSymbol('"
											+ name
											+ "','"
											+ value
											+"')\"><img border=\"0\" title=\"Edit\" src=\"images/convertigo-administration-picto-edit.png\"></a>",
								});
							if (edit) {
								updateSymbol();
								showInfo("The symbol has been successfully updated.");
							} else {
								showError("Can not update");
							}
						} else {
							showError("Can not update");
						}

					} else {
						showInfo("Please enter name and value"); 
					}
					$(this).dialog('close');
				},
				Cancel : function() {
					$(this).dialog('close');
					return false;
				}
			}
		}
	);
}

function updateSymbol() {
	var $symbol = $("#symbolsList").find('.ui-widget-content');
	var nbSymbol = $symbol.length;
	var symbols = [];
	var	isempty = nbSymbol < 1;
	$symbol.each(function(index) {
		var name = $($symbol.eq(index)).children("td")[0];
		var value = $($symbol.eq(index)).children("td")[1];
		symbols.push(name.title+"="+value.title);
	});

	$.post("services/global_symbols.Update",
		{symbols : symbols, isempty : isempty}, 
		function(xml){
			if($(xml).find("error").length != 0) {
				var message = $(xml).find("message").text()
				var stacktrace = $(xml).find("stacktrace").text()
				showError(message, stacktrace);
			} else {
				showInfo("The global symbols file has been successfully updated!");
				$("#updateSymbols").button("disable");
				globalSymbols_List_update();
			}

			globalSymbols_List_init();
		});
}

function initializeImportSymbol() {

	var ajaxUpload = new AjaxUpload("importSymbol", {
		action : "services/global_symbols.Import",			
		responseType : "xml",		
		onSubmit : function(file, ext) {			
			var str = ".properties";
			if (file.match(str + "$") != str) {
				showError("<p>The global symbols file '" + file + "' is not a valid properties file</p>");
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
				$("").dialog("close");
			}
			globalSymbols_List_update();
		}
	});	
}

function exportSymbol() {
	window.open( "services/global_symbols.Export");
}