/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.IOException;
import org.codehaus.jettison.json.JSONObject;

/** Exclusive, synchronous ownership. Every returned row is detached from its producer. */
public interface RowSource extends AutoCloseable {
	JSONObject next() throws IOException;
	default JSONObject metadata() throws IOException { return new JSONObject(); }
	@Override void close() throws IOException;
}
