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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/SequenceExecuteSelectedAction.java $
 * $Author: nathalieh $
 * $Revision: 30756 $
 * $Date: 2012-06-07 18:14:23 +0200 (Thu, 07 Jun 2012) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;


public class SequenceExecuteSelectedFromStubAction extends SequenceExecuteSelectedAction {

	public SequenceExecuteSelectedFromStubAction() {
		super();
	}

	@Override
	protected boolean isStubRequested() {
		return true;
	}
}
