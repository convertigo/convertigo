/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

function removeKey(key) {
	if (key.length > 0) {		
		showConfirm("Are you sure you want to delete this key?", function() {
			var $keys = $("<keys/>").append($("<key/>").attr("text", key));
			
			callService("keys.Remove", function(xml) {
				var message = "";
				$(xml).find("key").each(function() {
					var keyErrorMessage = $(this).attr("errorMessage");
					var keyText = $(this).attr("text");
					if (keyErrorMessage) {
						message += "Unable to remove the key '" + keyText + "'\n\n" + keyErrorMessage;
					} else {
						message += "The key '" + keyText + "' has been successfully removed.";
						keys_List_update();
					}
				});
				
				if (message.length) {
					showInfo(message);
				}			
			}, domToString($keys), undefined, {contentType : "application/xml"});
		});							
	}	
	return false;
} 

function keys_List_update() {
	callService("keys.List", function(xml) {
		updateKeysList(xml);
	});
}

function keys_List_init() {
	$("#keysGenerateNewKey button").button({ icons : { primary : "ui-icon-disk" } });

	$('#keysGenerateNewKey').submit(function() {
		var key = $("#keysNewKey").val();
		if (key.length == (16+1+16)) {
			var $keys = $("<keys/>").append($("<key/>").attr("text", key));
			
			callService("keys.Update", function(xml) {
				var message = "";
				$(xml).find("key").each(function() {
					var keyErrorMessage = $(this).attr("errorMessage");
					var keyText = $(this).attr("text");
					if (keyErrorMessage) {
						message += "Unable to add the key '" + keyText + "'\n\n" + keyErrorMessage;
					} else {
						message += "The key '" + keyText + "' has been successfully added.";
						keys_List_update();
					}
				});
				
				if (message.length) {
					showInfo(message);
				}
				$("#keysNewKey").val("");
			}, domToString($keys), undefined, {contentType : "application/xml"});
		}
		else {
			showInfo("Please enter a valid key!");
		}
		return false;
	});

	keys_List_update();
}

function updateKeysList(xml) {
	$('#keysListContent').empty();
	var $template = $("#key-template");
	var nb_valid_keys = $(xml).find("nb_valid_key").text();
	$(xml).find("category").each(function () {
		var $x_category = $(this);
		var $category = $template.find(".key-category").clone();
		var categoryName = $x_category.attr("name");
		
		var display = categoryName;
		if (categoryName == "Standard Edition") {
			if (parseInt(nb_valid_keys) > 1) {
				display = "Sessions";
			}
			if ($x_category.attr("overflow") == "true") {
				display += " (session overflow allowed)";
			}
		}
		$category.find(".key-category-name").text(display);
		
		$category.find(".key-category-total").text($x_category.attr("total"));
		$category.find(".key-category-remaining").text($x_category.attr("remaining"));
		$category.find(".key-category-used").text((parseInt($x_category.attr('total')) - parseInt($x_category.attr('remaining'))));
			
		var $category_table = $category.find("table:first");
		$x_category.find("key").each(function (i) {
			var $x_key = $(this);
			var $key = $template.find(".key-content").clone();
			$key.addClass(i % 2 == 0 ? "main_even" : "main_odd" )
			$key.find(".key-text").text($x_key.attr('text'));
			$key.find(".key-value").text($x_key.attr('value'));
			$key.find(".key-click").attr("onclick", "removeKey('" + $x_key.attr('text') + "')");

			if ($(this).attr('evaluation') == "true") {
				$key.find(".key-expiration").text(dateFormat(parseInt($(xml).find("firstStartDate").text()), "ddd, mmm dS yyyy"));
			} else {
				var dayOffset = parseInt($x_key.attr('expiration'));
				if (dayOffset == 0)	// unlimited
					$key.find(".key-expiration").text("Unlimited");
				else
					$key.find(".key-expiration").text(dateFormat(dayOffset*1000*3600*24, "ddd, mmm dS yyyy"));
			}

			if ($(this).attr('evaluation') == "true") {
				$key.find(".key-expired").show();				
			} else {
				$key.find(".key-expired").hide();
			}

			// made expired over rule demo flag
			if ($(this).attr('expired') == "true") {
				$key.find(".key-expired").show();				
			} else {
				$key.find(".key-expired").hide();
			}
			
			$category_table.prepend($key);
			
			if (categoryName == "Standard Edition") {
				$category_table.find("td:contains('simultaneous connections')").contents().filter(function() {
				    return this.nodeType === 3;
				  }).replaceWith(function() {
				      return this.nodeValue.replace('connections','sessions');
				  });				
			}
		});
		
		$("#keysListContent").append($category);
	});
}
