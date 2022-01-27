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

package com.twinsoft.convertigo.engine.studio.responses;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.util.DOMUtils;

public class MessageDialogResponse extends AbstractResponse {

	protected String title;
	protected String message;
	protected String[] buttons;

	public MessageDialogResponse(String title, String message, String[] buttons) {
		super();
		this.title = title;
		this.message = message;
		this.buttons = buttons;
	}

	@Override
	public Element toXml(Document document, String qname) throws Exception {
		// Create buttons
		Element eButtons = document.createElement("buttons");
		for (int i = 0; i < buttons.length; ++i) {
			Element eButton = document.createElement("button");
			eButton.appendChild(DOMUtils.createElementWithText(document, "text", buttons[i]));
			eButton.appendChild(DOMUtils.createElementWithText(document, "response", Integer.toString(i)));
			eButtons.appendChild(eButton);
		}

		// Create dialog: title + message + buttons
		Element dialog = document.createElement("messageDialog");
		dialog.appendChild(DOMUtils.createElementWithText(document, "title", title));
		dialog.appendChild(DOMUtils.createElementWithText(document, "message", message));
		dialog.appendChild(eButtons);

		// Create response
		Element response = super.toXml(document, qname);
		response.appendChild(dialog);

		return response;
	}
}
