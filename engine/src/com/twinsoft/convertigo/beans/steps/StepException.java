/*
 * Copyright (c) 2001-2023 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.steps;

import com.twinsoft.convertigo.engine.EngineException;

public class StepException extends EngineException {
	private static final long serialVersionUID = -3021122246529079529L;

	private String message;
	private String details;

	public StepException(String message, String details) {
		super(null);
		
		this.message = message;
		this.details = details;
	}
	
	@Override
	public String getErrorMessage() {
		return message;
	}

	@Override
	public String getErrorDetails() {
		return details;
	}

	@Override
	public String getMessage() {
		return message + " - " + details;
	}
}