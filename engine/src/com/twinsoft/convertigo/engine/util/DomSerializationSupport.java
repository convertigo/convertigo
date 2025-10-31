/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.io.Serial;
import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;

public final class DomSerializationSupport {
	private DomSerializationSupport() {
	}

	public enum DomType {
		DOCUMENT,
		ELEMENT
	}

	public record SerializedDom(DomType type, String xml) implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		public SerializedDom {
			if (type == null || xml == null) {
				throw new IllegalArgumentException("type and xml must be non-null");
			}
		}
	}

	public static SerializedDom serialize(Object value) {
		if (value instanceof Document document) {
			return serializeDocument(document);
		}
		if (value instanceof Element element) {
			return serializeElement(element);
		}
		return null;
	}

	public static SerializedDom serializeDocument(Document document) {
		if (document == null) {
			return null;
		}
		try {
			var xml = XMLUtils.prettyPrintDOM(document);
			return xml != null ? new SerializedDom(DomType.DOCUMENT, xml) : null;
		} catch (Exception e) {
			log("Failed to serialize document", e);
			return null;
		}
	}

	public static SerializedDom serializeElement(Element element) {
		if (element == null) {
			return null;
		}
		try {
			var xml = XMLUtils.prettyPrintElement(element, false, true);
			return xml != null ? new SerializedDom(DomType.ELEMENT, xml) : null;
		} catch (Exception e) {
			log("Failed to serialize element", e);
			return null;
		}
	}

	public static Object deserialize(SerializedDom serializedDom) {
		if (serializedDom == null) {
			return null;
		}
		return switch (serializedDom.type()) {
			case DOCUMENT -> deserializeDocument(serializedDom.xml());
			case ELEMENT -> deserializeElement(serializedDom.xml());
		};
	}

	public static Document deserializeDocument(String xml) {
		if (xml == null || xml.isEmpty()) {
			return null;
		}
		try {
			return XMLUtils.parseDOMFromString(xml);
		} catch (Exception e) {
			log("Failed to deserialize document", e);
			return null;
		}
	}

	public static Element deserializeElement(String xml) {
		if (xml == null || xml.isEmpty()) {
			return null;
		}
		try {
			var document = XMLUtils.parseDOMFromString(xml);
			return document != null ? document.getDocumentElement() : null;
		} catch (Exception e) {
			log("Failed to deserialize element", e);
			return null;
		}
	}

	private static void log(String message, Exception e) {
		try {
			if (Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(DomSerializationSupport) " + message, e);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}
}
