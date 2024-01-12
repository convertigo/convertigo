/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.studio.views.projectexplorer.model;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.Studio;
import com.twinsoft.convertigo.engine.studio.WrapStudio;

public class DatabaseObjectView implements WrapDatabaseObject {

    protected DatabaseObject dbo;
	protected CheStudio studio;

	public DatabaseObjectView(DatabaseObject dbo, WrapStudio studio) {
		this.dbo = dbo;
        this.studio = (CheStudio) studio;
	}

	@Override
	public DatabaseObject getObject() {
	    return dbo;
	}

	@Override
	public WrapDatabaseObject getParent() {
	    return Studio.getViewFromDbo(dbo.getParent(), studio);
	}

	@Override
	public String getName() {
		return dbo.getName();
	}

	@Override
	public void hasBeenModified(boolean hasBeenModified) {
	}

    @Override
    public ProjectView getProjectViewObject() {
        return new ProjectView(dbo.getProject(), studio);
    }
}
