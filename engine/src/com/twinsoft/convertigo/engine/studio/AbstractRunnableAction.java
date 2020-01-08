/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.studio;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.ConvertigoException;

public abstract class AbstractRunnableAction {

	protected WrapStudio studio;
	private boolean isDone = false;

	public AbstractRunnableAction(WrapStudio studio) {
		this.studio = studio;
	}

	public void run() throws Exception {
		run2();
		isDone = true;
	}

	protected abstract void run2() throws Exception;

	public Element toXml(Document document, String qname) throws ConvertigoException, Exception {
		// Can't generate XML while the action is not finished yet
		if (!isDone) {
			throw new ConvertigoException("The action is not finished yet.");
		}

		return null;
	}

	public boolean isDone() {
		return isDone;
	}
}
