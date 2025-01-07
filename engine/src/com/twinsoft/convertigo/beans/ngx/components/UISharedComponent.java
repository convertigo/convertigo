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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.WordUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboFolderType(type = FolderType.SHARED_COMPONENT)
public class UISharedComponent extends UIComponent implements IShared, IExposeAble {

	private static final long serialVersionUID = -2430482045373902567L;

	static public String getNsCompDirPath(UISharedComponent uisc) {
		if (uisc != null) {
			return getNsCompDirPath(uisc.getQName(), uisc.getName());
		}
		return null;
	}
	
	static public String getNsCompDirPath(String compQName, String compName) {
		if (compQName != null && compName != null) {
			String lowerCompName = compName.toLowerCase();
			try {
				String lowerDirName = getNsCompDirName(compQName, compName);
				return "_private/ionic/src/app/components/"+ lowerDirName;
			} catch (Exception e) {}
			return "_private/ionic/src/app/components/"+ lowerCompName;
		}
		return null;
	}
		
	static public String getNsCompDirName(UISharedComponent uisc) {
		if (uisc != null) {
			return getNsCompDirName(uisc.getQName(), uisc.getName());
		}
		return null;
	}
	
	static public String getNsCompDirName(String compQName, String compName) {
		return joinLower(compQName, compName, ".");
	}
	
	static public String getNsCompFileName(UISharedComponent uisc) {
		if (uisc != null) {
			return getNsCompFileName(uisc.getQName(), uisc.getName());
		}
		return null;
	}
	
	static public String getNsCompFileName(String compQName, String compName) {
		return joinLower(compQName, compName, "-");
	}
	
	static public String getNsCompIdentifier(UISharedComponent uisc) {
		if (uisc != null) {
			return getNsCompIdentifier(uisc.getQName(), uisc.getName());
		}
		return null;
	}
	
	static public String getNsCompIdentifier(String compQName, String compName) {
		return joinLower(compQName, compName, "_");
	}
	
	static public String getNsCompName(UISharedComponent uisc) {
		if (uisc != null) {
			return getNsCompName(uisc.getQName(), uisc.getName());
		}
		return null;
	}
	
	static public String getNsCompName(String compQName, String compName) {
		return joinCapitalize(compQName, compName, "_");
	}
	
	static protected String joinLower(String dboQName, String dboName, String separator) {
		if (dboQName != null && dboName != null) {
			String lowerDboName = dboName.toLowerCase();
			try {
				String lowerProjectName = dboQName.split("\\.")[0].toLowerCase();
				return lowerProjectName + separator + lowerDboName;
			} catch (Exception e) {}
			return lowerDboName;
		}
		return null;
	}
	
	static protected String joinCapitalize(String dboQName, String dboName, String separator) {
		if (dboQName != null && dboName != null) {
			try {
				String projectName = dboQName.split("\\.")[0];
				return WordUtils.capitalize(projectName) + "_" + WordUtils.capitalize(dboName);
			} catch (Exception e) {}
			return dboName;
		}
		return null;
	}
	
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
	
	public String getNsIdentifier() {
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
	
	private boolean exposed = true;
	
	@Override
	public boolean isExposed() {
		return exposed;
	}
	
	public void setExposed(boolean exposed) {
		this.exposed = exposed;
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
	
	public List<UIFont> getUIFontList() {
		List<UIFont> eventList = new ArrayList<>();
		for (UIComponent uiComponent : getUIComponentList()) {
			if (uiComponent instanceof UIFont) {
				eventList.add((UIFont) uiComponent);
			}
		}
		return eventList;
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
	
	public void reset() {
		
	}
	
	public boolean isReset() {
		return false;
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
}
