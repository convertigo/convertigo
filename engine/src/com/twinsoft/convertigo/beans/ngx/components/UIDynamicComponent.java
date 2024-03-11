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

package com.twinsoft.convertigo.beans.ngx.components;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
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

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement#getContributor()
	 */
	@Override
	protected Contributor getContributor() {
		Contributor contributor = super.getContributor();
		return new Contributor() {

			@Override
			public Map<String, String> getActionTsFunctions() {
				return contributor.getActionTsFunctions();
			}

			@Override
			public Map<String, String> getActionTsImports() {
				return contributor.getActionTsImports();
			}

			@Override
			public Map<String, File> getCompBeanDir() {
				Map<String, File> map = new HashMap<String, File>();
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					String compName = ionBean.getComponent();//ionBean.getName();
					File dir = ComponentManager.getCompBeanDir(compName);
					if (dir != null) {
						map.put(compName, dir);
					}
				}
				return map;
			}

			@Override
			public Map<String, String> getModuleTsImports() {
				return contributor.getModuleTsImports();
			}

			@Override
			public Set<String> getModuleNgImports() {
				return contributor.getModuleNgImports();
			}

			@Override
			public Set<String> getModuleNgProviders() {
				return contributor.getModuleNgProviders();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				return contributor.getModuleNgDeclarations();
			}
			
			@Override
			public Set<String> getModuleNgComponents() {
				return contributor.getModuleNgComponents();
			}
			
			@Override
			public Map<String, String> getPackageDependencies() {
				return contributor.getPackageDependencies();
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				return contributor.getConfigPlugins();
			}
			
			@Override
			public Set<String> getModuleNgRoutes(String pageSegment) {
				return contributor.getModuleNgRoutes(pageSegment);
			}

			@Override
			public Set<String> getBuildAssets() {
				return contributor.getBuildAssets();
			}

			@Override
			public Set<String> getBuildScripts() {
				return contributor.getBuildScripts();
			}

			@Override
			public Set<String> getBuildStyles() {
				return contributor.getBuildStyles();
			}
		};
	}
	
}
