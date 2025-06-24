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

function environmentVariables_List_init(gridId) {
	// Check if the parameter is sent
	var grid = $(typeof(gridId) == "undefined" ? "#environmentVariablesList" : gridId).jqGrid( {
		datatype : "local",
		colNames : ['Name', 'Value'],
		colModel : [ {
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
	environmentVariables_List_update(gridId);
}
	
function environmentVariables_List_update(gridId) {
	var grid = $(typeof(gridId) == "undefined" ? "#environmentVariablesList" : gridId);
	grid.jqGrid("clearGridData", true);
	callService("engine.GetEnvironmentVariablesList", function(xml) {
		var environmentVariableName = "";
		$(xml).find("environmentVariable").each(function(index) {
			grid.jqGrid(
				"addRowData",
				"environmentVariablesRow" + index,
				{
					name : $(this).attr("name"),
					value : $(this).attr("value")
				});
		});
		grid.jqGrid().setGridParam({sortname: 'name', sortorder: 'asc'}).trigger("reloadGrid");
	});
}