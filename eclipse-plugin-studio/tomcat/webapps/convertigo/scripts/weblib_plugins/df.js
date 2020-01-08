/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

C8O._init_df = function (params) {
    C8O.addHook("mashup_event", function (eventName, data) {
        try {
            if (window.parent.tcResponseItemClickedJSON) {
                if (eventName.indexOf('FormSubmitted_') === 0) {
                    window.parent.tcFormResponseSubmitted( window.name, document, eventName );
                } else {
                    window.parent.tcResponseItemClickedJSON(window.name, eventName, data);
                }
                return;
            }
        } catch (e) {}
    });

    $.ajax({
        data: params,
        success: function (xml) {
            $(xml).find("parameter").each(function () {
                var $param = $(this);
                C8O.addRecallParameter($param.attr("name"), $param.attr("value"));
            });
            C8O._init({});
        },
        type: "GET",
        url: "../../df/dfe/interface/provider"
    });
}