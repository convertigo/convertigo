/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.validation.Schema;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Retains parser configuration and counts only actual DOMs, not handles or JSON results. */
final class CountingDocumentBuilder extends DocumentBuilder {
	private final DocumentBuilder delegate;
	CountingDocumentBuilder(DocumentBuilder delegate) { this.delegate = delegate; }
	@Override public Document newDocument() {
		var document = delegate.newDocument();
		TimingTrace.documentCreated();
		return document;
	}
	@Override public Document parse(InputSource input) throws SAXException, IOException {
		var document = delegate.parse(input);
		TimingTrace.documentCreated();
		return document;
	}
	@Override public void reset() { delegate.reset(); }
	@Override public boolean isNamespaceAware() { return delegate.isNamespaceAware(); }
	@Override public boolean isValidating() { return delegate.isValidating(); }
	@Override public boolean isXIncludeAware() { return delegate.isXIncludeAware(); }
	@Override public Schema getSchema() { return delegate.getSchema(); }
	@Override public void setEntityResolver(EntityResolver resolver) { delegate.setEntityResolver(resolver); }
	@Override public void setErrorHandler(ErrorHandler handler) { delegate.setErrorHandler(handler); }
	@Override public DOMImplementation getDOMImplementation() { return delegate.getDOMImplementation(); }
}
