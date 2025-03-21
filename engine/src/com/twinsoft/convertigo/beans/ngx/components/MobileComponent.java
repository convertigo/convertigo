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

package com.twinsoft.convertigo.beans.ngx.components;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileObject;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

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
	
	public ApplicationComponent getApplication() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof ApplicationComponent) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null)
			return null;
		else
			return (ApplicationComponent) databaseObject;
	}
	
	
	@Override
	protected String getTplVersion() {
		try {
			return getApplication().getTplProjectVersion();
		} catch (Exception e) {
			return super.getTplVersion();
		}
	}

	protected String getTplVersionFromBuilder() {
		return super.getTplVersion();
	}
	
	@Override
	protected String getRequiredTplVersion() {
		//return "1.0.88";// the 7.5.0 has been released with CAF 1.0.88
		return "7.9.0.2"; // the 7.9.0 has been released with v7.9.0.2
	}

	@Override
	public String requiredTplVersion() {
		return requiredTplVersion(new HashSet<MobileComponent>());
	}
	
	protected transient String minTplVersion;
	
	public String requiredTplVersion(Set<MobileComponent> done) {
		String tplVersion = getRequiredTplVersion();
		if (done.add(this)) {
			minTplVersion = tplVersion;
		} else {
			tplVersion = minTplVersion;
		}
		return tplVersion;
	}
	
	public boolean isDeprecated() {
		// tpl version since bean is deprecated
		String tplDeprecatedVersion = getDeprecatedTplVersion();
		if (tplDeprecatedVersion.isEmpty()) {
			return false;
		}
		
		// project builder tpl version
		String tplBuilderVersion = getTplVersion();
		return MobileBuilder.compareVersions(tplBuilderVersion, tplDeprecatedVersion) >= 0;
	}

	public static String cleanStyle(String style) {
		try {
			if (style != null && !style.isEmpty()) {
				StringBuilder uses = new StringBuilder();
				StringBuilder imports = new StringBuilder();
				StringBuilder others = new StringBuilder();
				for (String line: Arrays.asList(style.split(System.lineSeparator()))) {
					line = line.trim();
					if (!line.isEmpty() && line.startsWith("@use")) {
						if (!uses.toString().contains(line)) {
							uses.append(line).append(System.getProperty("line.separator"));
						}
					} else if (!line.isEmpty() && line.startsWith("@import")) {
						if (!imports.toString().contains(line)) {
							imports.append(line).append(System.getProperty("line.separator"));
						}
					} else if (!line.isEmpty()) {
						others.append(line).append(System.getProperty("line.separator"));
					}
				}
				
				StringBuilder sb = new StringBuilder();
				if (uses.length() > 0) {
					sb.append(uses).append(System.getProperty("line.separator"));
				}
				if (imports.length() > 0) {
					sb.append(imports).append(System.getProperty("line.separator"));
				}
				if (others.length() > 0) {
					sb.append(others).append(System.getProperty("line.separator"));
				}
				return sb.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return style;
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
