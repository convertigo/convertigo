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

var requiredMajorVersion = 10;
var requiredMinorVersion = 0;
var requiredRevision = 0;
var jsReady = false;
var cems_request = null;

function call (data) {
	var flex = document.getElementById("convertiflex");
	if (typeof(data) == "string") {
		flex.launchURL(data);
	} else {
		flex.launchJSON(data);
	}
}

function doResize () {
	var lowest = 0;
	$("body, div, span").each(function () {
		lowest = Math.max(lowest, this.offsetTop + this.offsetHeight);
	});
	$(window.frameElement).animate({
		height: lowest + 30
	});
}

function isReady () {
	return jsReady;
}

function flexEventNotification (event, data) {
	doItemClick(event, data);
}

function flexLoadComplete (value) {
	call(cems_request);
}

//function to call to launch a DF event
//params : eventName (event name), 
//data (JSON object with one level key:value), 
function doItemClick (eventName, data) {
	// check if df functions are existing
	if (window.parent.tcResponseItemClickedJSON) {
		window.parent.tcResponseItemClickedJSON(window.name, eventName, data);
	}
}

function getQuery () {
	var l = window.location,
		q = l.search.length > 0 ? l.search.substring(1) : "",
		h = l.hash.length > 0 ? l.hash.substring(1) : "";
	return (q.length > 0 && h.length > 0) ? (q + "&" + h) : (q.length > 0 ? q : h);
}

$(document).ready(function () {
	var query = getQuery();
    // if called by DreamFace
	if (query.indexOf("__container=df") !== -1) {
		$.ajax({
			data: query,
			success: function (xml) {
				cems_request = {};
				$(xml).find("parameter").each(function () {
					var $param = $(this);
					cems_request[$param.attr("name")] = $param.attr("value");
				});
				jsReady = true;
			},
			type: "GET",
			url: "../../df/dfe/interface/provider"
		});
	} else {
		if (query.indexOf("__container=gatein") !== -1) {
			var widget_name = window.frameElement.src.replace(new RegExp(".*resources%2F(.*).xml#.*"), "$1");
			
			doItemClick = function (eventName, data) {
				parent.eXo.core.Topic.publish("/convertigo/" + widget_name, "/convertigo/mashup", {
					data: data,
					name: eventName,
					origin: widget_name,
					type: "mashup"
				});
			};
			
			parent.eXo.core.Topic.subscribe("/convertigo/" + widget_name, function (message) {
				var event = message.message;
				if (event.type === "call") {
	    			call(event.data);
				}
			});
		} else if (query.indexOf("__container=standalone") !== -1) {
			var widget_name = query.replace(new RegExp(".*widget_name=([^&]*)&?.*"), "$1");
			
			doItemClick = function (eventName, data) {
				parent.C8O_hub.publish({
					data: data,
					name: eventName,
					origin: widget_name,
					type: "mashup"
				});
			};
			
			parent.C8O_hub.subscribe(widget_name, function (event) {
				if (event.type === "call") {
					call(event.data);
				}
			});
		}
		cems_request = query;
		jsReady = true;
    }
    doResize();
});
