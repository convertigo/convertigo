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
C8O._init_gatein = function (params) {
    if (params.__hub_page) {
        C8O._getScript("../../scripts/weblib_plugins/hub.js", function () {
            C8O._hub.init(params);
            var subscribeID = parent.eXo.core.Topic.subscribe("/convertigo/mashup", function (message) {
                C8O._hub.receive_event(message.message);
            });
            $(window).unload(function() {
                parent.eXo.core.Topic.unsubscribe("/convertigo/mashup", subscribeID);
            });
            C8O._hub.publish_event = function (target, message) {
                parent.eXo.core.Topic.publish("/convertigo/mashup", "/convertigo/" + target, message);
            }
        });
    } else {
        var widget_name = new RegExp("__widget_name%3D(.+?)%26").exec(window.frameElement.src);
        if (widget_name != null && widget_name.length > 1) {
            widget_name = widget_name[1];
        } else {
            var widget_name = new RegExp("resources%2F(.*).xml#").exec(window.frameElement.src);
            widget_name = (widget_name != null && widget_name.length > 1) ? widget_name[1] : "unknow_widget";
        }
        C8O.addHook("mashup_event", function (eventName, payload) {
            parent.eXo.core.Topic.publish("/convertigo/" + widget_name, "/convertigo/mashup", {
                payload : payload,
                name : eventName,
                origin : widget_name,
                type : "mashup"
            });
        });
        var subscribeID = parent.eXo.core.Topic.subscribe("/convertigo/" + widget_name, function (message) {
            C8O._onMashupEvent(message.message);
        });
        $(window).unload(function() {
            parent.eXo.core.Topic.unsubscribe("/convertigo/" + widget_name, subscribeID);
        });
        if (!params.__context) {
            params.__context = widget_name;
        }
        C8O.ro_vars.widget_name = widget_name;
        try {
            C8O.ro_vars.portal_username = $(top.document).find("#UIUserInfoPortlet .Name a").text();
            if (C8O.vars.send_portal_username === "true") {
                C8O.addRecallParameter("portal_username", C8O.ro_vars.portal_username);
            }
        } catch (e) {
            // maybe due to cross-domain issue
        }
        C8O._init(params);
    }
    // fullscreen code introduced by #1734 - Zoom feature without reload for our gadgets in GateIn
    try {
        var $win = $(window.frameElement).closest(".UIWindow");
        var $maximize_icon = $win.find(".MaximizedIcon");
        if ($maximize_icon.length > 0) {
            var $page = $win.closest("#UIPage");
            var page_styles = $.extend({position : "absolute", width : $page.css("width"), height : $page.css("height")}, $page.position());
            var $background = $win.next(".UIWindowBackground");
            if ($background.length === 0) {
                $background = $win.after("<div/>").next().addClass(".UIWindowBackground").css(
                    $.extend({"background-color" : "grey", "z-index" : 10 }, page_styles)
                ).fadeTo(0, 0.7).hide();
            }
            $win.find(".ArrowDownIcon, .MinimizedIcon").remove();
            var $restore_icon = $maximize_icon.removeAttr("onclick").clone().removeClass("MaximizedIcon").addClass("NormalIcon").attr("title", "Restore").hide();
            $restore_icon.insertAfter($maximize_icon);
            $maximize_icon.add($restore_icon).click(function () {
                $maximize_icon.add($restore_icon).toggle();
                $background.fadeToggle();
                var styles = $.extend({"z-index" : 11}, page_styles);
                if ($maximize_icon.is(":visible")) {
                    for (var key in styles) {
                        styles[key] = "";
                    }
                }
                $win.css(styles);
                C8O.doResize();
                return false;
            });
        }
    } catch (e) {
        // resize failed, maybe due to cross-domain issue
    }
}