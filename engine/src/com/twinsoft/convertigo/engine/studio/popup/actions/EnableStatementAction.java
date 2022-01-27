/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine.studio.popup.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.responses.SetPropertyResponse;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapObject;

public class EnableStatementAction extends AbstractRunnableAction {

	public EnableStatementAction(WrapStudio studio) {
		super(studio);
	}

	@Override
	protected void run2() throws Exception {
        try {
			WrapObject[] treeObjects = studio.getSelectedObjects().toArray(new WrapObject[0]);

			for (int i = treeObjects.length - 1; i >= 0; --i) {
				WrapDatabaseObject treeObject = (WrapDatabaseObject) treeObjects[i];
				if (treeObject.instanceOf(Statement.class)) {
					//StepView stepTreeObject = (StepView) treeObject;

					Statement statement = (Statement) treeObject.getObject();
					statement.setEnabled(true);

					//stepTreeObject.setEnabled(true);
					//stepTreeObject.hasBeenModified(true);

//		                TreeObjectEvent treeObjectEvent = new TreeObjectEvent(stepTreeObject, "isEnable", false, true);
//		                explorerView.fireTreeObjectPropertyChanged(treeObjectEvent);
				}
			}
//				explorerView.refreshSelectedTreeObjects();
        }
        catch (Throwable e) {
            throw e;
        	//ConvertigoPlugin.logException(e, "Unable to enable step!");
        }
//        finally {
//			shell.setCursor(null);
//			waitCursor.dispose();
	}

	@Override
	public Element toXml(Document document, String qname) throws ConvertigoException, Exception {
		Element response = super.toXml(document, qname);
		if (response != null) {
			return response;
		}

		return new SetPropertyResponse("isEnabled").toXml(document, qname);
	}
}
