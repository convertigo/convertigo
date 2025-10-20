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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Contributor {

	public void forContainer(final MobileComponent container, ILambda lambda) {
		forContainer(container, false, lambda);
	}
	
	public void forContainer(final MobileComponent container, boolean isMainTs, ILambda lambda) {
		setContainer(container);
		setMainTs(isMainTs);
		lambda.apply();
		setMainTs(false);
		setContainer(null);
	}
	
	private MobileComponent container = null;
	
	protected void setContainer(MobileComponent container) {
		this.container = container;
	}
	
	protected MobileComponent getContainer() {
		return container;
	}
	
	private boolean isMainTs = false;
	
	protected void setMainTs(boolean isMainTs) {
		this.isMainTs = isMainTs;
	}
	
	protected boolean isMainTs() {
		return this.isMainTs;
	}
	
	private UIComponent link = null;
	
	protected UIComponent getLink() {
		return link;
	}

	protected void setLink(UIComponent link) {
		this.link = link;
	}

	public boolean isNullContainer() {
		return container == null ? true : false;
	}
	public boolean isAppContainer() {
		return container == null ? false : container instanceof ApplicationComponent;
	}
	public boolean isPageContainer() {
		return container == null ? false : container instanceof PageComponent;
	}
	public boolean isCompContainer() {
		return container == null ? false : container instanceof UISharedComponent;
	}
	
	public boolean isContainer(MobileComponent mc) {
		if (mc != null) {
			return mc.equals(container);
		}
		return false;
	}
	public boolean isNgModuleForApp() {
		return false;
	}
	
	@Override
	public String toString() {
        String result = getCompBeanDir().entrySet()
                			.stream()
                			.map(entry -> entry.getKey() + " - " + entry.getValue())
                			.collect(Collectors.joining(", "))
                		+ getActionTsFunctions().entrySet()
                			.stream()
                			.map(entry -> entry.getKey() + " - " + entry.getValue())
                			.collect(Collectors.joining(", "))
                    	+ getActionTsImports().entrySet()
                			.stream()
                			.map(entry -> entry.getKey() + " - " + entry.getValue())
                			.collect(Collectors.joining(", "))
                    	+ getModuleTsImports().entrySet()
                			.stream()
                			.map(entry -> entry.getKey() + " - " + entry.getValue())
                			.collect(Collectors.joining(", "))
                    	+ getPackageDependencies().entrySet()
                			.stream()
                			.map(entry -> entry.getKey() + " - " + entry.getValue())
                			.collect(Collectors.joining(", "))
                    	+ getConfigPlugins().entrySet()
                			.stream()
                			.map(entry -> entry.getKey() + " - " + entry.getValue())
                			.collect(Collectors.joining(", "))
                    	+ getModuleNgImports()
                    		.stream()
                			.collect(Collectors.joining(", "))
                    	+ getModuleNgProviders()
                			.stream()
                			.collect(Collectors.joining(", "))
                    	+ getModuleNgDeclarations()
                			.stream()
                			.collect(Collectors.joining(", "))
                    	+ getModuleNgComponents()
                			.stream()
                			.collect(Collectors.joining(", "))
                    	+ getModuleNgRoutes("")
                			.stream()
                			.collect(Collectors.joining(", "))
    				    + getBuildAssets()
    						.stream()
    						.collect(Collectors.joining(", "))
					    + getBuildScripts()
							.stream()
							.collect(Collectors.joining(", "))
					    + getBuildStyles()
							.stream()
							.collect(Collectors.joining(", "));	
        
		return result;
	}
	
	abstract public Map<String, File> getCompBeanDir();
	abstract public Map<String, String> getActionTsFunctions();
	abstract public Map<String, String> getActionTsImports();
	abstract public Map<String, String> getModuleTsImports();
	abstract public Map<String, String> getPackageDependencies();
	abstract public Map<String, String> getConfigPlugins();
	abstract public Set<String> getBuildAssets();
	abstract public Set<String> getBuildScripts();
	abstract public Set<String> getBuildStyles();
	abstract public Set<String> getModuleNgImports();
	abstract public Set<String> getModuleNgProviders();
	abstract public Set<String> getModuleNgDeclarations();
	abstract public Set<String> getModuleNgComponents();
	abstract public Set<String> getModuleNgRoutes(String pageSegment);
}
