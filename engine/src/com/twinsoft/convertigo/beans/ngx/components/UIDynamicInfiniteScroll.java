/*
 * Copyright (c) 2001-2020 Convertigo SA.
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
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.engine.EngineException;

public class UIDynamicInfiniteScroll extends UIDynamicComponent {

	private static final long serialVersionUID = 1712222687306753815L;

	public UIDynamicInfiniteScroll() {
		super();
	}

	public UIDynamicInfiniteScroll(String tagName) {
		super(tagName);
	}
	
	@Override
	public UIDynamicInfiniteScroll clone() throws CloneNotSupportedException {
		UIDynamicInfiniteScroll cloned = (UIDynamicInfiniteScroll) super.clone();
		return cloned;
	}

	private String scrollaction = "";
	
	
	public String getScrollAction() {
		return scrollaction;
	}

	public void setScrollAction(String scrollaction) {
		this.scrollaction = scrollaction;
	}

	private UIDynamicAction getTargetAction() {
		try {
			String qname =  getScrollAction();
			if (qname != null && qname.indexOf('.') != -1) {
				ApplicationComponent app  = getApplication();
				
				StringTokenizer strTkn = new StringTokenizer(qname, ".");
				if (strTkn.hasMoreElements()) {
					String token = null;
					do {
						token = strTkn.nextToken();
					} while (!app.getName().equals(token));
					
					token = strTkn.nextToken();
					
					MobileComponent mbc = null;
					try {
						mbc = app.getMenuComponentByName(token);
					} catch (Exception e1) {
						try {
							mbc = app.getPageComponentByName(token);
						} catch (Exception e2) {}
					}
					
					if (mbc != null) {
						UIDynamicAction uida = getTargetActionTask(strTkn, mbc);
						return uida;
					}
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private UIDynamicAction getTargetActionTask(StringTokenizer strTkn, MobileComponent mbc) {
		if (mbc != null) {
			if (strTkn.hasMoreTokens()) {
				String token = strTkn.nextToken();
				
				UIComponent uic;
				try {
					uic = mbc instanceof PageComponent ?
								((PageComponent)mbc).getUIComponentByName(token) :
									((UIComponent)mbc).getUIComponentByName(token);
					return getTargetActionTask(strTkn, uic);
				} catch (EngineException e) {}
			}
		}
		return null;
	}
	
	private UIDynamicAction getTargetActionTask(StringTokenizer strTkn, UIComponent uic) {
		if (uic != null) {
			int size = uic.getUIComponentList().size();
			boolean hasMoreTokens = strTkn.hasMoreTokens();
			
			if (!hasMoreTokens) {
				return uic instanceof UIDynamicAction ? ((UIDynamicAction)uic) : null;
			} else if (size > 0) {
				String token = strTkn.nextToken();
				
				UIComponent child;
				try {
					child = uic.getUIComponentByName(token);
					return getTargetActionTask(strTkn, child);
				} catch (EngineException e) {}
			}
		}
		return null;
	}
	
	@Override
	protected StringBuilder initAttributes() {
		StringBuilder sb = super.initAttributes();
		UIDynamicAction uida = getTargetAction();
		if (uida != null) {
			//System.out.println("found "+ uida.getQName());
			String actionName = uida.getActionName();
			boolean isCallSequence = "CallSequenceAction".equals(actionName);
			boolean isFullSyncView = "FullSyncViewAction".equals(actionName);
			if (isCallSequence || isFullSyncView) {
				String inputs = uida.computeActionInputs(true);
				String props = "{}", vars = "{}";
				Pattern pattern = Pattern.compile("\\{props:(\\{.*\\}), vars:(\\{.*\\})\\}");
				Matcher matcher = pattern.matcher(inputs);
				if (matcher.matches()) {
					props = matcher.group(1);
					vars = matcher.group(2);
				}
				
				sb.append(" [c8oPage]=\"this\"")
					.append(isCallSequence ? " [c8oSParams]=" : " [c8oVParams]=").append("\"merge(merge({},"+ props +"), "+ vars +")\"");
			}
		}
		return sb;
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
				Map<String, File> map = new HashMap<String, File>();
				for (String compName: contributor.getModuleNgComponents()) {
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
		};
	}
}
