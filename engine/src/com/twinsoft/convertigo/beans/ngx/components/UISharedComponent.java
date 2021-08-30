/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;

public class UISharedComponent extends UIComponent implements IShared {

	private static final long serialVersionUID = -2430482045373902567L;

	public UISharedComponent() {
		super();
	}

	public UISharedComponent(boolean isRegular) {
		super();
		this.isRegular = isRegular;
	}
	
	@Override
	public UISharedComponent clone() throws CloneNotSupportedException {
		UISharedComponent cloned = (UISharedComponent) super.clone();
		return cloned;
	}
	
	private boolean isRegular = false;

	public boolean isTemplate() {
		return !isRegular;
	}
	
	public boolean isRegular() {
		return isRegular;
	}
	
	public String getIdentifier() {
		return "";
	}
	
	public String getSelector() {
		return "";
	}
	
	protected FormatedContent scriptContent = new FormatedContent("");

	public FormatedContent getScriptContent() {
		return scriptContent;
	}

	public void setScriptContent(FormatedContent scriptContent) {
		this.scriptContent = scriptContent;
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
	
	public List<UICompEvent> getUICompEventList() {
		return new ArrayList<UICompEvent>();
	}
	
	@Override
	public String computeJsonModel() {
		JSONObject jsonModel = new JSONObject();
		try {
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UICompVariable) {
					UICompVariable var = (UICompVariable)component;
					jsonModel.put(var.getVariableName(), "");
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonModel.toString();
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

	protected synchronized void doComputeContents() {
		// does nothing
	}
	
	protected String computeTemplate(UIUseShared uiUse) {
		return "";
	}
	
	protected void computeScripts(UIUseShared uiUse, JSONObject jsonScripts) {
		// does nothing
	}
	
	protected String computeStyle(UIUseShared uiUse) {
		return "";
	}
	
	protected void addContributors(UIUseShared uiUse, Set<UIComponent> done, List<Contributor> contributors) {
		// does nothing
	}
	
	protected void addInfos(UIUseShared uiUse, Set<UIComponent> done, Map<String, Set<String>> infoMap) {
		// does nothing
	}

	public void addPageEvent(UIUseShared uiUse, Set<UIComponent> done, List<UIPageEvent> eventList) {
		// does nothing
	}

	public void addEventSubscriber(UIUseShared uiUse, Set<UIComponent> done, List<UIEventSubscriber> eventList) {
		// does nothing
	}

	@Override
	public FolderType getFolderType() {
		return FolderType.SHARED_COMPONENT;
	}
	
	public String getComputedTemplate() {
		return "";
	}
	
	public String getComputedStyle() {
		return "";
	}
	
	public String getComputedImports() {
		return "";
	}
	
	public String getComputedInterfaces() {
		return "";
	}
	
	public String getComputedDeclarations() {
		return "";
	}
	
	public String getComputedConstructors() {
		return "";
	}

	public String getComputedInitializations() {
		return "";
	}
	
	public String getComputedDispositions() {
		return "";
	}

	public String getComputedFunctions() {
		return "";
	}

	public List<Contributor> getContributors() {
		return new ArrayList<Contributor>();
	}
	
	public void markCompTsAsDirty() throws EngineException {
		// does nothing
	}
		
	public void markCompAsDirty() throws EngineException {
		// does nothing
	}
}
