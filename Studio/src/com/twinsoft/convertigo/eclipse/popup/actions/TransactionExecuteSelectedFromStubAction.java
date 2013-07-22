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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/TransactionExecuteSelectedAction.java $
 * $Author: jibrilk $
 * $Revision: 30409 $
 * $Date: 2012-05-09 14:54:25 +0200 (Wed, 09 May 2012) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;


public class TransactionExecuteSelectedFromStubAction extends TransactionExecuteSelectedAction {

	public TransactionExecuteSelectedFromStubAction() {
		super();
	}

	@Override
	protected boolean isStubRequested() {
		return true;
	}
}
