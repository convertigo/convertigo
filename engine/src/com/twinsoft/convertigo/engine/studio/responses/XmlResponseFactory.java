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

package com.twinsoft.convertigo.engine.studio.responses;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlResponseFactory {

	private XmlResponseFactory() {
	}

	public static Element createMessageBoxResponse(Document document, String qname, String message) throws Exception {
		return new MessageBoxResponse(message).toXml(document, qname);
	}

	public static Element createMessageDialogResponse(Document document, String qname, String title, String message, String[] buttons) throws Exception {
		return new MessageDialogResponse(title, message, buttons).toXml(document, qname);
	}
}
