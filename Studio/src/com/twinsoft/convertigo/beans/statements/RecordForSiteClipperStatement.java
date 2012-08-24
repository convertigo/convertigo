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

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class RecordForSiteClipperStatement extends Statement {
	private static final long serialVersionUID = 1481197301462352057L;
	
	private String urlRegex = ".*";
	private int entryLifetime = 120;

	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			com.twinsoft.convertigo.engine.Context context = getParentTransaction().context;
			context.newXulRecorder(urlRegex, entryLifetime);
			Engine.logBeans.debug("(RecordForSiteClipperStatement) Recording start for URL like '" + urlRegex + "'");
		}

		return isEnable();
	}
	
	@Override
	public String toJsString() {
		return "";
	}

	public String getUrlRegex() {
		return urlRegex;
	}

	public void setUrlRegex(String urlRegex) {
		this.urlRegex = urlRegex;
	}

	public int getEntryLifetime() {
		return entryLifetime;
	}

	public void setEntryLifetime(int entryLifetime) {
		this.entryLifetime = entryLifetime;
	}

}
