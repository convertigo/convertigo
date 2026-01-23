/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

$(document).ready(function () {
	initCommon(function () {
		call("projects.List", {}, function (xml) {
			var $xml = $(xml);
			$("#projects>tbody").empty();
			$xml.find("project[name^='demo_mashup']").each(function () {
				var $project = $(this);
				var display_name = $project.attr("name").replace(new RegExp("_", "g"), " ");
				var $li = $("#templates .menu_demo").clone();
				$li.find("a").attr("href", "projects/" + $project.attr("name") + "/demo.html");
				$li.find("img").attr("alt", display_name).attr("title", display_name);
				$li.find("span").text(display_name);
				$("#container ul:first").append($li);
			});
			$xml.find("project").each(function (i) {
				var $project = $(this);
				var $project_div = $("#templates .project").clone();
				$.each($project[0].attributes, function () {
					var $project_field = $project_div.find(".project_" + this.name);
					if (this.name == "version") {
						if (this.value != "") {
							$project_field.text("(" + this.value + ")");
						}
					} else {
						$project_field.text(this.value);
						if (this.name == "comment") {
							$project_field.html($project_field.html().replace(new RegExp("\\n","g"), "<br/>"));
						}
					}
				});
				$project_div.find(".project_link").attr("href", "project.html#" + $project.attr("name"));
				$project_div.find(".project_wsdl").attr("href", "projects/" + $project.attr("name") + "/.wsl?wsdl");
				$project_div.find(".table_cell").addClass("table_row_" + (i%2 === 0 ? "odd" : "even"));
				$("#projects>tbody").append($project_div);
			});
		});
		call("engine.GetStatus", {}, function (xml) {
			$.each($(xml).find("version")[0].attributes, function () {
				$(".cems_version_"+this.name).text(this.value);
			});			
		});
		call("engine.GetSystemInformation", {}, function (xml) {
			$.each($(xml).find("java")[0].attributes, function () {
				$(".java_"+this.name).text(this.value);
			});	
		});
		
		$("#swaggerLink").attr("href", "swagger/dist/index.html?url="+getEncodedYamlUri());
	});
	if (window.java) {
		console.log("in studio!");
		$("thead .table_row").css({whiteSpace: "nowrap"});
		var content = $(".content_bottom>table").detach();
		var menu = $("<div/>").css({display: "flex", alignItems: "end"});
		menu.append($(".menu a:first").detach());
		menu.append($(".container_info").removeClass("container_info").detach());
		$("body").empty().css({margin: "0px"}).append(menu).append(content);
	}
});