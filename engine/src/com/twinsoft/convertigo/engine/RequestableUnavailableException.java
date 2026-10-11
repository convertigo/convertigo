/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

/**
 * The project, sequence, connector or transaction a request names does not exist, none can be chosen, or the
 * caller may not execute it. The message tells which: an HTTP caller who may not execute private requestables
 * gets the same plain answer for all these cases instead, so that it cannot learn which requestables exist.
 */
public class RequestableUnavailableException extends EngineException {

	private static final long serialVersionUID = 6150268563493618934L;

	public RequestableUnavailableException(String message) {
		super(message);
	}
}
