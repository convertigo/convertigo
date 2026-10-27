/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.twinsoft.convertigo.engine.util.JsonValues;

/**
 * Applies pure row kernels in Java to each detached row of its input: no JavaScript realm per row. The metadata of
 * a non-empty result is fixed by the plan; an empty result asks the declared JavaScript producer for its metadata.
 */
public final class JavaRowTransform implements RowSource {
	private final RowSource input;
	private final Function<Object, Object> pipeline;
	private final Map<String, Object> metadata;
	private final Supplier<RowSource> emptyMetadata;
	private long rows;
	private RowSource empty;
	// rows go through the kernels by batches: one call for many rows
	private static final int BATCH = 64;
	private final java.util.ArrayDeque<Object> ready = new java.util.ArrayDeque<>();
	private boolean ended;

	public JavaRowTransform(RowSource input, Function<Object, Object> pipeline, Map<String, Object> metadata, Supplier<RowSource> emptyMetadata) {
		this.input = input; this.pipeline = pipeline; this.metadata = metadata; this.emptyMetadata = emptyMetadata;
	}

	@Override public Map<String, Object> next() throws IOException {
		while (ready.isEmpty()) {
			if (ended) return null;
			var batch = new java.util.ArrayList<Object>(BATCH);
			while (batch.size() < BATCH) {
				var row = input.next();
				if (row == null) { ended = true; break; }
				batch.add(row);
			}
			if (batch.isEmpty()) return null;
			rows += batch.size();
			List<Object> out;
			try {
				out = JsonValues.list(JsonValues.map(pipeline.apply(batch)).get("rows"));
			} catch (RuntimeException error) {
				throw new RowStreamException("ROW_STREAM_TRANSFORM", 500);
			}
			ready.addAll(out);
		}
		return JsonValues.map(ready.pollFirst());
	}

	@Override public Map<String, Object> metadata() throws IOException {
		if (rows > 0) return metadata;
		if (empty == null) empty = emptyMetadata.get();
		return empty.metadata();
	}

	@Override public void close() throws IOException {
		try { input.close(); } finally { if (empty != null) empty.close(); }
	}
}
