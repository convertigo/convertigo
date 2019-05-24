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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class UIUseShared extends UIElement {

	private static final long serialVersionUID = -6355983207888274045L;

	public UIUseShared() {
		super();
	}

	@Override
	public UIUseShared clone() throws CloneNotSupportedException {
		UIUseShared cloned = (UIUseShared) super.clone();
		cloned.target = null;
		return cloned;
	}
	
	private String sharedcomponent = "";
	
	public String getSharedComponentQName() {
		return sharedcomponent;
	}

	public void setSharedComponentQName(String sharedComponent) {
		this.sharedcomponent = sharedComponent;
	}

	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		throw new EngineException("You cannot add another component to this component");
	}
	
	@Override
	public List<UIComponent> getUIComponentList() {
		return super.getUIComponentList();
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			UISharedComponent uisc = getTargetSharedComponent();
			if (uisc != null) {
				return uisc.computeTemplate(this);
			}
		}
		return "";
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		if (isEnabled()) {
			UISharedComponent uisc = getTargetSharedComponent();
			if (uisc != null) {
				uisc.computeScripts(this, jsonScripts);
			}
		}
	}

	@Override
	public String computeStyle() {
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			return uisc.computeStyle(this);
		}
		return "";
	}
	
	@Override
	protected void addContributors(Set<UIComponent> done, List<Contributor> contributors) {
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			uisc.addContributors(this, done, contributors);
		}
	}
	
	@Override
	protected void addInfos(Map<String, Set<String>> infoMap) {
		UISharedComponent uisc = getTargetSharedComponent();
		if (uisc != null) {
			uisc.addInfos(this, infoMap);
		}
	}
	
	transient private UISharedComponent target = null;
	
	private UISharedComponent getTargetSharedComponent() {
		String qname =  getSharedComponentQName();
		if (target == null || !target.getQName().equals(qname)) {
			target = null;
			if (qname.indexOf('.') != -1) {
				String p_name = qname.substring(0, qname.indexOf('.'));
				Project project = this.getProject();
				if (project != null) {
					Project p = null;
					try {
						p = p_name.equals(project.getName()) ? project: Engine.theApp.databaseObjectsManager.getOriginalProjectByName(p_name);
					} catch (EngineException e) {
						Engine.logBeans.warn("(UIUseShared) For \""+  this.toString() +"\", targeted project \""+ p_name +"\" is missing !");
					}
					if (p != null) {
						for (UISharedComponent uisc: p.getMobileApplication().getApplicationComponent().getSharedComponentList()) {
							if (uisc.getQName().equals(qname)) {
								target = uisc;
							}
						}
						if (target == null) {
							Engine.logBeans.warn("(UIUseShared) For \""+  this.toString() +"\", targeted component \""+ qname +"\" is missing !");
						}
					}
				}
			} else {
				Engine.logBeans.warn("(UIUseShared) Component \""+ this.toString() +"\" has no target shared component defined !");
			}
		}
		return target;
	}
	
	@Override
	public String toString() {
		String compName = this.sharedcomponent.isEmpty() ? "?" : this.sharedcomponent.substring(this.sharedcomponent.lastIndexOf('.') + 1);
		return "use " + compName;
	}
}
