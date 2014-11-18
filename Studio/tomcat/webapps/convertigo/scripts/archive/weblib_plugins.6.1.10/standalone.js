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

C8O._init_standalone = function (params) {
    var widget_name = params.__widget_name;
    C8O.ro_vars.widget_name = widget_name;
    
    C8O.addHook("mashup_event", function (eventName, data) {
        parent.C8O_hub.publish({
                data: data,
                name: eventName,
                origin: widget_name,
                type: "mashup"
        });
    });
    
    parent.C8O_hub.subscribe(widget_name, function (event) {
        if (event.type === "call")
            C8O.call(event.data);
    });
        
    if (!params.__context) {
        params.__context = widget_name;
    }
    C8O._init(params);
}