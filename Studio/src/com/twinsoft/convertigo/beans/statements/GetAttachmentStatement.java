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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.statements;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.AttachmentManager.Policy;

public class GetAttachmentStatement extends Statement implements ITagsProperty {
	private static final long serialVersionUID = -7885157517110593391L;
	
	private long timeout = 60000;
	
	private long threshold = 500;

	private String policy = Policy.localfile_override.toString();
	
	private String filename = "";
	
	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				HtmlTransaction htmlTransaction = (HtmlTransaction)getParentTransaction();
				HtmlConnector htmlConnector = (HtmlConnector)htmlTransaction.getParent();
				evaluate(javascriptContext, scope, filename, "LogStatement", true);
				String evaluated_filename = (evaluated!=null && !(evaluated instanceof org.mozilla.javascript.Undefined)) ? evaluated.toString() : "";
				
				boolean ret = htmlConnector.getHtmlParser().getAttachment(htmlTransaction.context, getPolicyObject(), evaluated_filename, timeout, threshold);
				if (ret == false) {
					return true; //TODO: must be false, but do loop
				}
				return true;
			}
		}
		return false;
	}	

    @Override
	public String toJsString() {
		return null;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public long getThreshold() {
		return threshold;
	}

	public void setThreshold(long threshold) {
		this.threshold = threshold;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String[] getTagsForProperty(String propertyName) {
		if ("policy".equals(propertyName)) {
			Policy[] policies = Policy.values();
			String[] tags = new String[policies.length];
			for (int i=0; i < tags.length; i++) {
				tags[i] = policies[i].toString();
			}
			return tags;
		}
		return new String[0];
	}
	
	public Policy getPolicyObject() {
		Policy policy = Policy.valueOf(this.policy);
		return policy != null ? policy : Policy.localfile_override;
	}
}