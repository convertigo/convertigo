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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class UIDynamicInvoke extends UIDynamicAction {

	private static final long serialVersionUID = 2244390717640903291L;

	private String stack = "";
	
	public UIDynamicInvoke() {
		super();
	}

	public UIDynamicInvoke(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicInvoke clone() throws CloneNotSupportedException {
		UIDynamicInvoke cloned = (UIDynamicInvoke) super.clone();
		cloned.target = null;
		return cloned;
	}
	
	@Override
	protected boolean isBroken() {
		boolean isBroken = getSharedActionQName().isEmpty() || getTargetSharedAction() == null || !getTargetSharedAction().isEnabled();
		if (!isBroken) {
			isBroken = !ComponentRefManager.isEnabled(this);
		}
		return isBroken;
	}
	
	@Override
	public String getActionName() {
		return isBroken() ? "ErrorAction" : getTargetSharedAction().getActionName();
	}

	@Override
	public String getFunctionKey() {
		return getName() + "[" + getFunctionName() + "]";
	}
	
	public String getSharedActionQName() {
		return stack;
	}

	public void setSharedActionQName(String stack) {
		this.stack = stack;
	}

	public boolean isRecursive() {
		UIActionStack parentSharedAction = ((UIDynamicInvoke)this.getOriginal()).getSharedAction();
		// if UIDynamicInvoke is in a UIActionStack
		if (parentSharedAction != null) {
			UIActionStack targetSharedAction = this.getTargetSharedAction();
			// if UIDynamicInvoke has a target UIActionStack
			if (targetSharedAction != null) {
				// if they are the same
				if (parentSharedAction.priority == targetSharedAction.priority) {
					return true;
				}
			}
		}
		return false;
	}
	
	transient private UIActionStack target = null;
	
	public UIActionStack getTargetSharedAction() {
		String qname =  getSharedActionQName();
		if (target == null || !target.getQName().equals(qname)) {
			target = null;
			if (parent != null) { // parent may be null while dnd from palette
				if (qname.indexOf('.') != -1) {
					String p_name = qname.substring(0, qname.indexOf('.'));
					Project project = this.getProject();
					if (project != null) {
						Project p = null;
						try {
							p = Engine.theApp.referencedProjectManager.importProjectFrom(project, p_name);
							if (p == null) {
								throw new Exception();
							}
						} catch (Exception e) {
							Engine.logBeans.warn("(UIDynamicInvoke) For \""+  this.toString() +"\", targeted project \""+ p_name +"\" is missing !");
						}
						if (p != null) {
							if (p.getMobileApplication() != null) {
								try {
									ApplicationComponent app = (ApplicationComponent) p.getMobileApplication().getApplicationComponent();
									if (app != null) {
										for (UIActionStack uias: app.getSharedActionList()) {
											if (uias.getQName().equals(qname)) {
												target = uias;
												break;
											}
										}
									}
								} catch (ClassCastException e) {
									Engine.logBeans.warn("(UIDynamicInvoke) For \""+  this.toString() +"\", targeted action \""+ qname +"\" is not compatible !");
								}
							} else {
								Engine.logBeans.warn("(UIDynamicInvoke) For \""+  this.toString() +"\", targeted project \""+ p_name +"\" does not contain any mobile application !");
							}
							
							if (target == null) {
								Engine.logBeans.warn("(UIDynamicInvoke) For \""+  this.toString() +"\", targeted action \""+ qname +"\" is missing !");
							}
						}
					}
				} else {
					Engine.logBeans.warn("(UIDynamicInvoke) Action \""+ this.toString() +"\" has no target shared action defined !");
				}
			}
		}
		return target;
	}
	
	@Override
	protected Contributor getContributor() {
		// ErrorAction contributor or null
		return isBroken() ? super.getContributor() : null;
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		super.computeScripts(jsonScripts);
	}

	@Override
	protected void addContributors(Set<UIComponent> done, List<Contributor> contributors) {
		super.addContributors(done, contributors);
		
		if (!isEnabled()) return;
		
		// Now, add target stack contributors
		if (!isBroken()) {
			List<Contributor> _contributors = new ArrayList<Contributor>();
			getTargetSharedAction().addContributors(done, _contributors);
			for (Contributor contributor: _contributors) {
				contributor.setLink(this);
				contributors.add(contributor);
			}
		}
	}

	
	@Override
	protected void addInfos(Set<UIComponent> done, Map<String, Set<String>> infoMap) {
		super.addInfos(done, infoMap);
		
		if (!isEnabled()) return;
		
		// Now, add target stack infos
		if (!isBroken()) {
			getTargetSharedAction().addInfos(done, infoMap);
		}
	}

	@Override
	public String toString() {
		String stackName = this.stack.isEmpty() ? "?" : this.stack.substring(this.stack.lastIndexOf('.') + 1);
		return getName() + " (invoke " + stackName + ")";
	}
	
	@Override
	public String requiredTplVersion(Set<MobileComponent> done) {
		// initialize with invoke component min version required
		String tplVersion = getRequiredTplVersion();
		
		if (done.add(this)) {
			minTplVersion = tplVersion;
			
			// overwrites with target shared action min version required
			if (!stack.isEmpty()) {
				UIActionStack uias = getTargetSharedAction();
				if (uias == null && parent == null) { // palette dnd case
					try {
						String projectName = stack.split("\\.")[0];
						File f = Engine.projectFile(projectName);
						if (f != null && f.exists()) {
							uias = (UIActionStack) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(stack);
						}
					} catch (Exception e) {}
				}
				if (uias != null && uias.isEnabled()) {
					tplVersion = uias.requiredTplVersion(done);
				}
			}
			
			// overwrites with target child component min version required
			for (UIComponent uic : getUIComponentList()) {
				String uicTplVersion = uic.requiredTplVersion(done);
				if (MobileBuilder.compareVersions(tplVersion, uicTplVersion) <= 0) {
					tplVersion = uicTplVersion;
				}
			}
			
			minTplVersion = tplVersion;
		} else {
			tplVersion = minTplVersion;
		}
		
		return tplVersion;
	}
}
