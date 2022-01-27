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

function fullsync_Frame_init() {
	$("#fullsync_Frame").empty().append('<iframe src="_utils/" id="couchdbframe"></iframe>');
	$("#couchdbframe")[0].onload = function () {
		var $doc = $($("#couchdbframe")[0].contentWindow.document);
		$doc.find('#app-container > div > div').bind('DOMNodeInserted', function(event) {
			if (event.target.id == "dashboard") {
				if ($doc.find(".faux-header__breadcrumbs-element").text() == "Job Configuration") {
					$doc.find(".faux-header__breadcrumbs").prepend('<button type="button" class="faux-header__doc-header-backlink"><i class="faux-header__doc-header-backlink__icon fonticon fonticon-left-open"></i></button>');
					$doc.find(".faux-header__doc-header-backlink").click(function () {
						$("#couchdbframe")[0].contentWindow.history.back();
					});
				}
			}
			if ($doc.find("#btRefresh").length == 0) {
				$doc.find("#header-docs-left>div,#breadcrumbs>div").prepend("<button id=\"btRefresh\" type=\"button\" class=\"faux-header__doc-header-backlink\">↻</button>");
				$doc.find("#btRefresh").on("click", function () {
					$("#couchdbframe")[0].contentWindow.location.reload()
				});
			}
			if ($doc.find(".fauxton-table-list th:eq(1)").text() == "Size") {
				$doc.find(".fauxton-table-list th:eq(1)").remove();
				$doc.find(".fauxton-table-list tr").find("td:eq(1)").remove();
			}
			$doc.find(".design-doc-body li:has(a:contains('Metadata'))").remove();
			$doc.find(".nav-list li:has(a:contains('Permissions'))").remove()
			$doc.find(".nav-list li:has(a:contains('Changes'))").remove()
		});
	};
}

function fullsync_Frame_update() {
	if ($("#couchdbframe")[0].contentWindow.document.body.children.length == 0) {
		$("#couchdbframe")[0].contentWindow.location.reload(true);
	}
}
