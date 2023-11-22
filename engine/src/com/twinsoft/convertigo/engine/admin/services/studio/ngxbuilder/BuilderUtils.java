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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IScriptComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager.Mode;

public class BuilderUtils {

	public static void dboAdded(DatabaseObject dbo) {
		if (!(dbo instanceof MobileComponent))
			return;
		Set<ApplicationComponent> appSet = new HashSet<ApplicationComponent>();
		Set<Object> mcSet = new HashSet<Object>();
		for (ApplicationComponent app : getApplicationList()) {
			dboAdded(appSet, mcSet, app, dbo, false);
		}
		dboUpdated(dbo);
	}

	public static void dboChanged(DatabaseObject dbo, String propertyName, Object oldValue, Object newValue) {
		if (!(dbo instanceof MobileComponent))
			return;
		Set<ApplicationComponent> appSet = new HashSet<ApplicationComponent>();
		Set<Object> mcSet = new HashSet<Object>();
		for (ApplicationComponent app : getApplicationList()) {
			dboChanged(appSet, mcSet, app, dbo, propertyName, oldValue, newValue);
		}
		dboUpdated(dbo);
	}

	public static void dboMoved(DatabaseObject fromParent, DatabaseObject toParent, DatabaseObject movedObject) {
		if (!(movedObject instanceof MobileComponent))
			return;
		dboRemoved(fromParent, movedObject);
		dboAdded(movedObject);
	}

	public static void dboRemoved(DatabaseObject parentOfDeleted, DatabaseObject deletedObject) {
		if (!(deletedObject instanceof MobileComponent))
			return;
		Set<ApplicationComponent> appSet = new HashSet<ApplicationComponent>();
		Set<Object> mcSet = new HashSet<Object>();
		for (ApplicationComponent app : getApplicationList()) {
			dboRemoved(appSet, mcSet, app, parentOfDeleted, deletedObject);
		}
		dboUpdated(parentOfDeleted);
	}

	public static void dboUpdated(DatabaseObject dbo) {
		if (!(dbo instanceof MobileComponent))
			return;
		dboUpdated(dbo, new HashSet<Object>());
	}

	private static void dboUpdated(DatabaseObject dbo, Set<Object> reset) {
		try {
			if (dbo != null && dbo instanceof MobileComponent) {
				MobileComponent mc = (MobileComponent) dbo;
				resetMainScriptComponents(dbo, reset);
				mc.getApplication().updateSourceFiles();

				if (dbo instanceof UIComponent) {
					UIComponent uic = (UIComponent) dbo;
					UIActionStack uias = uic.getSharedAction();
					UISharedComponent uisc = uic.getSharedComponent();
					String qname = uias != null ? uias.getQName() : (uisc != null ? uisc.getQName() : null);
					if (qname != null) {
						for (String projectName : Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true)) {
							Project p = (Project) Engine.theApp.databaseObjectsManager
									.getDatabaseObjectByQName(projectName);
							if (!dbo.getProject().equals(p)) {
								if (ComponentRefManager.isCompUsedBy(qname, projectName)) {
									ApplicationComponent app = (ApplicationComponent) p.getMobileApplication()
											.getApplicationComponent();
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

	private static List<ApplicationComponent> getApplicationList() {
		List<ApplicationComponent> list = new ArrayList<ApplicationComponent>();
		for (String projectName : Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true)) {
			try {
				Project p = (Project) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(projectName);
				ApplicationComponent app = (ApplicationComponent) p.getMobileApplication().getApplicationComponent();
				list.add(app);
			} catch (Exception e) {
			}
		}
		return list;
	}

	private static void dboAdded(Set<ApplicationComponent> done, Set<Object> reset, ApplicationComponent app,
			DatabaseObject dbo, boolean force) {
		try {
			String projectName = app.getProject().getName();
			boolean isChildOf = dbo.getQName().startsWith(app.getQName());
			boolean doUpdate = false;
			boolean doIt = dbo.bNew || force;
			if (doIt) {
				if (dbo instanceof UIComponent) {
					if (isChildOf) {
						// a shared component has been added to this app
						if (dbo instanceof UISharedRegularComponent) {
							File iconFile = new File(app.getProject().getDirPath(),
									((UISharedRegularComponent) dbo).getIconFileName());
							if (!iconFile.exists()) {
								// TODO
							}
						}
						// a UIDynamicInvoke has been added to this app
						if (dbo instanceof UIDynamicInvoke) {
							UIDynamicInvoke uidi = (UIDynamicInvoke) dbo;
							String compQName = uidi.getSharedActionQName();
							if (!compQName.isEmpty()) {
								ComponentRefManager.get(Mode.use).addConsumer(compQName, uidi.getQName());
							}
						}
						// a UIUseShared has been added to this app
						if (dbo instanceof UIUseShared) {
							UIUseShared uius = (UIUseShared) dbo;
							String compQName = uius.getSharedComponentQName();
							if (!compQName.isEmpty()) {
								ComponentRefManager.get(Mode.use).addConsumer(compQName, uius.getQName());
							}
						}
					} else {
						// an external shared action has changed and is used in this app
						UIActionStack uias = ((UIComponent) dbo).getSharedAction();
						if (uias != null && uias.isEnabled()) {
							if (ComponentRefManager.isCompUsedBy(uias.getQName(), projectName)) {
								doUpdate = true;
							}
						}
						// an external shared component has changed and is used in this app
						UISharedComponent uisc = ((UIComponent) dbo).getSharedComponent();
						if (uisc != null && uisc.isEnabled()) {
							if (ComponentRefManager.isCompUsedBy(uisc.getQName(), projectName)) {
								doUpdate = true;
							}
						}
					}
				}
			}

			if (doIt && (isChildOf || doUpdate)) {
				if (!done.add(app)) {
					return;
				}
				if (reset.add(app)) {
					app.reset();
					Engine.logEngine.trace("Application " + app.getQName() + " has been reset");
				}
				resetMainScriptComponents(dbo, reset);
			}
		} catch (Exception e) {
		}
	}

	private static void dboChanged(Set<ApplicationComponent> done, Set<Object> reset, ApplicationComponent app,
			DatabaseObject dbo, String propertyName, Object oldValue, Object newValue) {
		try {
			boolean doUpdate = false;
			boolean isChildOf = dbo.getQName().startsWith(app.getQName());
			String projectName = app.getProject().getName();
			if (app.equals(dbo)) {
				// application tpl has changed
				if (propertyName.equals("tplProjectName")) {
					/*
					 * Engine.logStudio.info("tplProjectName property of " + projectName +
					 * " changed, reloading builder..."); // TODO }
					 */
					return;
				}
			}
			if (dbo instanceof UIComponent) {
				if (isChildOf) {
					if (dbo instanceof UISharedRegularComponent || dbo instanceof UIActionStack) {
						// a shared component or shared action of this app changed its name
						if (propertyName.equals("name")) {
							String oldName = (String) oldValue;
							String newName = (String) newValue;

							// modify consumers
							ComponentRefManager.get(Mode.use).copyKey(oldName, newName);

							// rename shared component icon file
							if (dbo instanceof UISharedRegularComponent) {
								UISharedRegularComponent uisc = (UISharedRegularComponent) dbo;
								try {
									File oldIconFile = new File(app.getProject().getDirPath(),
											uisc.getIconFileName(oldName));
									File newIconFile = new File(app.getProject().getDirPath(),
											uisc.getIconFileName(newName));
									if (oldIconFile.exists() && !newIconFile.exists()) {
										oldIconFile.renameTo(newIconFile);
									}
								} catch (Exception e) {
								}
							}
						}
					}

					if (dbo instanceof UIDynamicInvoke) {
						UIDynamicInvoke uidi = (UIDynamicInvoke) dbo;
						String useQName = uidi.getQName();
						// a UIDynamicInvoke of this app changed its target shared component
						if (propertyName.equals("stack")) {
							String oldCompQName = (String) oldValue;
							String newCompQName = (String) newValue;
							if (!oldCompQName.isEmpty()) {
								ComponentRefManager.get(Mode.use).removeConsumer(oldCompQName, useQName);
							}
							if (!newCompQName.isEmpty()) {
								ComponentRefManager.get(Mode.use).addConsumer(newCompQName, useQName);
							}
						}
						// a UIDynamicInvoke of this app changed its enablement
						if (propertyName.equals("isEnabled")) {
							boolean oldEnabled = (Boolean) oldValue;
							boolean newEnabled = (Boolean) newValue;
							String compQName = uidi.getSharedActionQName();
							if (!compQName.isEmpty() && !oldEnabled && newEnabled) {
								ComponentRefManager.get(Mode.use).addConsumer(compQName, useQName);
							}
							if (!compQName.isEmpty() && oldEnabled && !newEnabled) {
								ComponentRefManager.get(Mode.use).removeConsumer(compQName, useQName);
							}
						}
					}
					if (dbo instanceof UIUseShared) {
						UIUseShared uius = (UIUseShared) dbo;
						String useQName = uius.getQName();
						// a UIUseShared of this app changed its target shared component
						if (propertyName.equals("sharedcomponent")) {
							String oldCompQName = (String) oldValue;
							String newCompQName = (String) newValue;
							if (!oldCompQName.isEmpty()) {
								ComponentRefManager.get(Mode.use).removeConsumer(oldCompQName, useQName);
							}
							if (!newCompQName.isEmpty()) {
								ComponentRefManager.get(Mode.use).addConsumer(newCompQName, useQName);
							}
						}
						// a UIUseShared of this app changed its enablement
						if (propertyName.equals("isEnabled")) {
							boolean oldEnabled = (Boolean) oldValue;
							boolean newEnabled = (Boolean) newValue;
							String compQName = uius.getSharedComponentQName();
							if (!compQName.isEmpty() && !oldEnabled && newEnabled) {
								ComponentRefManager.get(Mode.use).addConsumer(compQName, useQName);
							}
							if (!compQName.isEmpty() && oldEnabled && !newEnabled) {
								ComponentRefManager.get(Mode.use).removeConsumer(compQName, useQName);
							}
						}
					}
				} else {
					// an external shared action has changed and is used in this app
					UIActionStack uias = ((UIComponent) dbo).getSharedAction();
					if (uias != null) {
						if (ComponentRefManager.isCompUsedBy(uias.getQName(), projectName)) {
							doUpdate = true;
						}
					}
					// an external shared component has changed and is used in this app
					UISharedComponent uisc = ((UIComponent) dbo).getSharedComponent();
					if (uisc != null) {
						if (ComponentRefManager.isCompUsedBy(uisc.getQName(), projectName)) {
							doUpdate = true;
						}
					}
				}
			}

			if (app.equals(dbo) || isChildOf || doUpdate) {
				if (!done.add(app)) {
					return;
				}
				if (reset.add(app)) {
					app.reset();
					Engine.logEngine.trace("Application " + app.getQName() + " has been reset");
				}
				resetMainScriptComponents(dbo, reset);
			}
		} catch (Exception e) {
		}
	}

	private static void dboRemoved(Set<ApplicationComponent> done, Set<Object> reset, ApplicationComponent app,
			DatabaseObject parentOfDeleted, DatabaseObject deletedObject) {
		try {
			String projectName = app.getProject().getName();
			boolean isChildOf = parentOfDeleted.getQName().startsWith(app.getQName());
			boolean doUpdate = false;
			if (deletedObject != null) {
				String deletedobjectQName = parentOfDeleted.getQName() + "." + deletedObject.getName();

				if (isChildOf) {
					resetMainScriptComponents(parentOfDeleted, reset);
				}
				for (String useQName : ComponentRefManager.getCompConsumersUsedBy(deletedobjectQName, projectName)) {
					resetMainScriptComponents(ComponentRefManager.getDatabaseObjectByQName(useQName), reset);
				}

				if (isChildOf) {
					// an shared object of this app has been deleted
					if (deletedObject instanceof UIActionStack || deletedObject instanceof UISharedRegularComponent) {
						for (String useQName : ComponentRefManager.getCompConsumersUsedBy(deletedobjectQName,
								projectName)) {
							ComponentRefManager.get(Mode.use).removeConsumer(deletedobjectQName, useQName);
						}

						// delete shared component icon file
						if (deletedObject instanceof UISharedRegularComponent) {
							// TODO
							// File iconFile = new File(app.getProject().getDirPath(),
							// ((UISharedRegularComponent)deletedObject).getIconFileName());
							// FileUtils.deleteQuietly(iconFile);
						}
					}
					// a UIUseShared has been deleted
					if (deletedObject instanceof UIUseShared) {
						UIUseShared uius = (UIUseShared) deletedObject;
						String compQName = uius.getSharedComponentQName();
						if (!compQName.isEmpty()) {
							ComponentRefManager.get(Mode.use).removeConsumer(compQName, deletedobjectQName);
						}
					}
					// a UIDynamicInvoke has been deleted
					if (deletedObject instanceof UIDynamicInvoke) {
						UIDynamicInvoke uidi = (UIDynamicInvoke) deletedObject;
						String compQName = uidi.getSharedActionQName();
						if (!compQName.isEmpty()) {
							ComponentRefManager.get(Mode.use).removeConsumer(compQName, deletedobjectQName);
						}
					}
				} else {
					// an external shared object has been deleted and was used in this app
					if (deletedObject instanceof UIActionStack || deletedObject instanceof UISharedRegularComponent) {
						for (String useQName : ComponentRefManager.getCompConsumersUsedBy(deletedobjectQName,
								projectName)) {
							ComponentRefManager.get(Mode.use).removeConsumer(deletedobjectQName, useQName);
							doUpdate = true;
						}
					}
					// an object has been removed from an external object used in this app
					if (parentOfDeleted instanceof UIComponent) {
						UIComponent puic = (UIComponent) parentOfDeleted;

						UIActionStack uias = puic.getSharedAction();
						if (uias != null && uias.isEnabled()) {
							if (ComponentRefManager.isCompUsedBy(uias.getQName(), projectName)) {
								doUpdate = true;
							}
						}
						UISharedComponent uisc = puic.getSharedComponent();
						if (uisc != null && uisc.isEnabled()) {
							if (ComponentRefManager.isCompUsedBy(uisc.getQName(), projectName)) {
								doUpdate = true;
							}
						}
					}
				}
			}

			if (isChildOf || doUpdate) {
				if (!done.add(app)) {
					return;
				}
				if (reset.add(app)) {
					app.reset();
					Engine.logEngine.trace("Application " + app.getQName() + " has been reset");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
