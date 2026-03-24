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

package com.twinsoft.convertigo.engine.requesters;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AttachmentManager;
import com.twinsoft.convertigo.engine.EngineException;

import jakarta.servlet.http.HttpServletRequest;

public class UrlMapperRequester extends InternalRequester {

	public enum ResponseMode {
		defaultContent,
		binary,
		cxml
	}

	private final ResponseMode responseMode;

	public UrlMapperRequester(Map<String, Object> request, HttpServletRequest httpServletRequest, ResponseMode responseMode) throws EngineException {
		super(request, httpServletRequest);
		this.responseMode = responseMode;
	}

	@Override
	public String getName() {
		return "UrlMapperRequester";
	}

	public boolean isCxmlResponse() {
		return responseMode == ResponseMode.cxml;
	}

	@Override
	public boolean isInternal() {
		return responseMode != ResponseMode.cxml;
	}

	@Override
	public void preGetDocument() {
		super.preGetDocument();
		if (responseMode == ResponseMode.cxml) {
			context.isXsltRequest = true;
		}
	}

	@Override
	public Object postGetDocument(Document document) throws Exception {
		if (responseMode == ResponseMode.binary) {
			var attachments = document.getDocumentElement().getElementsByTagName("attachment");
			if (attachments != null) {
				for (int i = attachments.getLength() - 1; i >= 0; i--) {
					var attachment = (Element) attachments.item(i);
					if (attachment.getParentNode() == attachment.getOwnerDocument().getDocumentElement()) {
						return AttachmentManager.getAttachment(attachment);
					}
				}
			}
		}
		return super.postGetDocument(document);
	}
}
