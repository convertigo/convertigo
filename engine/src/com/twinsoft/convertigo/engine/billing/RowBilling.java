/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.billing;

import java.util.function.Consumer;
import com.twinsoft.convertigo.engine.flow.rows.ResultDocument;

/** One deferred ticket, with detached identity and final response-owned work. */
public final class RowBilling {
	private RowBilling() { }
	public static void defer(ResultDocument document, Ticket ticket, Consumer<Ticket> sink) {
		document.onClose(completion -> {
			ticket.setResponseTime(ticket.getResponseTime() + completion.elapsedMillis());
			ticket.setCompletionStatus(completion.status());
			ticket.setResponseComplete(completion.complete());
			sink.accept(ticket);
		});
	}
}
