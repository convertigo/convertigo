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

import java.util.ArrayList;
import java.util.List;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ConnectorView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.DatabaseObjectView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.ProjectView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.SequenceView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.SheetView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.StepView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.TransactionView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapObject;

public abstract class Studio implements WrapStudio {

	private List<WrapObject> selectedObjects = new ArrayList<>();
	protected boolean isActionDone = false;
	protected int response;

	@Override
	public List<WrapObject> getSelectedObjects() {
		return selectedObjects;
	}

	@Override
	public WrapObject getFirstSelectedTreeObject() {
	    return getSelectedObjects().isEmpty() ? null : getSelectedObjects().get(0);
	}

    public Object getFirstSelectedDatabaseObject() {
        Object object = null;
        WrapDatabaseObject treeObject = (WrapDatabaseObject) getFirstSelectedTreeObject();
        if (treeObject != null) {
            object = treeObject.getObject();
        }
        return object;
    }

	@Override
	public void addSelectedObject(DatabaseObject dbo) {
		selectedObjects.add(getViewFromDbo(dbo, this));
	}

	public static DatabaseObjectView getViewFromDbo(DatabaseObject dbo, WrapStudio studio) {
        if (dbo instanceof Project) {
            return new ProjectView((Project) dbo, studio); 
        }
        if (dbo instanceof Connector) {
            return new ConnectorView((Connector) dbo, studio); 
        }
        if (dbo instanceof Sequence) {
            return new SequenceView((Sequence) dbo, studio); 
        }
        if (dbo instanceof Transaction) {
            return new TransactionView((Transaction) dbo, studio); 
        }
        if (dbo instanceof Step) {
            return new StepView((Step) dbo, studio);
        }
        if (dbo instanceof Sheet) {
            return new SheetView((Sheet) dbo, studio);
        }

        return new DatabaseObjectView(dbo, studio);
	}

	@Override
	public void addSelectedObjects(DatabaseObject ...dbos) {
		for (DatabaseObject dbo: dbos) {
			addSelectedObject(dbo);
		}
	}

	@Override
	public void setResponse(int response) {
		synchronized (this) {
			this.response = response;
			notify();
		}
	}

	@Override
	public boolean isActionDone() {
		return isActionDone;
	}
}
