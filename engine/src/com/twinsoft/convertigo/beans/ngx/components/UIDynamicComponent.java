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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class UIDynamicComponent extends UIDynamicElement {

	private static final long serialVersionUID = 4724936673248748018L;

	public UIDynamicComponent() {
		super();
	}

	public UIDynamicComponent(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicComponent clone() throws CloneNotSupportedException {
		UIDynamicComponent cloned = (UIDynamicComponent) super.clone();
		return cloned;
	}
	
	@Override
	protected String getRequiredTplVersion() {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			String beanTplVersion = ionBean.getTplVersion();
			if (MobileBuilder.compareVersions(beanTplVersion, "1.0.100") >= 0) {
				return beanTplVersion;
			}
		}
		return "1.0.100";
	}

	protected String getCompName() {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			String compName = ionBean.getComponent(); // since TPL 8.3.0.0
			if ("component".equals(compName)) {
				compName = ionBean.getName(); // before TPL 8.3.0.0
			}
			return compName;
		}
		return "unknow";
	}
	
	private boolean isValid() {
		File dir = ComponentManager.of(this).getCompBeanDir(getCompName());
		return dir != null && dir.exists() && dir.isDirectory();
	}
	
	
	@Override
	public String computeTemplate() {
		if (isValid()) {
			return super.computeTemplate();
		}
		String templateProjectName = ComponentManager.of(this).getTemplateProjectName();
		String invalidText = getCompName() + " (tagname: " + getTagName() + ")";
		Engine.logBeans.warn(invalidText + " does not exists in "+ templateProjectName);
		return "<!-- Warn:" + invalidText + " does not exists in "+ templateProjectName + "-->" + System.getProperty("line.separator");
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement#getContributor()
	 */
	@Override
	protected Contributor getContributor() {
		Contributor contributor = super.getContributor();
		return new Contributor() {

			@Override
			public Map<String, String> getActionTsFunctions() {
				return isValid() ? contributor.getActionTsFunctions() : new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getActionTsImports() {
				return isValid() ? contributor.getActionTsImports() : new HashMap<String, String>();
			}

			@Override
			public Map<String, File> getCompBeanDir() {
				Map<String, File> map = new HashMap<String, File>();
				String compName = getCompName();
				File dir = ComponentManager.of(getContainer()).getCompBeanDir(compName);
				if (dir != null) {
					map.put(compName, dir);
				}
				return map;
			}

			@Override
			public Map<String, String> getModuleTsImports() {
				return isValid() ? contributor.getModuleTsImports() : new HashMap<String, String>();
			}

			@Override
			public Set<String> getModuleNgImports() {
				return isValid() ? contributor.getModuleNgImports() : new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgProviders() {
				return isValid() ? contributor.getModuleNgProviders() : new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				return isValid() ? contributor.getModuleNgDeclarations() : new HashSet<String>();
			}
			
			@Override
			public Set<String> getModuleNgComponents() {
				return isValid() ? contributor.getModuleNgComponents() : new HashSet<String>();
			}
			
			@Override
			public Map<String, String> getPackageDependencies() {
				return isValid() ? contributor.getPackageDependencies() : new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				return isValid() ? contributor.getConfigPlugins() : new HashMap<String, String>();
			}
			
			@Override
			public Set<String> getModuleNgRoutes(String pageSegment) {
				return isValid() ? contributor.getModuleNgRoutes(pageSegment) : new HashSet<String>();
			}

			@Override
			public Set<String> getBuildAssets() {
				return isValid() ? contributor.getBuildAssets() : new HashSet<String>();
			}

			@Override
			public Set<String> getBuildScripts() {
				return isValid() ? contributor.getBuildScripts() : new HashSet<String>();
			}

			@Override
			public Set<String> getBuildStyles() {
				return isValid() ? contributor.getBuildStyles() : new HashSet<String>();
			}
		};
	}
	
}
