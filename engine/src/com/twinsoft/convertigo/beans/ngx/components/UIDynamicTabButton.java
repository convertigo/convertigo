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

package com.twinsoft.convertigo.beans.ngx.components;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class UIDynamicTabButton extends UIDynamicElement {

	private static final long serialVersionUID = 1116808929065077751L;

	public UIDynamicTabButton() {
		super();
	}

	public UIDynamicTabButton(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicTabButton clone() throws CloneNotSupportedException {
		UIDynamicTabButton cloned = (UIDynamicTabButton) super.clone();
		return cloned;
	}
	
	/*
	 * The page associated with tab
	 */
	private String tabpage = "";
	
	public String getTabQName() {
		return tabpage;
	}

	public void setTabQName(String tabpage) {
		this.tabpage = tabpage;
	}

	private String getTabName() {
		if (!tabpage.isEmpty()) {
			try {
				return tabpage.substring(tabpage.lastIndexOf('.')+1);
			} catch (Exception e) {}
		}
		return "";
	}
	
	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();
		String tabName = getTabName();
		if (!tabName.isEmpty()) {
			try {
				attributes.append(" tab").append("=").append("\""+ tabName +"\"");
			} catch (Exception e) {}
		}
		return attributes;
	}
	
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
				return contributor.getCompBeanDir();
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
				Set<String> set = contributor.getModuleNgRoutes(pageSegment);
				String tabName = getTabName();
				if (!tabName.isEmpty()) {
					String tabLower = tabName.toLowerCase();
					String tabModulePath = "../"+ tabLower +"/"+ tabLower +".module";
					String tabModuleName = tabName +"Module";
					set.add("{path: '', redirectTo: '/"+ pageSegment +"/"+tabName+"', pathMatch: 'full'}");
					set.add("{path: '"+ tabName +"', children: [{path: '', loadChildren: () => import('"+tabModulePath+"').then( m => m."+ tabModuleName +")}]}");
				}
				return set;
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
	
	@Override
	public String toString() {
		String tabName = getTabName();
		return super.toString() + ": " + (tabName.isEmpty() ? "?":tabName);
	}
}
