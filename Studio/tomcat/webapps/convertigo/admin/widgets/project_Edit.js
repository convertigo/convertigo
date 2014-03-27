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

$.getScript('js/jquery/jquery-treeview/jquery.treeview.js');

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
}

function project_Edit_update() {
	$("#project_Edit").hide();
}

function loadProjectGSymbol(projectName){	
	callService("projects.GetUndefinedSymbols", function(xml) {
		if ($(xml).find("undefined_symbols")) {
			var htmlCode = "<h2><img border=\"0\" class=\"iconAlertGlobalSymbols\" title=\"Click here to create undefined global symbols\" src=\"images/convertigo-administration-alert-global-symbols.png\"> Undefined Global Symbols</h2>";
			htmlCode += "<p>This are your \"Undefined Global Symbols\" for this project: </p>";
			htmlCode += "<ul>";
			$(xml).find("undefined_symbols").children().each(function() {
				htmlCode += "<li>"+$(this).text()+"</li>";
			});
			htmlCode += "</ul>";
			
			$("#projectEditUndefinedSymbolsInfoList").html(htmlCode);
			$("#projectEditUndefinedSymbolsInfo").show();
		}else {
			$("#projectEditUndefinedSymbolsInfo").hide();
		}
	}, 
	{"projectName":projectName});
	
	loadProject(projectName);
}

// This variable contains the XML DOM returned by the database_objects.Get service
var xmlDatabaseObject;

function loadProject(projectName) {

	startWait(30);

	callService("projects.Get", function(xml) {
		$("#project_Edit").show();
		
		// set the title of the widget
		$("#project_Edit h3").first().html("Project " + $(xml).find("project").attr("name"));
		
		// create the project node
		var htmlProjectEditDivTree = '<div class="projectEdit-selectableElement" qname="' + $(xml).find("project").first().attr('qname')
				+ '"><div ><span id="projectTreeWidgetRoot"><img src="services/database_objects.GetIcon?className=com.twinsoft.convertigo.beans.core.Project" />'
				+ $(xml).find("project").attr("name") + '</span></div></div>';
		htmlProjectEditDivTree += "<ul id=\"projectEditTree\"></ul>";				
		$("#projectEditDivTree").html(htmlProjectEditDivTree);

		// create the tree
		constructTree($(xml).find("project"), $("#projectEditTree"));
		$("#projectEditTree").treeview( {
			animated : "fast",
			collapsed : true
		});

		// interaction hover
		$(".projectEdit-selectableElement > div").hover(function() {
			$(this).addClass("hover");
		}, function() {
			$(this).removeClass("hover");
		});

		// interaction on click
		$(".projectEdit-selectableElement >div>span").click(function() {
			$("*[class~=projectEdit-editedObject]").each(function() {
				$(this).removeClass("projectEdit-editedObject");
			});
			$(this).addClass("projectEdit-editedObject");
			 
			
			loadElement($(this).parent().parent().attr("qname"), $(this));
			return false;
		});

		$("#projectTreeWidgetRoot").click();
		endWait();		
	}, {"projectName":projectName});

}

function constructTree($xml, $tree) {
	var tagName;
	var displayName;
	var img;		
	var treeCategories=new Array();	
	// for each element of project.Get
	$xml.children("*").each(function() {
		tagName = this.nodeName;
		// if the category (connector, transaction,...) is not already added
		if (!treeCategories[tagName]) {
			// add the category
			$tree.append('<li><span><img src="images/folder.gif" />' + formatFolderName(tagName) + '</span><ul></ul></li>');
			treeCategories[tagName]=$tree.find("ul").last();			
		}
		var $currentNode=treeCategories[tagName];
		displayName = $(this).attr("name");

		if (displayName != undefined) {
			// add the element			
			img = '<img src="services/database_objects.GetIcon?className=' + $(this).attr(
					"classname") + '" />';
			$currentNode.append('<li  class="projectEdit-selectableElement" qname="' + $(this).attr('qname') + '">'
					+ '<div><span>' + img + displayName + '</span></div><ul></ul></li>');
			// construct the sons of the element
			constructTree($(this), $currentNode.find("ul").last());
		}

	});

}

function formatFolderName(tagName){
	if( tagName == "screenclass" )
		return "Screen classes";	
	if( tagName == "extractionrule" )
		return "Extraction rules";		
	if( tagName == "criteria" )
		return "Criteria";	
	var newName = tagName.substring(0,1).toUpperCase()+ tagName.substring(1);	
	if( tagName.substring(tagName.length-1) == "s" ){
		newName += "es";
	}else{
		newName += "s";
	}
	return newName;
}

function loadElement(elementQName, $treeitem) {

	callService("database_objects.Get", function(xml) {

		var $projectEditObjectPropertiesListTable=$("#projectEditPropertyTable-template").clone();
		$projectEditObjectPropertiesListTable.attr("id","projectEditPropertyTable");		
		var $xml = $(xml);
		var caroleOdd=true;
		$projectEditObjectPropertiesListTable.find(".projectEditObjectType").text($xml.find("admin > *").first().attr("displayName"));		
		$projectEditObjectPropertiesListTable.find(".projectEditObjectVersion").text($xml.find("admin > *").first().attr("version"));
		$projectEditObjectPropertiesListTable.find(".projectEditObjectName").text($xml.find("property[name=name] > *").first().attr("value"));
		
		$xml.find("property[isHidden!=true]").each(
				function() {					
					var $propertyLine_xml;					
					$propertyLine_xml = $("#projectEdit-template").find(".projectEdit-propertyLine").first().clone();
					if(caroleOdd)
						$propertyLine_xml.attr("class","main_odd");
					else{
						$propertyLine_xml.attr("class","main_even");
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
					$projectEditObjectPropertiesListTable.append($propertyLine_xml);											
				});			
		
		$("#projectEditObjectPropertiesList").html($projectEditObjectPropertiesListTable);		
		$("input[type=checkbox]").click(function() {
			if($(this).attr("value")=="true") {
				$(this).attr("value", "false");
			} else {
				$(this).attr("value", "true");
			}
			$(this).prop("checked");
		});
		$(".projectEditorPropertyHelpIcon > img").click(function(){
			showInfo($(this).data("long_description"));
		});
		xmlDatabaseObject = xml;		
		//$("#projectEditObjectProperties").css("margin-top", Math.max(0, $treeitem.position().top - ($("#projectEditObjectProperties").height() / 2)));
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
		var $possibleValues=$xmlProperty.find("possibleValues");	
		
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
			$responseField=getInputCopyOf("projectEditInput-combo");
			$option=$responseField.find("option").clone();
			$responseField.children().remove();
			$possibleValues.find("value").each(function(){
				$responseField.append($option.clone().text($(this).text()));
			});
			$responseField.val(value).data("propertyName",propertyName);
		} else {
			
			if (propertyJavaClassName == "java.lang.Boolean") {				
				$responseField=getInputCopyOf("projectEditInput-checkbox");	
				if ($xmlPropertyValue.attr("value") == "true"){
					$responseField.prop("checked",true);
				}
			}else{
				if ($xmlProperty.attr("isMasked") == "true") {
					$responseField=$("#projectEditInput-password").clone();
				}
				else {
					$responseField=$("#projectEditInput-text").clone();
				}
			}
							
			$responseField.attr("value",value).data("propertyName",propertyName);	

			if (propertyEditor != "null" && propertyEditor != "TextEditor") {
				$responseField.attr("disabled","disabled");
			}
			else {				
				$responseField.attr("class","projectEdit-form-item");
				if ($responseField.attr("disabled")) {
					$responseField.removeAttr( "disabled" );
				}
			}				
			
		}
		if($xmlPropertyValue.attr("compiledValue")){
			$responseField.attr("title",$xmlPropertyValue.attr("compiledValue"));
		}
		if ($xmlProperty.attr("blackListed")) {			
			$responseField.attr("disabled","disabled");
		}
		$propertyContent.append($responseField);
		if ($xmlPropertyValue.attr('compiledValue') != undefined) {
			$propertyContent.append($xmlPropertyValue.attr("compiledValue"));
		}
	}
	
	return $propertyContent.children();
}

function getInputCopyOf(inputId){	
	$response=$("#"+inputId).clone();
	$response.removeAttr("id");
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

	return $propertyContentTable;
}

function projectEditObjectSubmitProperties() {

	var $xmlResponse = $(xmlDatabaseObject);

	$(".projectEdit-form-item").each(
		function() {
			var $property = $xmlResponse.find("property[name=" + $(this).data("propertyName") + "]");
			var $value = $property.find("*[value],*[value=]");
			var value = $(this).val();
			$property.find("*[value],*[value=]").attr('value', $(this).val());
		}
	);

	var $node = $xmlResponse.find('admin >*').first();
	var node = $node[0];	
	callService("database_objects.Set", 
		function(xml) {
			if ($(xml).find("response").attr("state")==="success") {
				showInfo("<p>"+$(xml).find("response").attr("message")+"</p>");
			}
			if ($(xml).find("response").attr("state")==="error") {
				showError("<p>"+$(xml).find("response").attr("message")+"</p>",$(xml).find("stackTrace").text()); 
			}

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
	var projectName = $(".projectEditObjectName").text();
	
	callService("global_symbols.Create",  
		function(xml) { 
			if ($(xml).find("response").attr("state")==="success") {
				showInfo("<p>"+$(xml).find("response").attr("message")+"</p>");
				project_Edit_init();
			}
			if ($(xml).find("response").attr("state")==="error") {
				showError("<p>"+$(xml).find("response").attr("message")+"</p>",$(xml).find("stackTrace").text()); 
			}
		} 
		, {"projectName":projectName}  
	); 
	
	projects_List_update();
} 

function projectStats() { 	 
	var projectName = $(".projectEditObjectName").text();
	
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
				$("#statisticsGlobalProjectName").html(projectName);
				$("#statisticsGlobalProjectInfo").html($(xml).find("statistics").children(projectName).text().replace("<br/>",", "));
				
				var htmlStats = "";
				$(xml).find("statistics").children("*").each(function() {
					if (this.tagName != projectName) {
						htmlStats += "<table><tr><td>"
						htmlStats += "<img src=\"images/stats_"+this.tagName.toLowerCase()+"_16x16.png\" /></td>";
						htmlStats += "<td><strong>"+this.tagName.replace("_"," ")+"</strong><br/>"+$(this).text()+"<br/><br/></td>";
						htmlStats += "</tr></table>";
					}
				});
								
				$("#statisticsDetails").html(htmlStats);
			} else {
				$("#statisticsProjectInfo").hide();
			}
		} 
		, {"projectName":projectName}  
	); 
} 
