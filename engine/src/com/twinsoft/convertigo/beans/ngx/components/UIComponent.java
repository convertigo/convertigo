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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.IEnableAble;
import com.twinsoft.convertigo.beans.core.IUIComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.InvalidSourceException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@DboCategoryInfo(
		getCategoryId = "UIComponent",
		getCategoryName = "UI Component",
		getIconClassCSS = "convertigo-action-newUIComponent"
	)
public abstract class UIComponent extends MobileComponent implements IUIComponent, IScriptGenerator, ITemplateGenerator, IContainerOrdered, IEnableAble {
	
	private static final long serialVersionUID = -1872010547443624681L;

	protected static Pattern paramsPattern = Pattern.compile("^params\\d+\\..+");
	
	transient private XMLVector<XMLVector<Long>> orderedComponents = new XMLVector<XMLVector<Long>>();
	
	private boolean isEnabled = true;
	
	public UIComponent() {
		super();
		
		this.priority = getNewOrderValue();
		
		orderedComponents = new XMLVector<XMLVector<Long>>();
		orderedComponents.add(new XMLVector<Long>());
	}

	@Override
	public UIComponent clone() throws CloneNotSupportedException {
		UIComponent cloned = (UIComponent) super.clone();
		cloned.vUIComponents = new LinkedList<UIComponent>();
		return cloned;
	}

	// Used by UISharedComponent for UIUseShared !
	protected UIComponent cloneSetParent(MobileComponent newParent) throws CloneNotSupportedException {
		UIComponent cloned = clone();
		cloned.parent = newParent;
		return cloned;
	}
	
	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		String version = element.getAttribute("version");
		long priority = Long.valueOf(element.getAttribute("priority")).longValue();

		//TODO: REMOVE BEFORE RELEASE !!!
		boolean doMigration = false;
		if (!doMigration) {
			return;
		}
		
		if (VersionUtils.compare(version, "7.9.0") < 0) {
			try {
				NodeList properties = element.getElementsByTagName("property");
				int len = properties.getLength();
				Element propElement;
				for (int i = 0; i < len; i++) {
					propElement = (Element) properties.item(i);
					if (propElement != null && propElement.getParentNode().equals(element)) {
						String propertyName = propElement.getAttribute("name");
						Element valueElement = (Element) XMLUtils.findChildNode(propElement, Node.ELEMENT_NODE);
						if (valueElement != null) {
							Document document = valueElement.getOwnerDocument();
							Object content = XMLUtils.readObjectFromXml(valueElement);
							
							// This is data of the peusdo-bean
							if ("beanData".equals(propertyName) && content instanceof String) {
								try {
									boolean needChange = false;
									List<String> logList = new ArrayList<String>();
									IonBean ionBean = new IonBean((String)content);
									List<IonProperty> propertyList = new ArrayList<IonProperty>();
									propertyList.addAll(ionBean.getProperties().values());
									// Walk through properties
									for (IonProperty ionProperty: propertyList) {
										String ionPropertyName = ionProperty.getName();
										String modeUpperCase = ionProperty.getMode().toUpperCase();
										if (Mode.SOURCE.equals(Mode.valueOf(modeUpperCase))) {
											MobileSmartSourceType msst = ionProperty.getSmartType();
											String smartValue = msst.getSmartValue();
											if (smartValue != null && !smartValue.isEmpty()) {
												try {
													MobileSmartSource mss = MobileSmartSource.migrate(smartValue);
													if (mss != null) {
														boolean migrated = !smartValue.equals(mss.toJsonString());
														if (migrated) {
															msst.setSmartValue(mss.toJsonString());
															ionBean.setPropertyValue(ionPropertyName, msst);
															needChange = true;
															logList.add("Done migration of \""+ ionPropertyName + "\" property for the object \"" 
																	+ getName() + "\" (priority: "+priority+")");
														}
													}
												}
												catch (Exception e) {
													if (e instanceof InvalidSourceException) {
														Engine.logBeans.warn("Failed to migrate \""+ ionPropertyName + "\" property for the object \"" 
																					+ getName() + "\" (priority: "+priority+"): " + e.getMessage());
													} else {
														Engine.logBeans.error("Failed to migrate \""+ ionPropertyName + "\" property for the object \"" 
																+ getName() + "\" (priority: "+priority+")", e);
													}
												}
											}
										}
									}
									// Store new beandata property value
									if (needChange) {
										String beanData = ionBean.toBeanData();
										Element newValueElement = (Element)XMLUtils.writeObjectToXml(document, beanData);
										propElement.replaceChild(newValueElement, valueElement);
										hasChanged = true;
										logList.forEach(s -> Engine.logBeans.warn(s));
									}
									
								}
								catch (Exception e) {
									Engine.logBeans.error("Failed to migrate \""+ propertyName + "\" property for the object \"" 
																	+ getName() + "\" (priority: "+priority+")", e);
								}
							}
							// This is a MobileSmartSourceType property
							else if (content instanceof MobileSmartSourceType) {
								MobileSmartSourceType msst = (MobileSmartSourceType) content;
								// Property is in 'SRC' mode
								if (Mode.SOURCE.equals(msst.getMode())) {
									try {
										String smartValue = msst.getSmartValue();
										if (smartValue != null && !smartValue.isEmpty()) {
											MobileSmartSource mss = MobileSmartSource.migrate(smartValue);
											if (mss != null) {
												boolean migrated = !smartValue.equals(mss.toJsonString());
												if (migrated) {
													msst.setSmartValue(mss.toJsonString());
													
													// Store new property value
													Element newValueElement = (Element)XMLUtils.writeObjectToXml(document, msst);
													propElement.replaceChild(newValueElement, valueElement);
													hasChanged = true;
													Engine.logBeans.warn("Done migration of \""+ propertyName + "\" property for the object \"" 
																				+ getName() + "\" (priority: "+priority+")");
												}
											}
										}
									}
									catch (Exception e) {
										if (e instanceof InvalidSourceException) {
											Engine.logBeans.warn("Failed to migrate \""+ propertyName + "\" property for the object \"" 
																		+ getName() + "\" (priority: "+priority+"): " + e.getMessage());
										} else {
											Engine.logBeans.error("Failed to migrate \""+ propertyName + "\" property for the object \"" 
																		+ getName() + "\" (priority: "+priority+")", e);
										}
									}
								}
							}
						}
					}
				}
			}
	        catch(Exception e) {
	            throw new EngineException("Unable to preconfigure the mobile uicomponent \"" + getName() + "\".", e);
	        }
		}
	}

	public XMLVector<XMLVector<Long>> getOrderedComponents() {
		return orderedComponents;
	}
    
	public void setOrderedComponents(XMLVector<XMLVector<Long>> orderedComponents) {
		this.orderedComponents = orderedComponents;
	}
	
    private void insertOrderedComponent(UIComponent component, Long after) {
    	List<Long> ordered = orderedComponents.get(0);
    	int size = ordered.size();
    	
    	if (ordered.contains(component.priority))
    		return;
    	
    	if (after == null) {
    		after = 0L;
    		if (size > 0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, component.priority);
    	hasChanged = !isImporting;
    }
    
    private void removeOrderedComponent(Long value) {
        Collection<Long> ordered = orderedComponents.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, priority);
	}
    
    protected void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
    	Long value = databaseObject.priority;
    	
    	if (databaseObject instanceof UIComponent)
    		ordered = orderedComponents.get(0);
    	
    	if (ordered == null || !ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos == 0)
    		return;
    	
    	if (before == null)
    		before = ordered.get(pos-1);
    	int pos1 = ordered.indexOf(before);
    	
    	ordered.add(pos1, value);
    	ordered.remove(pos+1);
    	hasChanged = true;
    	
    	markAsDirty();
    }
    
    protected void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
    	if (databaseObject instanceof UIComponent)
    		ordered = orderedComponents.get(0);
    	
    	if (ordered == null || !ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos+1 == ordered.size())
    		return;
    	
    	if (after == null)
    		after = ordered.get(pos+1);
    	int pos1 = ordered.indexOf(after);
    	
    	ordered.add(pos1+1, value);
    	ordered.remove(pos);
    	hasChanged = true;
    	
    	markAsDirty();
    }
    
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UIComponent)
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UIComponent)
			decreaseOrder(databaseObject,null);
	}
    
    /**
     * Get order for quick sort.
     */
    @Override
    public Object getOrderedValue() {
    	return priority;
    }
	
    /**
     * Get representation of order for quick sort of a given database object.
     */
	@Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof UIComponent) {
        	List<Long> ordered = orderedComponents.get(0);
        	long time = ((UIComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted component for page \""+ getName() +"\". UIComponent \""+ ((UIComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else return super.getOrder(object);
    }
    
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	public boolean isFormControlAttribute() {
		return false;
		
	}
	
	/**
	 * The list of available page component for this application.
	 */
	transient private List<UIComponent> vUIComponents = new LinkedList<UIComponent>();
	
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
		boolean isNew = uiComponent.bNew;
		boolean isCut = !isNew && uiComponent.getParent() == null && uiComponent.isSubLoaded;
		
		String newDatabaseObjectName = getChildBeanName(vUIComponents, uiComponent.getName(), uiComponent.bNew);
		uiComponent.setName(newDatabaseObjectName);
		
		vUIComponents.add(uiComponent);
		uiComponent.setParent(this);
		
        insertOrderedComponent(uiComponent, after);
        
        if (isNew || isCut) {
        	markAsDirty();
        }
	}
	
	protected void addUIComponent(UIComponent uiComponent) throws EngineException {
		addUIComponent(uiComponent, null);
	}

	protected void removeUIComponent(UIComponent uiComponent) throws EngineException {
		checkSubLoaded();
		
		vUIComponents.remove(uiComponent);
		uiComponent.setParent(null);
		
        removeOrderedComponent(uiComponent.priority);
        
    	markAsDirty();
	}

	public void addPageEvent(Set<UIComponent> done, List<UIPageEvent> eventList) {
		if (!done.add(this)) {
			return;
		}
		if (isEnabled()) {
			for (UIComponent uiComponent : getUIComponentList()) {
				if (uiComponent.isEnabled()) {
					if (uiComponent instanceof UIPageEvent) {
						eventList.add((UIPageEvent) uiComponent);
					} else {
						uiComponent.addPageEvent(done, eventList);
					}
				}
			}
		}
	}
	
	public void addEventSubscriber(Set<UIComponent> done, List<UIEventSubscriber> eventList) {
		if (!done.add(this)) {
			return;
		}
		if (isEnabled()) {
			for (UIComponent uiComponent : getUIComponentList()) {
				if (uiComponent.isEnabled()) {
					if (uiComponent instanceof UIEventSubscriber) {
						eventList.add((UIEventSubscriber)uiComponent);
					} else {
						uiComponent.addEventSubscriber(done, eventList);
					}
				}
			}
		}
	}
	
	public List<UIComponent> getUIComponentList() {
		checkSubLoaded();
		return sort(vUIComponents);
	}

	public boolean hasStyle() {
		for (UIComponent uic :getUIComponentList()) {
			if (uic instanceof UIStyle) {
				return true;
			}
		}
		return false;
	}
	
	public UIComponent getUIComponentByName(String uiName) throws EngineException {
		checkSubLoaded();
		for (UIComponent uiComponent : vUIComponents)
			if (uiComponent.getName().equalsIgnoreCase(uiName)) return uiComponent;
		throw new EngineException("There is no UI component named \"" + uiName + "\" found into this page.");
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getUIComponentList());
		return rep;
	}

	@Override
	public void add(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject instanceof UIComponent) {
			addUIComponent((UIComponent) databaseObject, after);
		} else {
			throw new EngineException("You cannot add to a page component a database object of type " + databaseObject.getClass().getName());
		}		
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UIComponent) {
			removeUIComponent((UIComponent) databaseObject);
		} else {
			throw new EngineException("You cannot remove from a page component a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }

	public IScriptComponent getMainScriptComponent() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof IScriptComponent) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null)
			return null;
		else
			return (IScriptComponent) databaseObject;
	}
	
	public UIDynamicMenu getMenu() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof UIDynamicMenu) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null)
			return null;
		else
			return (UIDynamicMenu) databaseObject;
	}
	
	public UIActionStack getSharedAction() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof UIActionStack) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null)
			return null;
		else
			return (UIActionStack) databaseObject;
	}
	
	public UISharedComponent getSharedComponent() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof UISharedComponent) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null)
			return null;
		else
			return (UISharedComponent) databaseObject;
	}
	
	public PageComponent getPage() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof PageComponent) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null)
			return null;
		else
			return (PageComponent) databaseObject;
	}
	
	public UIForm getUIForm() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof PageComponent) && 
				!(databaseObject instanceof UIForm) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null || databaseObject instanceof PageComponent)
			return null;
		else
			return (UIForm) databaseObject;
	}
	
	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isEnabled")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		return super.testAttribute(name, value);
	}

	public boolean updateSmartSources(String oldString, String newString) {
		boolean updated = updateSmartSource(oldString, newString);
		for (UIComponent uic : getUIComponentList()) {
			if (uic.updateSmartSources(oldString, newString)) {
				updated = true;
			}
		}
		return updated;
	}
	
	public boolean updateSmartSource(String oldString, String newString) {
		return false;
	}
	
	protected String computeInnerGet(String pageKey, String functionName) {
		String computed = "";
		computed += "\t\tlet get = function(keyName, keyVal) {"+ System.lineSeparator();
		computed += "\t\t\tlet val=undefined;"+ System.lineSeparator();
		computed += "\t\t\ttry {"+ System.lineSeparator();
		//computed += "\t\t\t\tval= keyVal === '' ? keyVal : eval(ts.transpile('('+ keyVal + ')'));"+ System.lineSeparator();
		computed += "\t\t\t\tval= keyVal === '' ? keyVal : eval('('+ keyVal + ')');"+ System.lineSeparator();
		
		computed += "\t\t\t\tif (val == undefined) {"+ System.lineSeparator();
		computed += "\t\t\t\t\t"+pageKey+".c8o.log.trace(\"[MB] "+functionName+": key=\"+ keyName +\" value=undefined\");"+ System.lineSeparator();
		computed += "\t\t\t\t} else {"+ System.lineSeparator();
		computed += "\t\t\t\t\t"+pageKey+".c8o.log.trace(\"[MB] "+functionName+": key=\"+ keyName +\" value=\"+ val);"+ System.lineSeparator();
		computed += "\t\t\t\t}"+ System.lineSeparator();
		
		computed += "\t\t\t} catch(e) {"+ System.lineSeparator();
		computed += "\t\t\t\tlet sKeyVal = keyVal == null ? \"null\" : (keyVal == undefined ? \"undefined\" : keyVal);"+ System.lineSeparator();
		computed += "\t\t\t\t"+pageKey+".c8o.log.warn(\"[MB] "+functionName+": For \"+ keyName +\":\"+ sKeyVal + \", \"+ e.message);"+ System.lineSeparator();
		computed += "\t\t\t}"+ System.lineSeparator();
		computed += "\t\t\treturn val;"+ System.lineSeparator();
		computed += "\t\t}" + System.lineSeparator();
		return computed;
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {		
		if (isEnabled()) {
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				component.computeScripts(jsonScripts);
			}
		}
	}
	
	public String computeJsonModel() {
		return "";
	}
	
	public void markAsDirty() throws EngineException {
    	PageComponent page = getPage();
    	if (page != null) {
    		page.markPageAsDirty();
    	} else {
	    	UIDynamicMenu menu = getMenu();
	    	if (menu != null) {
	    		menu.markMenuAsDirty();
	    	} else {
	    		ApplicationComponent app = getApplication();
	    		if (app != null) {
	    			app.markApplicationAsDirty();
	    		}
	    	}
    	}
	}

	protected Contributor getContributor() {
		return null;
	}
	
	protected void addContributors(Set<UIComponent> done, List<Contributor> contributors) {
		//if (isEnabled()) { // Commented until we can delete page folder again... : see forceEnable in MobileBuilder
			if (!done.add(this)) {
				return;
			}
			Contributor contributor = getContributor();
			if (contributor != null) {
				if (!contributors.contains(contributor)) {
					contributors.add(contributor);
				}
			}
			for (UIComponent uiComponent : getUIComponentList()) {
				uiComponent.addContributors(done, contributors);
			}
		//}
	}
	
	protected void addInfos(Set<UIComponent> done, Map<String, Set<String>> infoMap) {
		if (!done.add(this)) {
			return;
		}
		for (UIComponent uiComponent : getUIComponentList()) {
			uiComponent.addInfos(done, infoMap);
		}
	}
	
	@Override
	public String requiredTplVersion() {
		String tplVersion = getRequiredTplVersion();
		for (UIComponent uic : getUIComponentList()) {
			String uicTplVersion = uic.requiredTplVersion();
			if (MobileBuilder.compareVersions(tplVersion, uicTplVersion) <= 0) {
				tplVersion = uicTplVersion;
			}
		}
		return tplVersion;
	}
}
