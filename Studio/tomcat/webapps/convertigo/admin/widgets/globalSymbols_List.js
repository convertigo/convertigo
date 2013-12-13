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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/tomcat/webapps/convertigo/admin/widgets/globalSymbols_List.js $
 * $Author: rahmanf $
 * $Revision: 28390 $
 * $Date: 2012-06-22 16:52:53 +0200 (Wed, 22 Jun 2012) $
 */

function globalSymbols_List_init() {
	$("#updateSymbols").button({
		icons : {
			primary : "ui-icon-disk"
		}
	});
	$("#addSymbol").button({
		icons : {
			primary : "ui-icon-circle-plus"
		}
	});
	$("#updateSymbols").button("disable");
	
	callService("global_symbols.List", function(xml) {
		$("#symbolsList").jqGrid( {
			datatype : "local",
			colNames : ['Name', 'Value', 'Delete'],
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
				name : 'btnDelete',
				index : 'btnDelete',
				width : 20,
				sortable : false,
				align : "center"
			} ],
			autowidth : true,
			cellEdit : true,
			cellsubmit : 'clientArray',
			viewrecords : true,
			height : 'auto',
			sortable : true,
			pgbuttons : false,
			pginput : true,
			toppager : false,
			altRows : false,			
			rownumbers : false,
			afterEditCell: function() {
				$("#updateSymbols").button("disable")
	            e = jQuery.Event("keydown");
	            e.keyCode = $.ui.keyCode.ENTER;
	            edit = $(".edit-cell > *");
	            edit.blur(function() {
	            	//Simulate press ENTER event
	                edit.trigger(e);
	                $("#updateSymbols").button("enable");
	            });
	        }
		});
		
		updateGlobalSymbolsList(xml);
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
											btnDelete : "<a href=\"javascript: deleteSymbol('"
													+ symbolName
													+ "')\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>",
										});
					});
}

function deleteSymbol(symbolName) {
		$('<div></div>').html("<p>Do you really want to delete the symbol '" + symbolName + "'?</p>")
				.dialog(
						{
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

										$("#updateSymbols").button("enable");
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
		$("#dialog-add-symbol").dialog(
				{
					autoOpen : true,
					title : "Add symbol",
					modal : true,
					buttons : {
						"Add" : function() {
							$(this).dialog('close');
							var symbolName = $("#addName").val();
							var value = $("#addValue").val();
							
							if (symbolName && value) {
								var add = $("#symbolsList").jqGrid(
										"addRowData",
										symbolName,
										{
											name : symbolName,
											value : value,
											btnDelete : "<a href=\"javascript: deleteSymbol('"
													+ symbolName
													+ "')\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>",
											btnEdit : "<a href=\"javascript: editSymbol('"
													+ symbolName
													+ "')\"><img border=\"0\" title=\"Edit\" src=\"images/convertigo-administration-picto-edit.png\"></a>",
										});
								if (add) {
									showInfo("The symbol '" + symbolName
											+ "' has been successfully added.");
	
									$("#updateSymbols").button("enable");
									$("#addName").val("");
									$("#addValue").val("");
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

function updateSymbol() {
	var $symbol = $("#symbolsList").find('.ui-widget-content');
	var nbSymbol = $symbol.length;
	var symbols = [];
	$symbol.each(function(index) {
		var name = $($symbol.eq(index)).children("td")[0];
		var value = $($symbol.eq(index)).children("td")[1];
		symbols.push(name.title+"="+value.title);
	});
	
	$.post("services/global_symbols.Update",{
		symbols : symbols}, 
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
		});	
}


