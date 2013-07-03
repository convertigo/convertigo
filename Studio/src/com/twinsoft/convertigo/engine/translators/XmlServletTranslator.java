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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/engine/translators/DefaultServletTranslator.java $
 * $Author: nicolasa $
 * $Revision: 33947 $
 * $Date: 2013-04-08 17:26:26 +0200 (lun., 08 avr. 2013) $
 */

package com.twinsoft.convertigo.engine.translators;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class XmlServletTranslator extends DefaultServletTranslator {

	@Override
	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
		if (context.removeNamespaces) {
			Engine.logEngine.debug("Removing namespaces...");
			Document document = (Document) convertigoResponse;

			// Remove all namespace information (i.e declarations and namespace
			// prefixes in all nodes)
			Document newDocument = XMLUtils.copyDocumentWithoutNamespace(document);
			
			if (Engine.logEngine.isDebugEnabled()) {
				String result = XMLUtils.prettyPrintDOM(newDocument);
				Engine.logEngine.debug("Namespaces removed:\n" + result);
			}
			
			return newDocument;
		} else {
			return convertigoResponse;
		}
	}

}
