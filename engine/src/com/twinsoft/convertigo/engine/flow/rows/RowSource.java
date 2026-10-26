/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import com.twinsoft.convertigo.engine.util.JsonValues;

/** Exclusive, synchronous ownership. Every returned row is detached from its producer. */
public interface RowSource extends AutoCloseable {
	Map<String, Object> next() throws IOException;
	default Map<String, Object> metadata() throws IOException { return JsonValues.object(); }
	@Override void close() throws IOException;
}
