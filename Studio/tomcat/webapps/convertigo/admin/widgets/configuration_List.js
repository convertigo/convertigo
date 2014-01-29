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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/tomcat/webapps/convertigo/admin/widgets/configuration_List.js $
 * $Author: laetitiam $
 * $Revision: 33134 $
 * $Date: 2012-12-26 15:22:44 +0100 (mer., 26 déc. 2012) $
 */

var propertyMap = {};

function configuration_List_update () {
	if($("#logLevelCopyFromConfigurationButton").html()!=null){
		if($("#logLevelCopyFromConfigurationButton").html().length > 0){		
			var $configTable=$("#logLevelCopyFromConfiguration").find("tbody");	
			$("#tab-Logs").find("table:first").append($configTable);			
			var $buttonUpdate=$("#logLevelCopyFromConfigurationButton").find("button");
			$("#configFirstUpdateButtonLocation").append($buttonUpdate);					
		}
	}
}

function configuration_List_init () {
	$(".config-update").button({
		disabled: true,
		icons : {
			primary : "ui-icon-disk"
		}
	});
	
	//Get engine version
	var getEngineVersion = "";
	callService("engine.GetStatus", function(xml) {
		getEngineVersion = $(xml).find("version").attr("engine");
		if(getEngineVersion == "") {
			getEngineVersion = "latest";
		}
	});		
	
	var $template = $("#config-template");
	var $ul = $("<ul/>").attr("id","config-category-list");
	callService("configuration.List", function (xml) {
		$(xml).find("category").each(function () {
			var $x_category = $(this);
			var $category = $template.find(".config-category:first").clone();
			var $category_main = $category.find(".config-accordion-main-properties:first");
			var $category_advanced = $category.find(".config-accordion-advanced-properties:first");
			
			if ($x_category.find("property[isAdvanced=\"true\"]").length === 0) {
				$category.find(".config-pane>button").remove();
			}
			
			//Get Url for each widgets
			var urlHelp = "";
			switch ($x_category.attr("name"))
			{
				case "Main":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/422Mainparameters.html";
					break;
				case "Account":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/423Accounts.html";
					break;
				case "Logs":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/424Logs.html";
					break;
				case "Context":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/425Realtimeactivitymonitoring.html";
					break;
				case "XmlGeneration":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/426XMLgeneration.html";
					break;
				case "XulRunner":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/427HTMLparser.html";
					break;
				case "HttpClient":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/428HTTPclient.html";
					break;
				case "Network":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/429Network.html";
					break;
				case "Proxy":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/4210Proxy.html";
					break;
				case "SecurityToken":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/4211Securitytoken.html";
					break;
				case "Ssl":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/4212SSL.html";
					break;
				case "Cache":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/4213Cache.html";
					break;
				case "Carioca":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/4214LegacyCariocaportal.html";
					break;
				case "Billing":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/4215Supervision.html";
					break;
				case "Notifications":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/4216Notifications.html";
					break;
				case "MobileBuilder":
					urlHelp = "http://help.convertigo.com/" + getEngineVersion + "/topic/com.twinsoft.convertigo.studio.help/help/helpOperatingGuide/42Configuration.html";
					break;
			}

			
			$category.attr("id","tab-" + $x_category.attr("name"));
			$category.find(".config-category-title:first").text($x_category.attr("displayName"));
			$category.find(".config-category-title:first + a").attr("id", $x_category.attr("name")).attr("href",urlHelp);
			$ul.append(
					$("<li/>").append(
							$("<a/>").attr("href","#tab-" + $x_category.attr("name")).text($x_category.attr("displayName"))
					)
			);
			$x_category.find(">property").each(function () {
				var $x_property = $(this);
				var id = "config_key_" + $x_property.attr("name");
				var type = $x_property.attr("type");
				var $property = $template.find(".config-property" + (type === "Boolean" ? "-boolean" : "") + ":first").clone();
				$property.find(".config-property-name:first").text($x_property.attr("description")).attr("for", id);
				
				var $property_value;
				var value = $x_property.attr("value");
				var originalValue = $x_property.attr("originalValue");
				
				switch (type) {
				case "Text":
					$property_value = $template.find(".config-text:first").clone();
					break;
				case "PasswordPlain":
				case "PasswordHash":
					$property_value = $template.find(".config-password:first").clone();
					break;
				case "Boolean":
					$property_value = $property.find(".config-checkbox:first");
					if (originalValue === "true") {
						$property_value.attr("checked", "checked");
					}
					break;
				case "Array":
					$property_value = $template.find(".config-text-area").clone();
					originalValue = originalValue.replace(/;/g, "\r\n");
					originalValue = originalValue.replace(/\[\[pv\]\]/g, ";");
					break;
				case "Combo":
					$property_value = $template.find(".config-combo").clone();
					$x_property.find(">item").each(function () {
						var $x_item = $(this);
						var $combo_item = $template.find(".config-combo-item").clone();
						$combo_item.attr("value", $x_item.attr("value"));
						$combo_item.text($x_item.text());
						$property_value.append($combo_item);
					});
					break;
				}
				
				if ($property_value) {
					if (originalValue !== value) {
						$property_value.attr("title", value);
					}
					$property_value.val(originalValue);
					$property_value.attr("name", $x_property.attr("name")).attr("id", id);
					$property.find(".config-property-value:first").append($property_value);
				}
				
				if ($x_property.attr("isAdvanced") === "false") {
					$category_main.append($property);
				} else {
					$category_advanced.append($property);
				}
			});
		
			$("#configAccordion").append($category);
			$("#configAccordion").append($ul);
			
		});

		$( "#configAccordion" ).tabs().addClass( "ui-tabs-vertical ui-helper-clearfix" );
		$( "#configAccordion ul" ).removeClass( "ui-corner-all ui-widget-header");
	    $( "#configAccordion li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
		
		
		$(".config-toggle-advanced-properties").click(function () {
			$(this).next().slideToggle("slow");
		}).first(".config-category");

		$("input.config-text, input.config-password").keyup(function () {
			$(".config-update").button("enable");			
		}).change(function(){						
			propertyMap[$(this).attr("name")] = $(this).val();
		});
		
		$("select.config-combo").change(function () {
			changeProperty($(this).attr("name"), $(this).val());
		});

		$('textarea.config-text-area').keypress(function() {
			$(".config-update").button("enable");
		}).change(function(){
			var value = $(this).val();
			value = value.replace(/;/g, "[[pv]]");
			value = value.replace(/\r/g, "");
			value = value.replace(/\n/g, ";");			
			propertyMap[$(this).attr("name")] = value;
		});

		$('input.config-checkbox').change(function () {
			changeProperty($(this).attr("name"), $(this).is(':checked') ? "true" : "false");
		});

		$("form.config-update-form").submit(function () {
			updateConfiguration();
			return false;
		});

		$(".config-toggle-advanced-properties").button({
			icons : {
				primary : "ui-icon-star"
			}
		});
		
		//if the init was call by log_Show
		if(callFromLogShow){	
			$("#widgetButtonLogs").click();			
			$("#logToggleLevel").click();			
		}
	});
}

function changeProperty(key, value) {
	propertyMap[key] = value;
	$(".config-update").button("enable");
}

function createDOM(rootElement) {
	var xmlDoc;
	if ($.browser.msie) {
		// Bugfix IE #1624
		xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
		xmlDoc.async = false;
		xmlDoc.appendChild(xmlDoc.createElement(rootElement));
	}
	else {
		xmlDoc = document.implementation.createDocument("", rootElement, null);
	}
	
	return xmlDoc;
}

function updateConfiguration () {
	if (!$.isEmptyObject(propertyMap)) {
		var xmlDoc = createDOM("configuration");
		
		for (var key in propertyMap) {
			var propertyElement = xmlDoc.createElement("property");
			propertyElement.setAttribute("key", key);
			propertyElement.setAttribute("value", propertyMap[key]);
			xmlDoc.documentElement.appendChild(propertyElement);
		}

		callService("configuration.Update", function(xml) {
			showInfo("The configuration has been successfully updated!");
			propertyMap = {};
			$(".config-update").button("disable");
		}, domToString2(xmlDoc), undefined, {contentType : "application/xml"});
	} else {
		$(".config-update").button("disable");
	}
}