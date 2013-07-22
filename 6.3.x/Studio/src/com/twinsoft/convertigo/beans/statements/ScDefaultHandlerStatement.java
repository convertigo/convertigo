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

import com.twinsoft.convertigo.engine.EngineException;

public class ScDefaultHandlerStatement extends HandlerStatement {

	private static final long serialVersionUID = -8564360897367937541L;
	
	public static final String EVENT_ENTRY_HANDLER = "Entry";
	public static final String EVENT_EXIT_HANDLER = "Exit";
	
	public static final String RETURN_REDETECT = "redetect";
	public static final String RETURN_CONTINUE = "continue";
    public static final String RETURN_SKIP = "skip";
    public static final String RETURN_ACCUMULATE = "accumulate";
    
	public ScDefaultHandlerStatement(String handlerType) throws EngineException {
		super(handlerType,"");
		if (handlerType.equals(EVENT_ENTRY_HANDLER)) {
			setHandlerResult(RETURN_REDETECT);
			setName("onTransactionDefaultHandlerEntry");
		}
		else {
			setHandlerResult(RETURN_ACCUMULATE);
			setName("onTransactionDefaultHandlerExit");
		}
	}
	
	public String[] getTypeStrings() {
		return new String[] { EVENT_ENTRY_HANDLER, EVENT_EXIT_HANDLER };
	}
	
	public String[] getResultStrings() {
		if (getHandlerType().equals(EVENT_ENTRY_HANDLER))
			return new String[] { "", RETURN_CONTINUE, RETURN_REDETECT, RETURN_SKIP };
		else
			return new String[] { "", RETURN_ACCUMULATE };
	}
}
