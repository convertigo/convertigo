/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.flow;

import java.io.IOException;
import com.twinsoft.convertigo.engine.tags.TagContributions;
import com.twinsoft.convertigo.engine.tags.TagDocument;
import com.twinsoft.convertigo.engine.tags.TagManager;

/** Flow is an optional tag extension. Its descriptor and semantics belong to the JS engine. */
public final class FlowTagContribution {
	private FlowTagContribution() { }

	public static void register(TagContributions contributions) throws IOException {
		contributions.register("flow", context -> {
			if (context.scope() != TagManager.Scope.projectObjects || context.project() == null
					|| context.project().getFlowEngine() == null) return null;
			try {
				var result = new FlowEngineBridge().tagContribution(context.project().getFlowEngine());
				if (!result.optBoolean("ok", false) || result.optJSONObject("descriptor") == null)
					throw new IOException("Unable to describe Flow tag metadata: " + result.opt("error"));
				return TagDocument.parseObject(result.getJSONObject("descriptor").toString());
			} catch (com.twinsoft.convertigo.engine.EngineException | org.codehaus.jettison.json.JSONException e) {
				throw new IOException("Unable to describe Flow tag metadata", e);
			}
		});
	}
}
