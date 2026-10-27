/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.IOException;
import java.io.Writer;

import com.twinsoft.convertigo.engine.Context;

/**
 * The response of a servlet request, committed by the run that streams its emitted rows, while the servlet waits for
 * the end of that run: its headers are the ones the run has set at that moment. The servlet then writes nothing.
 */
public interface RowStreamCommit {
	/** The request attribute that holds the commit of a servlet able to stream rows. */
	String ATTRIBUTE = "convertigo.rowStreamCommit";

	/** Applies the status and headers of the response, then gives its writer. */
	Writer commit(Context context) throws IOException;

	/** true once a run has committed the response. */
	boolean committed();
}
