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

package com.twinsoft.convertigo.engine.studio.responses.sourcepicker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.responses.AbstractResponse;

public class SourcePickerViewFillHelpContentResponse extends AbstractResponse {

    private String tag;
    private String type;
    private String name;
    private String comment;
    private String textBtn;
    private boolean enableBtn;

    public SourcePickerViewFillHelpContentResponse(String tag, String type, String name, String comment, String textShowBtn, boolean enableBtn) {
        super();
        this.tag = tag;
        this.type = type;
        this.name = name;
        this.comment = comment;
        this.textBtn = textShowBtn;
        this.enableBtn = enableBtn;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);
        response.appendChild(DOMUtils.createElementWithText(document, "tag", tag));
        response.appendChild(DOMUtils.createElementWithText(document, "type", type));
        response.appendChild(DOMUtils.createElementWithText(document, "name", name));
        response.appendChild(DOMUtils.createElementWithText(document, "comment", comment));
        response.appendChild(DOMUtils.createElementWithText(document, "text_show_btn", textBtn));
        response.appendChild(DOMUtils.createElementWithText(document, "enable_btn", Boolean.toString(enableBtn)));

        return response;
    }
}
