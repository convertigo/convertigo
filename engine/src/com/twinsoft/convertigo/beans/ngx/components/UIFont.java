/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import com.twinsoft.convertigo.beans.common.FontSource;
import com.twinsoft.convertigo.beans.common.FormatedContent;

public class UIFont extends UIStyle implements IStyleGenerator {

	private static final long serialVersionUID = 4488989622835771303L;

	public UIFont() {
		super();
	}

	@Override
	public UIFont clone() throws CloneNotSupportedException {
		UIFont cloned = (UIFont) super.clone();
		return cloned;
	}
	
	private FontSource fontSource = new FontSource();

	public FontSource getFontSource() {
		return fontSource;
	}

	public void setFontSource(FontSource fontSource) {
		this.fontSource = fontSource;
	}

	private boolean isDefault = false;
	

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
	
	@Override
	public String computeStyle() {
		return fontSource.getStyleCssImport(!this.isTplStandalone());
	}
	
	@Override
	public FormatedContent getStyleContent() {
		return fontSource.getStyleContent();
	}

	@Override
	public String computeTemplate() {
//		if (isEnabled()) {
//			String computedStyle = getStyleContent().getString();
//			if (!computedStyle.isEmpty())
//				return computedStyle;
//		}
		return "";
	}
	
	@Override
	protected Contributor getContributor() {
		return new Contributor() {

			@Override
			public Map<String, File> getCompBeanDir() {
				return new HashMap<String, File>();
			}

			@Override
			public Map<String, String> getActionTsFunctions() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getActionTsImports() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getModuleTsImports() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getPackageDependencies() {
				Map<String, String> dependencies = new HashMap<String, String>();
				String fontSourceId = UIFont.this.getFontSource().getFontId();
				if (fontSourceId != null && !fontSourceId.isBlank()) {
					dependencies.put("@fontsource/"+ fontSourceId, "latest");
				}
				return dependencies;
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				return new HashMap<String, String>();
			}

			@Override
			public Set<String> getBuildAssets() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildScripts() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildStyles() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgImports() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgProviders() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgComponents() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgRoutes(String pageSegment) {
				return new HashSet<String>();
			}
		};
	}

	@Override
	public String toString() {
		return fontSource.toString();
	}
}
