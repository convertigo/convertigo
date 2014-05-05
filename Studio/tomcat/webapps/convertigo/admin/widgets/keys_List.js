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


function keys_List_update() {
	callService("keys.List", function(xml) {
		updateKeysList(xml);
	});
}

function keys_List_init() {
	$("#keysGenerateNewKey button").button({ icons : { primary : "ui-icon-disk" } });

	$('#keysGenerateNewKey').submit(function() {
		var key = $("#keysNewKey").val();
		if (key.length > 0) {
			var $keys = $("<keys/>").append($("<key/>").attr("text", key));
			
			callService("keys.Update", function(xml) {
				var message = "";
				$(xml).find("key").each(function() {
					var keyErrorMessage = $(this).attr("errorMessage");
					var keyText = $(this).attr("text");
					if (keyErrorMessage) {
						message += "Unable to add the key '" + keyText + "'<br/><br/>" + keyErrorMessage;
					} else {
						message += "The key <em>" + keyText + "</em> has been successfully added.";
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
			showInfo("Please enter a key!");
		}
		return false;
	});

	keys_List_update();
}

function updateKeysList(xml) {
	$('#keysListContent').empty();
	var $template = $("#key-template");
	$(xml).find("category").each(function () {
		var $x_category = $(this);
		var $category = $template.find(".key-category").clone();
		$category.find(".key-category-name").text($x_category.attr("name"));
		$category.find(".key-category-total").text($x_category.attr("total"));
		$category.find(".key-category-used").text($x_category.attr("used"));
		$category.find(".key-category-remaining").text((parseInt($x_category.attr('total')) - parseInt($x_category.attr('used'))));
		var $category_table = $category.find("table:first");
		$x_category.find("key").each(function (i) {
			var $x_key = $(this);
			var $key = $template.find(".key-content").clone();
			$key.addClass(i % 2 == 0 ? "main_even" : "main_odd" )
			$key.find(".key-text").text($x_key.attr('text'));
			$key.find(".key-value").text($x_key.attr('value'));
			if ($(this).attr('evaluation') != "true" || $(this).attr('expired') != "true") {
				$key.find(".key-expired").hide();
			}
			$category_table.prepend($key);
		});
		$("#keysListContent").append($category);
	});
}