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

package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public abstract class MobileObject extends DatabaseObject implements IMobileObject {
	
	private static final long serialVersionUID = -2681872537732721040L;

	public MobileObject() {
		super();
		databaseType = "MobileObject";
	}

	@Override
	public MobileObject clone() throws CloneNotSupportedException {
		MobileObject cloned = (MobileObject) super.clone();
		return cloned;
	}
	
	protected String getRequiredTplVersion() {
		return "1.0.88";// the 7.5.0 has been released with CAF 1.0.88
	}
	
	public IApplicationComponent getApplication() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof IApplicationComponent) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null)
			return null;
		else
			return (IApplicationComponent) databaseObject;
	}
	
	public String requiredTplVersion() {
		return getRequiredTplVersion();
	}
	
	protected String getTplVersion() {
		Project p = getProject();
		MobileBuilder mb = p == null ? null : p.getMobileBuilder();
		String version = mb == null ? null : mb.getTplVersion();
		
		if (p == null) {
			String message = "(MobileComponent.getTplVersion()) project is null for component " + getName() + 
								(Engine.isStudioMode() ? " (probably removed component)": "");
			if (Engine.isStudioMode()) {
				Engine.logBeans.trace(message);
			} else {
				Engine.logBeans.warn(message);
			}
		} else {
			if (mb == null) {
				Engine.logBeans.warn("(MobileComponent.getTplVersion()) MB is null for component " + getQName());
			} else if (version == null) {
				Engine.logBeans.warn("(MobileComponent.getTplVersion()) Tpl version is null for component " + getQName() +
						" (MB probably not intialized)");
			}
		}
		return version;
	}
	
	
	public int compareToTplVersion(String version) {
		int result = -1;
		if (version != null) {
			String tplVersion = getTplVersion();
			if (tplVersion != null) {
				if (tplVersion.trim().toLowerCase().equals("latest")) {
					result = 1;
				} else {
					result = MobileBuilder.compareVersions(tplVersion, version);
				}
			}
		}
		return result;
	}
	
}
