/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IScriptComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager;

public class Utils {
	public static final Pattern parseQName = Pattern.compile("(.*?)(?::(\\w+?))?");

	public static DatabaseObject getDbo(String id) throws Exception {
		var reg = parseQName.matcher(id);
		reg.matches();
		var ft = FolderType.parse(reg.group(2));
		var qname = ft == null ? id : reg.group(1);
		return Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
	}

	public static FolderType getFolderType(String id) throws Exception {
		var reg = parseQName.matcher(id);
		reg.matches();
		return FolderType.parse(reg.group(2));
	}

	public static void dboUpdated(DatabaseObject dbo) {
		dboUpdated(dbo, new HashSet<Object>());
	}

	public static void dboUpdated(DatabaseObject dbo, Set<Object> reset) {
		try {
			if (dbo != null && dbo instanceof MobileComponent) {
				MobileComponent mc = ((MobileComponent) dbo);
				resetMainScriptComponents(dbo, reset);
				mc.getApplication().updateSourceFiles();

				if (dbo instanceof UIComponent) {
					UIComponent uic = (UIComponent)dbo;
					UIActionStack uias = uic.getSharedAction();
					UISharedComponent  uisc = uic.getSharedComponent();
					String qname = uias != null ? uias.getQName() : (uisc != null ? uisc.getQName() : null);
					if (qname != null) {
						for (String projectName: Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true)) {
							Project p = (Project) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(projectName);
							if (!dbo.getProject().equals(p)) {
								if (ComponentRefManager.isCompUsedBy(qname, projectName)) {
									ApplicationComponent app = (ApplicationComponent)p.getMobileApplication().getApplicationComponent();
									resetMainScriptComponents(app, reset);
									app.updateSourceFiles();
								}
							}
						}
					}
				}
}
		} catch (Exception e) {
			Engine.logEngine.error("Unabled to update application sources", e);
		}
	}

	private static void resetMainScriptComponents(DatabaseObject dbo, Set<Object> reset) {
		try {
			if (dbo != null) {
				if (!reset.add(dbo)) {
					return;
				}

				if (dbo instanceof ApplicationComponent) {
					ApplicationComponent app = (ApplicationComponent) dbo;
					if (!app.isReset()) {
						app.reset();
						Engine.logEngine.trace("App " + app.getQName() + " has been reset");
						return;
					}
				} else if (dbo instanceof PageComponent) {
					PageComponent page = (PageComponent) dbo;
					if (!page.isReset()) {
						page.reset();
						Engine.logEngine.trace("Page " + page.getQName() + " has been reset");
						return;
					}
				} else if (dbo instanceof UIComponent) {
					UIComponent uic = (UIComponent) dbo;
					IScriptComponent main = uic.getMainScriptComponent();
					if (main != null) {
						reset.add(main);
						if (main instanceof ApplicationComponent) {
							ApplicationComponent app = (ApplicationComponent) main;
							if (!app.isReset()) {
								app.reset();
								Engine.logEngine.trace("App " + app.getQName() + " has been reset");
							}
						} else if (main instanceof PageComponent) {
							PageComponent page = (PageComponent) main;
							if (!page.isReset()) {
								page.reset();
								Engine.logEngine.trace("Page " + page.getQName() + " has been reset");
							}
						} else if (main instanceof UISharedComponent) {
							UISharedComponent uisc = (UISharedComponent) main;
							if (!uisc.isReset()) {
								uisc.reset();
								Engine.logEngine.trace("Comp " + uisc.getQName() + " has been reset");
							}
						}
					}

					// reset direct UIDynamicInvoke components
					UIActionStack uias = uic.getSharedAction();
					if (uias != null) {
						for (String useQName : ComponentRefManager.getCompConsumers(uias.getQName())) {
							resetMainScriptComponents(ComponentRefManager.getDatabaseObjectByQName(useQName), reset);
						}
					}
					// reset direct UIUseShared components
					UISharedComponent uisc = uic.getSharedComponent();
					if (uisc != null) {
						for (String useQName : ComponentRefManager.getCompConsumers(uisc.getQName())) {
							resetMainScriptComponents(ComponentRefManager.getDatabaseObjectByQName(useQName), reset);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
