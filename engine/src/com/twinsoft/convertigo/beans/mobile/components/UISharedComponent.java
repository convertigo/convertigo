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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;

public class UISharedComponent extends UIComponent implements IShared {

	private static final long serialVersionUID = -2430482045373902567L;

	public UISharedComponent() {
		super();
	}

	@Override
	public UISharedComponent clone() throws CloneNotSupportedException {
		UISharedComponent cloned = (UISharedComponent) super.clone();
		return cloned;
	}
	
	public List<UICompVariable> getVariables() {
		List<UICompVariable> list = new ArrayList<>();
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UICompVariable) {
				list.add((UICompVariable)component);
			}
		}
		return Collections.unmodifiableList(list);
	}
	
	@Override
	public String computeTemplate() {
		return null;
	}

	@Override
	public void computeScripts(JSONObject jsonScripts) {
		// does nothing
	}

	@Override
	protected void addContributors(Set<UIComponent> done, List<Contributor> contributors) {
		// does nothing
	}

	@Override
	protected void addInfos(Set<UIComponent> done, Map<String, Set<String>> infoMap) {
		// does nothing
	}

	protected String computeTemplate(UIUseShared uiUse) {
		String computed = "";
		if (isEnabled()) {
			computed += "<!-- '"+ getName() +"' shared component template -->" + System.lineSeparator();
			computed += "<ng-template #sc"+ this.priority +" let-params"+ this.priority +"=\"params"+ this.priority +"\" >" + System.lineSeparator();
			for (UIComponent uic: getUIComponentList()) {
				if (!(uic instanceof UICompVariable)) {
					try {
						computed += uic.cloneSetParent(uiUse).computeTemplate();
					} catch (CloneNotSupportedException e) {
						Engine.logBeans.warn("(UISharedComponent) computeTemplate: enabled to clone \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
					}
				}
			}
			computed += "</ng-template >" + System.lineSeparator();
		}
		return computed;
	}
	
	protected void computeScripts(UIUseShared uiUse, JSONObject jsonScripts) {
		if (isEnabled()) {
			for (UIComponent uic: getUIComponentList()) {
				try {
					uic.cloneSetParent(uiUse).computeScripts(jsonScripts);
				} catch (CloneNotSupportedException e) {
					Engine.logBeans.warn("(UISharedComponent) computeScripts: enabled to clone \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
				}
			}
		}
	}
	
	protected String computeStyle(UIUseShared uiUse) {
		String computed = "";
		if (isEnabled()) {
			for (UIComponent uic: getUIComponentList()) {
				if (uic instanceof UIElement) {
					try {
						computed += ((UIElement)uic.cloneSetParent(uiUse)).computeStyle();
					} catch (CloneNotSupportedException e) {
						Engine.logBeans.warn("(UISharedComponent) computeStyle: enabled to clone \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
					}
				}
			}
		}
		return computed;
	}
	
	protected void addContributors(UIUseShared uiUse, Set<UIComponent> done, List<Contributor> contributors) {
		if (!done.add(this)) {
			return;
		}
		Contributor contributor = getContributor();
		if (contributor != null) {
			if (!contributors.contains(contributor)) {
				contributors.add(contributor);
			}
		}
		for (UIComponent uic : getUIComponentList()) {
			try {
				uic.cloneSetParent(uiUse).addContributors(done, contributors);
			} catch (CloneNotSupportedException e) {
				Engine.logBeans.warn("(UISharedComponent) addContributors: enabled to clone \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
			}
		}
	}
	
	protected void addInfos(UIUseShared uiUse, Set<UIComponent> done, Map<String, Set<String>> infoMap) {
		if (!done.add(this)) {
			return;
		}
		for (UIComponent uic : getUIComponentList()) {
			try {
				uic.cloneSetParent(uiUse).addInfos(done, infoMap);
			} catch (CloneNotSupportedException e) {
				Engine.logBeans.warn("(UISharedComponent) addInfos: enabled to clone \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
			}
		}		
	}

	public void addPageEvent(UIUseShared uiUse, Set<UIComponent> done, List<UIPageEvent> eventList) {
		if (!done.add(this)) {
			return;
		}
		for (UIComponent uic : getUIComponentList()) {
			try {
				if (uic instanceof UIPageEvent) {
					eventList.add((UIPageEvent)uic);
				}
			} catch (Exception e) {
				Engine.logBeans.warn("(UISharedComponent) addPageEvent: enabled to add \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
			}
		}		
	}

	public void addEventSubscriber(UIUseShared uiUse, Set<UIComponent> done, List<UIEventSubscriber> eventList) {
		if (!done.add(this)) {
			return;
		}
		for (UIComponent uic : getUIComponentList()) {
			try {
				if (uic instanceof UIEventSubscriber) {
					eventList.add((UIEventSubscriber)uic);
				}
			} catch (Exception e) {
				Engine.logBeans.warn("(UISharedComponent) addEventSubscriber: enabled to add \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
			}
		}		
	}
}
