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

import java.util.Calendar;
import java.util.Locale;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class CreateEmptyStubAction extends AbstractStubAction {

	public CreateEmptyStubAction() {
		super();
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		try {
			super.selectionChanged(action, selection);
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
			Object ob = treeObject.getObject();
			action.setEnabled(!(ob instanceof SiteClipperTransaction));
		}
		catch (Exception e) {}
	}

	public Document getXML(TreeObject treeObject) throws Exception {
		Document dom = XMLUtils.createDom("java");
		Element documentElement = (Element) dom.createElement("document");
		Object requestable = treeObject.getObject();

		if (requestable instanceof Transaction) {
			Transaction transaction = (Transaction) requestable;
			documentElement.setAttribute("connector", transaction.getParent().getName());
			documentElement.setAttribute("fromcache", "false");
			documentElement.setAttribute("generated", Calendar.getInstance(Locale.getDefault()).getTime().toString());
			documentElement.setAttribute("project", transaction.getProject().getName());
			documentElement.setAttribute("sequence", "");
			documentElement.setAttribute("transaction", transaction.getName());
		} else if (requestable instanceof Sequence) {
			Sequence sequence = (Sequence) requestable;
			documentElement.setAttribute("connector", "");
			documentElement.setAttribute("fromcache", "false");
			documentElement.setAttribute("generated", Calendar.getInstance(Locale.getDefault()).getTime().toString());
			documentElement.setAttribute("project", sequence.getProject().getName());
			documentElement.setAttribute("sequence", sequence.getName());
			documentElement.setAttribute("transaction", "");
		}
		dom.appendChild(documentElement);
		return dom;
	}
}
