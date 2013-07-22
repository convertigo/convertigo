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

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;

public class HandlerStatement extends FunctionStatement implements ITagsProperty{

	private static final long serialVersionUID = -3721430508869315095L;

    public static final String EVENT_TRANSACTION_STARTED = "TransactionStarted";
	public static final String EVENT_XML_GENERATED = "XmlGenerated";
	public static final String RETURN_CANCEL = "cancel";
	
	private String handlerType = "";
	private String handlerResult = "";
	private boolean handlerLoopable = true;
	
	public HandlerStatement() throws EngineException {
		this(EVENT_TRANSACTION_STARTED,"");
	}
	
	public HandlerStatement(String handlerType, String handlerResult) throws EngineException {
		super();
		this.handlerType = handlerType;
		this.handlerResult = handlerResult;
		setName("on" + handlerType);
	}
	
	/**
	 * @return Returns the handlerType.
	 */
	public String getHandlerType() {
		return handlerType;
	}

	/**
	 * @param handlerType The handlerType to set.
	 */
	public void setHandlerType(String handlerType) {
		this.handlerType = handlerType;
	}

	/**
	 * @return Returns the handlerResult.
	 */
	public String getHandlerResult() {
		return handlerResult;
	}

	/**
	 * @param handlerResult The handlerResult to set.
	 */
	public void setHandlerResult(String handlerResult) {
		this.handlerResult = handlerResult;
	}

	@Override
	public Object getReturnedValue() {
		String value = this.handlerResult;
		if (this.returnedValue != null) {
			if (!(returnedValue instanceof org.mozilla.javascript.Undefined))
				value = this.returnedValue.toString();
		}
		return value;
	}

	@Override
	public String toString() {
		return getName();
	}
	
	public String[] getResultStrings() {
		return new String[] { "", RETURN_CANCEL };
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("handlerType")){
			return new String[] { EVENT_TRANSACTION_STARTED, EVENT_XML_GENERATED };
		}else if(propertyName.equals("handlerResult")){
			return getResultStrings();
		}
		return new String[0];
	}
	
	public boolean preventFromLoops() {
		return handlerLoopable;
	}

	public void setPreventFromLoops(boolean preventFromLoops) {
		this.handlerLoopable = preventFromLoops;
	}
}
