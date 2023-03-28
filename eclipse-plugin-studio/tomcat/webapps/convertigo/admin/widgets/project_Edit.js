/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

$.getScript('js/jquery/tree.jquery.js');

function project_Edit_init() {
	project_Edit_update();
	$('#projectEditObjectSubmitProperties').button( {
		icons : {
			primary : "ui-icon-disk"
		}
	}).click(function() {
		projectEditObjectSubmitProperties();
	});
	$('#projectEditUndefinedSymbolsInfoSubmit').button().click(function() {
		projectDeclareGlobalSymbols();
	});
	
	$('#projectEditStatsSubmit').button( {
		icons : {
			primary : "ui-icon-clipboard"
		}
	}).click(function() {
		projectStats();
	});
	
	$("#dialog-symbol-project").dialog({
		autoOpen : false,
		title : "set a value using symbols",
		width: 400,
		modal : true,
		buttons: [{
			id: "btn-symbol-OK",
			text: "OK",
			click: function() {
				var newVal = $("#symbol-input").val();
				$(this).dialog("close").data("option").text(newVal).parent().val(newVal).data("lastVal", newVal);
			}
		}, {
			id: "btn-symbol-Cancel",
			text: "Cancel",
			click: function() {
				var $select = $(this).dialog("close").data("option").parent();
				$select.val($select.data("lastVal"));
			}
		}]
	});
}

function project_Edit_update() {
	$("#project_Edit").hide();
}

function loadProjectGSymbol(projectName){
	project_Name = projectName;
	callService("projects.GetUndefinedSymbols", function(xml) {
		if ($(xml).find("undefined_symbols")) {
			var htmlCode = "<h2><img border=\"0\" class=\"iconAlertGlobalSymbols\" title=\"Click here to create undefined global symbols\" src=\"images/convertigo-administration-alert-global-symbols.png\"> Undefined Global Symbols</h2>";
			htmlCode += "<p>Find here the undefined Global Symbols for this project: </p>";
			htmlCode += "<ul>";
			$(xml).find("undefined_symbols").children().each(function() {
				htmlCode += "<li>" + htmlEncode($(this).text()) + "</li>";
			});
			htmlCode += "</ul>";
			
			$("#projectEditUndefinedSymbolsInfoList").html(htmlCode);
			if ($("#projectEditUndefinedSymbolsInfoList ul:has(li)").length == 0) {
				$("#projectEditUndefinedSymbolsInfoList").html("");
				$("#projectEditUndefinedSymbolsInfo").hide();
			} else {
				$("#projectEditUndefinedSymbolsInfo").show();
			}
		}else {
			$("#projectEditUndefinedSymbolsInfo").hide();
		}
	}, 
	{"projectName":project_Name});
}

// This variable contains the XML DOM returned by the database_objects.Get service
var xmlDatabaseObject;
var project_Name;
var treeItemId = 0;

function dboXmlToJson($xml) {
	var o = {};
	$xml.children().each(function() {
		var $this = $(this);
		var c = $this.attr("category");
		var a = c in o ? o[c] : (o[c] = []);
		var j = {
			id: treeItemId++,
			name: $this.attr("name"),
			qname: $this.attr("qname"),
			beanClass: $this.attr("beanClass"),
			comment: $this.attr("comment")
		};
		a.push(j);
		if ($this.attr("hasChildren") == "true") {
			j.children = dboXmlToJson($this);
		}
	});
	var ks = Object.keys(o);
	if (Object.keys(o).length == 0) {
		o = [];
	} else  if (Object.keys(o).length == 1) {
		o = Object.values(o)[0];
	} else {
		o = Object.keys(o).map(k => ({
			id: treeItemId++,
			name: k,
			children: o[k]
		}));	
	}
	return o;
}

function loadProject(projectName) {
	project_Name = projectName;
	startWait(30);
	$("#projectEditDivJQTree").tree("destroy");
	callService("studio.database_objects.GetChildren", function(xml) {
		// set the title of the widget
		$("#project_Edit h3").first().text("Project " + projectName);
		
		$("#project_Edit").show();
		
		var data = dboXmlToJson($(xml.documentElement));
		$("#projectEditDivJQTree").tree({
			data: data,
			onCreateLi: function(node, $li, isSelected) {
				if ('beanClass' in node) {
					$li.find('.jqtree-title').prepend('<img src="services/database_objects.GetIcon?__xsrfToken=' + encodeURIComponent(getXsrfToken()) + '&className=' + node.beanClass + '" />');
					$li.attr('title', node.comment);
				} else {
					$li.find('.jqtree-title').prepend('<img src="images/folder.gif" />');
				}
			}
		}).on("tree.open", function(e) {
			e.node.children.forEach(function(c) {
				if ('qname' in c) {
					callService("studio.database_objects.GetChildren", function(xml) {
						var subdata = dboXmlToJson($(xml).find("dbo:first"));
						$("#projectEditDivJQTree").tree("loadData", subdata, c);
					}, {qname: c.qname});
				}
			});
		}).on("tree.click", function(e) {
			$("#projectEditDivJQTree").tree('openNode', e.node);
			if ('qname' in e.node) {
				loadElement(e.node.qname);
			}
		});
		window.setTimeout(() => {
			$("#projectEditDivJQTree .jqtree-toggler:first").click().remove();
			endWait();
		}, 10);
	}, {qname: project_Name});
}

function loadElement(elementQName, $treeitem) {
	
	callService("database_objects.Get", function(xml) {
		
		var $projectEditObjectPropertiesListTable = $("#projectEditTemplate .projectEditPropertyTable").clone();
		var $xml = $(xml);
		var caroleOdd = true;
		$projectEditObjectPropertiesListTable.find(".projectEditObjectType").text($xml.find("admin > *").first().attr("displayName"));
		$projectEditObjectPropertiesListTable.find(".projectEditObjectVersion").text($xml.find("admin > *").first().attr("version"));
		$projectEditObjectPropertiesListTable.find(".projectEditObjectName").text($xml.find("property[name=name] > *").first().attr("value"));
		$projectEditObjectPropertiesListTable.find(".projectEditObjectName").text($xml.find("property[name=name] > *").first().attr("value"));
		
		var properties = $xml.find("property[isHidden!=true]").get().sort(function (p1, p2) {
			var e1 = p1.getAttribute("isExpert") == "true";
			var e2 = p2.getAttribute("isExpert") == "true";
			if (e1 == e2) {
				var n1 = p1.getAttribute("displayName");
				var n2 = p2.getAttribute("displayName");
				if (n1 == n2) {
					return 0;
				} else {
					return n1 < n2 ? -1 : 1;
				}
			} else {
				return e1 == true ? 1 : -1;
			}
		});
		
		var $expertLine = $projectEditObjectPropertiesListTable.find("tr:last");
		
		$(properties).each(
				function() {
					if ($expertLine != null && $(this).attr("isExpert") == "true") {
						$expertLine = null;
					}
					
					var $propertyLine_xml;
					$propertyLine_xml = $("#projectEditTemplate .projectEdit-propertyLine").clone();
					if(caroleOdd)
						$propertyLine_xml.attr("class", "main_odd");
					else{
						$propertyLine_xml.attr("class", "main_even");
					}
					caroleOdd =! caroleOdd;
					var short_description = 
						$(this).attr("shortDescription")
						.replace(/\|[\s\S]*/g, "")
						.replace(/{{.*?}}/g,"")
						.replace(/\*\*\*/g," ");
					
					//transformation of {{{value}}}
					var long_description = 
						$(this).attr("shortDescription")
						.replace(/\|/g, "<br/><br/>")
						.replace(/{{Computer}}/g,"<span class=longDescriptionComputer>")
						.replace(/{{-Computer}}/g,"</span>")
						.replace(/{{Reference}}/g,"<span class=longDescriptionReference>")
						.replace(/{{-Reference}}/g,"</span>")
						.replace(/{{Produit\/Fonction}}/g,"<span class=longDescriptionProductFonction>")
						.replace(/{{-Produit\/Fonction}}/g,"</span>");
					//transformation of the lists
					if(long_description.match(/\*\*\*/)!=null){
						//list which don't end at the end of the description
						if(long_description.match(/(\*\*\*[^\n]*\n)/)!=null){
							//creation of ul from the first *** from \n
							long_description = long_description.replace(/(\*\*\*[^\n]*\n)/g,"<ul>$1</ul>");
						}
						//creation list which end at the end of the description
						long_description = long_description.replace(/(\*\*\*.*$)/,"<ul>$1</ul>");
						//replace each ***
						long_description  = long_description.replace(/(\*\*\*)/g,"</li><li>");
						//correct the beginning and the end of each list
						long_description  = long_description.replace(/<ul><\/li>/g,"<ul>");
						long_description  = long_description.replace(/<\/ul>/g,"</li></ul>");
					}
										
					$propertyLine_xml.find("td").first().text($(this).attr("displayName"));
					$propertyLine_xml.find("td").attr("title",short_description);
					$propertyLine_xml.find("td > img").data("long_description",long_description);
					$propertyLine_xml.append(addProperty($(this)));
					if ($expertLine != null) {
						$expertLine.before($propertyLine_xml);
					} else {
						$projectEditObjectPropertiesListTable.append($propertyLine_xml);
					}
				});
		
		$("#projectEditObjectPropertiesList").html($projectEditObjectPropertiesListTable);
		$(".projectEditorPropertyHelpIcon > img").click(function(){
			showInfoHtml($(this).data("long_description"));
		});
		xmlDatabaseObject = xml;
	}, {"qname":elementQName});
}

function addProperty($xmlProperty) {

	var propertyName = $xmlProperty.attr("name");
	var editor = $xmlProperty.attr("editorClass");
	var $ResponseOfPropertyTd=$("<td/>").attr("class","projectEditorPropertyFieldValue");
		
	if ($xmlProperty.children().get(0).nodeName == "xmlizable") {
		return $ResponseOfPropertyTd.append(addVectorProperty(propertyName, editor, $xmlProperty));
	}

	// TODO handle array and table
	if ($xmlProperty.children().first().get(0).nodeName == "com.twinsoft.convertigo.beans.common.XMLVector"
			|| $xmlProperty.children().get(0).nodeName == "array"
			|| $xmlProperty.children().get(0).nodeName == "com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer") {

		return $ResponseOfPropertyTd.text("NOT AVAILABLE");
	}
	
	// find each element without children
	$xmlProperty.find("*").not(":has(*)").each(function() {
		$ResponseOfPropertyTd.append(addPropertyContent(propertyName, editor, $(this), $xmlProperty));
	});

	return $ResponseOfPropertyTd;
}

function addPropertyContent(propertyName, propertyEditor, $xmlPropertyValue, $xmlProperty) {
	
	var propertyJavaClassName = $xmlPropertyValue.get(0).nodeName;
	var $propertyContent = $("<container/>");

	if (propertyJavaClassName == "java.lang.Boolean" || propertyJavaClassName == "java.lang.String" ||
			propertyJavaClassName == "java.lang.Long" || propertyJavaClassName == "java.lang.Integer") {

		var value = $xmlPropertyValue.attr("value");
		var $responseField;
		var $option;
		var $possibleValues = $xmlProperty.find("possibleValues");
		
		if (propertyJavaClassName == "java.lang.Boolean" || $xmlPropertyValue.attr("compiledValueClass") == "java.lang.Boolean") {
			$possibleValues = $("#projectEditTemplate .projectEditInputTrueFalse");
		}
		
		if (propertyEditor == "SqlQueryEditor") {
			$responseField=getInputCopyOf("projectEditTextArea");
			$responseField
			.attr("cols", "80")
			.attr("rows", "20")
			.text(value)
			.data("propertyName",propertyName);
			
			
		} else if (value.length > 100) {
			$responseField=getInputCopyOf("projectEditTextArea");
			$responseField
			.attr("cols", "60")
			.attr("rows", "3")
			.text(value)
			.data("propertyName",propertyName);
			
			
		} else if ($possibleValues.length > 0){
			$responseField = getInputCopyOf("projectEditInputCombo");
			$option = $responseField.find("option").clone();
			$responseField.children().remove();
			var i = 0;
			$possibleValues.find("value").each(function(){
				if (propertyJavaClassName == "java.lang.Integer") {
					$responseField.append($option.clone().text($(this).text()).attr("value", "" + i));
					i++;
				} else {
					$responseField.append($option.clone().text($(this).text()));
				}
			});
			$responseField.val(value).data("propertyName",propertyName);
			if ($responseField.val() != null) {
				$responseField.append($option.clone().text("${symbol}"));
			} else {
				$responseField.append($option.clone().text(value));
				$responseField.val(value);
			}
			$responseField.data("lastVal", value).change(function () {
				var $lastOption = $responseField.find("option:last");
				if ($lastOption.prop("selected")) {
					$("#symbol-input").val($(this).val());
					$("#dialog-symbol-project").dialog("open").data("option", $lastOption);
				} else {
					$responseField.data("lastVal", value);
				}
			});
		} else {
			if ($xmlProperty.attr("isMasked") == "true") {
				$responseField= $("#projectEditTemplate .projectEditInputPassword").clone();
			} else if ($xmlProperty.attr("isMultiline") == "true" || $.inArray(propertyEditor, ["TextEditor", "JavascriptTextEditor", "GenericTextEditor"]) != -1) {
				$responseField = getInputCopyOf("projectEditTextArea");
				$responseField
					.attr("cols", "80")
					.attr("rows", "3")
					.text(value);
				if (propertyEditor != "null") {
					$responseField.prop("disabled", true);
				}
			} else {
				$responseField= $("#projectEditTemplate .projectEditInputText").clone();
				if (propertyEditor != "null") {
					$responseField.prop("disabled", true);
				} else {
					$responseField.attr("class","projectEdit-form-item");
					if ($responseField.prop("disabled")) {
						$responseField.prop("disabled", false);
					}
				}
			}
			
			if ($xmlProperty.attr("isScriptable") == "true") {
				$responseField.css("background-color", "lightcyan");
			}
			
			$responseField.attr("value", value).data("propertyName", propertyName);
		}
		if($xmlPropertyValue.attr("compiledValue")){
			$responseField.attr("title",$xmlPropertyValue.attr("compiledValue"));
		}
		if ($xmlProperty.attr("blackListed")) {
			$responseField.prop("disabled", true);
		}
		$propertyContent.append($responseField);
		if ($xmlPropertyValue.attr('compiledValue') != undefined) {
			$propertyContent.append($xmlPropertyValue.attr("compiledValue"));
		}
	}
	
	return $propertyContent.children();
}

function getInputCopyOf(inputId){
	$response = $("#projectEditTemplate ." + inputId).clone();
	return $response;
}

function addVectorProperty(propertyName, editor, $xmlProperty) {
	
	var $propertyContentTable=$("<table/>").append("<tbody/>");
	$xmlProperty.find("xmlizable >*").each(function() {
		var $propertyContentLine = $propertyContentTable.append("<tr/>");
		$(this).children().each(function() {
			$propertyContentLine.append(addPropertyContent(propertyName, editor, $(this), $xmlProperty));
		})
	});
	
	$propertyContentTable.find("input,textarea").attr("disabled", "disabled");
	return $propertyContentTable;
}

function projectEditObjectSubmitProperties() {
	var $xmlResponse = $(xmlDatabaseObject);

	$(".projectEdit-form-item").each(
		function() {
			var $property = $xmlResponse.find("property[name=" + $(this).data("propertyName") + "]");
			var $value = $property.find("[value]");
			var value = $(this).val();
			$property.find("[value]").attr('value', $(this).val());
		}
	);

	var $node = $xmlResponse.find('admin >*').first();
	var node = $node[0];
	callService("database_objects.Set", 
		function(xml) {
			if ($(xml).find("response").attr("state")==="success") {
				showInfo($(xml).find("response").attr("message"));
			}
			if ($(xml).find("response").attr("state")==="error") {
				showError($(xml).find("response").attr("message"),$(xml).find("stackTrace").text()); 
			}
			loadProjectGSymbol(project_Name);
			projects_List_update();
		}
		, domToString2(node)
		, undefined
		, {
			contentType : "application/xml"
		}
	);
	
}

function projectDeclareGlobalSymbols() { 	 
	callService("global_symbols.Create",  
		function(xml) { 
			if ($(xml).find("response").attr("state")==="success") {
				showInfo($(xml).find("response").attr("message"));
				project_Edit_update();
			}
			if ($(xml).find("response").attr("state")==="error") {
				showError($(xml).find("response").attr("message"),$(xml).find("stackTrace").text()); 
			}
			
			projects_List_update();
		} 
		, {"projectName":project_Name}  
	);
} 

function projectStats() {
	
	callService("projects.GetStatistic",
		function(xml) {
			if ($(xml).find("statistics")) {
				$("#dialog-statistics-project").dialog({
					autoOpen : true,
					title : "Statistics",
					width: 800,
					modal : true,
					buttons: [{
						id: "btn-stats-OK",
						text: "OK",
						click: function() {
							$( this ).dialog( "close" );
						}
					}]
				});
				$("#statisticsGlobalProjectName").text(project_Name);
				$("#statisticsGlobalProjectInfo").text($(xml).find("statistics").children(project_Name).text().replace("<br/>",", "));
				
				var htmlStats = "";
				$(xml).find("statistics").children("*").each(function() {
					if (this.tagName != project_Name) {
						htmlStats += "<table><tr><td>"
						htmlStats += "<img src=\"images/stats_" + htmlEncode(this.tagName.toLowerCase()) + "_16x16.png\" /></td>";
						htmlStats += "<td><strong>"+ htmlEncode(this.tagName.replace("_"," ")) + "</strong><br/>" + htmlEncode($(this).text()) + "<br/><br/></td>";
						htmlStats += "</tr></table>";
					}
				});
				
				$("#statisticsDetails").html(htmlStats);
			} else {
				$("#statisticsProjectInfo").hide();
			}
		}
		, {"projectName":project_Name}
	);
}
