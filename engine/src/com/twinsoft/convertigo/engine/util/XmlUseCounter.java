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


package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.validation.Schema;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * xml.count_documents: counts what a request does with XML, the threads it starts (requestables, internal requests)
 * included: the DOM documents created (new or parsed) by the document builders of XMLUtils, these builders and
 * their factories, and the documents serialized to text (logs, XML answers). The servlet logs the counts of each
 * request at its end.
 */
public final class XmlUseCounter {
	public static final class Counts {
		public final AtomicInteger documents = new AtomicInteger();
		public final AtomicInteger builders = new AtomicInteger();
		public final AtomicInteger factories = new AtomicInteger();
		public final AtomicInteger serialized = new AtomicInteger();

		@Override
		public String toString() {
			return "documents=" + documents + " builders=" + builders + " factories=" + factories + " serialized=" + serialized;
		}
	}

	private static final InheritableThreadLocal<Counts> current = new InheritableThreadLocal<Counts>();

	private XmlUseCounter() {
	}

	public static boolean isEnabled() {
		try {
			return EnginePropertiesManager.getPropertyAsBoolean(PropertyName.XML_COUNT_DOCUMENTS);
		} catch (Throwable t) {
			return false;
		}
	}

	public static Counts begin() {
		Counts counts = new Counts();
		current.set(counts);
		return counts;
	}

	public static void end() {
		current.remove();
	}

	static void document() {
		Counts counts = current.get();
		if (counts != null) {
			counts.documents.incrementAndGet();
		}
	}

	static void builder() {
		Counts counts = current.get();
		if (counts != null) {
			counts.builders.incrementAndGet();
		}
	}

	static void factory() {
		Counts counts = current.get();
		if (counts != null) {
			counts.factories.incrementAndGet();
		}
	}

	static void serialized() {
		Counts counts = current.get();
		if (counts != null) {
			counts.serialized.incrementAndGet();
		}
	}

	/** a document builder whose documents are counted */
	static DocumentBuilder counting(DocumentBuilder builder) {
		if (builder == null || !isEnabled()) {
			return builder;
		}
		return new DocumentBuilder() {
			@Override
			public Document parse(InputSource is) throws SAXException, IOException {
				document();
				return builder.parse(is);
			}

			@Override
			public Document newDocument() {
				document();
				return builder.newDocument();
			}

			@Override
			public boolean isNamespaceAware() {
				return builder.isNamespaceAware();
			}

			@Override
			public boolean isValidating() {
				return builder.isValidating();
			}

			@Override
			public void setEntityResolver(EntityResolver er) {
				builder.setEntityResolver(er);
			}

			@Override
			public void setErrorHandler(ErrorHandler eh) {
				builder.setErrorHandler(eh);
			}

			@Override
			public DOMImplementation getDOMImplementation() {
				return builder.getDOMImplementation();
			}

			@Override
			public void reset() {
				builder.reset();
			}

			@Override
			public Schema getSchema() {
				return builder.getSchema();
			}

			@Override
			public boolean isXIncludeAware() {
				return builder.isXIncludeAware();
			}
		};
	}
}
