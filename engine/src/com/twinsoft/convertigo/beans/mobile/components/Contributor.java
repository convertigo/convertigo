/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

import java.util.Set;
import java.io.File;
import java.util.Map;

public abstract class Contributor {
	public boolean isNgModuleForApp() {
		return false;
	}
	
	abstract public Map<String, File> getCompBeanDir();
	abstract public Map<String, String> getActionTsFunctions();
	abstract public Map<String, String> getActionTsImports();
	abstract public Map<String, String> getModuleTsImports();
	abstract public Map<String, String> getPackageDependencies();
	abstract public Map<String, String> getConfigPlugins();
	abstract public Set<String> getModuleNgImports();
	abstract public Set<String> getModuleNgProviders();
	abstract public Set<String> getModuleNgDeclarations();
	abstract public Set<String> getModuleNgComponents();
}
