/*
 * Copyright (c) 2001-2017 Convertigo. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of Convertigo.
 * The program(s) may  be used  and/or copied  only with the written
 *  permission  of  Convertigo  or in accordance  with  the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * Convertigo makes  no  representations  or  warranties  about  the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness for a particular purpose, or non-infringement. Convertigo
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
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
						message += "Unable to remove the key '" + keyText + "'<br/><br/>" + keyErrorMessage;
					} else {
						message += "The key <em>" + keyText + "</em> has been successfully removed.";
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
			
			if ($(this).attr('evaluation') != "true" || $(this).attr('expired') != "true") {
				$key.find(".key-expired").hide();
			}
			
			$category_table.prepend($key);
			
			if (categoryName == "Standard Edition") {
				$category_table.find("td:contains('simultaneous connections')").text("simultaneous sessions");
			}
		});
		
		$("#keysListContent").append($category);
	});
}
