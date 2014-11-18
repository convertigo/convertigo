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

C8O._hub = {
    init : function (params) {
        var basefile = document.location.pathname.replace(new RegExp("/[^/]*$"), "/hub/" + params.__hub_page);
        $.get(
            basefile + ".xml",
            function (xml) {
                C8O._hub.$xml = $(xml);
                $.get(
                    basefile + ".html",
                    function (html) {
                        C8O._fillBody(html);
                    },
                    "html"
                );
            },
            "xml"
        );
    },
    publish_event : function (target, payload) {
        // should be overrided by portal plugin
        // alert("publish_event for " + target + " with " + payload);
    },
    receive_event : function (event) {
        C8O._hub.$xml.find("hub>origin").each( function () {
            var $origin = $(this);
            if ($origin.attr("name") === event.origin) {
                $origin.find(">event").each( function () {
                    var $event = $(this);
                    if ($event.attr("name") === event.name) {
                        $event.find(">target").each( function () {
                            var $target = $(this);
                            var mapping = new Function("payload", $target.find(">mapping").text());
                            var payload = event.payload;
                            var clone = $.extend({}, payload);
                            payload = mapping.call(this, clone);
                            var new_event = {
                                name : event.name,
                                origin : event.origin,
                                payload : (typeof(payload) === "object") ? payload : clone,
                                target : $target.attr("name"),
                                type : (typeof($target.attr("type")) === "undefined") ? "call" : $target.attr("type")
                            };
                            
                            C8O._hub.publish_event($target.attr("name"), new_event);
                        });
                    }
                });
            }
        });
    }
}