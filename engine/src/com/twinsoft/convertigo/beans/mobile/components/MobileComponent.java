/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import com.twinsoft.convertigo.beans.core.MobileObject;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;

public abstract class MobileComponent extends MobileObject {

	private static final long serialVersionUID = 5069650793367687807L;
	
	public MobileComponent() {
		super();
		databaseType = DatabaseObjectTypes.MobileComponent.name();
	}
	
	@Override
	public MobileComponent clone() throws CloneNotSupportedException {
		MobileComponent cloned = (MobileComponent) super.clone();
		return cloned;
	}
	
	@Override
	public ApplicationComponent getApplication() {
		return (ApplicationComponent) super.getApplication();
	}
	
	@Override
	protected String getRequiredTplVersion() {
		return "1.0.88";// the 7.5.0 has been released with CAF 1.0.88
	}

//	public String requiredTplVersion() {
//		return getRequiredTplVersion();
//	}
//	
//	protected String getTplVersion() {
//		Project p = getProject();
//		MobileBuilder mb = p == null ? null : p.getMobileBuilder();
//		String version = mb == null ? null : mb.getTplVersion();
//		
//		if (p == null) {
//			String message = "(MobileComponent.getTplVersion()) project is null for component " + getName() + 
//								(Engine.isStudioMode() ? " (probably removed component)": "");
//			if (Engine.isStudioMode()) {
//				Engine.logBeans.trace(message);
//			} else {
//				Engine.logBeans.warn(message);
//			}
//		} else {
//			if (mb == null) {
//				Engine.logBeans.warn("(MobileComponent.getTplVersion()) MB is null for component " + getQName());
//			} else if (version == null) {
//				Engine.logBeans.warn("(MobileComponent.getTplVersion()) Tpl version is null for component " + getQName() +
//						" (MB probably not intialized)");
//			}
//		}
//		return version;
//	}
//	
//	
//	public int compareToTplVersion(String version) {
//		int result = -1;
//		if (version != null) {
//			String tplVersion = getTplVersion();
//			if (tplVersion != null) {
//				if (tplVersion.trim().toLowerCase().equals("latest")) {
//					result = 1;
//				} else {
//					result = MobileBuilder.compareVersions(tplVersion, version);
//				}
//			}
//		}
//		return result;
//	}
}
